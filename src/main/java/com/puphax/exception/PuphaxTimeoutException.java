package com.puphax.exception;

/**
 * Exception thrown when PUPHAX service calls timeout.
 * 
 * This exception is thrown when the SOAP service call exceeds
 * the configured timeout duration, indicating potential
 * network issues or service unavailability.
 */
public class PuphaxTimeoutException extends PuphaxServiceException {
    
    public PuphaxTimeoutException(String message) {
        super("TIMEOUT", message);
    }
    
    public PuphaxTimeoutException(String message, Throwable cause) {
        super("TIMEOUT", message, cause);
    }
}