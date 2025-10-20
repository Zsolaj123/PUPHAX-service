package com.puphax.service;

import com.puphax.client.PuphaxServiceMock;
import com.puphax.exception.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

/**
 * Service wrapper for PUPHAX SOAP client with resilience patterns.
 * 
 * This service provides a robust wrapper around the JAX-WS generated
 * SOAP client, implementing circuit breaker, retry, and timeout patterns
 * for reliable integration with the PUPHAX service.
 */
@Service
public class PuphaxSoapClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxSoapClient.class);
    
    private final PuphaxServiceMock puphaxService;
    
    @Autowired
    public PuphaxSoapClient(PuphaxServiceMock puphaxService) {
        this.puphaxService = puphaxService;
    }
    
    /**
     * Search for drugs using resilience patterns.
     * 
     * @param searchTerm The drug name or active ingredient to search for
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter
     * @return XML response from PUPHAX service
     */
    @CircuitBreaker(name = "puphax-service", fallbackMethod = "searchDrugsFallback")
    @TimeLimiter(name = "puphax-service")
    @Retry(name = "puphax-service")
    public CompletableFuture<String> searchDrugsAsync(String searchTerm, String manufacturer, String atcCode) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Searching drugs: term={}, manufacturer={}, atcCode={}", 
                searchTerm, manufacturer, atcCode);
            
            try {
                String response = handleSoapCall(
                    () -> puphaxService.searchDrugs(searchTerm, manufacturer, atcCode),
                    "searchDrugs"
                );
                
                logger.debug("Drug search successful for term: {}", searchTerm);
                return response;
                
            } catch (Exception e) {
                logger.error("Drug search failed for term: {}", searchTerm, e);
                throw e;
            }
        });
    }
    
    /**
     * Get detailed drug information using resilience patterns.
     * 
     * @param drugId The unique drug identifier
     * @return XML response with detailed drug information
     */
    @CircuitBreaker(name = "puphax-service", fallbackMethod = "getDrugDetailsFallback")
    @TimeLimiter(name = "puphax-service")
    @Retry(name = "puphax-service")
    public CompletableFuture<String> getDrugDetailsAsync(String drugId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Getting drug details for ID: {}", drugId);
            
            try {
                String response = handleSoapCall(
                    () -> puphaxService.getDrugDetails(drugId),
                    "getDrugDetails"
                );
                
                logger.debug("Drug details retrieved successfully for ID: {}", drugId);
                return response;
                
            } catch (Exception e) {
                logger.error("Failed to get drug details for ID: {}", drugId, e);
                throw e;
            }
        });
    }
    
    /**
     * Check PUPHAX service status.
     * 
     * @return Service status information
     */
    public String getServiceStatus() {
        logger.debug("Checking PUPHAX service status");
        
        try {
            return handleSoapCall(
                () -> puphaxService.getServiceStatus(),
                "getServiceStatus"
            );
        } catch (Exception e) {
            logger.error("Failed to get service status", e);
            throw e;
        }
    }
    
    /**
     * Generic SOAP call handler with error conversion.
     * 
     * @param soapCall The SOAP service call to execute
     * @param operation The operation name for logging
     * @return The SOAP response
     */
    private String handleSoapCall(java.util.function.Supplier<String> soapCall, String operation) {
        try {
            return soapCall.get();
            
        } catch (Exception e) {
            // Convert various exceptions to our custom exception hierarchy
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new PuphaxTimeoutException("Request timed out for operation: " + operation, e);
            } else if (e.getCause() instanceof ConnectException) {
                throw new PuphaxConnectionException("Connection failed for operation: " + operation, e);
            } else if (e.getMessage() != null && e.getMessage().contains("SOAP")) {
                throw new PuphaxSoapFaultException("SOAP_FAULT", e.getMessage(), e);
            } else {
                throw new PuphaxServiceException("Unexpected error in operation: " + operation, e);
            }
        }
    }
    
    /**
     * Fallback method for drug search when circuit breaker is open.
     */
    public CompletableFuture<String> searchDrugsFallback(String searchTerm, String manufacturer, 
                                                        String atcCode, Exception ex) {
        logger.warn("Circuit breaker activated for drug search: term={}, error={}", 
            searchTerm, ex.getMessage());
        
        return CompletableFuture.completedFuture("""
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>0</totalCount>
                <drugs></drugs>
                <error>
                    <code>SERVICE_UNAVAILABLE</code>
                    <message>PUPHAX service is temporarily unavailable. Please try again later.</message>
                </error>
            </drugSearchResponse>
            """);
    }
    
    /**
     * Fallback method for drug details when circuit breaker is open.
     */
    public CompletableFuture<String> getDrugDetailsFallback(String drugId, Exception ex) {
        logger.warn("Circuit breaker activated for drug details: drugId={}, error={}", 
            drugId, ex.getMessage());
        
        return CompletableFuture.completedFuture("""
            <?xml version="1.0" encoding="UTF-8"?>
            <drugDetailsResponse>
                <error>
                    <code>SERVICE_UNAVAILABLE</code>
                    <message>PUPHAX service is temporarily unavailable. Please try again later.</message>
                </error>
            </drugDetailsResponse>
            """);
    }
}