package com.puphax.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Raw HTTP client for PUPHAX SOAP service that handles character encoding properly.
 * This bypasses JAX-WS to avoid XML parsing issues with mixed encodings.
 */
@Component
public class PuphaxHttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxHttpClient.class);
    private static final String PUPHAX_ENDPOINT = "https://puphax.neak.gov.hu/PUPHAXWS";
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    
    static {
        // Setup digest authentication
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost().equals("puphax.neak.gov.hu")) {
                    return new PasswordAuthentication("PUPHAX", "puphax".toCharArray());
                }
                return null;
            }
        });
    }
    
    /**
     * Call PUPHAX TERMEKLISTA operation using raw HTTP.
     */
    public String callTermekLista(String searchFilter) throws Exception {
        logger.info("Making raw HTTP call to PUPHAX TERMEKLISTA with filter: {}", searchFilter);
        
        String soapRequest = buildTermekListaRequest(searchFilter);
        String response = sendSoapRequest(soapRequest, "TERMEKLISTA");
        
        // Convert the response from ISO-8859-2 to UTF-8
        String convertedResponse = convertResponseEncoding(response);
        logger.debug("Raw HTTP PUPHAX response received: {} characters", convertedResponse.length());
        
        return convertedResponse;
    }
    
    /**
     * Call PUPHAX TERMEKADAT operation using raw HTTP.
     */
    public String callTermekAdat(String drugId) throws Exception {
        logger.info("Making raw HTTP call to PUPHAX TERMEKADAT for drug ID: {}", drugId);
        
        String soapRequest = buildTermekAdatRequest(drugId);
        String response = sendSoapRequest(soapRequest, "TERMEKADAT");
        
        // Convert the response from ISO-8859-2 to UTF-8
        String convertedResponse = convertResponseEncoding(response);
        logger.debug("Raw HTTP PUPHAX drug details received: {} characters", convertedResponse.length());
        
        return convertedResponse;
    }
    
    /**
     * Build SOAP request for TERMEKLISTA operation.
     */
    private String buildTermekListaRequest(String filter) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Body>
                    <ter:TERMEKLISTA xmlns:ter="http://xmlns.oracle.com/orawsv/NEAK/PUPHAXWS">
                        <ter:C_OBJ_ID_LISTA_TERMEKLISTA-INPUT>
                            <ter:DSP-DATE-IN>%s</ter:DSP-DATE-IN>
                            <ter:SX-FILTER-VARCHAR2-IN>%s</ter:SX-FILTER-VARCHAR2-IN>
                        </ter:C_OBJ_ID_LISTA_TERMEKLISTA-INPUT>
                    </ter:TERMEKLISTA>
                </soap:Body>
            </soap:Envelope>
            """, currentDate, filter);
    }
    
    /**
     * Build SOAP request for TERMEKADAT operation.
     */
    private String buildTermekAdatRequest(String drugId) {
        // Extract numeric ID
        String numericId = drugId.replaceAll("[^0-9]", "");
        if (numericId.isEmpty()) {
            numericId = "1";
        }
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Body>
                    <ter:TERMEKADAT xmlns:ter="http://xmlns.oracle.com/orawsv/NEAK/PUPHAXWS">
                        <ter:C_OBJ_TERMEKADAT_TERMEKADAT-INPUT>
                            <ter:N_ID-NUMBER-IN>%s</ter:N_ID-NUMBER-IN>
                        </ter:C_OBJ_TERMEKADAT_TERMEKADAT-INPUT>
                    </ter:TERMEKADAT>
                </soap:Body>
            </soap:Envelope>
            """, numericId);
    }
    
    /**
     * Send SOAP request and get response.
     */
    private String sendSoapRequest(String soapRequest, String operation) throws Exception {
        URL url = new URL(PUPHAX_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            
            // Set headers
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            connection.setRequestProperty("SOAPAction", operation);
            connection.setRequestProperty("Accept", "text/xml, application/soap+xml");
            connection.setRequestProperty("Accept-Charset", "UTF-8, ISO-8859-2");
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] requestBytes = soapRequest.getBytes(StandardCharsets.UTF_8);
                os.write(requestBytes);
                os.flush();
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            logger.debug("PUPHAX HTTP response code: {}", responseCode);
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 
                            ? connection.getInputStream() 
                            : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            return response.toString();
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Convert response from ISO-8859-2 to UTF-8.
     */
    private String convertResponseEncoding(String response) {
        try {
            // Check if response contains encoding declaration
            if (response.contains("encoding=\"UTF-8\"")) {
                // The response claims to be UTF-8 but may contain ISO-8859-2 content
                // Try to detect and fix encoding issues
                
                // Find the start of actual content (after SOAP headers)
                int contentStart = response.indexOf("<soap:Body>");
                if (contentStart > 0) {
                    String header = response.substring(0, contentStart);
                    String content = response.substring(contentStart);
                    
                    // Convert content that might have ISO-8859-2 characters
                    String fixedContent = fixMixedEncoding(content);
                    
                    return header + fixedContent;
                }
            }
            
            // Fallback: return as-is
            return response;
            
        } catch (Exception e) {
            logger.warn("Failed to convert response encoding: {}", e.getMessage());
            return response;
        }
    }
    
    /**
     * Fix mixed encoding in content.
     */
    private String fixMixedEncoding(String content) {
        try {
            // Try to detect ISO-8859-2 sequences
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
            String converted = new String(contentBytes, ISO_8859_2);
            
            // Check if conversion improved the content
            if (isValidHungarianText(converted)) {
                logger.debug("Successfully converted content from ISO-8859-2");
                return converted;
            }
            
            // If not, try manual character fixes
            return fixHungarianCharacters(content);
            
        } catch (Exception e) {
            logger.warn("Mixed encoding fix failed: {}", e.getMessage());
            return content;
        }
    }
    
    /**
     * Check if text contains valid Hungarian characters.
     */
    private boolean isValidHungarianText(String text) {
        // Check for presence of Hungarian characters without encoding artifacts
        return (text.contains("á") || text.contains("é") || text.contains("ó") || 
                text.contains("ö") || text.contains("ő") || text.contains("ű")) &&
               !text.contains("\uFFFD") && !text.contains("�");
    }
    
    /**
     * Manually fix common Hungarian character encoding issues.
     */
    private String fixHungarianCharacters(String content) {
        return content
            // Common UTF-8 misinterpretation of ISO-8859-2
            .replace("Ã¡", "á")
            .replace("Ã©", "é")
            .replace("Ã­", "í")
            .replace("Ã³", "ó")
            .replace("Ãº", "ú")
            .replace("Ã¶", "ö")
            .replace("Ã¼", "ü")
            .replace("Å'", "ő")
            .replace("Å±", "ű")
            .replace("Ã", "Á")
            .replace("Ã‰", "É")
            .replace("Ã", "Í")
            .replace("Ó", "Ó")
            .replace("Ãš", "Ú")
            .replace("Ã–", "Ö")
            .replace("Ãœ", "Ü")
            .replace("Å", "Ő")
            .replace("Å°", "Ű")
            // Windows-1252 interpretations
            .replace("ĂĄ", "á")
            .replace("Ă©", "é")
            .replace("Ăł", "ó")
            .replace("Ĺ'", "ő")
            .replace("Ĺą", "ű");
    }
}