package com.puphax.exception;

/**
 * Exception thrown when connection to PUPHAX service fails.
 * 
 * This exception indicates network connectivity issues or
 * that the PUPHAX service endpoint is unreachable.
 */
public class PuphaxConnectionException extends PuphaxServiceException {
    
    public PuphaxConnectionException(String message) {
        super("CONNECTION_FAILED", message);
    }
    
    public PuphaxConnectionException(String message, Throwable cause) {
        super("CONNECTION_FAILED", message, cause);
    }
}