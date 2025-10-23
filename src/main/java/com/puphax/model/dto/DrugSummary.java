package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Comprehensive DTO for drug information in search results.
 *
 * This class represents complete drug information from the NEAK PUPHAX database,
 * exposing all 44 fields available in the CSV fallback data for maximum flexibility
 * in filtering, sorting, and display operations.
 *
 * Hungarian Field Names (Magyar mezőnevek):
 * - ID, PARENT_ID, ERV_KEZD, ERV_VEGE, TERMEKKOD, KOZHID, TTT, TK, TKTORLES, TKTORLESDAT
 * - EANKOD, BRAND_ID, NEV, KISZNEV, ATC, ISO, HATOANYAG, ADAGMOD, GYFORMA, RENDELHET
 * - EGYEN_ID, HELYETTESITH, POTENCIA, OHATO_MENNY, HATO_MENNY, HATO_EGYS, KISZ_MENNY
 * - KISZ_EGYS, DDD_MENNY, DDD_EGYS, DDD_FAKTOR, DOT, ADAG_MENNY, ADAG_EGYS, EGYEDI
 * - OLDALISAG, TOBBLGAR, PATIKA, DOBAZON, KERESZTJELZES, FORGENGT_ID, FORGALMAZ_ID
 * - FORGALOMBAN, KIHIRDETES_ID
 */
