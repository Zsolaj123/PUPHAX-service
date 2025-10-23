package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Extended DTO for drug information with all available PUPHAX fields.
 * 
 * This class extends DrugSummary to include additional fields available
 * from TERMEKADAT and TAMOGATADAT responses.
 */
public record ExtendedDrugSummary(
    
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
    
    @JsonProperty("tttCode")
    String tttCode,
    
    @JsonProperty("activeIngredients")
    @NotNull
    List<String> activeIngredients,
    
    @JsonProperty("prescriptionRequired")
    boolean prescriptionRequired,
    
    @JsonProperty("prescriptionStatus")
    String prescriptionStatus,
    
    @JsonProperty("reimbursable")
    boolean reimbursable,
    
    @JsonProperty("status")
    @NotNull
    DrugSummary.DrugStatus status,
    
    @JsonProperty("packaging")
    String packaging,
    
    @JsonProperty("registrationNumber")
    String registrationNumber,
    
    @JsonProperty("productForm")
    String productForm,
    
    @JsonProperty("strength")
    String strength,
    
    @JsonProperty("packSize")
    String packSize,
    
    @JsonProperty("productType")
    String productType,
    
    @JsonProperty("price")
    String price,
    
    @JsonProperty("bruttoFogyasztarAr")
    String bruttoFogyasztarAr,
    
    @JsonProperty("nettoFogyasztarAr")
    String nettoFogyasztarAr,
    
    @JsonProperty("termelesAr")
    String termelesAr,
    
    @JsonProperty("nagykerAr")
    String nagykerAr,
    
    @JsonProperty("supportPercent")
    String supportPercent,
    
    @JsonProperty("tamogatottAr")
    String tamogatottAr,
    
    @JsonProperty("teritesiDij")
    String teritesiDij,
    
    @JsonProperty("validFrom")
    String validFrom,
    
    @JsonProperty("validTo")
    String validTo,
    
    @JsonProperty("normativity")
    String normativity,
    
    @JsonProperty("supportType")
    String supportType,
    
    @JsonProperty("source")
    String source
) {
    
    /**
     * Convert to DrugSummary with all fields.
     */
    public DrugSummary toBasicSummary() {
        return new DrugSummary(
            id,
            name,
            manufacturer,
            atcCode,
            activeIngredients,
            prescriptionRequired,
            reimbursable,
            status,
            tttCode,
            productForm,
            strength,
            packSize,
            prescriptionStatus,
            validFrom,
            validTo,
            registrationNumber,
            price,
            supportPercent,
            source
        );
    }
    
    /**
     * Get prescription status description.
     */
    public String getPrescriptionStatusDescription() {
        if (prescriptionStatus == null) return "Unknown";
        
        return switch (prescriptionStatus) {
            case "VN" -> "Vényköteles (normál)";
            case "V5" -> "Vényköteles (5x ismételhető)";
            case "V1" -> "Vényköteles (1x ismételhető)";
            case "J" -> "Különleges rendelvényen";
            case "VK" -> "Vény nélkül kapható";
            case "SZK" -> "Szakorvosi javaslat";
            default -> prescriptionStatus;
        };
    }
    
    /**
     * Check if product has valid price information.
     */
    public boolean hasPriceInfo() {
        return price != null && !"N/A".equals(price) && !"".equals(price);
    }
    
    /**
     * Check if product has valid support information.
     */
    public boolean hasSupportInfo() {
        return supportPercent != null && !"0".equals(supportPercent) && !"".equals(supportPercent);
    }
}