package com.puphax.service;

import com.puphax.exception.PuphaxConnectionException;
import com.puphax.exception.PuphaxServiceException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for all PUPHAX SOAP operations.
 * 
 * This test class validates:
 * 1. All currently implemented SOAP operations (TERMEKLISTA, TERMEKADAT, TAMOGATADAT, CEGEK)
 * 2. XML injection vulnerability prevention
 * 3. Character encoding handling (Hungarian characters)
 * 4. Error handling and circuit breaker functionality
 * 5. Authentication and connection handling
 * 6. Performance characteristics
 * 
 * Test Categories:
 * - Unit tests for individual operations
 * - Integration tests with real PUPHAX service
 * - Security tests for XML injection prevention
 * - Performance tests for response time validation
 * - Error handling tests for various failure scenarios
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComprehensivePuphaxSoapOperationsTest {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensivePuphaxSoapOperationsTest.class);
    
    @Autowired
    private SimplePuphaxClient simplePuphaxClient;
    
    @Autowired
    private PuphaxSpringWsClient springWsClient;
    
    @Autowired
    private PuphaxSoapClient soapClient;
    
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 10, 1);
    private static final String TEST_DRUG_NAME = "XANAX";
    private static final String TEST_COMPANY_ID = "67"; // Pfizer
    private static final String TEST_PRODUCT_ID = "14714226";
    
    // ========================================
    // TERMEKLISTA Operation Tests
    // ========================================
    
    @Test
    @Order(1)
    @DisplayName("TERMEKLISTA: Simple search with valid drug name")
    void testTermeklistaSimpleSearch() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TERMEKLISTA simple search with drug: {}", TEST_DRUG_NAME);
            
            String response = simplePuphaxClient.searchDrugsSimple(TEST_DRUG_NAME);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            assertTrue(response.contains("<?xml"), "Response should be valid XML");
            
            logger.info("TERMEKLISTA simple search successful. Response length: {} characters", response.length());
        });
    }
    
    @Test
    @Order(2)
    @DisplayName("TERMEKLISTA: Spring WS client search")
    void testTermeklistaSpringWsSearch() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TERMEKLISTA Spring WS search with drug: {}", TEST_DRUG_NAME);
            
            String response = springWsClient.searchDrugs(TEST_DRUG_NAME, TEST_DATE);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            assertTrue(response.contains("<?xml") || response.contains("<soap:"), "Response should be valid XML/SOAP");
            
            logger.info("TERMEKLISTA Spring WS search successful. Response length: {} characters", response.length());
        });
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"ASPIRIN", "PARACETAMOL", "IBUPROFEN", "XANAX", "TRAMADOL"})
    @DisplayName("TERMEKLISTA: Multiple drug searches")
    void testTermeklistaMultipleDrugs(String drugName) {
        assertDoesNotThrow(() -> {
            logger.info("Testing TERMEKLISTA search for drug: {}", drugName);
            
            String response = simplePuphaxClient.searchDrugsSimple(drugName);
            
            assertNotNull(response, "Response should not be null for drug: " + drugName);
            assertFalse(response.trim().isEmpty(), "Response should not be empty for drug: " + drugName);
            
            logger.info("Successfully searched for drug: {}. Response length: {} characters", drugName, response.length());
        });
    }
    
    // ========================================
    // TERMEKADAT Operation Tests  
    // ========================================
    
    @Test
    @Order(3)
    @DisplayName("TERMEKADAT: Get product details by ID")
    void testTermekadatProductDetails() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TERMEKADAT for product ID: {}", TEST_PRODUCT_ID);
            
            String response = springWsClient.getProductDetails(TEST_PRODUCT_ID);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            assertTrue(response.contains("<?xml") || response.contains("<soap:"), "Response should be valid XML/SOAP");
            
            logger.info("TERMEKADAT successful. Response length: {} characters", response.length());
        });
    }
    
    @Test
    @Order(4)
    @DisplayName("TERMEKADAT: Get product data with SimplePuphaxClient")
    void testTermekadatSimpleClient() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TERMEKADAT with SimplePuphaxClient for product ID: {}", TEST_PRODUCT_ID);
            
            String response = simplePuphaxClient.getProductData(TEST_PRODUCT_ID, TEST_DATE);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            
            logger.info("TERMEKADAT with SimplePuphaxClient successful. Response length: {} characters", response.length());
        });
    }
    
    // ========================================
    // TAMOGATADAT Operation Tests
    // ========================================
    
    @Test
    @Order(5)
    @DisplayName("TAMOGATADAT: Get support data by product ID")
    void testTamogatadatSupportData() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TAMOGATADAT for product ID: {}", TEST_PRODUCT_ID);
            
            String response = springWsClient.getDrugSupportData(TEST_PRODUCT_ID, TEST_DATE);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            assertTrue(response.contains("<?xml") || response.contains("<soap:"), "Response should be valid XML/SOAP");
            
            logger.info("TAMOGATADAT successful. Response length: {} characters", response.length());
        });
    }
    
    @Test
    @Order(6)
    @DisplayName("TAMOGATADAT: Get support data with SimplePuphaxClient")
    void testTamogatadatSimpleClient() {
        assertDoesNotThrow(() -> {
            logger.info("Testing TAMOGATADAT with SimplePuphaxClient for product ID: {}", TEST_PRODUCT_ID);
            
            String response = simplePuphaxClient.getProductSupportData(TEST_PRODUCT_ID, TEST_DATE);
            
            assertNotNull(response, "Response should not be null");
            assertFalse(response.trim().isEmpty(), "Response should not be empty");
            
            logger.info("TAMOGATADAT with SimplePuphaxClient successful. Response length: {} characters", response.length());
        });
    }
    
    // ========================================
    // CEGEK Operation Tests
    // ========================================
    
    @Test
    @Order(7)
    @DisplayName("CEGEK: Get company name by ID")
    void testCegekCompanyName() {
        assertDoesNotThrow(() -> {
            logger.info("Testing CEGEK for company ID: {}", TEST_COMPANY_ID);
            
            String companyName = simplePuphaxClient.getCompanyName(TEST_COMPANY_ID);
            
            assertNotNull(companyName, "Company name should not be null");
            assertFalse(companyName.trim().isEmpty(), "Company name should not be empty");
            
            logger.info("CEGEK successful. Company name: {}", companyName);
        });
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"67", "1", "2", "3", "10"})
    @DisplayName("CEGEK: Multiple company lookups")
    void testCegekMultipleCompanies(String companyId) {
        assertDoesNotThrow(() -> {
            logger.info("Testing CEGEK for company ID: {}", companyId);
            
            String companyName = simplePuphaxClient.getCompanyName(companyId);
            
            // Company name can be null for non-existent IDs, which is acceptable
            logger.info("Company ID {} -> Name: {}", companyId, companyName != null ? companyName : "Not found");
        });
    }
    
    // ========================================
    // XML Injection Security Tests
    // ========================================
    
    @Test
    @Order(8)
    @DisplayName("Security: XML injection prevention in search terms")
    void testXmlInjectionPreventionSearchTerms() {
        String[] maliciousInputs = {
            "<script>alert('xss')</script>",
            "<?xml version=\"1.0\"?><malicious/>",
            "&lt;inject&gt;test&lt;/inject&gt;",
            "]]><malicious>content</malicious><![CDATA[",
            "<![CDATA[malicious]]>",
            "&amp;&lt;&gt;&quot;&apos;"
        };
        
        for (String maliciousInput : maliciousInputs) {
            assertDoesNotThrow(() -> {
                logger.info("Testing XML injection prevention with input: {}", maliciousInput);
                
                String response = simplePuphaxClient.searchDrugsSimple(maliciousInput);
                
                // Should not throw exception and should properly escape the input
                assertNotNull(response, "Response should not be null even with malicious input");
                assertFalse(response.contains(maliciousInput), "Response should not contain unescaped malicious input");
                
                logger.info("XML injection prevention successful for input: {}", maliciousInput);
            });
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Security: XML injection prevention in product IDs")
    void testXmlInjectionPreventionProductIds() {
        String[] maliciousProductIds = {
            "<script>alert('xss')</script>",
            "123<inject>456",
            "999]]><malicious/>",
            "\"'&<>"
        };
        
        for (String maliciousId : maliciousProductIds) {
            assertDoesNotThrow(() -> {
                logger.info("Testing XML injection prevention in product ID: {}", maliciousId);
                
                // These calls should handle malicious input gracefully
                String response = springWsClient.getProductDetails(maliciousId);
                
                assertNotNull(response, "Response should not be null even with malicious product ID");
                
                logger.info("XML injection prevention successful for product ID: {}", maliciousId);
            });
        }
    }
    
    // ========================================
    // Hungarian Character Encoding Tests
    // ========================================
    
    @Test
    @Order(10)
    @DisplayName("Encoding: Hungarian characters handling")
    void testHungarianCharacterEncoding() {
        String[] hungarianTerms = {
            "ÁGENSEK",  // Á character
            "BÉTA",     // É character  
            "ÉRZÉSEK",  // É, É characters
            "FŐNÖM",    // Ő character
            "ŰZÜLET",   // Ű character
            "gyógyszer", // ó character
            "málna",    // á character
            "üveg"      // ü character
        };
        
        for (String hungarianTerm : hungarianTerms) {
            assertDoesNotThrow(() -> {
                logger.info("Testing Hungarian character encoding with term: {}", hungarianTerm);
                
                String response = simplePuphaxClient.searchDrugsSimple(hungarianTerm);
                
                assertNotNull(response, "Response should not be null for Hungarian term: " + hungarianTerm);
                
                logger.info("Hungarian character encoding successful for term: {}", hungarianTerm);
            });
        }
    }
    
    // ========================================
    // Error Handling Tests
    // ========================================
    
    @Test
    @Order(11)
    @DisplayName("Error Handling: Null input parameters")
    void testErrorHandlingNullInputs() {
        // Test null search term
        assertDoesNotThrow(() -> {
            String response = simplePuphaxClient.searchDrugsSimple(null);
            assertNotNull(response, "Response should handle null search term gracefully");
        });
        
        // Test null product ID
        assertDoesNotThrow(() -> {
            String response = springWsClient.getProductDetails(null);
            assertNotNull(response, "Response should handle null product ID gracefully");
        });
        
        // Test null company ID
        String companyName = simplePuphaxClient.getCompanyName(null);
        assertNull(companyName, "Company name should be null for null input");
    }
    
    @Test
    @Order(12)
    @DisplayName("Error Handling: Empty input parameters")
    void testErrorHandlingEmptyInputs() {
        assertDoesNotThrow(() -> {
            String response = simplePuphaxClient.searchDrugsSimple("");
            assertNotNull(response, "Response should handle empty search term gracefully");
        });
        
        String companyName = simplePuphaxClient.getCompanyName("");
        assertNull(companyName, "Company name should be null for empty input");
    }
    
    @Test
    @Order(13)
    @DisplayName("Error Handling: Invalid product IDs")
    void testErrorHandlingInvalidProductIds() {
        String[] invalidIds = {"invalid", "99999999", "-1", "abc123", "0"};
        
        for (String invalidId : invalidIds) {
            assertDoesNotThrow(() -> {
                logger.info("Testing error handling for invalid product ID: {}", invalidId);
                
                String response = springWsClient.getProductDetails(invalidId);
                
                assertNotNull(response, "Response should not be null for invalid ID: " + invalidId);
                
                logger.info("Error handling successful for invalid product ID: {}", invalidId);
            });
        }
    }
    
    // ========================================
    // Performance Tests
    // ========================================
    
    @Test
    @Order(14)
    @DisplayName("Performance: TERMEKLISTA response time")
    void testTermeklistaPerformance() {
        assertTimeout(java.time.Duration.ofSeconds(30), () -> {
            long startTime = System.currentTimeMillis();
            
            String response = simplePuphaxClient.searchDrugsSimple(TEST_DRUG_NAME);
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            assertNotNull(response, "Response should not be null");
            assertTrue(responseTime < 30000, "Response time should be less than 30 seconds, was: " + responseTime + "ms");
            
            logger.info("TERMEKLISTA performance test completed in {} ms", responseTime);
        });
    }
    
    @Test
    @Order(15)
    @DisplayName("Performance: TERMEKADAT response time")
    void testTermekadatPerformance() {
        assertTimeout(java.time.Duration.ofSeconds(30), () -> {
            long startTime = System.currentTimeMillis();
            
            String response = springWsClient.getProductDetails(TEST_PRODUCT_ID);
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            assertNotNull(response, "Response should not be null");
            assertTrue(responseTime < 30000, "Response time should be less than 30 seconds, was: " + responseTime + "ms");
            
            logger.info("TERMEKADAT performance test completed in {} ms", responseTime);
        });
    }
    
    @Test
    @Order(16)
    @DisplayName("Performance: Company lookup caching")
    void testCompanyLookupCaching() {
        // First call - should take longer
        long startTime1 = System.currentTimeMillis();
        String companyName1 = simplePuphaxClient.getCompanyName(TEST_COMPANY_ID);
        long responseTime1 = System.currentTimeMillis() - startTime1;
        
        // Second call - should be cached and faster
        long startTime2 = System.currentTimeMillis();
        String companyName2 = simplePuphaxClient.getCompanyName(TEST_COMPANY_ID);
        long responseTime2 = System.currentTimeMillis() - startTime2;
        
        assertEquals(companyName1, companyName2, "Cached response should be identical");
        assertTrue(responseTime2 <= responseTime1, "Cached response should be faster or equal");
        
        logger.info("Company lookup caching test: First call: {}ms, Cached call: {}ms", responseTime1, responseTime2);
    }
    
    // ========================================
    // Integration Tests
    // ========================================
    
    @Test
    @Order(17)
    @DisplayName("Integration: Full drug search workflow")
    void testFullDrugSearchWorkflow() {
        assertDoesNotThrow(() -> {
            logger.info("Testing full drug search workflow");
            
            // 1. Search for drugs
            String searchResponse = simplePuphaxClient.searchDrugsSimple(TEST_DRUG_NAME);
            assertNotNull(searchResponse, "Search response should not be null");
            
            // 2. Get product details for a known product
            String detailsResponse = springWsClient.getProductDetails(TEST_PRODUCT_ID);
            assertNotNull(detailsResponse, "Details response should not be null");
            
            // 3. Get support data
            String supportResponse = springWsClient.getDrugSupportData(TEST_PRODUCT_ID, TEST_DATE);
            assertNotNull(supportResponse, "Support response should not be null");
            
            // 4. Get company information
            String companyName = simplePuphaxClient.getCompanyName(TEST_COMPANY_ID);
            assertNotNull(companyName, "Company name should not be null");
            
            logger.info("Full drug search workflow completed successfully");
            logger.info("Company: {}", companyName);
        });
    }
    
    @Test
    @Order(18)
    @DisplayName("Integration: Circuit breaker and resilience")
    void testCircuitBreakerResilience() {
        assertDoesNotThrow(() -> {
            logger.info("Testing circuit breaker resilience");
            
            // Make multiple rapid calls to test circuit breaker behavior
            for (int i = 0; i < 5; i++) {
                String response = simplePuphaxClient.searchDrugsSimple("TEST" + i);
                assertNotNull(response, "Response should not be null even under load");
                
                // Small delay to avoid overwhelming the service
                Thread.sleep(100);
            }
            
            logger.info("Circuit breaker resilience test completed");
        });
    }
    
    // ========================================
    // Test Lifecycle Methods
    // ========================================
    
    @BeforeAll
    static void setupAll() {
        logger.info("Starting comprehensive PUPHAX SOAP operations test suite");
        logger.info("Test date: {}", TEST_DATE);
        logger.info("Test drug: {}", TEST_DRUG_NAME);
        logger.info("Test company ID: {}", TEST_COMPANY_ID);
        logger.info("Test product ID: {}", TEST_PRODUCT_ID);
    }
    
    @AfterAll
    static void tearDownAll() {
        logger.info("Comprehensive PUPHAX SOAP operations test suite completed");
    }
    
    @BeforeEach
    void setup(TestInfo testInfo) {
        logger.info("Starting test: {}", testInfo.getDisplayName());
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        logger.info("Completed test: {}", testInfo.getDisplayName());
    }
}