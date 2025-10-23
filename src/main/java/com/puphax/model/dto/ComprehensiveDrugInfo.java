package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive drug information DTO with all available fields from PUPHAX database.
 *
 * This class represents the complete drug information including all 44 fields
 * available in the NEAK PUPHAX database, providing detailed pharmaceutical data.
 */
public record ComprehensiveDrugInfo(

    // Core identification
    @JsonProperty("id")
    @NotBlank
    String id,

    @JsonProperty("parentId")
    String parentId,

    @JsonProperty("productCode")
    String productCode,

    @JsonProperty("eanCode")
    String eanCode,

    // Names
    @JsonProperty("name")
    @NotBlank
    String name,

    @JsonProperty("shortName")
    String shortName,

    // Manufacturer and brand
    @JsonProperty("manufacturer")
    String manufacturer,

    @JsonProperty("brand")
    String brand,

    @JsonProperty("brandId")
    String brandId,

    // Classification
    @JsonProperty("atcCode")
    String atcCode,

    @JsonProperty("atcDescription")
    String atcDescription,

    @JsonProperty("isoCode")
    String isoCode,

    // Active ingredients
    @JsonProperty("activeIngredients")
    @NotNull
    List<String> activeIngredients,

    // Pharmaceutical form and administration
    @JsonProperty("pharmaceuticalForm")
    String pharmaceuticalForm,

    @JsonProperty("administrationMethod")
    String administrationMethod,

    // Strength and dosage
    @JsonProperty("strength")
    String strength,

    @JsonProperty("activeSubstanceAmount")
    String activeSubstanceAmount,

    @JsonProperty("activeSubstanceUnit")
    String activeSubstanceUnit,

    @JsonProperty("packageSize")
    String packageSize,

    @JsonProperty("packageSizeUnit")
    String packageSizeUnit,

    @JsonProperty("dddAmount")
    String dddAmount,

    @JsonProperty("dddUnit")
    String dddUnit,

    @JsonProperty("dddFactor")
    String dddFactor,

    @JsonProperty("dotCode")
    String dotCode,

    @JsonProperty("dosageAmount")
    String dosageAmount,

    @JsonProperty("dosageUnit")
    String dosageUnit,

    // Regulatory and prescription information
    @JsonProperty("prescriptionRequired")
    boolean prescriptionRequired,

    @JsonProperty("tttCode")
    String tttCode,

    @JsonProperty("tkCode")
    String tkCode,

    @JsonProperty("reimbursable")
    boolean reimbursable,

    @JsonProperty("prescribable")
    String prescribable,

    @JsonProperty("substitutable")
    String substitutable,

    // Special attributes
    @JsonProperty("pharmacyOnly")
    String pharmacyOnly,

    @JsonProperty("crossReference")
    String crossReference,

    @JsonProperty("specialIndication")
    String specialIndication,

    @JsonProperty("laterality")
    String laterality,

    @JsonProperty("multipleWarranty")
    String multipleWarranty,

    @JsonProperty("boxIdentifier")
    String boxIdentifier,

    // Validity and status
    @JsonProperty("validFrom")
    LocalDate validFrom,

    @JsonProperty("validTo")
    LocalDate validTo,

    @JsonProperty("status")
    @NotNull
    DrugStatus status,

    @JsonProperty("inStock")
    boolean inStock,

    // Distribution
    @JsonProperty("distributorId")
    String distributorId,

    @JsonProperty("publicationId")
    String publicationId,

    // Data source
    @JsonProperty("dataSource")
    @NotNull
    DataSource dataSource
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
     * Data source enumeration.
     */
    public enum DataSource {
        @JsonProperty("NEAK_SOAP")
        NEAK_SOAP("NEAK SOAP Web Service"),

        @JsonProperty("CSV_FALLBACK")
        CSV_FALLBACK("CSV Fallback (NEAK Historical Data 2007-2023)");

        private final String description;

        DataSource(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
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

    /**
     * Checks if this drug is currently in stock.
     *
     * @return true if in stock
     */
    public boolean isInStock() {
        return inStock;
    }

    /**
     * Gets a human-readable description of the data source.
     *
     * @return Data source description
     */
    public String getDataSourceDescription() {
        return dataSource.getDescription();
    }

    /**
     * Gets the full dosage information as a formatted string.
     *
     * @return Dosage string or null if not available
     */
    public String getFullDosage() {
        if (dosageAmount != null && dosageUnit != null) {
            return dosageAmount + " " + dosageUnit;
        }
        return null;
    }

    /**
     * Gets the full package size as a formatted string.
     *
     * @return Package size string or null if not available
     */
    public String getFullPackageSize() {
        if (packageSize != null && packageSizeUnit != null) {
            return packageSize + " " + packageSizeUnit;
        }
        return null;
    }

    /**
     * Gets the full active substance amount as a formatted string.
     *
     * @return Active substance amount string or null if not available
     */
    public String getFullActiveSubstanceAmount() {
        if (activeSubstanceAmount != null && activeSubstanceUnit != null) {
            return activeSubstanceAmount + " " + activeSubstanceUnit;
        }
        return null;
    }
}
