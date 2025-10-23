package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Summary DTO for drug information in search results.
 * 
 * This class represents the essential drug information returned
 * in search operations, providing enough detail for list views
 * without the full complexity of detailed drug records.
 */
public record DrugSummary(

    @JsonProperty("id")
    @NotBlank
    String id,

    @JsonProperty("name")
    @NotBlank
    String name,

    @JsonProperty("manufacturer")
    String manufacturer,

    @JsonProperty("atcCode")
    String atcCode,

    @JsonProperty("activeIngredients")
    @NotNull
    List<String> activeIngredients,

    @JsonProperty("prescriptionRequired")
    boolean prescriptionRequired,

    @JsonProperty("reimbursable")
    boolean reimbursable,

    @JsonProperty("status")
    @NotNull
    DrugStatus status,

    // Extended fields (nullable)
    @JsonProperty("tttCode")
    String tttCode,

    @JsonProperty("productForm")
    String productForm,

    @JsonProperty("strength")
    String strength,

    @JsonProperty("packSize")
    String packSize,

    @JsonProperty("prescriptionStatus")
    String prescriptionStatus,

    @JsonProperty("validFrom")
    String validFrom,

    @JsonProperty("validTo")
    String validTo,

    @JsonProperty("registrationNumber")
    String registrationNumber,

    @JsonProperty("price")
    String price,

    @JsonProperty("supportPercent")
    String supportPercent,

    @JsonProperty("source")
    String source
) {
    
    /**
     * Drug regulatory status enumeration.
     */
    public enum DrugStatus {
        @JsonProperty("ACTIVE")
        ACTIVE,
        
        @JsonProperty("SUSPENDED")
        SUSPENDED,
        
        @JsonProperty("WITHDRAWN")
        WITHDRAWN,
        
        @JsonProperty("PENDING")
        PENDING,
        
        @JsonProperty("ERROR")
        ERROR,
        
        @JsonProperty("UNKNOWN")
        UNKNOWN
    }
    
    /**
     * Creates a basic DrugSummary with minimal required information.
     *
     * @param id Drug identifier
     * @param name Drug name
     * @param manufacturer Manufacturer name
     * @return DrugSummary with default values
     */
    public static DrugSummary basic(String id, String name, String manufacturer) {
        return new DrugSummary(
            id,
            name,
            manufacturer,
            null,                    // No ATC code
            List.of(),              // No active ingredients
            false,                  // No prescription required
            false,                  // Not reimbursable
            DrugStatus.ACTIVE,      // Default to active
            null,                   // tttCode
            null,                   // productForm
            null,                   // strength
            null,                   // packSize
            null,                   // prescriptionStatus
            null,                   // validFrom
            null,                   // validTo
            null,                   // registrationNumber
            null,                   // price
            null,                   // supportPercent
            null                    // source
        );
    }
    
    /**
     * Gets the primary active ingredient if available.
     * 
     * @return First active ingredient or "Unknown" if none available
     */
    public String getPrimaryActiveIngredient() {
        return activeIngredients.isEmpty() ? "Unknown" : activeIngredients.get(0);
    }
    
    /**
     * Checks if this drug requires a prescription.
     * 
     * @return true if prescription is required
     */
    public boolean isPrescriptionDrug() {
        return prescriptionRequired;
    }
    
    /**
     * Checks if this drug is eligible for reimbursement.
     * 
     * @return true if reimbursable
     */
    public boolean isReimbursable() {
        return reimbursable;
    }
    
    /**
     * Checks if this drug is currently active in the system.
     * 
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return status == DrugStatus.ACTIVE;
    }
}
