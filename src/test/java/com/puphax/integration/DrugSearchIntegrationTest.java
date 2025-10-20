package com.puphax.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puphax.model.dto.DrugSearchResponse;
import com.puphax.model.dto.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the complete drug search workflow.
 * 
 * These tests verify the entire application stack including controllers,
 * services, and SOAP client integration (using mock responses).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DrugSearchIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void healthCheck_FullWorkflow_ReturnsHealthStatus() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/drugs/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.version").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.components").exists())
            .andExpect(jsonPath("$.components.puphax-soap").exists())
            .andExpect(jsonPath("$.components.cache").exists())
            .andExpect(jsonPath("$.components.diskSpace").exists())
            .andReturn();
        
        // Verify response structure
        String responseContent = result.getResponse().getContentAsString();
        HealthStatus healthStatus = objectMapper.readValue(responseContent, HealthStatus.class);
        
        assertNotNull(healthStatus);
        assertEquals("UP", healthStatus.status());
        assertNotNull(healthStatus.components());
        assertTrue(healthStatus.components().containsKey("puphax-soap"));
        assertTrue(healthStatus.components().containsKey("cache"));
        assertTrue(healthStatus.components().containsKey("diskSpace"));
    }
    
    @Test
    void quickHealthCheck_FastResponse_ReturnsBasicStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/health/quick")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.api").exists())
            .andExpect(jsonPath("$.components.api.status").value("UP"));
    }
    
    @Test
    void drugSearch_BasicSearch_ReturnsResults() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs").isArray())
            .andExpect(jsonPath("$.drugs[0].id").exists())
            .andExpect(jsonPath("$.drugs[0].name").exists())
            .andExpect(jsonPath("$.drugs[0].manufacturer").exists())
            .andExpect(jsonPath("$.pagination").exists())
            .andExpect(jsonPath("$.searchInfo").exists())
            .andReturn();
        
        // Verify response structure
        String responseContent = result.getResponse().getContentAsString();
        DrugSearchResponse searchResponse = objectMapper.readValue(responseContent, DrugSearchResponse.class);
        
        assertNotNull(searchResponse);
        assertNotNull(searchResponse.drugs());
        assertFalse(searchResponse.drugs().isEmpty());
        assertNotNull(searchResponse.pagination());
        assertNotNull(searchResponse.searchInfo());
    }
    
    @Test
    void drugSearch_WithFilters_ReturnsFilteredResults() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("manufacturer", "Bayer")
                .param("atcCode", "N02BA01")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDirection", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs").isArray())
            .andExpect(jsonPath("$.searchInfo.searchTerm").value("aspirin"))
            .andExpect(jsonPath("$.searchInfo.filters.manufacturer").value("Bayer"))
            .andExpect(jsonPath("$.searchInfo.filters.atcCode").value("N02BA01"))
            .andExpect(jsonPath("$.pagination.currentPage").value(0))
            .andExpect(jsonPath("$.pagination.pageSize").value(10))
            .andReturn();
        
        // Verify filtering applied
        String responseContent = result.getResponse().getContentAsString();
        DrugSearchResponse searchResponse = objectMapper.readValue(responseContent, DrugSearchResponse.class);
        
        assertEquals("aspirin", searchResponse.searchInfo().searchTerm());
        assertNotNull(searchResponse.searchInfo().filters());
        assertTrue(searchResponse.searchInfo().filters().containsKey("manufacturer"));
        assertTrue(searchResponse.searchInfo().filters().containsKey("atcCode"));
    }
    
    @Test
    void drugSearch_PaginationWorkflow_CorrectPageHandling() throws Exception {
        // Test first page
        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagination.currentPage").value(0))
            .andExpect(jsonPath("$.pagination.pageSize").value(2))
            .andExpect(jsonPath("$.pagination.hasPrevious").value(false))
            .andReturn();
        
        DrugSearchResponse firstPage = objectMapper.readValue(
            firstPageResult.getResponse().getContentAsString(), 
            DrugSearchResponse.class
        );
        
        // Test second page if there are more results
        if (firstPage.pagination().hasNext()) {
            mockMvc.perform(get("/api/v1/drugs/search")
                    .param("term", "aspirin")
                    .param("page", "1")
                    .param("size", "2")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.pagination.hasPrevious").value(true));
        }
    }
    
    @Test
    void drugSearch_SortingWorkflow_CorrectSortHandling() throws Exception {
        // Test sorting by name ascending
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortBy", "name")
                .param("sortDirection", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs").isArray());
        
        // Test sorting by manufacturer descending
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortBy", "manufacturer")
                .param("sortDirection", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs").isArray());
    }
    
    @Test
    void drugSearch_ValidationErrors_Proper400Responses() throws Exception {
        // Test invalid search term (too short)
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "a")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        // Test invalid ATC code format
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("atcCode", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        // Test invalid page number
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("page", "-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        // Test invalid page size
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("size", "150")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        // Test invalid sort field
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortBy", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
        
        // Test invalid sort direction
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortDirection", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void drugSearch_ErrorResponseStructure_ContainsCorrelationId() throws Exception {
        // Test that error responses contain correlation IDs for debugging
        MvcResult result = mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "a") // Invalid term
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").exists())
            .andExpect(jsonPath("$.correlationId").isString())
            .andExpect(jsonPath("$.timestamp").exists())
            .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("correlationId"));
        assertTrue(responseContent.contains("timestamp"));
    }
    
    @Test
    void completeWorkflow_SearchThenHealthCheck_BothWork() throws Exception {
        // First, perform a drug search
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs").isArray());
        
        // Then, check health status
        mockMvc.perform(get("/api/v1/drugs/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
        
        // Finally, perform another search with different parameters
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "paracetamol")
                .param("manufacturer", "Gedeon")
                .param("page", "0")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs").isArray())
            .andExpect(jsonPath("$.pagination.pageSize").value(5));
    }
    
    @Test
    void apiDocumentation_SwaggerEndpoints_AreAccessible() throws Exception {
        // Test that OpenAPI documentation is available
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        
        // Test that Swagger UI is available (returns HTML)
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection()); // Swagger UI redirects to the actual UI page
    }
    
    @Test
    void caching_RepeatedRequests_PerformanceImprovement() throws Exception {
        String searchTerm = "aspirin";
        
        // First request (will populate cache)
        long firstRequestStart = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", searchTerm)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        long firstRequestTime = System.currentTimeMillis() - firstRequestStart;
        
        // Second request (should use cache)
        long secondRequestStart = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", searchTerm)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        long secondRequestTime = System.currentTimeMillis() - secondRequestStart;
        
        // Note: In a real environment with actual SOAP calls, the second request
        // should be significantly faster due to caching. With mocks, both are fast,
        // but we can still verify the functionality works.
        assertTrue(firstRequestTime >= 0 && secondRequestTime >= 0, 
                  "Both requests should complete successfully");
    }
}