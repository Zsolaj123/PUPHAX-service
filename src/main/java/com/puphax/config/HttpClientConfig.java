package com.puphax.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * HTTP Client configuration optimized for PUPHAX service communication.
 * 
 * This configuration implements connection pooling, timeout management,
 * and other optimizations specifically for the NEAK PUPHAX service
 * which has specific limits (max 3 concurrent connections per client).
 */
@Configuration
public class HttpClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientConfig.class);
    
    @Value("${puphax.soap.connect-timeout:30000}")
    private int connectTimeout;
    
    @Value("${puphax.soap.request-timeout:60000}")
    private int requestTimeout;
    
    @Value("${puphax.soap.max-connections:20}")
    private int maxConnections;
    
    @Value("${puphax.soap.max-connections-per-route:3}")
    private int maxConnectionsPerRoute;
    
    /**
     * Creates a connection manager optimized for PUPHAX service.
     * 
     * NEAK PUPHAX has specific limits:
     * - Maximum 3 concurrent connections per client
     * - 256KB bandwidth limit per client
     * - Connection should be reused efficiently
     */
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        
        // Total connections across all routes
        connectionManager.setMaxTotal(maxConnections);
        
        // Maximum connections per route (NEAK limit: 3 per client)
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        
        // Connection eviction policy for stale connections
        connectionManager.closeExpired();
        connectionManager.closeIdle(Timeout.ofMinutes(2));
        
        logger.info("HTTP Connection Manager configured - maxTotal: {}, maxPerRoute: {}", 
                   maxConnections, maxConnectionsPerRoute);
        
        return connectionManager;
    }
    
    /**
     * Creates request configuration with optimal timeouts.
     */
    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(requestTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(5000)) // Pool timeout
                .build();
    }
    
    /**
     * Creates the main HTTP client with connection pooling and timeout configuration.
     */
    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager, 
                                         RequestConfig requestConfig) {
        
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofMinutes(2))
                // Retry handler for connection failures
                .setRetryStrategy(new org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy(3, 
                    org.apache.hc.core5.util.TimeValue.ofSeconds(1)))
                .build();
        
        logger.info("HTTP Client configured with connection pooling - connect timeout: {}ms, request timeout: {}ms", 
                   connectTimeout, requestTimeout);
        
        return httpClient;
    }
    
    /**
     * Connection manager bean cleanup.
     */
    @Bean
    public HttpClientConnectionCleanup connectionCleanup(PoolingHttpClientConnectionManager connectionManager) {
        return new HttpClientConnectionCleanup(connectionManager);
    }
    
    /**
     * Background task for cleaning up stale connections.
     */
    public static class HttpClientConnectionCleanup {
        private final PoolingHttpClientConnectionManager connectionManager;
        private final Logger logger = LoggerFactory.getLogger(HttpClientConnectionCleanup.class);
        
        public HttpClientConnectionCleanup(PoolingHttpClientConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            startCleanupTask();
        }
        
        private void startCleanupTask() {
            // Run cleanup every 30 seconds
            Thread cleanupThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(30000); // 30 seconds
                        
                        // Close expired connections
                        connectionManager.closeExpired();
                        
                        // Close idle connections older than 2 minutes
                        connectionManager.closeIdle(Timeout.ofMinutes(2));
                        
                        logger.debug("HTTP connection cleanup completed");
                    }
                } catch (InterruptedException e) {
                    logger.info("HTTP connection cleanup thread interrupted");
                    Thread.currentThread().interrupt();
                }
            });
            
            cleanupThread.setDaemon(true);
            cleanupThread.setName("http-connection-cleanup");
            cleanupThread.start();
            
            logger.info("HTTP connection cleanup task started");
        }
    }
}