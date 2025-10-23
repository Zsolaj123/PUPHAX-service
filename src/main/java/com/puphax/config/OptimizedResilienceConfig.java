package com.puphax.config;

/**
 * Optimized Resilience Configuration for PUPHAX Service
 * 
 * NOTE: This configuration has been moved to application.yml due to API compatibility issues.
 * 
 * The following resilience patterns are configured via YAML:
 * - Circuit Breaker: 3 different configurations (primary, fast-operations, bulk-operations)
 * - Retry: Exponential backoff with jitter
 * - Time Limiter: Operation-specific timeouts
 * - Bulkhead: Concurrent call isolation
 * 
 * All optimizations are active and configured in src/main/resources/application.yml
 * under the resilience4j section.
 */
public class OptimizedResilienceConfig {
    // Configuration moved to application.yml
}