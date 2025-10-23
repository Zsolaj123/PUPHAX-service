package com.puphax.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV-based fallback service using NEAK's historical data dump (2007-2023).
 * 
 * This service loads the complete PUPHAX historical dataset from CSV files
 * provided by NEAK to reduce server load. The dataset contains 19 tables
 * with data from 2007-04-01 to 2023-03-01.
 * 
 * Data source: https://www.neak.gov.hu - Ősfeltöltés2007-2023
 * Format: CSV with TAB delimiter, double-quote text qualifier
 */
@Service
public class PuphaxCsvFallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(PuphaxCsvFallbackService.class);
    
    // In-memory caches for quick lookup
    private final Map<String, ProductRecord> productsById = new HashMap<>();
    private final Map<String, String> brandNames = new HashMap<>();
    private final Map<String, String> atcCodes = new HashMap<>();
    private final Map<String, String> companies = new HashMap<>();
    
    // Search index for product names
    private final Map<String, List<ProductRecord>> nameSearchIndex = new HashMap<>();
    
    private boolean initialized = false;
    
    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing PUPHAX CSV fallback service with historical data (2007-2023)");
            long startTime = System.currentTimeMillis();
            
            loadBrands();
            loadAtcCodes();
            loadCompanies();
            loadProducts();
            buildSearchIndex();
            
            initialized = true;
            long duration = System.currentTimeMillis() - startTime;
            logger.info("CSV fallback service initialized successfully: {} products, {} brands, {} ATC codes in {}ms",
                       productsById.size(), brandNames.size(), atcCodes.size(), duration);
        } catch (Exception e) {
            logger.error("Failed to initialize CSV fallback service: {}", e.getMessage(), e);
            initialized = false;
        }
    }
    
    /**
     * Search for drugs using local CSV data.
     * Returns results in PUPHAX XML format for compatibility.
     */
    public String searchDrugs(String searchTerm) {
        if (!initialized) {
            logger.warn("CSV service not initialized, returning error response");
            return createErrorResponse("CSV fallback service not initialized");
        }
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return createErrorResponse("Search term cannot be empty");
        }
        
        logger.info("Searching local CSV data for: {}", searchTerm);
        String normalizedTerm = searchTerm.trim().toLowerCase();
        
        // Search in name index
        List<ProductRecord> results = nameSearchIndex.entrySet().stream()
            .filter(entry -> entry.getKey().contains(normalizedTerm))
            .flatMap(entry -> entry.getValue().stream())
            .distinct()
            .limit(50)  // Limit results to prevent overwhelming the UI
            .collect(Collectors.toList());
        
        logger.info("Found {} matching products in local data", results.size());
        
        return formatSearchResults(results, searchTerm);
    }
    
    /**
     * Load BRAND table (brand names).
     */
    private void loadBrands() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("puphax-data/BRAND.csv");
        if (is == null) {
            logger.warn("BRAND.csv not found in classpath, brand names will not be available");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // Skip header
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length >= 2) {
                    String id = fields[0].trim();
                    String name = unquote(fields[1]);
                    brandNames.put(id, name);
                    count++;
                }
            }
            logger.debug("Loaded {} brand names", count);
        }
    }
    
    /**
     * Load ATCKONYV table (ATC classification codes).
     */
    private void loadAtcCodes() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("puphax-data/ATCKONYV.csv");
        if (is == null) {
            logger.warn("ATCKONYV.csv not found in classpath");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // Skip header
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length >= 2) {
                    String atc = unquote(fields[0]);
                    String description = unquote(fields[1]);
                    atcCodes.put(atc, description);
                    count++;
                }
            }
            logger.debug("Loaded {} ATC codes", count);
        }
    }
    
    /**
     * Load CEGEK table (companies/manufacturers).
     */
    private void loadCompanies() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("puphax-data/CEGEK.csv");
        if (is == null) {
            logger.warn("CEGEK.csv not found in classpath");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // Skip header
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length >= 2) {
                    String id = fields[0].trim();
                    String name = unquote(fields[1]);
                    companies.put(id, name);
                    count++;
                }
            }
            logger.debug("Loaded {} companies", count);
        }
    }
    
    /**
     * Load TERMEK table (products) - the main table with 890K+ products.
     * Only loads currently valid products to save memory.
     */
    private void loadProducts() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("puphax-data/TERMEK.csv");
        if (is == null) {
            throw new IOException("TERMEK.csv not found in classpath - this is a critical file");
        }
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            logger.debug("TERMEK header: {}", headerLine);
            
            String line;
            int totalCount = 0;
            int validCount = 0;
            
            while ((line = reader.readLine()) != null) {
                totalCount++;
                try {
                    String[] fields = line.split("\t", -1); // -1 to keep empty trailing fields
                    
                    if (fields.length < 20) {
                        continue; // Skip malformed lines
                    }
                    
                    // Parse validity dates (fields 2 and 3)
                    LocalDate validFrom = parseDate(unquote(fields[2]), dateFormatter);
                    LocalDate validTo = parseDate(unquote(fields[3]), dateFormatter);
                    
                    // Only load currently valid or recently expired products (within last 2 years)
                    if (validTo != null && validTo.isBefore(today.minusYears(2))) {
                        continue; // Skip old expired products
                    }
                    
                    ProductRecord product = new ProductRecord();
                    // Core identification
                    product.id = fields[0].trim();
                    product.parentId = fields[1].trim();
                    product.validFrom = validFrom;
                    product.validTo = validTo;
                    product.termekKod = unquote(fields[4]);
                    product.kozHid = unquote(fields[5]);
                    product.ttt = unquote(fields[6]);
                    product.tk = unquote(fields[7]);
                    product.tkTorles = unquote(fields[8]);
                    product.tkTorlesDate = parseDate(unquote(fields[9]), dateFormatter);
                    product.eanKod = unquote(fields[10]);
                    product.brandId = fields[11].trim();

                    // Names
                    product.name = unquote(fields[12]);
                    product.shortName = unquote(fields[13]);

                    // Classification
                    product.atc = unquote(fields[14]);
                    product.iso = unquote(fields[15]);
                    product.activeIngredient = unquote(fields[16]);

                    // Administration and form
                    product.adagMod = unquote(fields[17]);
                    product.gyForma = unquote(fields[18]);
                    product.rendelhet = unquote(fields[19]);
                    product.egyenId = unquote(fields[20]);
                    product.helyettesith = unquote(fields[21]);

                    // Strength and dosage
                    product.potencia = unquote(fields[22]);
                    product.oHatoMenny = unquote(fields[23]);
                    product.hatoMenny = unquote(fields[24]);
                    product.hatoEgys = unquote(fields[25]);
                    product.kiszMenny = unquote(fields[26]);
                    product.kiszEgys = unquote(fields[27]);
                    product.dddMenny = unquote(fields[28]);
                    product.dddEgys = unquote(fields[29]);
                    product.dddFaktor = unquote(fields[30]);
                    product.dot = unquote(fields[31]);
                    product.adagMenny = unquote(fields[32]);
                    product.adagEgys = unquote(fields[33]);

                    // Special attributes
                    product.egyedi = unquote(fields[34]);
                    product.oldalIsag = unquote(fields[35]);
                    product.tobblGar = unquote(fields[36]);
                    product.patika = unquote(fields[37]);
                    product.dobAzon = unquote(fields[38]);
                    product.keresztJelzes = unquote(fields[39]);

                    // Distribution
                    product.forgEngtId = unquote(fields[40]);
                    product.forgazId = unquote(fields[41]);
                    product.inStock = "1".equals(fields[42]);
                    product.kihirdetesId = unquote(fields[43]);
                    
                    productsById.put(product.id, product);
                    validCount++;
                    
                    if (totalCount % 100000 == 0) {
                        logger.debug("Processed {} products, loaded {} valid products", totalCount, validCount);
                    }
                    
                } catch (Exception e) {
                    logger.debug("Error parsing product line {}: {}", totalCount, e.getMessage());
                }
            }
            
            logger.info("Loaded {} valid products out of {} total products", validCount, totalCount);
        }
    }
    
    /**
     * Build search index for fast name-based and active ingredient searches.
     */
    private void buildSearchIndex() {
        logger.debug("Building search index for {} products", productsById.size());

        for (ProductRecord product : productsById.values()) {
            // Index by product name
            if (product.name != null && !product.name.isEmpty()) {
                String normalizedName = product.name.toLowerCase();

                // Index by words in the name
                String[] words = normalizedName.split("\\s+");
                for (String word : words) {
                    if (word.length() >= 3) { // Only index words with 3+ characters
                        nameSearchIndex
                            .computeIfAbsent(word, k -> new ArrayList<>())
                            .add(product);
                    }
                }

                // Also index the full name
                nameSearchIndex
                    .computeIfAbsent(normalizedName, k -> new ArrayList<>())
                    .add(product);
            }

            // Index by active ingredient (hatóanyag)
            if (product.activeIngredient != null && !product.activeIngredient.isEmpty()) {
                String normalizedIngredient = product.activeIngredient.toLowerCase();

                // Index by words in the active ingredient
                String[] words = normalizedIngredient.split("\\s+");
                for (String word : words) {
                    if (word.length() >= 3) {
                        nameSearchIndex
                            .computeIfAbsent(word, k -> new ArrayList<>())
                            .add(product);
                    }
                }

                // Also index the full active ingredient
                nameSearchIndex
                    .computeIfAbsent(normalizedIngredient, k -> new ArrayList<>())
                    .add(product);
            }
        }

        logger.debug("Search index built with {} keys (includes names and active ingredients)", nameSearchIndex.size());
    }
    
    /**
     * Format search results as comprehensive PUPHAX-compatible XML with all available fields.
     */
    private String formatSearchResults(List<ProductRecord> products, String searchTerm) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<drugSearchResponse>\n");
        xml.append("  <totalCount>").append(products.size()).append("</totalCount>\n");
        xml.append("  <source>CSV Fallback (NEAK Historical Data 2007-2023)</source>\n");
        xml.append("  <drugs>\n");

        for (ProductRecord product : products) {
            xml.append("    <drug>\n");

            // Core identification
            xml.append("      <id>").append(escapeXml(product.id)).append("</id>\n");
            if (product.parentId != null && !product.parentId.isEmpty()) {
                xml.append("      <parentId>").append(escapeXml(product.parentId)).append("</parentId>\n");
            }
            xml.append("      <productCode>").append(escapeXml(product.termekKod)).append("</productCode>\n");
            if (product.eanKod != null && !product.eanKod.isEmpty()) {
                xml.append("      <eanCode>").append(escapeXml(product.eanKod)).append("</eanCode>\n");
            }

            // Names
            xml.append("      <name>").append(escapeXml(product.name)).append("</name>\n");
            if (product.shortName != null && !product.shortName.isEmpty()) {
                xml.append("      <shortName>").append(escapeXml(product.shortName)).append("</shortName>\n");
            }

            // Manufacturer
            String manufacturer = companies.getOrDefault(product.brandId, "Unknown");
            String brand = brandNames.getOrDefault(product.brandId, manufacturer);
            xml.append("      <manufacturer>").append(escapeXml(manufacturer)).append("</manufacturer>\n");
            xml.append("      <brand>").append(escapeXml(brand)).append("</brand>\n");

            // Classification
            if (product.atc != null && !product.atc.isEmpty()) {
                xml.append("      <atcCode>").append(escapeXml(product.atc)).append("</atcCode>\n");
                String atcDescription = atcCodes.getOrDefault(product.atc, "");
                if (!atcDescription.isEmpty()) {
                    xml.append("      <atcDescription>").append(escapeXml(atcDescription)).append("</atcDescription>\n");
                }
            }

            // Active ingredients
            xml.append("      <activeIngredients>\n");
            if (product.activeIngredient != null && !product.activeIngredient.isEmpty()) {
                xml.append("        <ingredient><name>").append(escapeXml(product.activeIngredient)).append("</name></ingredient>\n");
            }
            xml.append("      </activeIngredients>\n");

            // Form and administration
            if (product.gyForma != null && !product.gyForma.isEmpty()) {
                xml.append("      <pharmaceuticalForm>").append(escapeXml(product.gyForma)).append("</pharmaceuticalForm>\n");
            }
            if (product.adagMod != null && !product.adagMod.isEmpty()) {
                xml.append("      <administrationMethod>").append(escapeXml(product.adagMod)).append("</administrationMethod>\n");
            }

            // Strength and dosage
            if (product.potencia != null && !product.potencia.isEmpty()) {
                xml.append("      <strength>").append(escapeXml(product.potencia)).append("</strength>\n");
            }
            if (product.hatoMenny != null && !product.hatoMenny.isEmpty()) {
                xml.append("      <activeSubstanceAmount>").append(escapeXml(product.hatoMenny));
                if (product.hatoEgys != null && !product.hatoEgys.isEmpty()) {
                    xml.append(" ").append(escapeXml(product.hatoEgys));
                }
                xml.append("</activeSubstanceAmount>\n");
            }
            if (product.kiszMenny != null && !product.kiszMenny.isEmpty()) {
                xml.append("      <packageSize>").append(escapeXml(product.kiszMenny));
                if (product.kiszEgys != null && !product.kiszEgys.isEmpty()) {
                    xml.append(" ").append(escapeXml(product.kiszEgys));
                }
                xml.append("</packageSize>\n");
            }

            // DDD (Defined Daily Dose)
            if (product.dddMenny != null && !product.dddMenny.isEmpty()) {
                xml.append("      <ddd>").append(escapeXml(product.dddMenny));
                if (product.dddEgys != null && !product.dddEgys.isEmpty()) {
                    xml.append(" ").append(escapeXml(product.dddEgys));
                }
                if (product.dddFaktor != null && !product.dddFaktor.isEmpty()) {
                    xml.append(" (factor: ").append(escapeXml(product.dddFaktor)).append(")");
                }
                xml.append("</ddd>\n");
            }

            // Regulatory status
            xml.append("      <prescriptionRequired>").append(product.ttt != null && product.ttt.startsWith("2")).append("</prescriptionRequired>\n");
            if (product.ttt != null && !product.ttt.isEmpty()) {
                xml.append("      <tttCode>").append(escapeXml(product.ttt)).append("</tttCode>\n");
            }
            if (product.tk != null && !product.tk.isEmpty()) {
                xml.append("      <tkCode>").append(escapeXml(product.tk)).append("</tkCode>\n");
            }
            xml.append("      <reimbursable>true</reimbursable>\n");
            if (product.rendelhet != null && !product.rendelhet.isEmpty()) {
                xml.append("      <prescribable>").append(escapeXml(product.rendelhet)).append("</prescribable>\n");
            }
            if (product.helyettesith != null && !product.helyettesith.isEmpty()) {
                xml.append("      <substitutable>").append(escapeXml(product.helyettesith)).append("</substitutable>\n");
            }

            // Special attributes
            if (product.patika != null && !product.patika.isEmpty()) {
                xml.append("      <pharmacyOnly>").append(escapeXml(product.patika)).append("</pharmacyOnly>\n");
            }
            if (product.keresztJelzes != null && !product.keresztJelzes.isEmpty()) {
                xml.append("      <crossReference>").append(escapeXml(product.keresztJelzes)).append("</crossReference>\n");
            }

            // Validity dates
            if (product.validFrom != null) {
                xml.append("      <validFrom>").append(product.validFrom.toString()).append("</validFrom>\n");
            }
            if (product.validTo != null) {
                xml.append("      <validTo>").append(product.validTo.toString()).append("</validTo>\n");
            }

            // Status
            xml.append("      <status>").append(product.inStock ? "ACTIVE" : "INACTIVE").append("</status>\n");
            xml.append("      <inStock>").append(product.inStock).append("</inStock>\n");

            xml.append("    </drug>\n");
        }

        xml.append("  </drugs>\n");
        xml.append("</drugSearchResponse>");

        return xml.toString();
    }
    
    private String createErrorResponse(String message) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <drugSearchResponse>
                <totalCount>0</totalCount>
                <drugs></drugs>
                <error>%s</error>
            </drugSearchResponse>
            """, escapeXml(message));
    }
    
    private String unquote(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
    
    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("99")) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Product record from TERMEK table with all 44 CSV fields.
     */
    private static class ProductRecord {
        // Core identification fields
        String id;                      // ID (column 0)
        String parentId;                // PARENT_ID (column 1)
        LocalDate validFrom;            // ERV_KEZD (column 2)
        LocalDate validTo;              // ERV_VEGE (column 3)
        String termekKod;               // TERMEKKOD (column 4)
        String kozHid;                  // KOZHID (column 5)
        String ttt;                     // TTT (column 6)
        String tk;                      // TK (column 7)
        String tkTorles;                // TKTORLES (column 8)
        LocalDate tkTorlesDate;         // TKTORLESDAT (column 9)
        String eanKod;                  // EANKOD (column 10)
        String brandId;                 // BRAND_ID (column 11)

        // Name and description fields
        String name;                    // NEV (column 12)
        String shortName;               // KISZNEV (column 13)

        // Classification fields
        String atc;                     // ATC (column 14)
        String iso;                     // ISO (column 15)
        String activeIngredient;        // HATOANYAG (column 16)

        // Administration and form fields
        String adagMod;                 // ADAGMOD (column 17)
        String gyForma;                 // GYFORMA (column 18)
        String rendelhet;               // RENDELHET (column 19)
        String egyenId;                 // EGYEN_ID (column 20)
        String helyettesith;            // HELYETTESITH (column 21)

        // Strength and dosage fields
        String potencia;                // POTENCIA (column 22)
        String oHatoMenny;              // OHATO_MENNY (column 23)
        String hatoMenny;               // HATO_MENNY (column 24)
        String hatoEgys;                // HATO_EGYS (column 25)
        String kiszMenny;               // KISZ_MENNY (column 26)
        String kiszEgys;                // KISZ_EGYS (column 27)
        String dddMenny;                // DDD_MENNY (column 28)
        String dddEgys;                 // DDD_EGYS (column 29)
        String dddFaktor;               // DDD_FAKTOR (column 30)
        String dot;                     // DOT (column 31)
        String adagMenny;               // ADAG_MENNY (column 32)
        String adagEgys;                // ADAG_EGYS (column 33)

        // Special attributes
        String egyedi;                  // EGYEDI (column 34)
        String oldalIsag;               // OLDALISAG (column 35)
        String tobblGar;                // TOBBLGAR (column 36)
        String patika;                  // PATIKA (column 37)
        String dobAzon;                 // DOBAZON (column 38)
        String keresztJelzes;           // KERESZTJELZES (column 39)

        // Distribution fields
        String forgEngtId;              // FORGENGT_ID (column 40)
        String forgazId;                // FORGALMAZ_ID (column 41)
        boolean inStock;                // FORGALOMBAN (column 42)
        String kihirdetesId;            // KIHIRDETES_ID (column 43)
    }
}
