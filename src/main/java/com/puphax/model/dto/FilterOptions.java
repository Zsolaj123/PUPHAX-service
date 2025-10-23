package com.puphax.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO containing available filter options for the drug search interface.
 *
 * This provides the frontend with lists of valid values for dropdowns,
 * checkboxes, and other filter controls, extracted from the CSV data.
 *
 * Cached to minimize server load - filters don't change frequently.
 */
public record FilterOptions(

    @JsonProperty("manufacturers")
    List<String> manufacturers,          // All unique manufacturers (from CEGEK.csv)

    @JsonProperty("atcCodes")
    List<AtcOption> atcCodes,            // ATC codes with descriptions (from ATCKONYV.csv)

    @JsonProperty("productForms")
    List<String> productForms,           // Pharmaceutical forms (GYFORMA field)

    @JsonProperty("prescriptionTypes")
    List<PrescriptionType> prescriptionTypes,  // Prescription requirement types

    @JsonProperty("administrationMethods")
    List<String> administrationMethods,  // Administration routes (ADAGMOD)

    @JsonProperty("tttCodes")
    List<TttOption> tttCodes,            // TTT classification codes

    @JsonProperty("brands")
    List<String> brands,                 // Brand names (from BRAND.csv)

    @JsonProperty("strengthRanges")
    StrengthRange strengthRanges,        // Min/max strength values for range slider

    @JsonProperty("totalProducts")
    long totalProducts,                  // Total products in database

    @JsonProperty("inStockCount")
    long inStockCount,                   // Products currently in stock

    @JsonProperty("cachedAt")
    String cachedAt                      // Cache timestamp (ISO-8601)
) {

    /**
     * ATC code with description for dropdown display.
     */
    public record AtcOption(
        @JsonProperty("code")
        String code,                     // e.g., "N02BA01"

        @JsonProperty("description")
        String description,              // e.g., "Acetylsalicylic acid"

        @JsonProperty("level")
        int level                        // ATC hierarchy level (1-5)
    ) {
        /**
         * Format for display in dropdown: "N02BA01 - Acetylsalicylic acid"
         */
        public String displayName() {
            return code + " - " + description;
        }
    }

    /**
     * Prescription requirement type with description.
     */
    public record PrescriptionType(
        @JsonProperty("code")
        String code,                     // e.g., "VN", "VK", "J"

        @JsonProperty("description")
        String description,              // e.g., "Vényköteles (normál)"

        @JsonProperty("prescriptionRequired")
        boolean prescriptionRequired     // true for VN, V5, V1, J
    ) {}

    /**
     * TTT classification code with description.
     */
    public record TttOption(
        @JsonProperty("code")
        String code,                     // e.g., "2", "3", "5"

        @JsonProperty("description")
        String description               // Description of TTT category
    ) {}

    /**
     * Strength range for slider control.
     */
    public record StrengthRange(
        @JsonProperty("min")
        double min,                      // Minimum strength value

        @JsonProperty("max")
        double max,                      // Maximum strength value

        @JsonProperty("unit")
        String unit,                     // Most common unit (e.g., "mg")

        @JsonProperty("commonValues")
        List<Double> commonValues        // Common discrete values (e.g., 100, 250, 500)
    ) {}

    /**
     * Create empty filter options (for error scenarios).
     */
    public static FilterOptions empty() {
        return new FilterOptions(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new StrengthRange(0.0, 0.0, "", List.of()),
            0L,
            0L,
            java.time.Instant.now().toString()
        );
    }

    /**
     * Get standard prescription types (Hungarian pharmaceutical system).
     */
    public static List<PrescriptionType> getStandardPrescriptionTypes() {
        return List.of(
            new PrescriptionType("VN", "Vényköteles (normál)", true),
            new PrescriptionType("V5", "Vényköteles (5x ismételhető)", true),
            new PrescriptionType("V1", "Vényköteles (1x ismételhető)", true),
            new PrescriptionType("J", "Különleges rendelvényen", true),
            new PrescriptionType("VK", "Vény nélkül kapható", false),
            new PrescriptionType("SZK", "Szakorvosi javaslat", true)
        );
    }

    /**
     * Get standard TTT codes (Hungarian drug classification).
     */
    public static List<TttOption> getStandardTttCodes() {
        return List.of(
            new TttOption("1", "Vény nélkül kapható gyógyszer"),
            new TttOption("2", "Vényköteles gyógyszer"),
            new TttOption("3", "Korlátozott forgalmazású gyógyszer"),
            new TttOption("4", "Külön rendelvényen rendelhető"),
            new TttOption("5", "Kábítószer-rendelvényen rendelhető"),
            new TttOption("6", "Kórházi gyógyszer"),
            new TttOption("7", "Különleges rendelkezésű gyógyszer")
        );
    }
}
