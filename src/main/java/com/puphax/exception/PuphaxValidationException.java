package com.puphax.exception;

/**
 * Exception thrown for input validation errors.
 * 
 * This exception is thrown when request parameters fail validation
 * before being sent to the PUPHAX service, such as invalid ATC codes
 * or malformed search terms.
 */
public class PuphaxValidationException extends PuphaxServiceException {
    
    private final String fieldName;
    private final Object rejectedValue;
    
    public PuphaxValidationException(String fieldName, Object rejectedValue, String message) {
        super("VALIDATION_ERROR", message);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
}