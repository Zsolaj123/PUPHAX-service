package com.puphax.model.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for drug search operations.
 * 
 * This class encapsulates all search parameters that can be used
 * to filter and paginate drug search results.
 */
public record DrugSearchRequest(
    
    @NotBlank(message = "Search term cannot be blank")
    @Size(min = 2, max = 100, message = "Search term must be between 2 and 100 characters")
    String term,
    
    @Size(max = 100, message = "Manufacturer filter cannot exceed 100 characters")
    String manufacturer,
    
    @Pattern(regexp = "^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$", message = "ATC code must follow the format: A10AB01")
    String atcCode,
    
    @Min(value = 0, message = "Page number must be 0 or greater")
    int page,
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    int size,
    
    @Pattern(regexp = "^(name|manufacturer|atcCode)$", message = "Sort field must be one of: name, manufacturer, atcCode")
    String sortBy,
    
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    String sortDirection
) {
    
    /**
     * Creates a DrugSearchRequest with default pagination and sorting.
     * 
     * @param term Search term (required)
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter
     * @return DrugSearchRequest with default values
     */
    public static DrugSearchRequest withDefaults(String term, String manufacturer, String atcCode) {
        return new DrugSearchRequest(
            term,
            manufacturer,
            atcCode,
            0,      // Default page
            20,     // Default size
            "name", // Default sort by name
            "ASC"   // Default ascending order
        );
    }
    
    /**
     * Creates a basic search request with just the search term.
     * 
     * @param term Search term (required)
     * @return DrugSearchRequest with default values and no filters
     */
    public static DrugSearchRequest basic(String term) {
        return withDefaults(term, null, null);
    }
}
