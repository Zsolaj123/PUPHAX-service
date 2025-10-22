package com.puphax.service;

import com.puphax.model.dto.*;
import com.puphax.exception.PuphaxServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for drug search operations.
 * 
 * This service handles the business logic for drug searches,
 * including SOAP response parsing, caching, and result formatting.
 */
@Service
public class DrugService {
    
    private static final Logger logger = LoggerFactory.getLogger(DrugService.class);
    
    private final PuphaxSoapClient soapClient;
    private final PuphaxRealDataService realDataService;
    
    @Autowired
    public DrugService(PuphaxSoapClient soapClient, PuphaxRealDataService realDataService) {
        this.soapClient = soapClient;
        this.realDataService = realDataService;
    }
    
    /**
     * Searches for drugs based on the provided criteria.
     * 
     * @param searchTerm Drug name or partial name to search for
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter
     * @param page Page number (0-based)
     * @param size Page size (1-100)
     * @param sortBy Sort field (name, manufacturer, atcCode)
     * @param sortDirection Sort direction (ASC, DESC)
     * @return DrugSearchResponse with paginated results
     */
    // @Cacheable(value = "drugSearchCache", key = "#searchTerm + '_' + #manufacturer + '_' + #atcCode + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortDirection")
    public DrugSearchResponse searchDrugs(String searchTerm, String manufacturer, String atcCode,
                                         int page, int size, String sortBy, String sortDirection) {
        
        logger.debug("Searching for drugs: term='{}', manufacturer='{}', atcCode='{}', page={}, size={}", 
                    searchTerm, manufacturer, atcCode, page, size);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // First try the real data service with proper encoding handling
            String xmlResponse;
            try {
                logger.info("Attempting to fetch real PUPHAX data for search term: {}", searchTerm);
                xmlResponse = realDataService.searchDrugsReal(searchTerm);
                logger.info("Successfully retrieved real PUPHAX data");
            } catch (Exception e) {
                logger.warn("Real data service failed, falling back to SOAP client: {}", e.getMessage());
                // Fall back to SOAP client if real data service fails
                CompletableFuture<String> soapResponseFuture = soapClient.searchDrugsAsync(searchTerm, manufacturer, atcCode);
                xmlResponse = soapResponseFuture.get();
            }
            
            logger.debug("Received response: {} characters", xmlResponse.length());
            
            // Parse XML response
            List<DrugSummary> allDrugs = parseSearchResponse(xmlResponse);
            
            // Apply sorting
            List<DrugSummary> sortedDrugs = applySorting(allDrugs, sortBy, sortDirection);
            
            // Apply pagination
            List<DrugSummary> paginatedDrugs = applyPagination(sortedDrugs, page, size);
            
            // Create pagination info - use total count before pagination
            PaginationInfo pagination = PaginationInfo.of(page, size, sortedDrugs.size());
            
            // Create search info
            long responseTime = System.currentTimeMillis() - startTime;
            SearchInfo searchInfo = SearchInfo.withFilters(searchTerm, manufacturer, atcCode, responseTime, false);
            
            logger.debug("Search completed: {} results found in {}ms", sortedDrugs.size(), responseTime);
            
            return new DrugSearchResponse(paginatedDrugs, pagination, searchInfo);
            
        } catch (Exception e) {
            logger.error("Error during drug search: {}", e.getMessage(), e);
            
            if (e instanceof PuphaxServiceException) {
                throw (PuphaxServiceException) e;
            }
            
            throw new PuphaxServiceException("Unexpected error in operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses the XML response from PUPHAX SOAP service into DrugSummary objects.
     * 
     * @param xmlResponse XML response string from SOAP service
     * @return List of DrugSummary objects
     * @throws PuphaxServiceException if XML parsing fails
     */
    private List<DrugSummary> parseSearchResponse(String xmlResponse) throws PuphaxServiceException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8));
            Document document = builder.parse(inputStream);
            
            List<DrugSummary> drugs = new ArrayList<>();
            
