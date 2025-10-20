package com.puphax.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the PUPHAX REST API.
 * 
 * This handler provides centralized exception handling with
 * proper error response formatting, logging, and correlation IDs
 * for debugging and monitoring purposes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle PUPHAX service exceptions.
     */
    @ExceptionHandler(PuphaxServiceException.class)
    public ResponseEntity<ApiErrorResponse> handlePuphaxServiceException(
            PuphaxServiceException ex, WebRequest request) {
        
        String correlationId = generateCorrelationId();
        HttpStatus status = determineHttpStatus(ex);
        
        logger.error("PUPHAX service error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            Instant.now(),
            status.value(),
            determineErrorType(ex),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            correlationId
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String correlationId = generateCorrelationId();
        
        logger.warn("Validation error [{}]: {}", correlationId, ex.getMessage());
        
        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Request validation failed",
            request.getDescription(false).replace("uri=", ""),
            correlationId,
            fieldErrors
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle PUPHAX validation exceptions.
     */
    @ExceptionHandler(PuphaxValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handlePuphaxValidationException(
            PuphaxValidationException ex, WebRequest request) {
        
        String correlationId = generateCorrelationId();
        
        logger.warn("PUPHAX validation error [{}]: {}", correlationId, ex.getMessage());
        
        List<FieldErrorDto> fieldErrors = List.of(
            new FieldErrorDto(ex.getFieldName(), ex.getRejectedValue(), ex.getMessage())
        );
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            correlationId,
            fieldErrors
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        String correlationId = generateCorrelationId();
        
        logger.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please contact support with correlation ID: " + correlationId,
            request.getDescription(false).replace("uri=", ""),
            correlationId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Determine HTTP status based on exception type.
     */
    private HttpStatus determineHttpStatus(PuphaxServiceException ex) {
        return switch (ex.getErrorCode()) {
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "CONNECTION_FAILED", "TIMEOUT" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "SOAP_FAULT" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Determine error type for response.
     */
    private String determineErrorType(PuphaxServiceException ex) {
        return switch (ex.getErrorCode()) {
            case "VALIDATION_ERROR" -> "Bad Request";
            case "CONNECTION_FAILED" -> "Service Unavailable";
            case "TIMEOUT" -> "Gateway Timeout";
            case "SOAP_FAULT" -> "Bad Gateway";
            default -> "Internal Server Error";
        };
    }
    
    /**
     * Map Spring validation field error to our DTO.
     */
    private FieldErrorDto mapFieldError(FieldError fieldError) {
        return new FieldErrorDto(
            fieldError.getField(),
            fieldError.getRejectedValue(),
            fieldError.getDefaultMessage()
        );
    }
    
    /**
     * Generate unique correlation ID for request tracing.
     */
    private String generateCorrelationId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
}