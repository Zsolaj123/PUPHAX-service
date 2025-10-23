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
                    product.id = fields[0].trim();
                    product.termekKod = unquote(fields[4]);
                    product.ttt = unquote(fields[6]);
                    product.name = unquote(fields[12]);
                    product.shortName = unquote(fields[13]);
                    product.atc = unquote(fields[14]);
                    product.iso = unquote(fields[15]);
                    product.activeIngredient = unquote(fields[16]);
                    product.brandId = fields[11].trim();
                    product.validFrom = validFrom;
                    product.validTo = validTo;
                    product.inStock = "1".equals(fields[42]);
                    
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
     * Format search results as PUPHAX-compatible XML.
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
            xml.append("      <id>").append(escapeXml(product.id)).append("</id>\n");
            xml.append("      <name>").append(escapeXml(product.name)).append("</name>\n");
            
            String manufacturer = companies.getOrDefault(product.brandId, "Unknown");
            xml.append("      <manufacturer>").append(escapeXml(manufacturer)).append("</manufacturer>\n");
            
            xml.append("      <atcCode>").append(escapeXml(product.atc)).append("</atcCode>\n");
            xml.append("      <activeIngredients>\n");
            if (product.activeIngredient != null && !product.activeIngredient.isEmpty()) {
                xml.append("        <ingredient><name>").append(escapeXml(product.activeIngredient)).append("</name></ingredient>\n");
            }
            xml.append("      </activeIngredients>\n");
            xml.append("      <prescriptionRequired>").append(product.ttt != null && product.ttt.startsWith("2")).append("</prescriptionRequired>\n");
            xml.append("      <reimbursable>true</reimbursable>\n");
            xml.append("      <status>").append(product.inStock ? "ACTIVE" : "INACTIVE").append("</status>\n");
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
     * Product record from TERMEK table.
     */
    private static class ProductRecord {
        String id;
        String termekKod;
        String ttt;
        String name;
        String shortName;
        String atc;
        String iso;
        String activeIngredient;
        String brandId;
        LocalDate validFrom;
        LocalDate validTo;
        boolean inStock;
    }
}
