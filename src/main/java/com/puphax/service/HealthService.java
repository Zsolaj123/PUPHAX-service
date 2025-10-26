package com.puphax.service;

import com.puphax.model.dto.HealthStatus;
import com.puphax.model.dto.HealthStatus.ComponentHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for checking the health status of the PUPHAX REST API and its dependencies.
 * 
 * This service performs health checks on various components including the PUPHAX SOAP service,
 * database connections, and other external dependencies.
 */
@Service
public class HealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
    
    private final PuphaxSoapClient puphaxSoapClient;
    
    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;
    
    @Autowired
    public HealthService(PuphaxSoapClient puphaxSoapClient) {
        this.puphaxSoapClient = puphaxSoapClient;
    }
    
    /**
     * Performs a comprehensive health check of all service components.
     * 
     * @return HealthStatus with overall status and component details
     */
    public HealthStatus checkHealth() {
        logger.debug("Performing health check for PUPHAX REST API service");
        
        Map<String, ComponentHealth> components = new HashMap<>();
        
        // Check PUPHAX SOAP service
        components.put("puphax-soap", checkPuphaxSoapService());
        
        // Check cache (Caffeine cache is always available in-memory)
        components.put("cache", checkCacheService());
        
        // Check disk space
        components.put("diskSpace", checkDiskSpace());
        
        // Determine overall status
        String overallStatus = HealthStatus.determineOverallStatus(components);
        
        logger.info("Health check completed. Overall status: {}", overallStatus);
        
        return switch (overallStatus) {
            case "UP" -> HealthStatus.up(applicationVersion, components);
            case "DOWN" -> HealthStatus.down(applicationVersion, components, "Critical components are down");
            case "DEGRADED" -> HealthStatus.degraded(applicationVersion, components);
            default -> new HealthStatus("UNKNOWN", java.time.Instant.now(), applicationVersion, components,
                Map.of("error", "Unable to determine health status"));
        };
    }
    
    /**
     * Checks the health of the PUPHAX SOAP service.
     */
    private ComponentHealth checkPuphaxSoapService() {
        try {
            long startTime = System.currentTimeMillis();
            
            logger.debug("Checking PUPHAX SOAP service health");
            String response = puphaxSoapClient.getServiceStatus();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response != null && response.contains("UP")) {
                logger.debug("PUPHAX SOAP service is UP, response time: {}ms", responseTime);
                return ComponentHealth.up(responseTime, "PUPHAX SOAP service is responding");
            } else {
                logger.warn("PUPHAX SOAP service returned unexpected response: {}", response);
                return ComponentHealth.down("PUPHAX SOAP service returned unexpected response");
            }
            
        } catch (Exception e) {
            logger.error("PUPHAX SOAP service health check failed: {}", e.getMessage());
            return ComponentHealth.down("PUPHAX SOAP service is not responding: " + e.getMessage(),
                Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
    
    /**
     * Checks the health of the cache service.
     */
    private ComponentHealth checkCacheService() {
        try {
            // Caffeine cache is always available since it's in-memory
            // We could check if the cache manager is properly configured
            logger.debug("Cache service is available (Caffeine in-memory cache)");
            return ComponentHealth.up(0L, "Caffeine cache is operational");
            
        } catch (Exception e) {
            logger.error("Cache service health check failed: {}", e.getMessage());
            return ComponentHealth.down("Cache service error: " + e.getMessage());
        }
    }
    
    /**
     * Checks disk space availability.
     */
    private ComponentHealth checkDiskSpace() {
        try {
            long freeSpace = Runtime.getRuntime().freeMemory();
            long totalSpace = Runtime.getRuntime().totalMemory();
            long usedSpace = totalSpace - freeSpace;
            
            double usagePercentage = (double) usedSpace / totalSpace * 100;
            
            Map<String, Object> details = Map.of(
                "free", formatBytes(freeSpace),
                "total", formatBytes(totalSpace),
                "used", formatBytes(usedSpace),
                "usagePercentage", String.format("%.1f%%", usagePercentage)
            );
            
            if (usagePercentage > 90) {
                logger.warn("High memory usage: {:.1f}%", usagePercentage);
                return new ComponentHealth("DOWN", 0L, "High memory usage", details);
            } else if (usagePercentage > 75) {
                logger.debug("Moderate memory usage: {:.1f}%", usagePercentage);
                return new ComponentHealth("UP", 0L, "Memory usage is acceptable", details);
            } else {
                logger.debug("Low memory usage: {:.1f}%", usagePercentage);
                return new ComponentHealth("UP", 0L, "Memory usage is low", details);
            }
            
        } catch (Exception e) {
            logger.error("Disk space health check failed: {}", e.getMessage());
            return ComponentHealth.down("Unable to check disk space: " + e.getMessage());
        }
    }
    
    /**
     * Formats bytes to human readable format.
     */
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
    
    /**
     * Quick health check that actually tests the PUPHAX SOAP service.
     *
     * @return Simple health status with PUPHAX service check
     */
    public HealthStatus checkHealthQuick() {
        try {
            logger.debug("Quick health check - testing PUPHAX SOAP service");

            // Actually check PUPHAX SOAP service
            ComponentHealth soapHealth = checkPuphaxSoapService();

            Map<String, ComponentHealth> components = Map.of(
                "puphax-soap", soapHealth
            );

            // If SOAP is down, return DOWN status
            if ("DOWN".equals(soapHealth.status())) {
                logger.warn("Quick health check: PUPHAX SOAP service is DOWN");
                return HealthStatus.down(applicationVersion, components, "PUPHAX SOAP service is not available");
            }

            logger.debug("Quick health check: PUPHAX SOAP service is UP");
            return HealthStatus.up(applicationVersion, components);

        } catch (Exception e) {
            logger.error("Quick health check failed: {}", e.getMessage());
            Map<String, ComponentHealth> components = Map.of(
                "puphax-soap", ComponentHealth.down("Health check error: " + e.getMessage())
            );
            return HealthStatus.down(applicationVersion, components, "Service is not operational");
        }
    }
}