package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Response DTO for drug search operations.
 * 
 * This class encapsulates the complete search response including
 * drug results, pagination information, and search metadata.
 */
public record DrugSearchResponse(
    
    @JsonProperty("drugs")
    @NotNull
    @Valid
    List<DrugSummary> drugs,
    
    @JsonProperty("pagination")
    @NotNull
    @Valid
    PaginationInfo pagination,
    
    @JsonProperty("searchInfo")
    @NotNull
    @Valid
    SearchInfo searchInfo
) {
    
    /**
     * Creates an empty search response for when no results are found.
     * 
     * @param searchTerm The original search term
     * @param pageSize The requested page size
     * @return Empty DrugSearchResponse
     */
    public static DrugSearchResponse empty(String searchTerm, int pageSize) {
        return new DrugSearchResponse(
            List.of(),
            new PaginationInfo(0, pageSize, 0, 0L, false, false),
            SearchInfo.basic(searchTerm, 0L)
        );
    }
    
    /**
     * Gets the total number of drugs returned in this response.
     * 
     * @return Number of drugs in the current page
     */
    public int getCurrentPageSize() {
        return drugs.size();
    }
    
    /**
     * Checks if this response contains any results.
     * 
     * @return true if drugs list is not empty
     */
    public boolean hasResults() {
        return !drugs.isEmpty();
    }
}
