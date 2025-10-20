package com.puphax.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Configuration for caching SOAP responses to improve performance.
 * 
 * This configuration sets up Caffeine cache with appropriate TTL
 * settings to cache PUPHAX service responses for 5-15 minutes,
 * reducing load on the external SOAP service.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Primary cache manager using Caffeine for in-memory caching.
     * 
     * Cache settings:
     * - Maximum 1000 entries per cache
     * - 10 minute expiration after write
     * - Automatic eviction of least recently used entries
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()
        );
        
        // Pre-configure cache names
        cacheManager.setCacheNames(java.util.List.of(
            "puphax-drugs",
            "puphax-drug-details"
        ));
        
        return cacheManager;
    }
    
    /**
     * Specialized cache for drug search results with shorter TTL.
     * Search results change more frequently and should have shorter cache time.
     */
    @Bean("searchCacheManager")
    public CacheManager searchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("puphax-search");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
        );
        
        return cacheManager;
    }
    
    /**
     * Long-term cache for drug details that rarely change.
     * Drug details are more stable and can be cached longer.
     */
    @Bean("detailsCacheManager")
    public CacheManager detailsCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("puphax-details");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
        );
        
        return cacheManager;
    }
}