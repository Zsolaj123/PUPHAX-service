package com.puphax.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Health status response DTO.
 * 
 * This class represents the health status of the PUPHAX REST API service
 * including the status of external dependencies like the PUPHAX SOAP service.
 */
public record HealthStatus(
    String status,
    Instant timestamp,
    String version,
    Map<String, ComponentHealth> components,
    Map<String, Object> details
) {
    
    /**
     * Represents the health status of individual components.
     */
    public record ComponentHealth(
        String status,
        Long responseTimeMs,
        String message,
        Map<String, Object> details
    ) {
        
        public static ComponentHealth up(long responseTimeMs, String message) {
            return new ComponentHealth("UP", responseTimeMs, message, Map.of());
        }
        
        public static ComponentHealth down(String message) {
            return new ComponentHealth("DOWN", null, message, Map.of());
        }
        
        public static ComponentHealth down(String message, Map<String, Object> details) {
            return new ComponentHealth("DOWN", null, message, details);
        }
        
        public static ComponentHealth unknown(String message) {
            return new ComponentHealth("UNKNOWN", null, message, Map.of());
        }
    }
    
    /**
     * Creates a healthy status response.
     */
    public static HealthStatus up(String version, Map<String, ComponentHealth> components) {
        return new HealthStatus(
            "UP",
            Instant.now(),
            version,
            components,
            Map.of(
                "application", "PUPHAX REST API Service",
                "description", "REST API service for NEAK PUPHAX drug database"
            )
        );
    }
    
    /**
     * Creates a degraded status response (some components down).
     */
    public static HealthStatus degraded(String version, Map<String, ComponentHealth> components) {
        return new HealthStatus(
            "DEGRADED",
            Instant.now(),
            version,
            components,
            Map.of(
                "application", "PUPHAX REST API Service",
                "description", "REST API service for NEAK PUPHAX drug database",
                "warning", "Some components are not functioning properly"
            )
        );
    }
    
    /**
     * Creates a down status response.
     */
    public static HealthStatus down(String version, Map<String, ComponentHealth> components, String reason) {
        return new HealthStatus(
            "DOWN",
            Instant.now(),
            version,
            components,
            Map.of(
                "application", "PUPHAX REST API Service",
                "description", "REST API service for NEAK PUPHAX drug database",
                "error", reason
            )
        );
    }
    
    /**
     * Determines overall status based on component health.
     */
    public static String determineOverallStatus(Map<String, ComponentHealth> components) {
        if (components.isEmpty()) {
            return "UNKNOWN";
        }
        
        boolean allUp = components.values().stream()
            .allMatch(component -> "UP".equals(component.status()));
        
        boolean anyDown = components.values().stream()
            .anyMatch(component -> "DOWN".equals(component.status()));
        
        if (allUp) {
            return "UP";
        } else if (anyDown) {
            return "DEGRADED";
        } else {
            return "UNKNOWN";
        }
    }
}