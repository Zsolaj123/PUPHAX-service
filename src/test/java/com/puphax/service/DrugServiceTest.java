package com.puphax.service;

import com.puphax.model.dto.*;
import com.puphax.exception.PuphaxServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DrugService with mocked SOAP responses.
 * 
 * These tests verify the business logic and SOAP response
 * parsing works correctly with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
class DrugServiceTest {
    
    @Mock
    private PuphaxSoapClient soapClient;
    
    @Mock
    private PuphaxRealDataService realDataService;
    
    private DrugService drugService;
    
    @BeforeEach
    void setUp() {
        drugService = new DrugService(soapClient, realDataService);
    }
    
    @Test
    void searchDrugs_ValidRequest_ReturnsSearchResults() {
        // Given
        String mockXmlResponse = """
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
                            </ingredient>
                        </activeIngredients>
                        <prescriptionRequired>false</prescriptionRequired>
                        <reimbursable>true</reimbursable>
                        <status>ACTIVE</status>
                    </drug>
                </drugs>
            </drugSearchResponse>
            """;
        
        when(soapClient.searchDrugsAsync(eq("aspirin"), isNull(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse result = drugService.searchDrugs("aspirin", null, null, 0, 20, "name", "ASC");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.drugs().size());
        assertEquals("HU001234", result.drugs().get(0).id());
        assertEquals("Aspirin 100mg", result.drugs().get(0).name());
        assertEquals("Bayer Hungary Kft.", result.drugs().get(0).manufacturer());
        assertEquals("N02BA01", result.drugs().get(0).atcCode());
        assertFalse(result.drugs().get(0).prescriptionRequired());
        assertTrue(result.drugs().get(0).reimbursable());
        assertEquals(DrugSummary.DrugStatus.ACTIVE, result.drugs().get(0).status());
        
        assertEquals(0, result.pagination().currentPage());
        assertEquals(20, result.pagination().pageSize());
        assertEquals(1, result.pagination().totalElements());
        
        assertEquals("aspirin", result.searchInfo().searchTerm());
        assertFalse(result.searchInfo().cacheHit());
        
        verify(soapClient).searchDrugsAsync("aspirin", null, null);
    }
    
    @Test
    void searchDrugs_WithFilters_PassesFiltersToSoapClient() {
        // Given
        String mockXmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>1</totalCount>
                <drugs>
                    <drug>
                        <id>HU001234</id>
                        <name>Aspirin 100mg</name>
                        <manufacturer>Bayer Hungary Kft.</manufacturer>
                        <atcCode>N02BA01</atcCode>
                        <activeIngredients>
                            <ingredient><name>Acetylsalicylic acid</name></ingredient>
                        </activeIngredients>
                        <prescriptionRequired>false</prescriptionRequired>
                        <reimbursable>true</reimbursable>
                        <status>ACTIVE</status>
                    </drug>
                </drugs>
            </drugSearchResponse>
            """;
        
        when(soapClient.searchDrugsAsync(eq("aspirin"), eq("Bayer"), eq("N02BA01")))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse result = drugService.searchDrugs("aspirin", "Bayer", "N02BA01", 0, 10, "name", "ASC");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.drugs().size());
        assertTrue(result.searchInfo().filters().containsKey("manufacturer"));
        assertTrue(result.searchInfo().filters().containsKey("atcCode"));
        assertEquals("Bayer", result.searchInfo().filters().get("manufacturer"));
        assertEquals("N02BA01", result.searchInfo().filters().get("atcCode"));
        
        verify(soapClient).searchDrugsAsync("aspirin", "Bayer", "N02BA01");
    }
    
    @Test
    void searchDrugs_EmptyResponse_ReturnsEmptyResults() {
        // Given
        String mockXmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>0</totalCount>
                <drugs></drugs>
            </drugSearchResponse>
            """;
        
        when(soapClient.searchDrugsAsync(eq("nonexistentdrug"), isNull(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse result = drugService.searchDrugs("nonexistentdrug", null, null, 0, 20, "name", "ASC");
        
        // Then
        assertNotNull(result);
        assertTrue(result.drugs().isEmpty());
        assertEquals(0, result.pagination().totalElements());
        assertEquals("nonexistentdrug", result.searchInfo().searchTerm());
        
        verify(soapClient).searchDrugsAsync("nonexistentdrug", null, null);
    }
    
    @Test
    void searchDrugs_SoapClientException_ThrowsPuphaxServiceException() {
        // Given
        when(soapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.failedFuture(new PuphaxServiceException("SOAP service error")));
        
        // When & Then
        assertThrows(PuphaxServiceException.class, () -> {
            drugService.searchDrugs("aspirin", null, null, 0, 20, "name", "ASC");
        });
        
        verify(soapClient).searchDrugsAsync("aspirin", null, null);
    }
    
    @Test
    void searchDrugs_MalformedXmlResponse_HandlesGracefully() {
        // Given
        String malformedXml = "<invalid>xml response</malformed>";
        
        when(soapClient.searchDrugsAsync(eq("aspirin"), isNull(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(malformedXml));
        
        // When & Then
        assertThrows(PuphaxServiceException.class, () -> {
            drugService.searchDrugs("aspirin", null, null, 0, 20, "name", "ASC");
        });
        
        verify(soapClient).searchDrugsAsync("aspirin", null, null);
    }
    
    @Test
    void searchDrugs_PaginationCalculation_IsCorrect() {
        // Given
        String mockXmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>45</totalCount>
                <drugs>
                    <drug>
                        <id>HU001234</id>
                        <name>Aspirin 100mg</name>
                        <manufacturer>Bayer Hungary Kft.</manufacturer>
                        <atcCode>N02BA01</atcCode>
                        <activeIngredients>
                            <ingredient><name>Acetylsalicylic acid</name></ingredient>
                        </activeIngredients>
                        <prescriptionRequired>false</prescriptionRequired>
                        <reimbursable>true</reimbursable>
                        <status>ACTIVE</status>
                    </drug>
                </drugs>
            </drugSearchResponse>
            """;
        
        when(soapClient.searchDrugsAsync(eq("aspirin"), isNull(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse result = drugService.searchDrugs("aspirin", null, null, 1, 20, "name", "ASC");
        
        // Then
        PaginationInfo pagination = result.pagination();
        assertEquals(1, pagination.currentPage());
        assertEquals(20, pagination.pageSize());
        assertEquals(3, pagination.totalPages());  // 45 items / 20 per page = 3 pages
        assertEquals(45L, pagination.totalElements());
        assertTrue(pagination.hasNext());
        assertTrue(pagination.hasPrevious());
        
        verify(soapClient).searchDrugsAsync("aspirin", null, null);
    }
}