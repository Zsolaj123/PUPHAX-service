package com.puphax.exception;

/**
 * Base exception for PUPHAX service errors.
 * 
 * This is the root exception class for all PUPHAX-related errors,
 * providing a common base for handling various types of service failures.
 */
public class PuphaxServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public PuphaxServiceException(String message) {
        super(message);
        this.errorCode = "PUPHAX_ERROR";
    }
    
    public PuphaxServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PUPHAX_ERROR";
    }
    
    public PuphaxServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PuphaxServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}