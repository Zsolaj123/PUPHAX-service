package com.puphax.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for enhanced logging and request correlation.
 * 
 * This class provides methods for managing Mapped Diagnostic Context (MDC)
 * to enable structured logging and request tracing across the application.
 */
public class LoggingUtils {
    
    // MDC keys for consistent logging
    public static final String CORRELATION_ID = "correlationId";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION = "operation";
    public static final String SEARCH_TERM = "searchTerm";
    public static final String MANUFACTURER = "manufacturer";
    public static final String ATC_CODE = "atcCode";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String RESPONSE_TIME_MS = "responseTimeMs";
    public static final String RESULT_COUNT = "resultCount";
    public static final String ERROR_CODE = "errorCode";
    public static final String CLIENT_IP = "clientIp";
    
    private LoggingUtils() {
        // Utility class
    }
    
    /**
     * Generates a unique correlation ID for request tracking.
     */
    public static String generateCorrelationId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Sets up MDC for a drug search operation.
     */
    public static void setupSearchContext(String correlationId, String searchTerm, 
                                        String manufacturer, String atcCode, 
                                        int page, int size) {
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(OPERATION, "drug-search");
        MDC.put(SEARCH_TERM, searchTerm);
        if (manufacturer != null) {
            MDC.put(MANUFACTURER, manufacturer);
        }
        if (atcCode != null) {
            MDC.put(ATC_CODE, atcCode);
        }
        MDC.put(PAGE, String.valueOf(page));
        MDC.put(SIZE, String.valueOf(size));
    }
    
    /**
     * Sets up MDC for a health check operation.
     */
    public static void setupHealthCheckContext(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(OPERATION, "health-check");
    }
    
    /**
     * Sets up MDC for error tracking.
     */
    public static void setupErrorContext(String correlationId, String errorCode, String operation) {
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(ERROR_CODE, errorCode);
        if (operation != null) {
            MDC.put(OPERATION, operation);
        }
    }
    
    /**
     * Sets response time in MDC for performance tracking.
     */
    public static void setResponseTime(long responseTimeMs) {
        MDC.put(RESPONSE_TIME_MS, String.valueOf(responseTimeMs));
    }
    
    /**
     * Sets result count in MDC for tracking search effectiveness.
     */
    public static void setResultCount(int resultCount) {
        MDC.put(RESULT_COUNT, String.valueOf(resultCount));
    }
    
    /**
     * Sets client IP in MDC for tracking request origins.
     */
    public static void setClientIp(String clientIp) {
        if (clientIp != null && !clientIp.isEmpty()) {
            MDC.put(CLIENT_IP, clientIp);
        }
    }
    
    /**
     * Clears all MDC context to prevent memory leaks.
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clears specific MDC keys.
     */
    public static void clearKeys(String... keys) {
        for (String key : keys) {
            MDC.remove(key);
        }
    }
    
    /**
     * Gets the current correlation ID from MDC.
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
    
    /**
     * Executes an operation with temporary MDC context.
     */
    public static <T> T withContext(String correlationId, String operation, java.util.function.Supplier<T> action) {
        String previousCorrelationId = MDC.get(CORRELATION_ID);
        String previousOperation = MDC.get(OPERATION);
        
        try {
            MDC.put(CORRELATION_ID, correlationId);
            MDC.put(OPERATION, operation);
            return action.get();
        } finally {
            // Restore previous context
            if (previousCorrelationId != null) {
                MDC.put(CORRELATION_ID, previousCorrelationId);
            } else {
                MDC.remove(CORRELATION_ID);
            }
            
            if (previousOperation != null) {
                MDC.put(OPERATION, previousOperation);
            } else {
                MDC.remove(OPERATION);
            }
        }
    }
    
    /**
     * Executes a runnable with temporary MDC context.
     */
    public static void withContext(String correlationId, String operation, Runnable action) {
        withContext(correlationId, operation, () -> {
            action.run();
            return null;
        });
    }
    
    /**
     * Creates a context map for structured logging.
     */
    public static java.util.Map<String, String> createContextMap(String correlationId, String operation) {
        java.util.Map<String, String> context = new java.util.HashMap<>();
        context.put(CORRELATION_ID, correlationId);
        context.put(OPERATION, operation);
        return context;
    }
}