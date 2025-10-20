package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Pagination information DTO for paginated responses.
 * 
 * This class provides comprehensive pagination metadata
 * that allows clients to implement proper pagination controls.
 */
public record PaginationInfo(
    
    @JsonProperty("currentPage")
    @Min(0)
    int currentPage,
    
    @JsonProperty("pageSize")
    @Min(1)
    int pageSize,
    
    @JsonProperty("totalPages")
    @Min(0)
    int totalPages,
    
    @JsonProperty("totalElements")
    @Min(0)
    Long totalElements,
    
    @JsonProperty("hasNext")
    @NotNull
    Boolean hasNext,
    
    @JsonProperty("hasPrevious")
    @NotNull
    Boolean hasPrevious
) {
    
    /**
     * Creates PaginationInfo from current page, size, and total elements.
     * Automatically calculates derived fields.
     * 
     * @param currentPage Current page number (0-based)
     * @param pageSize Number of items per page
     * @param totalElements Total number of items across all pages
     * @return PaginationInfo with calculated values
     */
    public static PaginationInfo of(int currentPage, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages - 1;
        boolean hasPrevious = currentPage > 0;
        
        return new PaginationInfo(
            currentPage,
            pageSize,
            totalPages,
            totalElements,
            hasNext,
            hasPrevious
        );
    }
    
    /**
     * Creates an empty pagination info for responses with no results.
     * 
     * @param pageSize The requested page size
     * @return PaginationInfo representing empty results
     */
    public static PaginationInfo empty(int pageSize) {
        return new PaginationInfo(0, pageSize, 0, 0L, false, false);
    }
    
    /**
     * Gets the zero-based offset for this page.
     * 
     * @return The starting index for items on this page
     */
    public int getOffset() {
        return currentPage * pageSize;
    }
    
    /**
     * Gets the 1-based page number for display purposes.
     * 
     * @return Page number starting from 1
     */
    public int getDisplayPageNumber() {
        return currentPage + 1;
    }
    
    /**
     * Gets the next page number if available.
     * 
     * @return Next page number or null if no next page
     */
    public Integer getNextPage() {
        return hasNext ? currentPage + 1 : null;
    }
    
    /**
     * Gets the previous page number if available.
     * 
     * @return Previous page number or null if no previous page
     */
    public Integer getPreviousPage() {
        return hasPrevious ? currentPage - 1 : null;
    }
    
    /**
     * Checks if this represents the first page.
     * 
     * @return true if this is page 0
     */
    public boolean isFirstPage() {
        return currentPage == 0;
    }
    
    /**
     * Checks if this represents the last page.
     * 
     * @return true if this is the last page
     */
    public boolean isLastPage() {
        return !hasNext;
    }
    
    /**
     * Gets the range of items on this page (e.g., "1-20 of 150").
     * 
     * @return Human-readable range description
     */
    public String getItemRange() {
        if (totalElements == 0) {
            return "0 of 0";
        }
        
        int start = getOffset() + 1;
        int end = Math.min(getOffset() + pageSize, totalElements.intValue());
        
        return String.format("%d-%d of %d", start, end, totalElements);
    }
}
