package com.puphax.service;

import com.puphax.model.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DrugService pagination logic.
 * 
 * These tests verify that pagination functionality works correctly
 * with different page sizes, page numbers, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class DrugServicePaginationTest {
    
    @Mock
    private PuphaxSoapClient mockSoapClient;
    
    private DrugService drugService;
    
    @BeforeEach
    void setUp() {
        drugService = new DrugService(mockSoapClient);
    }
    
    @Test
    void searchDrugs_FirstPage_ReturnsCorrectPagination() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(5);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 0, 3, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(3, response.drugs().size()); // Page size limit
        
        PaginationInfo pagination = response.pagination();
        assertEquals(0, pagination.currentPage());
        assertEquals(3, pagination.pageSize());
        assertEquals(2, pagination.totalPages()); // 5 drugs / 3 per page = 2 pages (ceil)
        assertEquals(5, pagination.totalElements());
        assertFalse(pagination.hasPrevious());
        assertTrue(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_SecondPage_ReturnsCorrectPagination() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(5);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 1, 3, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(2, response.drugs().size()); // Remaining drugs on last page
        
        PaginationInfo pagination = response.pagination();
        assertEquals(1, pagination.currentPage());
        assertEquals(3, pagination.pageSize());
        assertEquals(2, pagination.totalPages());
        assertEquals(5, pagination.totalElements());
        assertTrue(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_PageBeyondResults_ReturnsEmptyPage() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(3);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 5, 10, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(0, response.drugs().size()); // No drugs on page beyond results
        
        PaginationInfo pagination = response.pagination();
        assertEquals(5, pagination.currentPage());
        assertEquals(10, pagination.pageSize());
        assertEquals(1, pagination.totalPages()); // 3 drugs / 10 per page = 1 page
        assertEquals(3, pagination.totalElements());
        assertTrue(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_SinglePageResults_CorrectPagination() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(2);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 0, 10, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(2, response.drugs().size());
        
        PaginationInfo pagination = response.pagination();
        assertEquals(0, pagination.currentPage());
        assertEquals(10, pagination.pageSize());
        assertEquals(1, pagination.totalPages());
        assertEquals(2, pagination.totalElements());
        assertFalse(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_EmptyResults_CorrectPagination() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(0);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When
        DrugSearchResponse response = drugService.searchDrugs("nonexistent", null, null, 0, 20, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(0, response.drugs().size());
        
        PaginationInfo pagination = response.pagination();
        assertEquals(0, pagination.currentPage());
        assertEquals(20, pagination.pageSize());
        assertEquals(0, pagination.totalPages());
        assertEquals(0, pagination.totalElements());
        assertFalse(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_ExactPageBoundary_CorrectPagination() throws Exception {
        // Given - exactly 6 drugs with page size 3 should give exactly 2 pages
        String mockXmlResponse = createMockXmlWithMultipleDrugs(6);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When - test second page
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 1, 3, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(3, response.drugs().size()); // Exactly 3 drugs on second page
        
        PaginationInfo pagination = response.pagination();
        assertEquals(1, pagination.currentPage());
        assertEquals(3, pagination.pageSize());
        assertEquals(2, pagination.totalPages()); // 6 drugs / 3 per page = exactly 2 pages
        assertEquals(6, pagination.totalElements());
        assertTrue(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_LargePageSize_SinglePage() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(10);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When - page size larger than total results
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 0, 50, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(10, response.drugs().size()); // All drugs fit on one page
        
        PaginationInfo pagination = response.pagination();
        assertEquals(0, pagination.currentPage());
        assertEquals(50, pagination.pageSize());
        assertEquals(1, pagination.totalPages());
        assertEquals(10, pagination.totalElements());
        assertFalse(pagination.hasPrevious());
        assertFalse(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_SmallPageSize_ManyPages() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(10);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When - very small page size
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 0, 1, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(1, response.drugs().size()); // Only 1 drug per page
        
        PaginationInfo pagination = response.pagination();
        assertEquals(0, pagination.currentPage());
        assertEquals(1, pagination.pageSize());
        assertEquals(10, pagination.totalPages()); // 10 drugs / 1 per page = 10 pages
        assertEquals(10, pagination.totalElements());
        assertFalse(pagination.hasPrevious());
        assertTrue(pagination.hasNext());
    }
    
    @Test
    void searchDrugs_MiddlePage_CorrectNavigation() throws Exception {
        // Given
        String mockXmlResponse = createMockXmlWithMultipleDrugs(10);
        when(mockSoapClient.searchDrugsAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockXmlResponse));
        
        // When - middle page
        DrugSearchResponse response = drugService.searchDrugs("aspirin", null, null, 2, 3, "name", "ASC");
        
        // Then
        assertNotNull(response);
        assertEquals(3, response.drugs().size());
        
        PaginationInfo pagination = response.pagination();
        assertEquals(2, pagination.currentPage());
        assertEquals(3, pagination.pageSize());
        assertEquals(4, pagination.totalPages()); // 10 drugs / 3 per page = 4 pages (ceil)
        assertEquals(10, pagination.totalElements());
        assertTrue(pagination.hasPrevious()); // Has previous pages
        assertTrue(pagination.hasNext()); // Has next page
    }
    
    /**
     * Creates a mock XML response with the specified number of drugs.
     */
    private String createMockXmlWithMultipleDrugs(int drugCount) {
        if (drugCount == 0) {
            return """
                <?xml version="1.0" encoding="UTF-8"?>
                <drugSearchResponse>
                    <totalCount>0</totalCount>
                    <drugs></drugs>
                </drugSearchResponse>
                """;
        }
        
        StringBuilder drugs = new StringBuilder();
        for (int i = 1; i <= drugCount; i++) {
            drugs.append(String.format("""
                <drug>
                    <id>HU%06d</id>
                    <name>Drug %d 100mg tabletta</name>
                    <manufacturer>Manufacturer %d</manufacturer>
                    <atcCode>N02BA%02d</atcCode>
                    <activeIngredients>
                        <ingredient>
                            <name>Active Ingredient %d</name>
                        </ingredient>
                    </activeIngredients>
                    <prescriptionRequired>false</prescriptionRequired>
                    <reimbursable>true</reimbursable>
                    <status>ACTIVE</status>
                </drug>
                """, i, i, (i % 3) + 1, i % 100, i));
        }
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>%d</totalCount>
                <drugs>%s</drugs>
            </drugSearchResponse>
            """, drugCount, drugs.toString());
    }
}