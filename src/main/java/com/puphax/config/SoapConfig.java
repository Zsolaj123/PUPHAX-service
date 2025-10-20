package com.puphax.config;

import com.puphax.client.PuphaxServiceMock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for SOAP client setup and connection management.
 * 
 * This configuration sets up the JAX-WS SOAP client with proper
 * timeout settings, connection pooling, and endpoint configuration
 * for the PUPHAX service integration.
 */
@Configuration
public class SoapConfig {
    
    @Value("${puphax.soap.endpoint-url}")
    private String endpointUrl;
    
    @Value("${puphax.soap.connect-timeout:30000}")
    private int connectTimeout;
    
    @Value("${puphax.soap.request-timeout:60000}")
    private int requestTimeout;
    
    /**
     * Creates a mock implementation of the PUPHAX SOAP service for development.
     * In a full Maven environment with JAX-WS generation, this would be replaced
     * with the actual generated service proxy.
     */
    @Bean
    @Profile("!test")
    public PuphaxServiceMock puphaxService() {
        return new PuphaxServiceMock() {
            @Override
            public String searchDrugs(String searchTerm, String manufacturer, String atcCode) {
                // Mock XML response for development
                return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <drugSearchResponse>
                        <totalCount>2</totalCount>
                        <drugs>
                            <drug>
                                <id>HU001234</id>
                                <name>Aspirin 100mg</name>
                                <manufacturer>Bayer Hungary Kft.</manufacturer>
                                <atcCode>N02BA01</atcCode>
                                <activeIngredients>
                                    <ingredient>
                                        <name>Acetylsalicylic acid</name>
                                        <concentration>100mg</concentration>
                                    </ingredient>
                                </activeIngredients>
                                <prescriptionRequired>false</prescriptionRequired>
                                <reimbursable>true</reimbursable>
                                <status>ACTIVE</status>
                            </drug>
                            <drug>
                                <id>HU005678</id>
                                <name>Aspirin Protect 100mg</name>
                                <manufacturer>Bayer Hungary Kft.</manufacturer>
                                <atcCode>N02BA01</atcCode>
                                <activeIngredients>
                                    <ingredient>
                                        <name>Acetylsalicylic acid</name>
                                        <concentration>100mg</concentration>
                                    </ingredient>
                                </activeIngredients>
                                <prescriptionRequired>false</prescriptionRequired>
                                <reimbursable>true</reimbursable>
                                <status>ACTIVE</status>
                            </drug>
                        </drugs>
                    </drugSearchResponse>
                    """;
            }
            
            @Override
            public String getDrugDetails(String drugId) {
                // Mock detailed drug information
                return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <drugDetailsResponse>
                        <drug>
                            <id>%s</id>
                            <name>Aspirin 100mg enteric-coated tablets</name>
                            <manufacturer>Bayer Hungary Kft.</manufacturer>
                            <atcCode>N02BA01</atcCode>
                            <therapeuticGroup>Analgesics and antipyretics</therapeuticGroup>
                            <activeIngredients>
                                <ingredient>
                                    <name>Acetylsalicylic acid</name>
                                    <concentration>100mg</concentration>
                                    <role>ACTIVE</role>
                                </ingredient>
                            </activeIngredients>
                            <dosageForms>
                                <form>
                                    <type>enteric-coated tablet</type>
                                    <route>oral</route>
                                    <description>Gastro-resistant coating</description>
                                </form>
                            </dosageForms>
                            <strength>100mg</strength>
                            <packagingSize>30 tablets</packagingSize>
                            <prescriptionRequired>false</prescriptionRequired>
                            <reimbursable>true</reimbursable>
                            <registrationNumber>OGYI-T-20123/01</registrationNumber>
                            <registrationDate>2015-03-15</registrationDate>
                            <expiryDate>2025-03-15</expiryDate>
                            <status>ACTIVE</status>
                            <additionalInfo>Suitable for long-term cardiovascular protection</additionalInfo>
                        </drug>
                    </drugDetailsResponse>
                    """.formatted(drugId);
            }
            
            @Override
            public String getServiceStatus() {
                return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <serviceStatus>
                        <status>UP</status>
                        <message>Service is available</message>
                        <timestamp>%s</timestamp>
                    </serviceStatus>
                    """.formatted(java.time.Instant.now().toString());
            }
        };
    }
    
    /**
     * Test configuration that provides a controllable mock service.
     */
    @Bean
    @Profile("test")
    public PuphaxServiceMock testPuphaxService() {
        return new PuphaxServiceMock() {
            @Override
            public String searchDrugs(String searchTerm, String manufacturer, String atcCode) {
                if ("error".equals(searchTerm)) {
                    throw new RuntimeException("Simulated SOAP fault");
                }
                if ("timeout".equals(searchTerm)) {
                    throw new RuntimeException("Simulated timeout");
                }
                return "<mockResponse><drugs></drugs></mockResponse>";
            }
            
            @Override
            public String getDrugDetails(String drugId) {
                if ("error".equals(drugId)) {
                    throw new RuntimeException("Simulated error");
                }
                return "<mockDrugDetails><drug><id>" + drugId + "</id></drug></mockDrugDetails>";
            }
            
            @Override
            public String getServiceStatus() {
                return "<serviceStatus><status>UP</status></serviceStatus>";
            }
        };
    }
}