            NodeList drugNodes = document.getElementsByTagName("drug");
            
            for (int i = 0; i < drugNodes.getLength(); i++) {
                Element drugElement = (Element) drugNodes.item(i);
                DrugSummary drug = parseDrugElement(drugElement);
                drugs.add(drug);
            }
            
            logger.debug("Parsed {} drugs from XML response", drugs.size());
            return drugs;
            
        } catch (Exception e) {
            logger.error("Failed to parse XML response: {}", e.getMessage(), e);
            throw new PuphaxServiceException("Failed to parse SOAP response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a single drug element from XML into a DrugSummary object.
     * 
     * @param drugElement XML element representing a drug
     * @return DrugSummary object
     */
    private DrugSummary parseDrugElement(Element drugElement) {
        String id = getElementText(drugElement, "id");
        String name = getElementText(drugElement, "name");
        String manufacturer = getElementText(drugElement, "manufacturer");
        String atcCode = getElementText(drugElement, "atcCode");
        
        // Parse active ingredients
        List<String> activeIngredients = parseActiveIngredients(drugElement);
        
        // Parse boolean fields
        boolean prescriptionRequired = Boolean.parseBoolean(getElementText(drugElement, "prescriptionRequired"));
        boolean reimbursable = Boolean.parseBoolean(getElementText(drugElement, "reimbursable"));
        
        // Parse status
        DrugSummary.DrugStatus status = parseStatus(getElementText(drugElement, "status"));
        
        // Check if we have extended data available
        String tttCode = getElementText(drugElement, "tttCode");
        if (tttCode != null) {
            // We have extended data - create ExtendedDrugSummary
            String packaging = getElementText(drugElement, "packaging");
            String registrationNumber = getElementText(drugElement, "registrationNumber");
            String prescriptionStatus = getElementText(drugElement, "prescriptionStatus");
            String productForm = getElementText(drugElement, "productForm");
            String strength = getElementText(drugElement, "strength");
            String packSize = getElementText(drugElement, "packSize");
            String productType = getElementText(drugElement, "productType");
            String price = getElementText(drugElement, "price");
            String supportPercent = getElementText(drugElement, "supportPercent");
            String validFrom = getElementText(drugElement, "validFrom");
            String validTo = getElementText(drugElement, "validTo");
            String bruttoFogyasztarAr = getElementText(drugElement, "bruttoFogyasztarAr");
            String nettoFogyasztarAr = getElementText(drugElement, "nettoFogyasztarAr");
            String termelesAr = getElementText(drugElement, "termelesAr");
            String nagykerAr = getElementText(drugElement, "nagykerAr");
            String tamogatottAr = getElementText(drugElement, "tamogatottAr");
            String teritesiDij = getElementText(drugElement, "teritesiDij");
            String normativity = getElementText(drugElement, "normativity");
            String supportType = getElementText(drugElement, "supportType");
            String source = getElementText(drugElement, "source");
            
            ExtendedDrugSummary extendedSummary = new ExtendedDrugSummary(
                id != null ? id : "UNKNOWN",
                name != null ? name : "Unknown Drug",
                manufacturer,
                atcCode,
                tttCode,
                activeIngredients,
                prescriptionRequired,
                prescriptionStatus,
                reimbursable,
                status,
                packaging,
                registrationNumber,
                productForm,
                strength,
                packSize,
                productType,
                price,
                bruttoFogyasztarAr,
                nettoFogyasztarAr,
                termelesAr,
                nagykerAr,
                supportPercent,
                tamogatottAr,
                teritesiDij,
                validFrom,
                validTo,
                normativity,
                supportType,
                source
            );
            
            // Convert to basic DrugSummary for compatibility
            return extendedSummary.toBasicSummary();
        }
        
        // Basic DrugSummary (backward compatibility)
        return new DrugSummary(
            id != null ? id : "UNKNOWN",
            name != null ? name : "Unknown Drug",
            manufacturer,
            atcCode,
            activeIngredients,
            prescriptionRequired,
            reimbursable,
            status
        );
    }
    
    /**
     * Extracts text content from a child element.
     * 
     * @param parent Parent element
     * @param tagName Child element tag name
     * @return Text content or null if not found
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null && !text.trim().isEmpty() ? text.trim() : null;
        }
        return null;
    }
    
    /**
     * Parses active ingredients from the drug element.
     * 
     * @param drugElement Drug XML element
     * @return List of active ingredient names
     */
    private List<String> parseActiveIngredients(Element drugElement) {
        List<String> ingredients = new ArrayList<>();
        
        NodeList activeIngredientsNodes = drugElement.getElementsByTagName("activeIngredients");
        if (activeIngredientsNodes.getLength() > 0) {
            Element activeIngredientsElement = (Element) activeIngredientsNodes.item(0);
            NodeList ingredientNodes = activeIngredientsElement.getElementsByTagName("ingredient");
            
            for (int i = 0; i < ingredientNodes.getLength(); i++) {
                Element ingredientElement = (Element) ingredientNodes.item(i);
                String ingredientName = getElementText(ingredientElement, "name");
                if (ingredientName != null) {
                    ingredients.add(ingredientName);
                }
            }
        }
        
        return ingredients;
    }
    
    /**
     * Parses drug status from string to enum.
     * 
     * @param statusText Status text from XML
     * @return DrugStatus enum value
     */
    private DrugSummary.DrugStatus parseStatus(String statusText) {
        if (statusText == null) {
            return DrugSummary.DrugStatus.ACTIVE;
        }
        
        try {
            return DrugSummary.DrugStatus.valueOf(statusText.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown drug status '{}', defaulting to ACTIVE", statusText);
            return DrugSummary.DrugStatus.ACTIVE;
        }
    }
    
    /**
     * Applies sorting to the drug list.
     * 
     * @param drugs List of drugs to sort
     * @param sortBy Sort field
     * @param sortDirection Sort direction
     * @return Sorted list of drugs
     */
    private List<DrugSummary> applySorting(List<DrugSummary> drugs, String sortBy, String sortDirection) {
        if (drugs.isEmpty() || sortBy == null) {
            return drugs;
        }
        
        boolean ascending = "ASC".equalsIgnoreCase(sortDirection);
        
        List<DrugSummary> sortedDrugs = new ArrayList<>(drugs);
        
        switch (sortBy.toLowerCase()) {
            case "name":
                sortedDrugs.sort((a, b) -> {
                    int result = a.name().compareToIgnoreCase(b.name());
                    return ascending ? result : -result;
                });
                break;
                
            case "manufacturer":
                sortedDrugs.sort((a, b) -> {
                    String mfgA = a.manufacturer() != null ? a.manufacturer() : "";
                    String mfgB = b.manufacturer() != null ? b.manufacturer() : "";
                    int result = mfgA.compareToIgnoreCase(mfgB);
                    return ascending ? result : -result;
                });
                break;
                
            case "atccode":
                sortedDrugs.sort((a, b) -> {
                    String atcA = a.atcCode() != null ? a.atcCode() : "";
                    String atcB = b.atcCode() != null ? b.atcCode() : "";
                    int result = atcA.compareToIgnoreCase(atcB);
                    return ascending ? result : -result;
                });
                break;
                
            default:
                logger.warn("Unknown sort field '{}', keeping original order", sortBy);
        }
        
        return sortedDrugs;
    }
    
    /**
     * Applies pagination to the drug list.
     * 
     * @param drugs Full list of drugs
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated sublist
     */
    private List<DrugSummary> applyPagination(List<DrugSummary> drugs, int page, int size) {
        if (drugs.isEmpty()) {
            return drugs;
        }
        
        int startIndex = page * size;
        if (startIndex >= drugs.size()) {
            return new ArrayList<>();
        }
        
        int endIndex = Math.min(startIndex + size, drugs.size());
        return drugs.subList(startIndex, endIndex);
    }
}