public record DrugSummary(

    // ===== CORE IDENTIFICATION FIELDS (Azonosítási mezők) =====

    @JsonProperty("id")
    @NotBlank
    String id,                          // ID - Unique product identifier

    @JsonProperty("parentId")
    String parentId,                    // PARENT_ID - Parent product ID (for variants)

    @JsonProperty("name")
    @NotBlank
    String name,                        // NEV - Product name

    @JsonProperty("shortName")
    String shortName,                   // KISZNEV - Short product name

    @JsonProperty("brandId")
    String brandId,                     // BRAND_ID - Brand identifier

    // ===== VALIDITY AND REGISTRATION (Érvényesség és regisztráció) =====

    @JsonProperty("validFrom")
    String validFrom,                   // ERV_KEZD - Validity start date

    @JsonProperty("validTo")
    String validTo,                     // ERV_VEGE - Validity end date

    @JsonProperty("termekKod")
    String termekKod,                   // TERMEKKOD - Product code

    @JsonProperty("kozHid")
    String kozHid,                      // KOZHID - Public bridge identifier

    @JsonProperty("tttCode")
    String tttCode,                     // TTT - TTT classification code

    @JsonProperty("tk")
    String tk,                          // TK - TK code

    @JsonProperty("tkTorles")
    String tkTorles,                    // TKTORLES - TK deletion flag

    @JsonProperty("tkTorlesDate")
    String tkTorlesDate,                // TKTORLESDAT - TK deletion date

    @JsonProperty("eanKod")
    String eanKod,                      // EANKOD - EAN barcode

    @JsonProperty("registrationNumber")
    String registrationNumber,          // Registration number (derived)

    // ===== CLASSIFICATION (Osztályozás) =====

    @JsonProperty("atcCode")
    String atcCode,                     // ATC - Anatomical Therapeutic Chemical code

    @JsonProperty("iso")
    String iso,                         // ISO - ISO classification

    @JsonProperty("activeIngredient")
    String activeIngredient,            // HATOANYAG - Active ingredient(s)

    @JsonProperty("activeIngredients")
    @NotNull
    List<String> activeIngredients,     // Parsed active ingredients list

    // ===== ADMINISTRATION AND FORM (Adagolás és forma) =====

    @JsonProperty("adagMod")
    String adagMod,                     // ADAGMOD - Administration method

    @JsonProperty("productForm")
    String productForm,                 // GYFORMA - Pharmaceutical form

    @JsonProperty("rendelhet")
    String rendelhet,                   // RENDELHET - Prescribability flag

    @JsonProperty("prescriptionRequired")
    boolean prescriptionRequired,       // Derived from rendelhet

    @JsonProperty("egyenId")
    String egyenId,                     // EGYEN_ID - Equivalence ID

    @JsonProperty("helyettesith")
    String helyettesith,                // HELYETTESITH - Substitutability

    // ===== STRENGTH AND DOSAGE (Hatóanyag-tartalom és adagolás) =====

    @JsonProperty("potencia")
    String potencia,                    // POTENCIA - Strength/potency

    @JsonProperty("strength")
    String strength,                    // Alias for potencia

    @JsonProperty("oHatoMenny")
    String oHatoMenny,                  // OHATO_MENNY - Original active amount

    @JsonProperty("hatoMenny")
    String hatoMenny,                   // HATO_MENNY - Active ingredient amount

    @JsonProperty("hatoEgys")
    String hatoEgys,                    // HATO_EGYS - Active ingredient unit

    @JsonProperty("kiszMenny")
    String kiszMenny,                   // KISZ_MENNY - Package quantity

    @JsonProperty("kiszEgys")
    String kiszEgys,                    // KISZ_EGYS - Package unit

    @JsonProperty("packSize")
    String packSize,                    // Derived from kiszMenny + kiszEgys

    // ===== DDD (Defined Daily Dose) FIELDS =====

    @JsonProperty("dddMenny")
    String dddMenny,                    // DDD_MENNY - DDD amount

    @JsonProperty("dddEgys")
    String dddEgys,                     // DDD_EGYS - DDD unit

    @JsonProperty("dddFaktor")
    String dddFaktor,                   // DDD_FAKTOR - DDD factor

    @JsonProperty("dot")
    String dot,                         // DOT - Days of therapy

    @JsonProperty("adagMenny")
    String adagMenny,                   // ADAG_MENNY - Dose amount

    @JsonProperty("adagEgys")
    String adagEgys,                    // ADAG_EGYS - Dose unit

    // ===== SPECIAL ATTRIBUTES (Speciális tulajdonságok) =====

    @JsonProperty("egyedi")
    String egyedi,                      // EGYEDI - Individual/special marker

    @JsonProperty("oldalIsag")
    String oldalIsag,                   // OLDALISAG - Laterality

    @JsonProperty("tobblGar")
    String tobblGar,                    // TOBBLGAR - Multi-guarantee

    @JsonProperty("patika")
    String patika,                      // PATIKA - Pharmacy availability

    @JsonProperty("dobAzon")
    String dobAzon,                     // DOBAZON - Box identifier

    @JsonProperty("keresztJelzes")
    String keresztJelzes,               // KERESZTJELZES - Cross-marking

    // ===== DISTRIBUTION AND AVAILABILITY (Forgalmazás) =====

    @JsonProperty("forgEngtId")
    String forgEngtId,                  // FORGENGT_ID - Marketing authorization ID

    @JsonProperty("forgazId")
    String forgazId,                    // FORGALMAZ_ID - Distributor ID

    @JsonProperty("manufacturer")
    String manufacturer,                // Derived from forgazId lookup

    @JsonProperty("inStock")
    boolean inStock,                    // FORGALOMBAN - Currently in stock

    @JsonProperty("kihirdetesId")
    String kihirdetesId,                // KIHIRDETES_ID - Publication ID

    // ===== REIMBURSEMENT (Támogatás) =====

    @JsonProperty("reimbursable")
    boolean reimbursable,               // Derived - reimbursement eligibility

    @JsonProperty("supportPercent")
    String supportPercent,              // Support percentage (if available)

    @JsonProperty("price")
    String price,                       // Price information (if available)

    // ===== STATUS AND SOURCE =====

    @JsonProperty("status")
    @NotNull
    DrugStatus status,                  // Derived regulatory status

    @JsonProperty("source")
    String source,                      // Data source (SOAP/CSV)

    @JsonProperty("prescriptionStatus")
    String prescriptionStatus           // Detailed prescription requirements
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
     * All optional fields are set to null or default values.
     *
     * @param id Drug identifier
     * @param name Drug name
     * @param manufacturer Manufacturer name
     * @return DrugSummary with default values for all 55 fields
     */
    public static DrugSummary basic(String id, String name, String manufacturer) {
        return new DrugSummary(
            // Core identification
            id, null, name, null, null,
            // Validity and registration
            null, null, null, null, null, null, null, null, null, null,
            // Classification
            null, null, null, List.of(),
            // Administration and form
            null, null, null, false, null, null,
            // Strength and dosage
            null, null, null, null, null, null, null, null,
            // DDD fields
            null, null, null, null, null, null,
            // Special attributes
            null, null, null, null, null, null,
            // Distribution
            null, null, manufacturer, false, null,
            // Reimbursement
            false, null, null,
            // Status and source
            DrugStatus.ACTIVE, null, null
        );
    }

    /**
     * Builder-style factory for creating DrugSummary instances with full control.
     * All fields from ProductRecord are mapped.
     *
     * @param id Unique product identifier (required)
     * @param name Product name (required)
     * @return Builder instance for fluent API
     */
    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    /**
     * Builder class for creating DrugSummary instances with optional fields.
     */
    public static class Builder {
        // Required fields
        private final String id;
        private final String name;

        // Core identification
        private String parentId;
        private String shortName;
        private String brandId;

        // Validity and registration
        private String validFrom;
        private String validTo;
        private String termekKod;
        private String kozHid;
        private String tttCode;
        private String tk;
        private String tkTorles;
        private String tkTorlesDate;
        private String eanKod;
        private String registrationNumber;

        // Classification
        private String atcCode;
        private String iso;
        private String activeIngredient;
        private List<String> activeIngredients = List.of();

        // Administration and form
        private String adagMod;
        private String productForm;
        private String rendelhet;
        private boolean prescriptionRequired;
        private String egyenId;
        private String helyettesith;

        // Strength and dosage
        private String potencia;
        private String strength;
        private String oHatoMenny;
        private String hatoMenny;
        private String hatoEgys;
        private String kiszMenny;
        private String kiszEgys;
        private String packSize;

        // DDD fields
        private String dddMenny;
        private String dddEgys;
        private String dddFaktor;
        private String dot;
        private String adagMenny;
        private String adagEgys;

        // Special attributes
        private String egyedi;
        private String oldalIsag;
        private String tobblGar;
        private String patika;
        private String dobAzon;
        private String keresztJelzes;

        // Distribution
        private String forgEngtId;
        private String forgazId;
        private String manufacturer;
        private boolean inStock;
        private String kihirdetesId;

        // Reimbursement
        private boolean reimbursable;
        private String supportPercent;
        private String price;

        // Status and source
        private DrugStatus status = DrugStatus.ACTIVE;
        private String source;
        private String prescriptionStatus;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder parentId(String val) { this.parentId = val; return this; }
        public Builder shortName(String val) { this.shortName = val; return this; }
        public Builder brandId(String val) { this.brandId = val; return this; }
        public Builder validFrom(String val) { this.validFrom = val; return this; }
        public Builder validTo(String val) { this.validTo = val; return this; }
        public Builder termekKod(String val) { this.termekKod = val; return this; }
        public Builder kozHid(String val) { this.kozHid = val; return this; }
        public Builder tttCode(String val) { this.tttCode = val; return this; }
        public Builder tk(String val) { this.tk = val; return this; }
        public Builder tkTorles(String val) { this.tkTorles = val; return this; }
        public Builder tkTorlesDate(String val) { this.tkTorlesDate = val; return this; }
        public Builder eanKod(String val) { this.eanKod = val; return this; }
        public Builder registrationNumber(String val) { this.registrationNumber = val; return this; }
        public Builder atcCode(String val) { this.atcCode = val; return this; }
        public Builder iso(String val) { this.iso = val; return this; }
        public Builder activeIngredient(String val) { this.activeIngredient = val; return this; }
        public Builder activeIngredients(List<String> val) { this.activeIngredients = val != null ? val : List.of(); return this; }
        public Builder adagMod(String val) { this.adagMod = val; return this; }
        public Builder productForm(String val) { this.productForm = val; return this; }
        public Builder rendelhet(String val) { this.rendelhet = val; return this; }
        public Builder prescriptionRequired(boolean val) { this.prescriptionRequired = val; return this; }
        public Builder egyenId(String val) { this.egyenId = val; return this; }
        public Builder helyettesith(String val) { this.helyettesith = val; return this; }
        public Builder potencia(String val) { this.potencia = val; this.strength = val; return this; }
        public Builder strength(String val) { this.strength = val; return this; }
        public Builder oHatoMenny(String val) { this.oHatoMenny = val; return this; }
        public Builder hatoMenny(String val) { this.hatoMenny = val; return this; }
        public Builder hatoEgys(String val) { this.hatoEgys = val; return this; }
        public Builder kiszMenny(String val) { this.kiszMenny = val; return this; }
        public Builder kiszEgys(String val) { this.kiszEgys = val; return this; }
        public Builder packSize(String val) { this.packSize = val; return this; }
        public Builder dddMenny(String val) { this.dddMenny = val; return this; }
        public Builder dddEgys(String val) { this.dddEgys = val; return this; }
        public Builder dddFaktor(String val) { this.dddFaktor = val; return this; }
        public Builder dot(String val) { this.dot = val; return this; }
        public Builder adagMenny(String val) { this.adagMenny = val; return this; }
        public Builder adagEgys(String val) { this.adagEgys = val; return this; }
        public Builder egyedi(String val) { this.egyedi = val; return this; }
        public Builder oldalIsag(String val) { this.oldalIsag = val; return this; }
        public Builder tobblGar(String val) { this.tobblGar = val; return this; }
        public Builder patika(String val) { this.patika = val; return this; }
        public Builder dobAzon(String val) { this.dobAzon = val; return this; }
        public Builder keresztJelzes(String val) { this.keresztJelzes = val; return this; }
        public Builder forgEngtId(String val) { this.forgEngtId = val; return this; }
        public Builder forgazId(String val) { this.forgazId = val; return this; }
        public Builder manufacturer(String val) { this.manufacturer = val; return this; }
        public Builder inStock(boolean val) { this.inStock = val; return this; }
        public Builder kihirdetesId(String val) { this.kihirdetesId = val; return this; }
        public Builder reimbursable(boolean val) { this.reimbursable = val; return this; }
        public Builder supportPercent(String val) { this.supportPercent = val; return this; }
        public Builder price(String val) { this.price = val; return this; }
        public Builder status(DrugStatus val) { this.status = val != null ? val : DrugStatus.ACTIVE; return this; }
        public Builder source(String val) { this.source = val; return this; }
        public Builder prescriptionStatus(String val) { this.prescriptionStatus = val; return this; }

        public DrugSummary build() {
            return new DrugSummary(
                // Core identification
                id, parentId, name, shortName, brandId,
                // Validity and registration
                validFrom, validTo, termekKod, kozHid, tttCode, tk, tkTorles, tkTorlesDate, eanKod, registrationNumber,
                // Classification
                atcCode, iso, activeIngredient, activeIngredients,
                // Administration and form
                adagMod, productForm, rendelhet, prescriptionRequired, egyenId, helyettesith,
                // Strength and dosage
                potencia, strength, oHatoMenny, hatoMenny, hatoEgys, kiszMenny, kiszEgys, packSize,
                // DDD fields
                dddMenny, dddEgys, dddFaktor, dot, adagMenny, adagEgys,
                // Special attributes
                egyedi, oldalIsag, tobblGar, patika, dobAzon, keresztJelzes,
                // Distribution
                forgEngtId, forgazId, manufacturer, inStock, kihirdetesId,
                // Reimbursement
                reimbursable, supportPercent, price,
                // Status and source
                status, source, prescriptionStatus
            );
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
}
