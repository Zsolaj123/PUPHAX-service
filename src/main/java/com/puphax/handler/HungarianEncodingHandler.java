package com.puphax.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * SOAP Handler to fix Hungarian character encoding issues with PUPHAX responses.
 * 
 * This handler intercepts SOAP responses before JAX-WS tries to parse them
 * and converts ISO-8859-2 content to UTF-8 to prevent parsing errors.
 */
public class HungarianEncodingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private static final Logger logger = LoggerFactory.getLogger(HungarianEncodingHandler.class);
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        
        // Only handle inbound (response) messages
        if (!outbound) {
            try {
                SOAPMessage message = context.getMessage();
                
                // Get the original message content as string
                ByteArrayOutputStream originalStream = new ByteArrayOutputStream();
                message.writeTo(originalStream);
                String originalContent = originalStream.toString(StandardCharsets.UTF_8);
                
                // Check if this looks like a PUPHAX response with encoding issues
                if (isPuphaxResponseWithEncodingIssues(originalContent)) {
                    logger.debug("Detected PUPHAX response with potential Hungarian encoding issues");
                    
                    // Log some details about the content for debugging
                    logger.debug("Original content length: {}, contains PUPHAX indicators: {}", 
                                originalContent.length(), 
                                originalContent.contains("TERMEKLISTA") || originalContent.contains("puphax"));
                    
                    // For now, just log that we detected the issue but don't modify the message
                    // This allows us to see if our detection logic works without breaking XML parsing
                    logger.info("Hungarian encoding handler detected PUPHAX response (not modifying for now)");
                }
                
            } catch (Exception e) {
                logger.warn("Failed to analyze SOAP message for Hungarian encoding: {}", e.getMessage());
                // Don't fail the processing, let it continue with original message
            }
        }
        
        return true; // Continue processing
    }
    
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // Let faults pass through unchanged
        return true;
    }
    
    @Override
    public void close(MessageContext context) {
        // Nothing to clean up
    }
    
    @Override
    public Set<QName> getHeaders() {
        // No specific headers to handle
        return null;
    }
    
    /**
     * Check if this appears to be a PUPHAX response with encoding issues.
     */
    private boolean isPuphaxResponseWithEncodingIssues(String content) {
        // Look for indicators that this is a PUPHAX response
        return content.contains("TERMEKLISTA") || 
               content.contains("TERMEKADAT") || 
               content.contains("puphax") ||
               content.contains("NEAK") ||
               // Check for common encoding issue patterns
               content.contains("\\uFFFD") || // Replacement character
               content.contains("?"); // Often appears when encoding fails
    }
    
    /**
     * Attempt to fix Hungarian character encoding issues.
     */
    private String fixHungarianEncoding(String content) {
        try {
            // Try to detect and fix common Hungarian character encoding issues
            
            // First, try to identify if the content has been double-encoded
            byte[] bytes = content.getBytes(StandardCharsets.ISO_8859_1);
            String decoded = new String(bytes, ISO_8859_2);
            
            // Check if the decoded version looks better (contains fewer replacement chars)
            if (countReplacementChars(decoded) < countReplacementChars(content)) {
                logger.debug("Applied ISO-8859-1 to ISO-8859-2 conversion");
                return decoded;
            }
            
            // If that didn't help, try direct character replacements for common issues
            String fixed = content
                // Replace common double-encoding artifacts
                .replace("Ã¡", "á")  // á encoded as UTF-8 then read as ISO-8859-1
                .replace("Ã©", "é")  // é
                .replace("Ã­", "í")  // í
                .replace("Ã³", "ó")  // ó
                .replace("Ãº", "ú")  // ú
                .replace("Ã¶", "ö")  // ö
                .replace("Ã¼", "ü")  // ü
                .replace("Å'", "ő")  // ő
                .replace("Å±", "ű")  // ű
                
                // Uppercase versions
                .replace("Ã\u0081", "Á")
                .replace("Ã\u0089", "É")
                .replace("Ã\u008d", "Í")
                .replace("Ã\u0093", "Ó")
                .replace("Ã\u009a", "Ú")
                .replace("Ã\u0096", "Ö")
                .replace("Ã\u009c", "Ü")
                .replace("Å\u0090", "Ő")
                .replace("Å°", "Ű");
            
            if (!fixed.equals(content)) {
                logger.debug("Applied Hungarian character replacement fixes");
                return fixed;
            }
            
        } catch (Exception e) {
            logger.warn("Error during Hungarian encoding fix: {}", e.getMessage());
        }
        
        // If all else fails, return the original content
        return content;
    }
    
    /**
     * Count replacement characters to assess encoding quality.
     */
    private int countReplacementChars(String content) {
        int count = 0;
        for (char c : content.toCharArray()) {
            if (c == '\uFFFD' || c == '?') {
                count++;
            }
        }
        return count;
    }
}