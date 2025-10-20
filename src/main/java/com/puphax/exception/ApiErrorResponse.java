package com.puphax.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Standardized API error response format.
 * 
 * This class provides a consistent structure for all error responses
 * from the PUPHAX REST API, including correlation IDs for tracing
 * and debugging purposes.
 */
public record ApiErrorResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant timestamp,
    
    int status,
    String error,
    String message,
    String path,
    String correlationId
) {
    
    /**
     * Create an error response for service unavailable scenarios.
     */
    public static ApiErrorResponse serviceUnavailable(String path, String correlationId) {
        return new ApiErrorResponse(
            Instant.now(),
            503,
            "Service Unavailable",
            "PUPHAX service is temporarily unavailable. Please try again later.",
            path,
            correlationId
        );
    }
    
    /**
     * Create an error response for validation failures.
     */
    public static ApiErrorResponse badRequest(String message, String path, String correlationId) {
        return new ApiErrorResponse(
            Instant.now(),
            400,
            "Bad Request",
            message,
            path,
            correlationId
        );
    }
    
    /**
     * Create an error response for not found scenarios.
     */
    public static ApiErrorResponse notFound(String message, String path, String correlationId) {
        return new ApiErrorResponse(
            Instant.now(),
            404,
            "Not Found",
            message,
            path,
            correlationId
        );
    }
}