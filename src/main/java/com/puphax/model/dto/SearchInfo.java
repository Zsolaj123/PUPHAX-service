package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Search metadata DTO containing information about the search operation.
 * 
 * This class provides clients with useful metadata about their search,
 * including performance metrics and applied filters.
 */
public record SearchInfo(
    
    @JsonProperty("searchTerm")
    @NotBlank
    String searchTerm,
    
    @JsonProperty("filters")
    @NotNull
    Map<String, String> filters,
    
    @JsonProperty("responseTimeMs")
    @Min(0)
    Long responseTimeMs,
    
    @JsonProperty("cacheHit")
    @NotNull
    Boolean cacheHit,
    
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @NotNull
    Instant timestamp
) {
    
    /**
     * Creates a basic SearchInfo with minimal information.
     * 
     * @param searchTerm The search term used
     * @param responseTimeMs Response time in milliseconds
     * @return SearchInfo with default values
     */
    public static SearchInfo basic(String searchTerm, Long responseTimeMs) {
        return new SearchInfo(
            searchTerm,
            new HashMap<>(),
            responseTimeMs,
            false,
            Instant.now()
        );
    }
    
    /**
     * Creates SearchInfo with applied filters.
     * 
     * @param searchTerm The search term used
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter
     * @param responseTimeMs Response time in milliseconds
     * @param cacheHit Whether this was served from cache
     * @return SearchInfo with filter information
     */
    public static SearchInfo withFilters(String searchTerm, String manufacturer, String atcCode, 
                                        Long responseTimeMs, Boolean cacheHit) {
        Map<String, String> filters = new HashMap<>();
        
        if (manufacturer != null && !manufacturer.trim().isEmpty()) {
            filters.put("manufacturer", manufacturer);
        }
        
        if (atcCode != null && !atcCode.trim().isEmpty()) {
            filters.put("atcCode", atcCode);
        }
        
        return new SearchInfo(
            searchTerm,
            filters,
            responseTimeMs,
            cacheHit != null ? cacheHit : false,
            Instant.now()
        );
    }
    
    /**
     * Creates SearchInfo for cached responses.
     * 
     * @param searchTerm The search term used
     * @param filters Applied filters
     * @param cacheResponseTime Cache lookup time in milliseconds
     * @return SearchInfo marked as cache hit
     */
    public static SearchInfo cached(String searchTerm, Map<String, String> filters, Long cacheResponseTime) {
        return new SearchInfo(
            searchTerm,
            filters != null ? filters : new HashMap<>(),
            cacheResponseTime,
            true,
            Instant.now()
        );
    }
    
    /**
     * Checks if any filters were applied to this search.
     * 
     * @return true if filters map is not empty
     */
    public boolean hasFilters() {
        return !filters.isEmpty();
    }
    
    /**
     * Gets the number of applied filters.
     * 
     * @return Count of active filters
     */
    public int getFilterCount() {
        return filters.size();
    }
    
    /**
     * Checks if the response was fast (under 1 second).
     * 
     * @return true if response time is under 1000ms
     */
    public boolean isFastResponse() {
        return responseTimeMs != null && responseTimeMs < 1000;
    }
    
    /**
     * Gets a human-readable description of applied filters.
     * 
     * @return Filter description string
     */
    public String getFilterDescription() {
        if (filters.isEmpty()) {
            return "No filters applied";
        }
        
        StringBuilder description = new StringBuilder("Filtered by: ");
        filters.forEach((key, value) -> {
            if (description.length() > "Filtered by: ".length()) {
                description.append(", ");
            }
            description.append(key).append("=").append(value);
        });
        
        return description.toString();
    }
    
    /**
     * Gets performance category based on response time.
     * 
     * @return Performance category string
     */
    public String getPerformanceCategory() {
        if (responseTimeMs == null) return "Unknown";
        if (cacheHit) return "Cache Hit";
        if (responseTimeMs < 500) return "Fast";
        if (responseTimeMs < 2000) return "Normal";
        return "Slow";
    }
}
