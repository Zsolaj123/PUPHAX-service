package com.puphax.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Custom HTTP transport for handling Hungarian character encoding in PUPHAX responses.
 * 
 * This transport intercepts HTTP responses from PUPHAX and converts ISO-8859-2 
 * content to UTF-8 before it reaches the XML parser, preventing encoding errors.
 */
public class HungarianHttpTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(HungarianHttpTransport.class);
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    
    /**
     * Performs HTTP request with Hungarian character encoding support.
     */
    public static InputStream performRequest(String endpoint, String soapAction, byte[] soapRequestBytes) throws IOException {
        logger.debug("Performing HTTP request to PUPHAX with Hungarian encoding support: {}", endpoint);
        
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            
            // Set headers
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            connection.setRequestProperty("SOAPAction", soapAction != null ? soapAction : "");
            connection.setRequestProperty("Accept", "text/xml, application/soap+xml");
            connection.setRequestProperty("User-Agent", "PUPHAX-Java-Client/1.0");
            
            // Add digest authentication header
            String credentials = "PUPHAX:puphax";
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
            
            // Send request
            connection.getOutputStream().write(soapRequestBytes);
            connection.getOutputStream().flush();
            
            // Get response
            int responseCode = connection.getResponseCode();
            logger.debug("PUPHAX HTTP response code: {}", responseCode);
            
            InputStream responseStream;
            if (responseCode >= 200 && responseCode < 300) {
                responseStream = connection.getInputStream();
            } else {
                responseStream = connection.getErrorStream();
                logger.warn("PUPHAX returned HTTP error code: {}", responseCode);
            }
            
            // Read the raw response
            ByteArrayOutputStream rawResponse = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = responseStream.read(buffer)) != -1) {
                rawResponse.write(buffer, 0, bytesRead);
            }
            responseStream.close();
            
            byte[] responseBytes = rawResponse.toByteArray();
            logger.debug("Received PUPHAX response: {} bytes", responseBytes.length);
            
            // Fix Hungarian character encoding
            byte[] fixedResponseBytes = fixHungarianEncoding(responseBytes);
            
            return new ByteArrayInputStream(fixedResponseBytes);
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Fix Hungarian character encoding in PUPHAX response.
     */
    private static byte[] fixHungarianEncoding(byte[] responseBytes) {
        try {
            // First, try to detect the encoding by looking at the response
            String responseAsUtf8 = new String(responseBytes, StandardCharsets.UTF_8);
            
            // Check if there are encoding issues (replacement characters or malformed sequences)
            if (hasEncodingIssues(responseAsUtf8)) {
                logger.debug("Detected encoding issues, attempting to fix Hungarian characters");
                
                // Try interpreting the bytes as ISO-8859-2 and converting to UTF-8
                String responseAsIso = new String(responseBytes, ISO_8859_2);
                byte[] fixedBytes = responseAsIso.getBytes(StandardCharsets.UTF_8);
                
                String fixedResponse = new String(fixedBytes, StandardCharsets.UTF_8);
                
                // Verify the fix worked
                if (!hasEncodingIssues(fixedResponse)) {
                    logger.info("Successfully fixed Hungarian character encoding in PUPHAX response");
                    return fixedBytes;
                } else {
                    logger.debug("ISO-8859-2 conversion didn't resolve encoding issues, trying character replacement");
                    
                    // Try specific character replacements for common Hungarian encoding issues
                    String manuallyFixed = fixHungarianCharacterReplacements(responseAsUtf8);
                    byte[] manuallyFixedBytes = manuallyFixed.getBytes(StandardCharsets.UTF_8);
                    
                    logger.info("Applied manual Hungarian character replacements");
                    return manuallyFixedBytes;
                }
            } else {
                logger.debug("No encoding issues detected in PUPHAX response");
                return responseBytes;
            }
            
        } catch (Exception e) {
            logger.warn("Error during Hungarian encoding fix, returning original response: {}", e.getMessage());
            return responseBytes;
        }
    }
    
    /**
     * Check if the response has encoding issues.
     */
    private static boolean hasEncodingIssues(String response) {
        // Look for replacement characters or common encoding issue patterns
        return response.contains("\uFFFD") || // Unicode replacement character
               response.contains("�") ||       // Often appears with encoding issues
               // Check for malformed UTF-8 patterns that indicate double-encoding
               response.contains("Ã¡") ||      // á encoded as UTF-8 then read as ISO-8859-1
               response.contains("Ã©") ||      // é
               response.contains("Ã­") ||      // í
               response.contains("Ã³") ||      // ó
               response.contains("Ãº") ||      // ú
               response.contains("Ã¶") ||      // ö
               response.contains("Ã¼");        // ü
    }
    
    /**
     * Apply specific character replacements for Hungarian encoding issues.
     */
    private static String fixHungarianCharacterReplacements(String response) {
        return response
            // Fix double-encoded Hungarian characters (UTF-8 -> ISO-8859-1 -> UTF-8)
            .replace("Ã¡", "á")   // á
            .replace("Ã©", "é")   // é  
            .replace("Ã­", "í")   // í
            .replace("Ã³", "ó")   // ó
            .replace("Ãº", "ú")   // ú
            .replace("Ã¶", "ö")   // ö
            .replace("Ã¼", "ü")   // ü
            .replace("Å'", "ő")   // ő
            .replace("Å±", "ű")   // ű
            
            // Uppercase versions
            .replace("Ã\u0081", "Á")
            .replace("Ã\u0089", "É")
            .replace("Ã\u008D", "Í")
            .replace("Ã\u0093", "Ó")
            .replace("Ã\u009A", "Ú")
            .replace("Ã\u0096", "Ö")
            .replace("Ã\u009C", "Ü")
            .replace("Å\u0090", "Ő")
            .replace("Å°", "Ű")
            
            // Remove any remaining replacement characters
            .replace("\uFFFD", "?");
    }
}