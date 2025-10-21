package com.puphax.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * SOAP handler to intercept PUPHAX responses before encoding issues occur.
 * This captures the raw XML response for manual parsing.
 */
public class PuphaxResponseInterceptor implements SOAPHandler<SOAPMessageContext> {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxResponseInterceptor.class);
    private static final ThreadLocal<String> lastRawResponse = new ThreadLocal<>();
    
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        
        if (!outbound) { // This is an inbound response
            try {
                SOAPMessage message = context.getMessage();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                message.writeTo(out);
                String rawXml = out.toString("UTF-8");
                
                logger.debug("Intercepted PUPHAX raw response: {} characters", rawXml.length());
                logger.debug("Raw XML first 500 chars: {}", 
                    rawXml.length() > 500 ? rawXml.substring(0, 500) + "..." : rawXml);
                
                // Store for later retrieval
                lastRawResponse.set(rawXml);
                
            } catch (Exception e) {
                logger.warn("Failed to intercept PUPHAX response: {}", e.getMessage());
            }
        }
        
        return true; // Continue processing
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
    
    /**
     * Get the last intercepted raw response.
     */
    public static String getLastRawResponse() {
        return lastRawResponse.get();
    }
    
    /**
     * Clear the stored response.
     */
    public static void clearLastRawResponse() {
        lastRawResponse.remove();
    }
}