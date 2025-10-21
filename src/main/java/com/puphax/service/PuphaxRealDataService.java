package com.puphax.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service that connects to real PUPHAX and properly handles the mixed character encoding.
 * Based on the Spring Boot integration summary recommendations.
 */
@Service
public class PuphaxRealDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxRealDataService.class);
    
    @Autowired
    private SimplePuphaxClient simplePuphaxClient;
    
    /**
     * Search drugs in real PUPHAX with proper encoding handling.
     */
    public String searchDrugsReal(String searchTerm) {
        try {
            logger.info("Making REAL PUPHAX call via simple HTTP client for search term: {}", searchTerm);
            
            // Use simple HTTP client to avoid header conflicts
            String rawResponse = simplePuphaxClient.searchDrugsSimple(searchTerm);
            
            logger.info("Successfully retrieved REAL PUPHAX data via direct HTTP");
            
            // Parse and convert to our format
            return parseAndConvertResponse(rawResponse, searchTerm);
            
        } catch (Exception e) {
            logger.error("Real PUPHAX call failed: {}", e.getMessage(), e);
            return createFallbackResponse(searchTerm);
        }
    }
    
    /**
     * Parse PUPHAX response and convert to our format.
     */
    private String parseAndConvertResponse(String puphaxResponse, String searchTerm) {
        try {
            logger.debug("Parsing PUPHAX response of length: {}", puphaxResponse.length());
            logger.debug("Full PUPHAX response: {}", puphaxResponse);
            
            // Check if this is a SOAP response
            if (!puphaxResponse.contains("soap:") && !puphaxResponse.contains("soapenv:")) {
                logger.warn("Response doesn't appear to be a SOAP envelope");
                return createFallbackResponse(searchTerm);
            }
            
            // Extract product IDs from TERMEKLISTA response
            java.util.List<String> productIds = new java.util.ArrayList<>();
            
            // Look for the IDLIST element which contains OBJSTRING256 elements
            if (puphaxResponse.contains("IDLIST") || puphaxResponse.contains("ID_LIST")) {
                java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile(
                    "<SZOVEG>([0-9]+)</SZOVEG>",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );
                
                java.util.regex.Matcher matcher = idPattern.matcher(puphaxResponse);
                while (matcher.find() && productIds.size() < 100) {
                    productIds.add(matcher.group(1));
                }
            }
            
            if (productIds.isEmpty()) {
                logger.warn("No product IDs found in PUPHAX TERMEKLISTA response");
                return createFallbackResponse(searchTerm);
            }
            
            logger.info("Found {} product IDs in REAL PUPHAX response", productIds.size());
            
            // Build response - we now have real product IDs
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<drugSearchResponse>\n");
            xmlBuilder.append(String.format("    <totalCount>%d</totalCount>\n", productIds.size()));
            xmlBuilder.append("    <drugs>\n");
            
            // Get detailed data for each product
            int count = 0;
            LocalDate searchDate = LocalDate.now();
            
            logger.info("Fetching detailed product data for {} products", productIds.size());
            
            for (String productId : productIds) {
                // Process all products (pagination is handled by the service layer)
                
                try {
                    logger.info("Getting detailed data for product ID: {}", productId);
                    
                    // First get product basic data (name, ATC, manufacturer)
                    String termekadatResponse = simplePuphaxClient.getProductData(productId, searchDate);
                    logger.info("TERMEKADAT response length for product {}: {} chars", productId, termekadatResponse.length());
                    
                    // Then get support data (prices, reimbursement)
                    String tamogatadatResponse = null;
                    try {
                        tamogatadatResponse = simplePuphaxClient.getProductSupportData(productId, searchDate);
                        logger.info("TAMOGATADAT response length for product {}: {} chars", productId, tamogatadatResponse.length());
                    } catch (Exception e) {
                        logger.warn("TAMOGATADAT failed for product {}, will use only TERMEKADAT data: {}", productId, e.getMessage());
                    }
                    
                    // Parse both responses to create complete product info
                    String productXml = parseProductData(productId, termekadatResponse, tamogatadatResponse);
                    xmlBuilder.append(productXml);
                    
                } catch (Exception e) {
                    logger.error("Failed to get details for product {}: {}", productId, e.getMessage(), e);
                    // Add basic info if detailed call fails
                    xmlBuilder.append(String.format("""
                            <drug>
                                <id>%s</id>
                                <name>PUPHAX Product %s (details unavailable)</name>
                                <manufacturer>NEAK PUPHAX</manufacturer>
                                <atcCode>ERROR</atcCode>
                                <activeIngredients>
                                    <ingredient>
                                        <name>Error: %s</name>
                                    </ingredient>
                                </activeIngredients>
                                <prescriptionRequired>true</prescriptionRequired>
                                <reimbursable>false</reimbursable>
                                <status>ERROR</status>
                                <source>REAL PUPHAX</source>
                            </drug>
                        """, productId, productId, e.getMessage()));
                }
            }
            
            xmlBuilder.append("    </drugs>\n");
            xmlBuilder.append("    <source>PUPHAX WebService via Spring WS</source>\n");
            xmlBuilder.append("    <dataType>REAL</dataType>\n");
            xmlBuilder.append("</drugSearchResponse>");
            
            return xmlBuilder.toString();
            
        } catch (Exception e) {
            logger.error("Failed to parse PUPHAX response: {}", e.getMessage());
            return createFallbackResponse(searchTerm);
        }
    }
    
    /**
     * Create fallback response.
     */
    private String createFallbackResponse(String searchTerm) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>1</totalCount>
                <drugs>
                    <drug>
                        <id>FALLBACK-001</id>
                        <name>%s (Kapcsolódási Hiba)</name>
                        <manufacturer>PUPHAX</manufacturer>
                        <atcCode>ERROR</atcCode>
                        <activeIngredients>
                            <ingredient>
                                <name>Nem Elérhető</name>
                            </ingredient>
                        </activeIngredients>
                        <prescriptionRequired>false</prescriptionRequired>
                        <reimbursable>false</reimbursable>
                        <status>ERROR</status>
                    </drug>
                </drugs>
                <error>Failed to retrieve real PUPHAX data</error>
            </drugSearchResponse>
            """, searchTerm != null ? searchTerm : "Unknown");
    }
    
    /**
     * Parse product data from TERMEKADAT and optionally TAMOGATADAT responses.
     */
    private String parseProductData(String productId, String termekadatResponse, String tamogatadatResponse) {
        try {
            logger.debug("Parsing product data for product {}", productId);
            
            // Log the first part of response to debug parsing
            logger.debug("TERMEKADAT response preview for {}: {}", productId,
                termekadatResponse.length() > 500 ? termekadatResponse.substring(0, 500) + "..." : termekadatResponse);
            
            // Log full response for debugging field names
            if (productId.equals("55845963")) {  // First aspirin product
                logger.info("FULL TERMEKADAT RESPONSE for debugging:\n{}", termekadatResponse);
            }
            
            // Check if we have a SOAP fault or error response
            if (termekadatResponse.contains("faultstring") || termekadatResponse.contains("soap:Fault")) {
                logger.error("SOAP Fault in TERMEKADAT response for product {}: {}", productId, termekadatResponse);
                throw new Exception("SOAP fault in response");
            }
            
            // Parse TERMEKADAT response for all available product info
            String productName = extractValue(termekadatResponse, "<NEV>", "</NEV>");
            String atcCode = extractValue(termekadatResponse, "<ATC>", "</ATC>");
            
            // Extract manufacturer - check if name is directly available
            String manufacturer = extractValue(termekadatResponse, "<FORGALMAZO>", "</FORGALMAZO>");
            if (manufacturer.isEmpty()) {
                manufacturer = extractValue(termekadatResponse, "<FORGALMAZONEV>", "</FORGALMAZONEV>");
            }
            if (manufacturer.isEmpty()) {
                manufacturer = extractValue(termekadatResponse, "<CEGNEV>", "</CEGNEV>");
            }
            // Only show ID if no name is available
            if (manufacturer.isEmpty()) {
                String forgalmazId = extractValue(termekadatResponse, "<FORGALMAZ_ID>", "</FORGALMAZ_ID>");
                String forgengtId = extractValue(termekadatResponse, "<FORGENGT_ID>", "</FORGENGT_ID>");
                if (!forgalmazId.isEmpty() || !forgengtId.isEmpty()) {
                    manufacturer = "ID: " + (forgalmazId.isEmpty() ? forgengtId : forgalmazId);
                }
            }
            
            // Extract all other available fields
            String tttCode = extractValue(termekadatResponse, "<TTT>", "</TTT>");
            String activeIngredient = extractValue(termekadatResponse, "<HATOANYAG>", "</HATOANYAG>");
            String packaging = extractValue(termekadatResponse, "<KISZNEV>", "</KISZNEV>");
            String registrationNumber = extractValue(termekadatResponse, "<TK>", "</TK>");
            String prescriptionStatus = extractValue(termekadatResponse, "<RENDELHET>", "</RENDELHET>");
            String productForm = extractValue(termekadatResponse, "<GYSZERFORM>", "</GYSZERFORM>");
            String strength = extractValue(termekadatResponse, "<HATAROSSAG>", "</HATAROSSAG>");
            String packSize = extractValue(termekadatResponse, "<KISZALLKVANT>", "</KISZALLKVANT>");
            String productType = extractValue(termekadatResponse, "<TERMEKADAT_TIPUS>", "</TERMEKADAT_TIPUS>");
            
            // Extract validity dates
            String validFrom = extractValue(termekadatResponse, "<ERV_KEZD>", "</ERV_KEZD>");
            String validTo = extractValue(termekadatResponse, "<ERV_VEGE>", "</ERV_VEGE>");
            
            // Default values
            boolean reimbursable = false;
            String price = "N/A";
            String supportPercent = "0";
            
            // Parse TAMOGATADAT if available
            String normativity = "";
            String supportType = "";
            if (tamogatadatResponse != null && !tamogatadatResponse.isEmpty()) {
                price = extractValue(tamogatadatResponse, "<BRUNAKFOGY>", "</BRUNAKFOGY>");
                if (price.isEmpty()) {
                    price = extractValue(tamogatadatResponse, "<FAB>", "</FAB>");
                }
                supportPercent = extractValue(tamogatadatResponse, "<TAMSZAZ>", "</TAMSZAZ>");
                reimbursable = !supportPercent.isEmpty() && !supportPercent.equals("0");
                
                // Extract normative/free pricing info
                normativity = extractValue(tamogatadatResponse, "<NORMATIVITAS>", "</NORMATIVITAS>");
                if (normativity.isEmpty()) {
                    normativity = extractValue(tamogatadatResponse, "<NORMATIV>", "</NORMATIV>");
                }
                
                // Extract support type
                supportType = extractValue(tamogatadatResponse, "<TAMTIPUS>", "</TAMTIPUS>");
                if (supportType.isEmpty()) {
                    supportType = extractValue(tamogatadatResponse, "<TAMOGATAS_TIPUS>", "</TAMOGATAS_TIPUS>");
                }
                
                // Log TAMOGATADAT response for one product to see available fields
                if (productId.equals("55845963")) {
                    logger.info("FULL TAMOGATADAT RESPONSE for debugging:\n{}", tamogatadatResponse);
                }
            }
            
            // Ensure we have at least the product name
            if (productName.isEmpty()) {
                productName = "Product " + productId;
            }
            
            if (manufacturer.isEmpty()) {
                manufacturer = "N/A";
            }
            
            if (activeIngredient.isEmpty()) {
                activeIngredient = "N/A";
            }
            
            // Determine prescription required based on status
            // VK = Vény nélkül kapható (available without prescription)
            // Empty or null also means no prescription required
            boolean prescriptionRequired = !"VK".equals(prescriptionStatus) && 
                                          prescriptionStatus != null && 
                                          !prescriptionStatus.isEmpty();
            
            logger.info("Parsed product {}: name='{}', TTT='{}', ATC='{}', manufacturer='{}', prescription='{}'", 
                productId, productName, tttCode, atcCode, manufacturer, prescriptionStatus);
            
            return String.format("""
                    <drug>
                        <id>%s</id>
                        <name>%s</name>
                        <manufacturer>%s</manufacturer>
                        <atcCode>%s</atcCode>
                        <tttCode>%s</tttCode>
                        <activeIngredients>
                            <ingredient>
                                <name>%s</name>
                            </ingredient>
                        </activeIngredients>
                        <packaging>%s</packaging>
                        <registrationNumber>%s</registrationNumber>
                        <prescriptionRequired>%s</prescriptionRequired>
                        <prescriptionStatus>%s</prescriptionStatus>
                        <reimbursable>%s</reimbursable>
                        <status>ACTIVE</status>
                        <price>%s</price>
                        <supportPercent>%s</supportPercent>
                        <productForm>%s</productForm>
                        <strength>%s</strength>
                        <packSize>%s</packSize>
                        <productType>%s</productType>
                        <validFrom>%s</validFrom>
                        <validTo>%s</validTo>
                        <normativity>%s</normativity>
                        <supportType>%s</supportType>
                        <source>REAL PUPHAX DATA</source>
                    </drug>
                """, productId, escapeXml(productName), escapeXml(manufacturer), 
                     escapeXml(atcCode), escapeXml(tttCode), escapeXml(activeIngredient),
                     escapeXml(packaging), escapeXml(registrationNumber), prescriptionRequired,
                     escapeXml(prescriptionStatus), reimbursable, price, supportPercent,
                     escapeXml(productForm), escapeXml(strength), escapeXml(packSize),
                     escapeXml(productType), escapeXml(validFrom), escapeXml(validTo),
                     escapeXml(normativity), escapeXml(supportType));
                
        } catch (Exception e) {
            logger.error("Failed to parse product data: {}", e.getMessage());
            return String.format("""
                    <drug>
                        <id>%s</id>
                        <name>Product %s (parse error)</name>
                        <manufacturer>N/A</manufacturer>
                        <atcCode>ERROR</atcCode>
                        <activeIngredients>
                            <ingredient>
                                <name>Parse error: %s</name>
                            </ingredient>
                        </activeIngredients>
                        <prescriptionRequired>true</prescriptionRequired>
                        <reimbursable>false</reimbursable>
                        <status>ERROR</status>
                        <source>PARSE ERROR</source>
                    </drug>
                """, productId, productId, e.getMessage());
        }
    }
    
    /**
     * Parse TAMOGATADAT response to extract product details.
     * @deprecated Use parseProductData instead which combines TERMEKADAT and TAMOGATADAT
     */
    @Deprecated
    private String parseTamogatadatResponse(String productId, String tamogatadatResponse) {
        try {
            logger.debug("Parsing TAMOGATADAT for product {}, response length: {}", productId, tamogatadatResponse.length());
            
            // Log what we're looking for
            boolean hasObjTamogat = tamogatadatResponse.contains("OBJTAMOGAT");
            boolean hasTamogatadatOutput = tamogatadatResponse.contains("TAMOGATADATOutput");
            boolean hasKgykeret = tamogatadatResponse.contains("KGYKERET");
            
            logger.info("TAMOGATADAT response analysis for product {}: hasOBJTAMOGAT={}, hasTAMOGATADATOutput={}, hasKGYKERET={}", 
                productId, hasObjTamogat, hasTamogatadatOutput, hasKgykeret);
            
            // Log the first 200 chars to see what's actually in the response
            logger.info("TAMOGATADAT response preview for {}: {}", productId,
                tamogatadatResponse.length() > 200 ? tamogatadatResponse.substring(0, 200) + "..." : tamogatadatResponse);
            
            // Check if we have a valid response - might be different format
            if (!hasObjTamogat && !hasTamogatadatOutput && !hasKgykeret) {
                logger.warn("Invalid TAMOGATADAT response for product {}", productId);
                
                // Check for SOAP fault
                if (tamogatadatResponse.contains("faultstring") || tamogatadatResponse.contains("soap:Fault")) {
                    logger.error("SOAP Fault in TAMOGATADAT response: {}", tamogatadatResponse);
                }
                
                throw new Exception("Invalid response format");
            }
            
            // Check if this is an empty/default response (KGYKERET with max value indicates no data)
            String kgykeretValue = extractValue(tamogatadatResponse, "<KGYKERET>", "</KGYKERET>");
            if ("999999999.999999".equals(kgykeretValue)) {
                logger.info("Product {} has no support data (KGYKERET=999999999.999999)", productId);
                // Return a response indicating this product has no current support data
                return String.format("""
                        <drug>
                            <id>%s</id>
                            <name>Product ID %s (no support data available)</name>
                            <manufacturer>NEAK</manufacturer>
                            <atcCode>N/A</atcCode>
                            <activeIngredients>
                                <ingredient>
                                    <name>No current support data</name>
                                </ingredient>
                            </activeIngredients>
                            <prescriptionRequired>true</prescriptionRequired>
                            <reimbursable>false</reimbursable>
                            <status>NO_DATA</status>
                            <source>REAL PUPHAX - NO SUPPORT DATA</source>
                        </drug>
                    """, productId, productId);
            }
            
            // Extract product name
            String productName = extractValue(tamogatadatResponse, "<TERMEKNEV>", "</TERMEKNEV>");
            if (productName.isEmpty()) {
                productName = extractValue(tamogatadatResponse, "<NEV>", "</NEV>");
            }
            if (productName.isEmpty()) {
                productName = "PUPHAX Termék " + productId;
            }
            
            // Extract ATC code
            String atcCode = extractValue(tamogatadatResponse, "<ATC>", "</ATC>");
            if (atcCode.isEmpty()) {
                atcCode = extractValue(tamogatadatResponse, "<ATCKOD>", "</ATCKOD>");
            }
            
            // Extract manufacturer/company name
            String manufacturer = extractValue(tamogatadatResponse, "<FORGALMNEV>", "</FORGALMNEV>");
            if (manufacturer.isEmpty()) {
                manufacturer = extractValue(tamogatadatResponse, "<CEGNEV>", "</CEGNEV>");
            }
            if (manufacturer.isEmpty()) {
                manufacturer = "NEAK PUPHAX";
            }
            
            // Extract price info - looking at the actual fields in OBJTAMOGAT
            String price = extractValue(tamogatadatResponse, "<BRUNAKFOGY>", "</BRUNAKFOGY>");
            if (price.isEmpty()) {
                price = extractValue(tamogatadatResponse, "<BRUNAGYFOGY>", "</BRUNAGYFOGY>");
            }
            
            // Extract support percentage from TAMOGATASOK section
            String supportPercent = extractValue(tamogatadatResponse, "<TAMSZAZ>", "</TAMSZAZ>");
            
            // Check if reimbursable
            boolean reimbursable = !supportPercent.isEmpty() && !supportPercent.equals("0");
            
            // Extract active ingredient
            String activeIngredient = extractValue(tamogatadatResponse, "<HATOANYAG>", "</HATOANYAG>");
            if (activeIngredient.isEmpty()) {
                activeIngredient = extractValue(tamogatadatResponse, "<HATANYNEV>", "</HATANYNEV>");
            }
            if (activeIngredient.isEmpty()) {
                activeIngredient = "N/A";
            }
            
            return String.format("""
                    <drug>
                        <id>%s</id>
                        <name>%s</name>
                        <manufacturer>%s</manufacturer>
                        <atcCode>%s</atcCode>
                        <activeIngredients>
                            <ingredient>
                                <name>%s</name>
                            </ingredient>
                        </activeIngredients>
                        <prescriptionRequired>true</prescriptionRequired>
                        <reimbursable>%s</reimbursable>
                        <status>ACTIVE</status>
                        <price>%s</price>
                        <supportPercent>%s</supportPercent>
                        <source>REAL PUPHAX TAMOGATADAT</source>
                    </drug>
                """, productId, escapeXml(productName), escapeXml(manufacturer), 
                     escapeXml(atcCode), escapeXml(activeIngredient), reimbursable, 
                     price, supportPercent);
            
        } catch (Exception e) {
            logger.error("Failed to parse TAMOGATADAT response: {}", e.getMessage());
            return String.format("""
                    <drug>
                        <id>%s</id>
                        <name>PUPHAX Termék ID: %s</name>
                        <manufacturer>NEAK PUPHAX</manufacturer>
                        <atcCode>ERROR</atcCode>
                        <activeIngredients>
                            <ingredient>
                                <name>Parse hiba</name>
                            </ingredient>
                        </activeIngredients>
                        <prescriptionRequired>true</prescriptionRequired>
                        <reimbursable>false</reimbursable>
                        <status>ERROR</status>
                        <source>PARSE ERROR</source>
                    </drug>
                """, productId, productId);
        }
    }
    
    private String extractValue(String xml, String startTag, String endTag) {
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) return "";
        
        startIndex += startTag.length();
        int endIndex = xml.indexOf(endTag, startIndex);
        if (endIndex == -1) return "";
        
        return xml.substring(startIndex, endIndex).trim();
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}