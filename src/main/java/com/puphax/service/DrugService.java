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
    private final PuphaxCsvFallbackService csvFallbackService;

    @Autowired
    public DrugService(PuphaxSoapClient soapClient, PuphaxRealDataService realDataService,
                      PuphaxCsvFallbackService csvFallbackService) {
        this.soapClient = soapClient;
        this.realDataService = realDataService;
        this.csvFallbackService = csvFallbackService;
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
     * Advanced drug search using comprehensive DrugSearchFilter.
     *
     * This method uses the CSV fallback service's advanced filtering capabilities
     * to search across 43,930 products with support for 20+ filter criteria.
     *
     * @param filter Comprehensive filter criteria
     * @return DrugSearchResponse with paginated results and enhanced DrugSummary (55 fields)
     */
    public DrugSearchResponse searchDrugsAdvanced(DrugSearchFilter filter) {
        logger.debug("Advanced drug search with {} active filters", filter.getActiveFilterCount());

        long startTime = System.currentTimeMillis();

        try {
            // Use CSV fallback service for advanced filtering
            List<PuphaxCsvFallbackService.ProductRecord> products =
                csvFallbackService.searchWithAdvancedFilters(filter);

            // Convert ProductRecord to enhanced DrugSummary (with all 55 fields)
            List<DrugSummary> allDrugs = products.stream()
                .map(p -> convertProductRecordToDrugSummary(p))
                .collect(java.util.stream.Collectors.toList());

            // Apply pagination (sorting already done in CSV service)
            int page = filter.page() != null ? filter.page() : 0;
            int size = filter.size() != null ? filter.size() : 20;
            List<DrugSummary> paginatedDrugs = applyPagination(allDrugs, page, size);

            // Create pagination info
            PaginationInfo pagination = PaginationInfo.of(page, size, allDrugs.size());

            // Create search info with filter details
            long responseTime = System.currentTimeMillis() - startTime;
            Map<String, String> filterMap = buildFilterMap(filter);
            SearchInfo searchInfo = new SearchInfo(
                filter.searchTerm() != null ? filter.searchTerm() : "",
                filterMap,
                responseTime,
                false,  // Not using fallback since we're directly using CSV
                Instant.now()
            );

            logger.info("Advanced search completed: {} results (from {} total) in {}ms with {} filters",
                       paginatedDrugs.size(), allDrugs.size(), responseTime, filter.getActiveFilterCount());

            return new DrugSearchResponse(paginatedDrugs, pagination, searchInfo);

        } catch (Exception e) {
            logger.error("Error during advanced drug search: {}", e.getMessage(), e);
            throw new PuphaxServiceException("Advanced search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert ProductRecord to enhanced DrugSummary with all 55 fields.
     */
    private DrugSummary convertProductRecordToDrugSummary(PuphaxCsvFallbackService.ProductRecord p) {
        // Look up manufacturer name from forgEngtId (marketing authorization holder, not distributor)
        String manufacturerName = csvFallbackService.getCompanyName(p.forgEngtId);

        return DrugSummary.builder(p.id, p.name)
            // Core identification
            .parentId(p.parentId)
            .shortName(p.shortName)
            .brandId(p.brandId)
            // Validity and registration
            .validFrom(p.validFrom != null ? p.validFrom.toString() : null)
            .validTo(p.validTo != null ? p.validTo.toString() : null)
            .termekKod(p.termekKod)
            .kozHid(p.kozHid)
            .tttCode(p.ttt)
            .tk(p.tk)
            .tkTorles(p.tkTorles)
            .tkTorlesDate(p.tkTorlesDate != null ? p.tkTorlesDate.toString() : null)
            .eanKod(p.eanKod)
            .registrationNumber(p.kozHid)
            // Classification
            .atcCode(p.atc)
            .iso(p.iso)
            .activeIngredient(p.activeIngredient)
            .activeIngredients(p.activeIngredient != null ? List.of(p.activeIngredient) : List.of())
            // Administration and form
            .adagMod(p.adagMod)
            .productForm(p.gyForma)
            .prescriptionStatus(p.rendelhet)
            .egyenId(p.egyenId)
            .helyettesith(p.helyettesith)
            .patika(p.patika)
            // Strength and dosage
            .potencia(p.potencia)
            .oHatoMenny(p.oHatoMenny)
            .hatoMenny(p.hatoMenny)
            .hatoEgys(p.hatoEgys)
            .kiszMenny(p.kiszMenny)
            .kiszEgys(p.kiszEgys)
            .packSize(p.kiszMenny)
            // DDD fields
            .dddMenny(p.dddMenny)
            .dddEgys(p.dddEgys)
            .dddFaktor(p.dddFaktor)
            .dot(p.dot)
            .adagMenny(p.adagMenny)
            .adagEgys(p.adagEgys)
            // Special attributes
            .egyedi(p.egyedi)
            .oldalIsag(p.oldalIsag)
            .tobblGar(p.tobblGar)
            .dobAzon(p.dobAzon)
            // Distribution and availability
            .manufacturer(manufacturerName)
            .inStock(p.inStock)
            // Status and source
            .status(DrugSummary.DrugStatus.ACTIVE)
            .source("CSV")
            // Derived fields
            .prescriptionRequired(isPrescriptionRequired(p.rendelhet))
            .reimbursable(p.tk != null && !p.tk.trim().isEmpty())
            .build();
    }

    /**
     * Build filter map for SearchInfo from DrugSearchFilter.
     */
    private Map<String, String> buildFilterMap(DrugSearchFilter filter) {
        Map<String, String> filters = new HashMap<>();
        if (filter.atcCodes() != null && !filter.atcCodes().isEmpty())
            filters.put("atcCodes", String.join(", ", filter.atcCodes()));
        if (filter.manufacturers() != null && !filter.manufacturers().isEmpty())
            filters.put("manufacturers", String.join(", ", filter.manufacturers()));
        if (filter.productForms() != null && !filter.productForms().isEmpty())
            filters.put("productForms", String.join(", ", filter.productForms()));
        if (filter.prescriptionRequired() != null)
            filters.put("prescriptionRequired", filter.prescriptionRequired().toString());
        if (filter.reimbursable() != null)
            filters.put("reimbursable", filter.reimbursable().toString());
        if (filter.inStock() != null)
            filters.put("inStock", filter.inStock().toString());
        // Add more as needed
        return filters;
    }

    /**
     * Check if prescription is required based on prescription status code.
     */
    private boolean isPrescriptionRequired(String rendelhet) {
        if (rendelhet == null) return false;
        return rendelhet.equals("VN") || rendelhet.equals("V5") ||
               rendelhet.equals("V1") || rendelhet.equals("J");
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
                if (drug != null) {
                    drugs.add(drug);
                } else {
                    logger.warn("Skipping null drug element at index {}", i);
                }
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
        try {
            // Essential fields with validation
            String id = getElementText(drugElement, "id");
            if (id == null || id.trim().isEmpty()) {
                logger.warn("Drug element missing required 'id' field, skipping");
                return null;
            }
            
            String name = getElementText(drugElement, "name");
            if (name == null || name.trim().isEmpty()) {
                logger.warn("Drug element with id '{}' missing required 'name' field, using default", id);
                name = "Unknown Drug";
            }
            
            // Optional fields with safe defaults
            String manufacturer = getElementText(drugElement, "manufacturer");
            if (manufacturer == null || manufacturer.trim().isEmpty()) {
                manufacturer = "Unknown Manufacturer";
            }
            
            String atcCode = getElementText(drugElement, "atcCode");
            
            // Parse active ingredients with error handling
            List<String> activeIngredients;
            try {
                activeIngredients = parseActiveIngredients(drugElement);
            } catch (Exception e) {
                logger.warn("Failed to parse active ingredients for drug '{}': {}", id, e.getMessage());
                activeIngredients = List.of(); // Empty list as fallback
            }
            
            // Parse boolean fields with safe defaults
            boolean prescriptionRequired = safeParseBool(getElementText(drugElement, "prescriptionRequired"), false);
            boolean reimbursable = safeParseBool(getElementText(drugElement, "reimbursable"), false);
            
            // Parse status with enhanced error handling
            DrugSummary.DrugStatus status = parseStatus(getElementText(drugElement, "status"));

            // Extract all extended fields (may be null)
            String tttCode = getElementText(drugElement, "tttCode");
            String productForm = getElementText(drugElement, "productForm");
            String strength = getElementText(drugElement, "strength");
            String packSize = getElementText(drugElement, "packSize");
            String prescriptionStatus = getElementText(drugElement, "prescriptionStatus");
            String validFrom = getElementText(drugElement, "validFrom");
            String validTo = getElementText(drugElement, "validTo");
            String registrationNumber = getElementText(drugElement, "registrationNumber");
            String price = getElementText(drugElement, "price");
            String supportPercent = getElementText(drugElement, "supportPercent");
            String source = getElementText(drugElement, "source");

            // Extract composition and dosage fields (new in Phase 2)
            String hatoMenny = getElementText(drugElement, "hatoMenny");
            String hatoEgys = getElementText(drugElement, "hatoEgys");
            String kiszMenny = getElementText(drugElement, "kiszMenny");
            String kiszEgys = getElementText(drugElement, "kiszEgys");
            String adagMenny = getElementText(drugElement, "adagMenny");
            String adagEgys = getElementText(drugElement, "adagEgys");
            String adagMod = getElementText(drugElement, "administrationMethod");

            // Return DrugSummary using builder pattern (SOAP service has limited fields)
            return DrugSummary.builder(
                    id != null ? id : "UNKNOWN",
                    name != null ? name : "Unknown Drug"
                )
                .manufacturer(manufacturer)
                .atcCode(atcCode)
                .activeIngredients(activeIngredients)
                .activeIngredient(activeIngredients.isEmpty() ? null : String.join(", ", activeIngredients))
                .prescriptionRequired(prescriptionRequired)
                .reimbursable(reimbursable)
                .status(status)
                .tttCode(tttCode)
                .productForm(productForm)
                .potencia(strength)
                .packSize(packSize)
                .prescriptionStatus(prescriptionStatus)
                .validFrom(validFrom)
                .validTo(validTo)
                .registrationNumber(registrationNumber)
                .price(price)
                .supportPercent(supportPercent)
                .source(source)
                // Add composition and dosage fields (Phase 2)
                .hatoMenny(hatoMenny)
                .hatoEgys(hatoEgys)
                .kiszMenny(kiszMenny)
                .kiszEgys(kiszEgys)
                .adagMenny(adagMenny)
                .adagEgys(adagEgys)
                .adagMod(adagMod)
                .build();
        } catch (Exception e) {
            logger.error("Failed to parse drug element: {}", e.getMessage(), e);
            // Return a minimal drug summary to prevent total failure
            return DrugSummary.basic("ERROR", "Parse Error", "Unknown");
        }
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
        if (statusText == null || statusText.trim().isEmpty()) {
            return DrugSummary.DrugStatus.ACTIVE;
        }
        
        String normalizedStatus = statusText.trim().toUpperCase();
        
        try {
            return DrugSummary.DrugStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown drug status '{}', mapping to UNKNOWN", statusText);
            return DrugSummary.DrugStatus.UNKNOWN;
        }
    }
    
    /**
     * Safely parses boolean values with fallback.
     * 
     * @param value String value to parse
     * @param defaultValue Default value if parsing fails
     * @return Parsed boolean or default value
     */
    private boolean safeParseBool(String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            logger.warn("Failed to parse boolean value '{}', using default: {}", value, defaultValue);
            return defaultValue;
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
