package com.puphax.service;

import com.puphax.client.PuphaxServiceMock;
import com.puphax.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PuphaxSoapClient with timeout and error scenarios.
 * 
 * These tests verify that the SOAP client wrapper properly handles
 * various error conditions and applies resilience patterns correctly.
 */
@ExtendWith(MockitoExtension.class)
class PuphaxSoapClientTest {
    
    @Mock
    private PuphaxServiceMock mockPuphaxService;
    
    private PuphaxSoapClient soapClient;
    
    @BeforeEach
    void setUp() {
        soapClient = new PuphaxSoapClient(mockPuphaxService);
    }
    
    @Test
    void searchDrugsAsync_SuccessfulCall_ReturnsResponse() throws Exception {
        // Given
        String expectedResponse = "<drugSearchResponse><drugs></drugs></drugSearchResponse>";
        when(mockPuphaxService.searchDrugs(eq("aspirin"), isNull(), isNull()))
            .thenReturn(expectedResponse);
        
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        String result = future.get();
        
        // Then
        assertEquals(expectedResponse, result);
        verify(mockPuphaxService).searchDrugs("aspirin", null, null);
    }
    
    @Test
    void searchDrugsAsync_TimeoutException_ThrowsPuphaxTimeoutException() {
        // Given
        when(mockPuphaxService.searchDrugs(anyString(), any(), any()))
            .thenThrow(new RuntimeException("Timeout", new SocketTimeoutException("Request timed out")));
        
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PuphaxTimeoutException.class, exception.getCause());
        assertEquals("TIMEOUT", ((PuphaxTimeoutException) exception.getCause()).getErrorCode());
        
        verify(mockPuphaxService).searchDrugs("aspirin", null, null);
    }
    
    @Test
    void searchDrugsAsync_ConnectionException_ThrowsPuphaxConnectionException() {
        // Given
        when(mockPuphaxService.searchDrugs(anyString(), any(), any()))
            .thenThrow(new RuntimeException("Connection failed", new ConnectException("Connection refused")));
        
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PuphaxConnectionException.class, exception.getCause());
        assertEquals("CONNECTION_FAILED", ((PuphaxConnectionException) exception.getCause()).getErrorCode());
        
        verify(mockPuphaxService).searchDrugs("aspirin", null, null);
    }
    
    @Test
    void searchDrugsAsync_SoapFault_ThrowsPuphaxSoapFaultException() {
        // Given
        when(mockPuphaxService.searchDrugs(anyString(), any(), any()))
            .thenThrow(new RuntimeException("SOAP fault: Invalid parameter"));
        
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PuphaxSoapFaultException.class, exception.getCause());
        assertEquals("SOAP_FAULT", ((PuphaxSoapFaultException) exception.getCause()).getErrorCode());
        
        verify(mockPuphaxService).searchDrugs("aspirin", null, null);
    }
    
    @Test
    void searchDrugsAsync_GenericException_ThrowsPuphaxServiceException() {
        // Given
        when(mockPuphaxService.searchDrugs(anyString(), any(), any()))
            .thenThrow(new RuntimeException("Unexpected error"));
        
        // When
        CompletableFuture<String> future = soapClient.searchDrugsAsync("aspirin", null, null);
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PuphaxServiceException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Unexpected error in operation"));
        
        verify(mockPuphaxService).searchDrugs("aspirin", null, null);
    }
    
    @Test
    void getDrugDetailsAsync_SuccessfulCall_ReturnsResponse() throws Exception {
        // Given
        String expectedResponse = "<drugDetailsResponse><drug><id>HU001234</id></drug></drugDetailsResponse>";
        when(mockPuphaxService.getDrugDetails(eq("HU001234")))
            .thenReturn(expectedResponse);
        
        // When
        CompletableFuture<String> future = soapClient.getDrugDetailsAsync("HU001234");
        String result = future.get();
        
        // Then
        assertEquals(expectedResponse, result);
        verify(mockPuphaxService).getDrugDetails("HU001234");
    }
    
    @Test
    void getDrugDetailsAsync_ErrorScenario_ThrowsAppropriateException() {
        // Given
        when(mockPuphaxService.getDrugDetails(eq("INVALID")))
            .thenThrow(new RuntimeException("Drug not found"));
        
        // When
        CompletableFuture<String> future = soapClient.getDrugDetailsAsync("INVALID");
        
        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PuphaxServiceException.class, exception.getCause());
        
        verify(mockPuphaxService).getDrugDetails("INVALID");
    }
    
    @Test
    void getServiceStatus_SuccessfulCall_ReturnsStatus() {
        // Given
        String expectedResponse = "<serviceStatus><status>UP</status></serviceStatus>";
        when(mockPuphaxService.getServiceStatus())
            .thenReturn(expectedResponse);
        
        // When
        String result = soapClient.getServiceStatus();
        
        // Then
        assertEquals(expectedResponse, result);
        verify(mockPuphaxService).getServiceStatus();
    }
    
    @Test
    void getServiceStatus_ServiceDown_ThrowsException() {
        // Given
        when(mockPuphaxService.getServiceStatus())
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // When & Then
        assertThrows(PuphaxServiceException.class, () -> {
            soapClient.getServiceStatus();
        });
        
        verify(mockPuphaxService).getServiceStatus();
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
    }
}