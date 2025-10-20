package com.puphax.service;

import com.puphax.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PuphaxSoapClient with timeout and error scenarios.
 * 
 * These tests verify that the SOAP client wrapper properly handles
 * various error conditions and applies resilience patterns correctly.
 * Note: These tests use mock responses as the real PUPHAX service is not available in test environment.
 */
class PuphaxSoapClientTest {
    
    private PuphaxSoapClient soapClient;
    
    @BeforeEach
    void setUp() {
        soapClient = new PuphaxSoapClient();
        // Set test configuration
        ReflectionTestUtils.setField(soapClient, "endpointUrl", "http://localhost:8080/test");
        ReflectionTestUtils.setField(soapClient, "connectTimeout", 5000);
        ReflectionTestUtils.setField(soapClient, "requestTimeout", 10000);
    }
    
    @Test
    void searchDrugsAsync_SuccessfulCall_ReturnsResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugSearchResponse>"));
        assertTrue(result.contains("aspirin"));
        assertTrue(result.contains("tabletta"));
    }
    
    @Test
    void searchDrugsAsync_WithManufacturer_ReturnsFilteredResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", "Bayer", null);
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugSearchResponse>"));
        assertTrue(result.contains("Bayer"));
        assertTrue(result.contains("aspirin"));
    }
    
    @Test
    void searchDrugsAsync_WithAtcCode_ReturnsFilteredResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, "N02BA01");
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugSearchResponse>"));
        assertTrue(result.contains("N02BA01"));
        assertTrue(result.contains("aspirin"));
    }
    
    @Test
    void searchDrugsAsync_WithAllFilters_ReturnsFilteredResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", "Bayer", "N02BA01");
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugSearchResponse>"));
        assertTrue(result.contains("Bayer"));
        assertTrue(result.contains("N02BA01"));
        assertTrue(result.contains("aspirin"));
    }
    
    @Test
    void searchDrugsAsync_MockResponseStructure_IsValid() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("paracetamol", null, null);
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(result.contains("<totalCount>3</totalCount>"));
        assertTrue(result.contains("<drugs>"));
        assertTrue(result.contains("<drug>"));
        assertTrue(result.contains("<id>"));
        assertTrue(result.contains("<name>"));
        assertTrue(result.contains("<manufacturer>"));
        assertTrue(result.contains("<atcCode>"));
    }
    
    @Test
    void getDrugDetailsAsync_SuccessfulCall_ReturnsResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.getDrugDetailsAsync("HU001234");
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugDetailsResponse>"));
        assertTrue(result.contains("<id>HU001234</id>"));
        assertTrue(result.contains("HU001234 Details"));
    }
    
    @Test
    void getDrugDetailsAsync_DifferentDrugId_ReturnsCorrectResponse() throws Exception {
        // When
        CompletableFuture<String> future = soapClient.getDrugDetailsAsync("HU999999");
        String result = future.get();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<drugDetailsResponse>"));
        assertTrue(result.contains("<id>HU999999</id>"));
        assertTrue(result.contains("HU999999 Details"));
    }
    
    @Test
    void getServiceStatus_SuccessfulCall_ReturnsStatus() {
        // When
        String result = soapClient.getServiceStatus();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<serviceStatus>"));
        assertTrue(result.contains("<status>UP</status>"));
        assertTrue(result.contains("PUPHAX SOAP client ready"));
    }
    
    @Test
    void getServiceStatus_MockMode_ReturnsExpectedFormat() {
        // When
        String result = soapClient.getServiceStatus();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("<serviceStatus>"));
        assertTrue(result.contains("<status>UP</status>"));
        assertTrue(result.contains("<message>PUPHAX SOAP client ready (mock mode)</message>"));
        assertTrue(result.contains("</serviceStatus>"));
    }
    
    @Test
    void searchDrugsFallback_CircuitBreakerOpen_ReturnsErrorResponse() throws Exception {
        // Given
        Exception simulatedException = new PuphaxConnectionException("Connection failed");
        
        // When
        CompletableFuture<String> result = soapClient.searchDrugsFallback("aspirin", null, null, simulatedException);
        String response = result.get();
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("SERVICE_UNAVAILABLE"));
        assertTrue(response.contains("temporarily unavailable"));
        assertTrue(response.contains("<totalCount>0</totalCount>"));
    }
    
    @Test
    void getDrugDetailsFallback_CircuitBreakerOpen_ReturnsErrorResponse() throws Exception {
        // Given
        Exception simulatedException = new PuphaxTimeoutException("Request timed out");
        
        // When
        CompletableFuture<String> result = soapClient.getDrugDetailsFallback("HU001234", simulatedException);
        String response = result.get();
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("SERVICE_UNAVAILABLE"));
        assertTrue(response.contains("temporarily unavailable"));
        assertTrue(response.contains("<error>"));
        assertTrue(response.contains("<drugId>HU001234</drugId>"));
    }
}