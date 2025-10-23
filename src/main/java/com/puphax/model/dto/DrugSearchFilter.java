package com.puphax.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Comprehensive filter criteria for drug search operations.
 *
 * Supports multi-field filtering on all major drug attributes
 * from the NEAK PUPHAX CSV database (43,930 products).
 *
 * All filters are optional and combined with AND logic.
 */
public record DrugSearchFilter(

    // ===== TEXT SEARCH =====

    @Size(min = 2, max = 100, message = "Search term must be between 2 and 100 characters")
    String searchTerm,                   // Free text search (name, active ingredient)

    // ===== CLASSIFICATION FILTERS =====

    List<String> atcCodes,               // Filter by ATC codes (can be multiple)

    List<String> manufacturers,          // Filter by manufacturers (can be multiple)

    List<String> productForms,           // Filter by pharmaceutical forms (tablet, capsule, etc.)

    List<String> administrationMethods,  // Filter by administration routes (oral, IV, etc.)

    // ===== REGULATORY FILTERS =====

    List<String> tttCodes,               // Filter by TTT classification codes

    Boolean prescriptionRequired,        // Filter by prescription requirement (true/false/null=all)

    Boolean reimbursable,                // Filter by reimbursement eligibility (true/false/null=all)

    Boolean inStock,                     // Filter by stock availability (true/false/null=all)

    List<String> prescriptionTypes,      // Filter by specific prescription types (VN, VK, J, etc.)

    // ===== STRENGTH/DOSAGE FILTERS =====

    Double minStrength,                  // Minimum strength value

    Double maxStrength,                  // Maximum strength value

    List<String> strengthUnits,          // Filter by strength units (mg, ml, etc.)

    // ===== SPECIAL FILTERS =====

    List<String> brands,                 // Filter by brand names

    Boolean specialMarker,               // Filter by "egyedi" (special/individual) marker

    List<String> laterality,             // Filter by laterality (oldalIsag) - left/right/bilateral

    // ===== VALIDITY FILTERS =====

    Boolean currentlyValid,              // Only show currently valid products (validTo >= today)

    String validFromDate,                // Filter by validity start date (ISO-8601)

    String validToDate,                  // Filter by validity end date (ISO-8601)

    // ===== PAGINATION & SORTING =====

    @Min(value = 0, message = "Page number must be 0 or greater")
    Integer page,                        // Page number (0-indexed)

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    Integer size,                        // Page size (max 100)

    String sortBy,                       // Sort field (name, atcCode, manufacturer, strength, etc.)

    String sortDirection                 // Sort direction (ASC/DESC)
) {

    /**
     * Create a basic filter with just search term and defaults.
     */
    public static DrugSearchFilter basic(String searchTerm) {
        return new DrugSearchFilter(
            searchTerm,
            null, null, null, null,      // Classification filters
            null, null, null, null, null, // Regulatory filters
            null, null, null,            // Strength filters
            null, null, null,            // Special filters
            true, null, null,            // Validity (only current)
            0, 20,                       // Pagination (first page, 20 items)
            "name", "ASC"                // Sort by name ascending
        );
    }

    /**
     * Create empty filter (no restrictions).
     */
    public static DrugSearchFilter empty() {
        return new DrugSearchFilter(
            null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            null, null, null,
            null, null, null,
            0, 20,
            "name", "ASC"
        );
    }

    /**
     * Builder for fluent filter construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String searchTerm;
        private List<String> atcCodes;
        private List<String> manufacturers;
        private List<String> productForms;
        private List<String> administrationMethods;
        private List<String> tttCodes;
        private Boolean prescriptionRequired;
        private Boolean reimbursable;
        private Boolean inStock;
        private List<String> prescriptionTypes;
        private Double minStrength;
        private Double maxStrength;
        private List<String> strengthUnits;
        private List<String> brands;
        private Boolean specialMarker;
        private List<String> laterality;
        private Boolean currentlyValid = true;  // Default: only current products
        private String validFromDate;
        private String validToDate;
        private Integer page = 0;
        private Integer size = 20;
        private String sortBy = "name";
        private String sortDirection = "ASC";

        public Builder searchTerm(String val) { this.searchTerm = val; return this; }
        public Builder atcCodes(List<String> val) { this.atcCodes = val; return this; }
        public Builder manufacturers(List<String> val) { this.manufacturers = val; return this; }
        public Builder productForms(List<String> val) { this.productForms = val; return this; }
        public Builder administrationMethods(List<String> val) { this.administrationMethods = val; return this; }
        public Builder tttCodes(List<String> val) { this.tttCodes = val; return this; }
        public Builder prescriptionRequired(Boolean val) { this.prescriptionRequired = val; return this; }
        public Builder reimbursable(Boolean val) { this.reimbursable = val; return this; }
        public Builder inStock(Boolean val) { this.inStock = val; return this; }
        public Builder prescriptionTypes(List<String> val) { this.prescriptionTypes = val; return this; }
        public Builder minStrength(Double val) { this.minStrength = val; return this; }
        public Builder maxStrength(Double val) { this.maxStrength = val; return this; }
        public Builder strengthUnits(List<String> val) { this.strengthUnits = val; return this; }
        public Builder brands(List<String> val) { this.brands = val; return this; }
        public Builder specialMarker(Boolean val) { this.specialMarker = val; return this; }
        public Builder laterality(List<String> val) { this.laterality = val; return this; }
        public Builder currentlyValid(Boolean val) { this.currentlyValid = val; return this; }
        public Builder validFromDate(String val) { this.validFromDate = val; return this; }
        public Builder validToDate(String val) { this.validToDate = val; return this; }
        public Builder page(Integer val) { this.page = val; return this; }
        public Builder size(Integer val) { this.size = val; return this; }
        public Builder sortBy(String val) { this.sortBy = val; return this; }
        public Builder sortDirection(String val) { this.sortDirection = val; return this; }

        public DrugSearchFilter build() {
            return new DrugSearchFilter(
                searchTerm,
                atcCodes, manufacturers, productForms, administrationMethods,
                tttCodes, prescriptionRequired, reimbursable, inStock, prescriptionTypes,
                minStrength, maxStrength, strengthUnits,
                brands, specialMarker, laterality,
                currentlyValid, validFromDate, validToDate,
                page, size, sortBy, sortDirection
            );
        }
    }

    /**
     * Check if any filters are active (beyond basic search).
     */
    public boolean hasAdvancedFilters() {
        return atcCodes != null || manufacturers != null || productForms != null ||
               administrationMethods != null || tttCodes != null || prescriptionRequired != null ||
               reimbursable != null || inStock != null || prescriptionTypes != null ||
               minStrength != null || maxStrength != null || strengthUnits != null ||
               brands != null || specialMarker != null || laterality != null ||
               validFromDate != null || validToDate != null;
    }

    /**
     * Count active filters.
     */
    public int getActiveFilterCount() {
        int count = 0;
        if (atcCodes != null && !atcCodes.isEmpty()) count++;
        if (manufacturers != null && !manufacturers.isEmpty()) count++;
        if (productForms != null && !productForms.isEmpty()) count++;
        if (administrationMethods != null && !administrationMethods.isEmpty()) count++;
        if (tttCodes != null && !tttCodes.isEmpty()) count++;
        if (prescriptionRequired != null) count++;
        if (reimbursable != null) count++;
        if (inStock != null) count++;
        if (prescriptionTypes != null && !prescriptionTypes.isEmpty()) count++;
        if (minStrength != null || maxStrength != null) count++;
        if (strengthUnits != null && !strengthUnits.isEmpty()) count++;
        if (brands != null && !brands.isEmpty()) count++;
        if (specialMarker != null) count++;
        if (laterality != null && !laterality.isEmpty()) count++;
        if (validFromDate != null || validToDate != null) count++;
        return count;
    }
}
