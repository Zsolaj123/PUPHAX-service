package com.puphax.controller;

import com.puphax.model.dto.DrugSearchResponse;
import com.puphax.model.dto.HealthStatus;
import com.puphax.service.DrugService;
import com.puphax.service.HealthService;
import com.puphax.exception.PuphaxValidationException;
import com.puphax.util.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for drug search operations.
 * 
 * This controller provides endpoints for searching drugs in the PUPHAX database
 * with support for filtering, pagination, and sorting.
 */
@RestController
@RequestMapping("/api/v1/drugs")
@Validated
@Tag(name = "Drug Search", description = "API for searching drugs in the PUPHAX database")
public class DrugController {
    
    private static final Logger logger = LoggerFactory.getLogger(DrugController.class);
    
    private final DrugService drugService;
    private final HealthService healthService;
    
    @Autowired
    public DrugController(DrugService drugService, HealthService healthService) {
        this.drugService = drugService;
        this.healthService = healthService;
    }
    
    /**
     * Searches for drugs based on the provided criteria.
     * 
     * @param term Drug name or partial name to search for (required)
     * @param manufacturer Optional manufacturer filter
     * @param atcCode Optional ATC code filter (format: A10AB01)
     * @param page Page number, starting from 0 (default: 0)
     * @param size Page size, maximum 100 (default: 20)
     * @param sortBy Sort field: name, manufacturer, or atcCode (default: name)
     * @param sortDirection Sort direction: ASC or DESC (default: ASC)
     * @return DrugSearchResponse with paginated results
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search for drugs",
        description = "Search for drugs by name with optional filtering by manufacturer and ATC code. Supports pagination and sorting."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DrugSearchResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "503",
            description = "PUPHAX service unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    public ResponseEntity<DrugSearchResponse> searchDrugs(
        
        @Parameter(
            description = "Drug name or partial name to search for",
            required = true,
            example = "aspirin"
        )
        @RequestParam("term")
        @NotBlank(message = "Search term cannot be blank")
        @Size(min = 2, max = 100, message = "Search term must be between 2 and 100 characters")
        String term,
        
        @Parameter(
            description = "Filter by manufacturer name",
            required = false,
            example = "Bayer"
        )
        @RequestParam(value = "manufacturer", required = false)
        @Size(max = 100, message = "Manufacturer filter cannot exceed 100 characters")
        String manufacturer,
        
        @Parameter(
            description = "Filter by ATC code (format: A10AB01)",
            required = false,
            example = "N02BA01"
        )
        @RequestParam(value = "atcCode", required = false)
        @Pattern(
            regexp = "^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$",
            message = "ATC code must follow the format: A10AB01"
        )
        String atcCode,
        
        @Parameter(
            description = "Page number (0-based)",
            required = false,
            example = "0"
        )
        @RequestParam(value = "page", defaultValue = "0")
        @Min(value = 0, message = "Page number must be 0 or greater")
        int page,
        
        @Parameter(
            description = "Page size (1-100)",
            required = false,
            example = "20"
        )
        @RequestParam(value = "size", defaultValue = "20")
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        int size,
        
        @Parameter(
            description = "Sort field",
            required = false,
            example = "name"
        )
        @RequestParam(value = "sortBy", defaultValue = "name")
        @Pattern(
            regexp = "^(name|manufacturer|atcCode)$",
            message = "Sort field must be one of: name, manufacturer, atcCode"
        )
        String sortBy,
        
        @Parameter(
            description = "Sort direction",
            required = false,
            example = "ASC"
        )
        @RequestParam(value = "sortDirection", defaultValue = "ASC")
        @Pattern(
            regexp = "^(ASC|DESC)$",
            message = "Sort direction must be ASC or DESC"
        )
        String sortDirection,
        HttpServletRequest request
    ) {
        
        String correlationId = LoggingUtils.generateCorrelationId();
        long startTime = System.currentTimeMillis();
        
        try {
            // Set up logging context
            LoggingUtils.setupSearchContext(correlationId, term, manufacturer, atcCode, page, size);
            LoggingUtils.setClientIp(getClientIpAddress(request));
            
            logger.info("Drug search request started: term='{}', manufacturer='{}', atcCode='{}', page={}, size={}, sortBy={}, sortDirection={}",
                       term, manufacturer, atcCode, page, size, sortBy, sortDirection);
            
            // Validate inputs
            validateSearchParameters(term, manufacturer, atcCode, page, size, sortBy, sortDirection);
            
            // Perform search
            DrugSearchResponse response = drugService.searchDrugs(
                term, manufacturer, atcCode, page, size, sortBy, sortDirection
            );
            
            // Log success metrics
            long responseTime = System.currentTimeMillis() - startTime;
            LoggingUtils.setResponseTime(responseTime);
            LoggingUtils.setResultCount(response.getCurrentPageSize());
            
            logger.info("Drug search completed successfully: {} results found for term '{}', total elements: {}, response time: {}ms",
                       response.getCurrentPageSize(), term, response.pagination().totalElements(), responseTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            LoggingUtils.setResponseTime(responseTime);
            LoggingUtils.setupErrorContext(correlationId, 
                e instanceof PuphaxValidationException ? "VALIDATION_ERROR" : "SEARCH_ERROR", 
                "drug-search");
            
            logger.error("Drug search failed for term '{}': {} (response time: {}ms)", 
                        term, e.getMessage(), responseTime);
            throw e;
            
        } finally {
            LoggingUtils.clearContext();
        }
    }
    
    /**
     * Validates search parameters and throws appropriate exceptions for invalid input.
     * 
     * @param term Search term
     * @param manufacturer Manufacturer filter
     * @param atcCode ATC code filter
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDirection Sort direction
     * @throws PuphaxValidationException if validation fails
     */
    private void validateSearchParameters(String term, String manufacturer, String atcCode,
                                        int page, int size, String sortBy, String sortDirection) {
        
        // Term validation
        if (term == null || term.trim().isEmpty()) {
            throw new PuphaxValidationException("term", term, "Search term cannot be blank");
        }
        
        if (term.length() < 2 || term.length() > 100) {
            throw new PuphaxValidationException("term", term, "Search term must be between 2 and 100 characters");
        }
        
        // Manufacturer validation
        if (manufacturer != null && manufacturer.length() > 100) {
            throw new PuphaxValidationException("manufacturer", manufacturer, "Manufacturer filter cannot exceed 100 characters");
        }
        
        // ATC code validation
        if (atcCode != null && !atcCode.matches("^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$")) {
            throw new PuphaxValidationException("atcCode", atcCode, "ATC code must follow the format: A10AB01");
        }
        
        // Pagination validation
        if (page < 0) {
            throw new PuphaxValidationException("page", page, "Page number must be 0 or greater");
        }
        
        if (size < 1 || size > 100) {
            throw new PuphaxValidationException("size", size, "Page size must be between 1 and 100");
        }
        
        // Sort validation
        if (!"name".equals(sortBy) && !"manufacturer".equals(sortBy) && !"atcCode".equals(sortBy)) {
            throw new PuphaxValidationException("sortBy", sortBy, "Sort field must be one of: name, manufacturer, atcCode");
        }
        
        if (!"ASC".equals(sortDirection) && !"DESC".equals(sortDirection)) {
            throw new PuphaxValidationException("sortDirection", sortDirection, "Sort direction must be ASC or DESC");
        }
    }
    
