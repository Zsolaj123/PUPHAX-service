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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Advanced unit tests for DrugController focusing on User Story 2 features.
 * 
 * These tests verify advanced search parameters, filtering, pagination,
 * and sorting functionality work correctly with proper error handling.
 */
@WebMvcTest(DrugController.class)
class DrugControllerAdvancedTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DrugService drugService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchDrugs_WithManufacturerFilter_ReturnsFilteredResults() throws Exception {
        // Given
        DrugSummary drugSummary = new DrugSummary(
            "HU001234",
            "Aspirin 100mg",
            "Bayer Hungary Kft.",
            "N02BA01",
            List.of("Acetylsalicylic acid"),
            false,
            true,
            DrugSummary.DrugStatus.ACTIVE
        );
        
        PaginationInfo pagination = new PaginationInfo(0, 20, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            Map.of("manufacturer", "Bayer"),
            156L,
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs[0].manufacturer").value("Bayer Hungary Kft."))
            .andExpect(jsonPath("$.searchInfo.filters.manufacturer").value("Bayer"));
    }

    @Test
    void searchDrugs_WithAtcCodeFilter_ReturnsFilteredResults() throws Exception {
        // Given
        DrugSummary drugSummary = new DrugSummary(
            "HU001234",
            "Aspirin 100mg",
            "Bayer Hungary Kft.",
            "N02BA01",
            List.of("Acetylsalicylic acid"),
            false,
            true,
            DrugSummary.DrugStatus.ACTIVE
        );
        
        PaginationInfo pagination = new PaginationInfo(0, 20, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            Map.of("atcCode", "N02BA01"),
            189L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), isNull(), eq("N02BA01"), eq(0), eq(20), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("atcCode", "N02BA01")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs[0].atcCode").value("N02BA01"))
            .andExpect(jsonPath("$.searchInfo.filters.atcCode").value("N02BA01"));
    }

    @Test
    void searchDrugs_WithBothFilters_ReturnsFilteredResults() throws Exception {
        // Given
        DrugSummary drugSummary = new DrugSummary(
            "HU001234",
            "Aspirin 100mg",
            "Bayer Hungary Kft.",
            "N02BA01",
            List.of("Acetylsalicylic acid"),
            false,
            true,
            DrugSummary.DrugStatus.ACTIVE
        );
        
        PaginationInfo pagination = new PaginationInfo(0, 10, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            Map.of("manufacturer", "Bayer", "atcCode", "N02BA01"),
            234L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), eq("Bayer"), eq("N02BA01"), eq(0), eq(10), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("manufacturer", "Bayer")
                .param("atcCode", "N02BA01")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs[0].manufacturer").value("Bayer Hungary Kft."))
            .andExpect(jsonPath("$.drugs[0].atcCode").value("N02BA01"))
            .andExpect(jsonPath("$.searchInfo.filters.manufacturer").value("Bayer"))
            .andExpect(jsonPath("$.searchInfo.filters.atcCode").value("N02BA01"))
            .andExpect(jsonPath("$.pagination.pageSize").value(10));
    }

    @Test
    void searchDrugs_WithCustomPagination_ReturnsCorrectPage() throws Exception {
        // Given
        DrugSummary drugSummary = new DrugSummary(
            "HU001235",
            "Aspirin 500mg",
            "Gedeon Richter",
            "N02BA01",
            List.of("Acetylsalicylic acid"),
            false,
            true,
            DrugSummary.DrugStatus.ACTIVE
        );
        
        PaginationInfo pagination = new PaginationInfo(2, 5, 10, 45L, true, true);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            Map.of(),
            178L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), isNull(), isNull(), eq(2), eq(5), eq("name"), eq("ASC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("page", "2")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.pagination.currentPage").value(2))
            .andExpect(jsonPath("$.pagination.pageSize").value(5))
            .andExpect(jsonPath("$.pagination.totalPages").value(10))
            .andExpect(jsonPath("$.pagination.totalElements").value(45))
            .andExpect(jsonPath("$.pagination.hasNext").value(true))
            .andExpect(jsonPath("$.pagination.hasPrevious").value(true));
    }

    @Test
    void searchDrugs_WithSortingByManufacturer_ReturnsSortedResults() throws Exception {
        // Given
        DrugSummary drugSummary = new DrugSummary(
            "HU001234",
            "Aspirin 100mg",
            "Bayer Hungary Kft.",
            "N02BA01",
            List.of("Acetylsalicylic acid"),
            false,
            true,
            DrugSummary.DrugStatus.ACTIVE
        );
        
        PaginationInfo pagination = new PaginationInfo(0, 20, 1, 1L, false, false);
        SearchInfo searchInfo = new SearchInfo(
            "aspirin",
            Map.of(),
            145L,
            false,
            Instant.now()
        );
        
        DrugSearchResponse expectedResponse = new DrugSearchResponse(
            List.of(drugSummary),
            pagination,
            searchInfo
        );
        
        when(drugService.searchDrugs(eq("aspirin"), isNull(), isNull(), eq(0), eq(20), eq("manufacturer"), eq("DESC")))
            .thenReturn(expectedResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortBy", "manufacturer")
                .param("sortDirection", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.drugs[0].manufacturer").value("Bayer Hungary Kft."));
    }

    @Test
    void searchDrugs_WithInvalidAtcCode_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("atcCode", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void searchDrugs_WithNegativePage_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("page", "-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void searchDrugs_WithInvalidSortField_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortBy", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void searchDrugs_WithInvalidSortDirection_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("sortDirection", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void searchDrugs_WithPageSizeExceedingLimit_ReturnsBadRequest() throws Exception {
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
    void searchDrugs_WithLongManufacturerName_ReturnsBadRequest() throws Exception {
        // Given - manufacturer name over 100 characters
        String longManufacturer = "A".repeat(101);
        
        // When & Then
        mockMvc.perform(get("/api/v1/drugs/search")
                .param("term", "aspirin")
                .param("manufacturer", longManufacturer)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}