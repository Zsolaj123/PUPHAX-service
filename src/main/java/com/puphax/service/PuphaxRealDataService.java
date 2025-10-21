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
            
            // TEMPORARILY DISABLED TAMOGATADAT - just show real IDs
            logger.warn("TAMOGATADAT calls temporarily disabled for debugging");
            
            for (String productId : productIds) {
                if (count++ >= 10) break; // Limit to first 10
                
                // Add basic info with REAL product ID from PUPHAX
                xmlBuilder.append(String.format("""
                        <drug>
                            <id>%s</id>
                            <name>REAL PUPHAX ID: %s (from TERMEKLISTA)</name>
                            <manufacturer>NEAK PUPHAX</manufacturer>
                            <atcCode>DEBUG</atcCode>
                            <activeIngredients>
                                <ingredient>
                                    <name>Real ID from PUPHAX</name>
                                </ingredient>
                            </activeIngredients>
                            <prescriptionRequired>true</prescriptionRequired>
                            <reimbursable>true</reimbursable>
                            <status>ACTIVE</status>
                            <source>REAL PUPHAX TERMEKLISTA</source>
                        </drug>
                    """, productId, productId));
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
     * Parse TAMOGATADAT response to extract product details.
     */
    private String parseTamogatadatResponse(String productId, String tamogatadatResponse) {
        try {
            logger.debug("Parsing TAMOGATADAT for product {}, response length: {}", productId, tamogatadatResponse.length());
            
            // Check if we have a valid response
            if (!tamogatadatResponse.contains("OBJTAMOGAT") && !tamogatadatResponse.contains("TAMOGATADATOutput")) {
                logger.warn("Invalid TAMOGATADAT response for product {}", productId);
                
                // Check for SOAP fault
                if (tamogatadatResponse.contains("faultstring") || tamogatadatResponse.contains("soap:Fault")) {
                    logger.error("SOAP Fault in TAMOGATADAT response: {}", tamogatadatResponse);
                }
                
                throw new Exception("Invalid response format");
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