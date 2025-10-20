package com.puphax.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling character encoding issues with PUPHAX SOAP responses.
 * 
 * PUPHAX returns SOAP responses in UTF-8 format but the actual content within the XML
 * may contain Hungarian characters encoded in ISO-8859-2. This utility handles the
 * conversion and cleaning of such mixed-encoding content.
 */
public class EncodingUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(EncodingUtils.class);
    
    // ISO-8859-2 charset (Central European encoding used by PUPHAX)
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    
    // Pattern to detect potential encoding issues in SOAP responses
    private static final Pattern ENCODING_ERROR_PATTERN = Pattern.compile(
        "Invalid byte|UTF-8 sequence|encoding|charset", Pattern.CASE_INSENSITIVE
    );
    
    // Pattern to extract XML content from SOAP fault messages
    private static final Pattern SOAP_FAULT_CONTENT_PATTERN = Pattern.compile(
        "<soap:Body>.*?<soap:Fault>.*?</soap:Fault>.*?</soap:Body>", Pattern.DOTALL
    );
    
    /**
     * Attempts to fix character encoding issues in PUPHAX SOAP responses.
     * 
     * This method tries multiple approaches to handle mixed encoding:
     * 1. First attempts to parse as UTF-8
     * 2. If that fails, tries to convert ISO-8859-2 content to UTF-8
     * 3. As a last resort, applies character replacement for common Hungarian characters
     * 
     * @param rawSoapResponse The raw SOAP response string
     * @return The corrected response string with proper UTF-8 encoding
     */
    public static String fixPuphaxEncoding(String rawSoapResponse) {
        if (rawSoapResponse == null || rawSoapResponse.isEmpty()) {
            return rawSoapResponse;
        }
        
        try {
            // First attempt: validate UTF-8 encoding
            byte[] utf8Bytes = rawSoapResponse.getBytes(StandardCharsets.UTF_8);
            String utf8Test = new String(utf8Bytes, StandardCharsets.UTF_8);
            
            // If this succeeds without corruption, the string is already properly encoded
            if (utf8Test.equals(rawSoapResponse)) {
                logger.debug("PUPHAX response is already properly UTF-8 encoded");
                return rawSoapResponse;
            }
            
        } catch (Exception e) {
            logger.debug("Initial UTF-8 validation failed, attempting encoding conversion: {}", e.getMessage());
        }
        
        // Second attempt: convert from ISO-8859-2 to UTF-8
        try {
            String converted = convertIso88592ToUtf8(rawSoapResponse);
            logger.info("Successfully converted PUPHAX response from ISO-8859-2 to UTF-8");
            return converted;
            
        } catch (Exception e) {
            logger.warn("ISO-8859-2 conversion failed, attempting character replacement: {}", e.getMessage());
        }
        
        // Third attempt: replace problematic characters
        try {
            String cleaned = replaceHungarianCharacters(rawSoapResponse);
            logger.info("Applied Hungarian character replacement to PUPHAX response");
            return cleaned;
            
        } catch (Exception e) {
            logger.error("All encoding fix attempts failed for PUPHAX response: {}", e.getMessage());
            return rawSoapResponse; // Return original as last resort
        }
    }
    
    /**
     * Converts content from ISO-8859-2 encoding to UTF-8.
     * 
     * @param iso88592Content Content encoded in ISO-8859-2
     * @return UTF-8 encoded content
     * @throws IOException if conversion fails
     */
    private static String convertIso88592ToUtf8(String iso88592Content) throws IOException {
        // Convert string to ISO-8859-2 bytes
        byte[] iso88592Bytes = iso88592Content.getBytes(ISO_8859_2);
        
        // Create UTF-8 string from ISO-8859-2 bytes
        ByteArrayInputStream inputStream = new ByteArrayInputStream(iso88592Bytes);
        InputStreamReader reader = new InputStreamReader(inputStream, ISO_8859_2);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        // Copy and convert character by character
        int character;
        while ((character = reader.read()) != -1) {
            writer.write(character);
        }
        
        writer.flush();
        writer.close();
        reader.close();
        
        return outputStream.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Replaces common Hungarian characters that may cause encoding issues.
     * 
     * This is a fallback method that replaces problematic characters with
     * their closest ASCII equivalents or proper UTF-8 representations.
     * 
     * @param content Content with potential Hungarian character issues
     * @return Content with characters replaced
     */
    private static String replaceHungarianCharacters(String content) {
        if (content == null) {
            return null;
        }
        
        // Replace common Hungarian characters that cause encoding issues
        String result = content
            // á, é, í, ó, ú - acute accents
            .replaceAll("\\uFFFD\\uFFFD", "á")  // Common replacement for corrupted á
            .replaceAll("\\uFFFD", "?")        // Replace any remaining replacement characters
            
            // Direct character replacements for common Hungarian letters
            .replace("á", "á")
            .replace("é", "é") 
            .replace("í", "í")
            .replace("ó", "ó")
            .replace("ú", "ú")
            .replace("ö", "ö")
            .replace("ü", "ü")
            .replace("ő", "ő")
            .replace("ű", "ű")
            
            // Uppercase versions
            .replace("Á", "Á")
            .replace("É", "É")
            .replace("Í", "Í")
            .replace("Ó", "Ó")
            .replace("Ú", "Ú")
            .replace("Ö", "Ö")
            .replace("Ü", "Ü")
            .replace("Ő", "Ő")
            .replace("Ű", "Ű");
        
        logger.debug("Applied Hungarian character replacement to {} characters", content.length());
        return result;
    }
    
    /**
     * Checks if an error message indicates a character encoding issue.
     * 
     * @param errorMessage The error message to check
     * @return true if the error appears to be encoding-related
     */
    public static boolean isEncodingError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return false;
        }
        
        Matcher matcher = ENCODING_ERROR_PATTERN.matcher(errorMessage);
        boolean isEncodingError = matcher.find();
        
        if (isEncodingError) {
            logger.debug("Detected encoding error pattern in message: {}", errorMessage);
        }
        
        return isEncodingError;
    }
    
    /**
     * Extracts readable content from SOAP fault messages with encoding issues.
     * 
     * @param soapFaultMessage The SOAP fault message
     * @return Cleaned and readable fault content
     */
    public static String cleanSoapFaultMessage(String soapFaultMessage) {
        if (soapFaultMessage == null || soapFaultMessage.isEmpty()) {
            return "Unknown SOAP fault";
        }
        
        try {
            // First try to fix encoding
            String fixed = fixPuphaxEncoding(soapFaultMessage);
            
            // Extract meaningful content from SOAP fault
            Matcher matcher = SOAP_FAULT_CONTENT_PATTERN.matcher(fixed);
            if (matcher.find()) {
                return matcher.group(0);
            }
            
            return fixed;
            
        } catch (Exception e) {
            logger.warn("Failed to clean SOAP fault message: {}", e.getMessage());
            return "SOAP fault (encoding issues): " + soapFaultMessage.substring(0, Math.min(100, soapFaultMessage.length()));
        }
    }
    
    /**
     * Validates if a string contains valid UTF-8 characters.
     * 
     * @param content The content to validate
     * @return true if the content is valid UTF-8
     */
    public static boolean isValidUtf8(String content) {
        if (content == null) {
            return true;
        }
        
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            String recreated = new String(bytes, StandardCharsets.UTF_8);
            return content.equals(recreated);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Logs encoding statistics for debugging purposes.
     * 
     * @param original Original content
     * @param converted Converted content
     */
    public static void logEncodingStats(String original, String converted) {
        if (logger.isDebugEnabled()) {
            logger.debug("Encoding conversion stats: original length={}, converted length={}, utf8Valid={}", 
                        original != null ? original.length() : 0,
                        converted != null ? converted.length() : 0,
                        isValidUtf8(converted));
        }
    }
}