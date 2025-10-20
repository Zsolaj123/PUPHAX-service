package com.puphax.service;

import com.puphax.model.dto.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthService.
 * 
 * These tests verify that health checks work correctly for various
 * service states and component configurations.
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {
    
    @Mock
    private PuphaxSoapClient mockSoapClient;
    
    private HealthService healthService;
    
    @BeforeEach
    void setUp() {
        healthService = new HealthService(mockSoapClient);
        ReflectionTestUtils.setField(healthService, "applicationVersion", "1.0.0-test");
    }
    
    @Test
    void checkHealth_AllComponentsUp_ReturnsUpStatus() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenReturn("<serviceStatus><status>UP</status><message>Service operational</message></serviceStatus>");
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        assertNotNull(result);
        assertEquals("UP", result.status());
        assertEquals("1.0.0-test", result.version());
        assertNotNull(result.timestamp());
        assertNotNull(result.components());
        
        // Check individual components
        assertTrue(result.components().containsKey("puphax-soap"));
        assertTrue(result.components().containsKey("cache"));
        assertTrue(result.components().containsKey("diskSpace"));
        
        assertEquals("UP", result.components().get("puphax-soap").status());
        assertEquals("UP", result.components().get("cache").status());
        assertEquals("UP", result.components().get("diskSpace").status());
        
        verify(mockSoapClient).getServiceStatus();
    }
    
    @Test
    void checkHealth_PuphaxSoapDown_ReturnsDegradedStatus() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenThrow(new RuntimeException("Connection failed"));
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        assertNotNull(result);
        assertEquals("DEGRADED", result.status());
        assertEquals("1.0.0-test", result.version());
        
        // Check that PUPHAX SOAP is down but other components are up
        assertEquals("DOWN", result.components().get("puphax-soap").status());
        assertEquals("UP", result.components().get("cache").status());
        assertEquals("UP", result.components().get("diskSpace").status());
        
        verify(mockSoapClient).getServiceStatus();
    }
    
    @Test
    void checkHealth_PuphaxSoapInvalidResponse_ReturnsDegradedStatus() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenReturn("<serviceStatus><status>UNKNOWN</status></serviceStatus>");
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        assertNotNull(result);
        assertEquals("DEGRADED", result.status());
        
        assertEquals("DOWN", result.components().get("puphax-soap").status());
        assertTrue(result.components().get("puphax-soap").message().contains("unexpected response"));
        
        verify(mockSoapClient).getServiceStatus();
    }
    
    @Test
    void checkHealthQuick_ServiceOperational_ReturnsUpStatus() {
        // When
        HealthStatus result = healthService.checkHealthQuick();
        
        // Then
        assertNotNull(result);
        assertEquals("UP", result.status());
        assertEquals("1.0.0-test", result.version());
        
        assertTrue(result.components().containsKey("api"));
        assertEquals("UP", result.components().get("api").status());
        assertEquals("API is operational", result.components().get("api").message());
        
        // Should not call PUPHAX service for quick check
        verifyNoInteractions(mockSoapClient);
    }
    
    @Test
    void checkHealth_ComponentHealthUp_CorrectResponseTime() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenReturn("<serviceStatus><status>UP</status><message>Service operational</message></serviceStatus>");
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        HealthStatus.ComponentHealth puphaxHealth = result.components().get("puphax-soap");
        assertNotNull(puphaxHealth.responseTimeMs());
        assertTrue(puphaxHealth.responseTimeMs() >= 0);
        assertEquals("PUPHAX SOAP service is responding", puphaxHealth.message());
    }
    
    @Test
    void checkHealth_ComponentDetails_ArePresent() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenReturn("<serviceStatus><status>UP</status></serviceStatus>");
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        assertNotNull(result.details());
        assertTrue(result.details().containsKey("application"));
        assertTrue(result.details().containsKey("description"));
        assertEquals("PUPHAX REST API Service", result.details().get("application"));
    }
    
    @Test
    void checkHealth_DiskSpaceComponent_HasDetails() {
        // Given
        when(mockSoapClient.getServiceStatus())
            .thenReturn("<serviceStatus><status>UP</status></serviceStatus>");
        
        // When
        HealthStatus result = healthService.checkHealth();
        
        // Then
        HealthStatus.ComponentHealth diskSpaceHealth = result.components().get("diskSpace");
        assertNotNull(diskSpaceHealth);
        assertEquals("UP", diskSpaceHealth.status());
        
        // Check that details contain memory information
        assertNotNull(diskSpaceHealth.details());
        assertTrue(diskSpaceHealth.details().containsKey("free"));
        assertTrue(diskSpaceHealth.details().containsKey("total"));
        assertTrue(diskSpaceHealth.details().containsKey("used"));
        assertTrue(diskSpaceHealth.details().containsKey("usagePercentage"));
    }
    
    @Test
    void determineOverallStatus_AllUp_ReturnsUp() {
        // Given
        var components = java.util.Map.of(
            "service1", HealthStatus.ComponentHealth.up(100L, "OK"),
            "service2", HealthStatus.ComponentHealth.up(200L, "OK")
        );
        
        // When
        String status = HealthStatus.determineOverallStatus(components);
        
        // Then
        assertEquals("UP", status);
    }
    
    @Test
    void determineOverallStatus_OneDown_ReturnsDegraded() {
        // Given
        var components = java.util.Map.of(
            "service1", HealthStatus.ComponentHealth.up(100L, "OK"),
            "service2", HealthStatus.ComponentHealth.down("Error")
        );
        
        // When
        String status = HealthStatus.determineOverallStatus(components);
        
        // Then
        assertEquals("DEGRADED", status);
    }
    
    @Test
    void determineOverallStatus_EmptyComponents_ReturnsUnknown() {
        // Given
        var components = java.util.Map.<String, HealthStatus.ComponentHealth>of();
        
        // When
        String status = HealthStatus.determineOverallStatus(components);
        
        // Then
        assertEquals("UNKNOWN", status);
    }
}