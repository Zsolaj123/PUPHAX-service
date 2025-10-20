package com.puphax.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Detailed validation error response format.
 * 
 * This class extends the basic error response to include
 * specific field validation errors, helping clients
 * understand exactly what went wrong with their request.
 */
public record ValidationErrorResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant timestamp,
    
    int status,
    String error,
    String message,
    String path,
    String correlationId,
    List<FieldErrorDto> fieldErrors
) {
}

/**
 * Individual field validation error details.
 */
record FieldErrorDto(
    String field,
    Object rejectedValue,
    String message
) {
}