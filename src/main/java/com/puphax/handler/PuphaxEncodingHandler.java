package com.puphax.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.Node;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * SOAP handler that fixes character encoding issues in PUPHAX responses.
 * PUPHAX returns XML with UTF-8 declaration but actual content in ISO-8859-2.
 * This handler converts the content before JAX-WS parsing.
 */
public class PuphaxEncodingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxEncodingHandler.class);
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        
        if (!outbound) { // This is an inbound response
            try {
                SOAPMessage message = context.getMessage();
                
                // Get the raw SOAP response as bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                message.writeTo(baos);
                byte[] rawBytes = baos.toByteArray();
                
                // Convert the raw response to string for analysis
                String rawXml = new String(rawBytes, StandardCharsets.UTF_8);
                logger.debug("Raw PUPHAX response first 200 chars: {}", 
                    rawXml.length() > 200 ? rawXml.substring(0, 200) + "..." : rawXml);
                
                // Check if this contains Hungarian characters that might be ISO-8859-2
                if (containsHungarianEncodingIssues(rawXml)) {
                    logger.info("Detected potential ISO-8859-2 encoding in PUPHAX response, converting...");
                    
                    // Try to fix the encoding by treating the content as ISO-8859-2
                    String fixedXml = fixEncodingInXml(rawXml);
                    
                    // Replace the message content with the fixed version
                    ByteArrayInputStream bais = new ByteArrayInputStream(fixedXml.getBytes(StandardCharsets.UTF_8));
                    SOAPMessage newMessage = jakarta.xml.soap.MessageFactory.newInstance().createMessage(null, bais);
                    
                    // Copy the fixed content to the original message
                    message.getSOAPPart().setContent(newMessage.getSOAPPart().getContent());
                    
                    logger.info("Successfully converted PUPHAX response encoding from ISO-8859-2 to UTF-8");
                }
                
            } catch (Exception e) {
                logger.error("Failed to handle encoding conversion in PUPHAX response: {}", e.getMessage(), e);
                // Don't throw exception - let the message continue processing
            }
        }
        
        return true; // Continue processing
    }
    
    /**
     * Detect if the XML contains potential Hungarian encoding issues.
     */
    private boolean containsHungarianEncodingIssues(String xml) {
        // Check for common signs of encoding problems
        return xml.contains("\uFFFD") || // Replacement character
               xml.contains("�") ||       // Another form of replacement character
               containsInvalidUtf8Sequences(xml);
    }
    
    /**
     * Check if string contains invalid UTF-8 sequences.
     */
    private boolean containsInvalidUtf8Sequences(String str) {
        try {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            String recreated = new String(bytes, StandardCharsets.UTF_8);
            return !str.equals(recreated);
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Fix encoding issues in the XML by converting ISO-8859-2 content to UTF-8.
     */
    private String fixEncodingInXml(String xmlWithEncodingIssues) {
        try {
            // First, try to extract the SOAP body content where encoding issues typically occur
            int bodyStart = xmlWithEncodingIssues.indexOf("<soap:Body>");
            int bodyEnd = xmlWithEncodingIssues.indexOf("</soap:Body>");
            
            if (bodyStart > 0 && bodyEnd > bodyStart) {
                String beforeBody = xmlWithEncodingIssues.substring(0, bodyStart + 11); // Include <soap:Body>
                String bodyContent = xmlWithEncodingIssues.substring(bodyStart + 11, bodyEnd);
                String afterBody = xmlWithEncodingIssues.substring(bodyEnd); // Include </soap:Body>
                
                // Convert the body content from ISO-8859-2 to UTF-8
                String fixedBodyContent = convertContentEncoding(bodyContent);
                
                // Reconstruct the XML
                return beforeBody + fixedBodyContent + afterBody;
            }
            
            // If we can't find SOAP body, try to fix the entire content
            return convertContentEncoding(xmlWithEncodingIssues);
            
        } catch (Exception e) {
            logger.warn("Failed to fix XML encoding, returning original: {}", e.getMessage());
            return xmlWithEncodingIssues;
        }
    }
    
    /**
     * Convert content that may have ISO-8859-2 encoded characters to proper UTF-8.
     */
    private String convertContentEncoding(String content) {
        try {
            // First attempt: Try to interpret bytes as ISO-8859-2
            byte[] possibleIso88592 = content.getBytes(StandardCharsets.ISO_8859_1);
            String converted = new String(possibleIso88592, ISO_8859_2);
            
            // Check if conversion improved the content
            if (!converted.contains("\uFFFD") && isValidXml(converted)) {
                logger.debug("Successfully converted content from ISO-8859-2 to UTF-8");
                return converted;
            }
            
            // Second attempt: Manual character replacement for common Hungarian characters
            return fixHungarianCharacters(content);
            
        } catch (Exception e) {
            logger.warn("Encoding conversion failed: {}", e.getMessage());
            return content;
        }
    }
    
    /**
     * Manually fix common Hungarian character encoding issues.
     */
    private String fixHungarianCharacters(String content) {
        // Common ISO-8859-2 to UTF-8 character mappings
        return content
            // Fix lowercase Hungarian characters
            .replace("\u00E1", "á")  // á
            .replace("\u00E9", "é")  // é
            .replace("\u00ED", "í")  // í
            .replace("\u00F3", "ó")  // ó
            .replace("\u00FA", "ú")  // ú
            .replace("\u00F6", "ö")  // ö
            .replace("\u00FC", "ü")  // ü
            .replace("\u0151", "ő")  // ő
            .replace("\u0171", "ű")  // ű
            // Fix uppercase Hungarian characters
            .replace("\u00C1", "Á")  // Á
            .replace("\u00C9", "É")  // É
            .replace("\u00CD", "Í")  // Í
            .replace("\u00D3", "Ó")  // Ó
            .replace("\u00DA", "Ú")  // Ú
            .replace("\u00D6", "Ö")  // Ö
            .replace("\u00DC", "Ü")  // Ü
            .replace("\u0150", "Ő")  // Ő
            .replace("\u0170", "Ű")  // Ű
            // Fix common encoding artifacts
            .replace("Ã¡", "á")
            .replace("Ã©", "é")
            .replace("Ã­", "í")
            .replace("Ã³", "ó")
            .replace("Ãº", "ú")
            .replace("Ã¶", "ö")
            .replace("Ã¼", "ü")
            .replace("Å'", "ő")
            .replace("Å±", "ű");
    }
    
    /**
     * Basic XML validation check.
     */
    private boolean isValidXml(String xml) {
        try {
            // Basic check for well-formed XML
            return xml.contains("<?xml") && 
                   xml.contains("<") && 
                   xml.contains(">") &&
                   !xml.contains("\uFFFD");
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.debug("SOAP fault occurred in PUPHAX response");
        return true;
    }
    
    @Override
    public void close(MessageContext context) {
        // Cleanup if needed
    }
    
    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}