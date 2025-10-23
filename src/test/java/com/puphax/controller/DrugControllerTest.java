package com.puphax.controller;

import com.puphax.service.DrugService;
import com.puphax.model.dto.DrugSearchResponse;
import com.puphax.model.dto.DrugSummary;
import com.puphax.model.dto.PaginationInfo;
import com.puphax.model.dto.SearchInfo;
import com.puphax.exception.PuphaxValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DrugController.
 * 
 * These tests verify the REST API endpoints work correctly
 * with proper request/response handling and error scenarios.
 */
@WebMvcTest(DrugController.class)
class DrugControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DrugService drugService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void searchDrugs_ValidRequest_ReturnsSearchResults() throws Exception {
        // Given
        DrugSummary drugSummary = DrugSummary.builder("HU001234", "Aspirin 100mg")
            .manufacturer("Bayer Hungary Kft.")
            .atcCode("N02BA01")
            .activeIngredients(List.of("Acetylsalicylic acid"))
            .activeIngredient("Acetylsalicylic acid")
            .prescriptionRequired(false)
            .reimbursable(true)
            .status(DrugSummary.DrugStatus.ACTIVE)
            .build();
        
        PaginationInfo pagination = new PaginationInfo(0, 20, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            java.util.Map.of(),
            245L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), isNull(), isNull(), eq(0), eq(20), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs").isArray())
            .andExpect(jsonPath("$.drugs[0].id").value("HU001234"))
            .andExpect(jsonPath("$.drugs[0].name").value("Aspirin 100mg"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1))
            .andExpect(jsonPath("$.searchInfo.searchTerm").value("aspirin"));
    }
    
    @Test
    void searchDrugs_MissingTerm_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void searchDrugs_InvalidPageSize_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("size", "150")  // Exceeds max of 100
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void searchDrugs_WithFilters_ReturnsFilteredResults() throws Exception {
        // Given
        DrugSummary drugSummary = DrugSummary.builder("HU001234", "Aspirin 100mg")
            .manufacturer("Bayer Hungary Kft.")
            .atcCode("N02BA01")
            .activeIngredients(List.of("Acetylsalicylic acid"))
            .activeIngredient("Acetylsalicylic acid")
            .prescriptionRequired(false)
            .reimbursable(true)
            .status(DrugSummary.DrugStatus.ACTIVE)
            .build();
        
        PaginationInfo pagination = new PaginationInfo(0, 20, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            java.util.Map.of("manufacturer", "Bayer"),
            189L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), eq("Bayer"), isNull(), eq(0), eq(20), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("manufacturer", "Bayer")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs[0].manufacturer").value("Bayer Hungary Kft."))
            .andExpect(jsonPath("$.searchInfo.filters.manufacturer").value("Bayer"));
    }
    
    @Test
    void searchDrugs_ServiceException_ReturnsServiceError() throws Exception {
        // Given
        when(drugService.searchDrugs(anyString(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
            .thenThrow(new PuphaxValidationException("term", "invalid", "Invalid search term"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.correlationId").exists());
    }
    
    @Test
    void searchDrugs_EmptyResults_ReturnsEmptyArray() throws Exception {
        // Given
        PaginationInfo pagination = new PaginationInfo(0, 20, 0, 0L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "nonexistentdrug",
            java.util.Map.of(),
            89L,
            true,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("nonexistentdrug"), isNull(), isNull(), eq(0), eq(20), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "nonexistentdrug")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drugs").isArray())
            .andExpect(jsonPath("$.drugs").isEmpty())
            .andExpect(jsonPath("$.pagination.totalElements").value(0))
            .andExpect(jsonPath("$.searchInfo.cacheHit").value(true));
    }
}