    /**
     * Comprehensive health check endpoint for the drug search service.
     * 
     * @return Detailed health status including PUPHAX service connectivity
     */
    @GetMapping("/health")
    @Operation(
        summary = "Check drug search service health",
        description = "Comprehensive health check endpoint that verifies PUPHAX service connectivity and component status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy or partially healthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthStatus.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service is down or unavailable",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthStatus.class)
            )
        )
    })
    public ResponseEntity<HealthStatus> health(HttpServletRequest request) {
        String correlationId = LoggingUtils.generateCorrelationId();
        long startTime = System.currentTimeMillis();
        
        try {
            LoggingUtils.setupHealthCheckContext(correlationId);
            LoggingUtils.setClientIp(getClientIpAddress(request));
            
            logger.debug("Health check requested for drug search service");
            
            HealthStatus healthStatus = healthService.checkHealth();
            
            long responseTime = System.currentTimeMillis() - startTime;
            LoggingUtils.setResponseTime(responseTime);
            
            // Return 503 Service Unavailable if the service is down
            if ("DOWN".equals(healthStatus.status())) {
                logger.warn("Health check failed: service is DOWN (response time: {}ms)", responseTime);
                return ResponseEntity.status(503).body(healthStatus);
            }
            
            // Return 200 OK for UP or DEGRADED status
            logger.info("Health check completed: service status is {} (response time: {}ms)", 
                       healthStatus.status(), responseTime);
            return ResponseEntity.ok(healthStatus);
            
        } finally {
            LoggingUtils.clearContext();
        }
    }
    
    /**
     * Quick health check endpoint for load balancers and monitoring.
     * 
     * @return Simple health status
     */
    @GetMapping("/health/quick")
    @Operation(
        summary = "Quick health check",
        description = "Lightweight health check endpoint for load balancers and monitoring systems"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is operational",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
    )
    public ResponseEntity<HealthStatus> healthQuick() {
        logger.debug("Quick health check requested");
        
        HealthStatus healthStatus = healthService.checkHealthQuick();
        
        if ("DOWN".equals(healthStatus.status())) {
            return ResponseEntity.status(503).body(healthStatus);
        }
        
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * Simple test endpoint to verify the service is working.
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok("{\"message\":\"DrugController is working!\",\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }
    
    /**
     * Simple search endpoint for testing without complex logic.
     */
    @GetMapping("/search-simple")
    public ResponseEntity<String> searchSimple(@RequestParam("term") String term) {
        logger.info("Simple search called with term: {}", term);
        
        String response = String.format("""
            {
                "searchTerm": "%s",
                "drugs": [
                    {
                        "id": "HU001234",
                        "name": "%s 100mg tabletta",
                        "manufacturer": "Bayer Hungary Kft.",
                        "atcCode": "N02BA01",
                        "activeIngredients": ["Acetylsalicylic acid"],
                        "prescriptionRequired": false,
                        "reimbursable": true,
                        "status": "ACTIVE"
                    }
                ],
                "pagination": {
                    "currentPage": 0,
                    "pageSize": 20,
                    "totalPages": 1,
                    "totalElements": 1
                }
            }
            """, term, term);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extracts the client IP address from the HTTP request.
     * 
     * @param request HTTP servlet request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
