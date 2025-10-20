package com.puphax.client;

/**
 * Mock SOAP service interface for PUPHAX integration.
 * This interface represents the generated JAX-WS client that would normally
 * be created by the wsimport Maven plugin from the PUPHAX WSDL.
 * 
 * In a full Maven environment, this would be auto-generated from:
 * - WSDL: src/main/resources/wsdl/PUPHAXWS.wsdl
 * - Package: com.puphax.client
 * 
 * The actual implementation will be provided by the PuphaxSoapClient wrapper.
 */
public interface PuphaxServiceMock {
    
    /**
     * Search for drugs by name or active ingredient.
     * 
     * @param searchTerm The drug name or active ingredient to search for
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter
     * @return XML response containing drug information
     */
    String searchDrugs(String searchTerm, String manufacturer, String atcCode);
    
    /**
     * Get detailed information for a specific drug.
     * 
     * @param drugId The unique drug identifier
     * @return XML response containing detailed drug information
     */
    String getDrugDetails(String drugId);
    
    /**
     * Check service availability.
     * 
     * @return Service status information
     */
    String getServiceStatus();
}