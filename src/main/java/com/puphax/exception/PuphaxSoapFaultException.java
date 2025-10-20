package com.puphax.exception;

/**
 * Exception thrown when the PUPHAX SOAP service returns a SOAP fault.
 * 
 * This exception encapsulates SOAP fault information and provides
 * specific handling for SOAP-level errors returned by the PUPHAX service.
 */
public class PuphaxSoapFaultException extends PuphaxServiceException {
    
    private final String faultCode;
    private final String faultString;
    
    public PuphaxSoapFaultException(String faultCode, String faultString) {
        super("SOAP_FAULT", "SOAP fault: " + faultString);
        this.faultCode = faultCode;
        this.faultString = faultString;
    }
    
    public PuphaxSoapFaultException(String faultCode, String faultString, Throwable cause) {
        super("SOAP_FAULT", "SOAP fault: " + faultString, cause);
        this.faultCode = faultCode;
        this.faultString = faultString;
    }
    
    public String getFaultCode() {
        return faultCode;
    }
    
    public String getFaultString() {
        return faultString;
    }
}