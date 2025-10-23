package com.puphax.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting configuration to protect the PUPHAX API from abuse.
 * 
 * Implements a simple token bucket algorithm with IP-based rate limiting.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Configure rate limiting filter.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registrationBean;
    }

    /**
     * Rate limiting filter implementation using token bucket algorithm.
     */
    public static class RateLimitFilter implements Filter {

        private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

        // Rate limiting configuration
        private static final int MAX_REQUESTS_PER_MINUTE = 60; // 60 requests per minute per IP
        private static final int MAX_REQUESTS_PER_HOUR = 1000;  // 1000 requests per hour per IP
        private static final long CLEANUP_INTERVAL_MS = 60000;  // Clean up old entries every minute

        // Storage for rate limiting data
        private final ConcurrentHashMap<String, ClientRateData> clientRateData = new ConcurrentHashMap<>();
        private volatile long lastCleanupTime = System.currentTimeMillis();

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            logger.info("Rate limiting filter initialized - {} req/min, {} req/hour per IP", 
                       MAX_REQUESTS_PER_MINUTE, MAX_REQUESTS_PER_HOUR);
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Get client identifier (IP address)
            String clientId = getClientId(httpRequest);
            
            // Clean up old entries periodically
            performPeriodicCleanup();

            // Check rate limits
            if (!isRequestAllowed(clientId)) {
                handleRateLimitExceeded(httpResponse, clientId);
                return;
            }

            // Add rate limiting headers
            addRateLimitHeaders(httpResponse, clientId);

            chain.doFilter(request, response);
        }

        /**
         * Get client identifier for rate limiting (IP address with forwarded header support).
         */
        private String getClientId(HttpServletRequest request) {
            // Check for forwarded IP (for load balancers/proxies)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }

        /**
         * Check if request is allowed based on rate limits.
         */
        private boolean isRequestAllowed(String clientId) {
            ClientRateData rateData = clientRateData.computeIfAbsent(clientId, k -> new ClientRateData());
            
            long currentTime = System.currentTimeMillis();
            
            // Reset counters if needed
            rateData.updateCounters(currentTime);
            
            // Check minute limit
            if (rateData.requestsThisMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
                logger.warn("Rate limit exceeded for client {} - {} requests in current minute", 
                           clientId, rateData.requestsThisMinute.get());
                return false;
            }
            
            // Check hour limit
            if (rateData.requestsThisHour.get() >= MAX_REQUESTS_PER_HOUR) {
                logger.warn("Rate limit exceeded for client {} - {} requests in current hour", 
                           clientId, rateData.requestsThisHour.get());
                return false;
            }
            
            // Increment counters
            rateData.requestsThisMinute.incrementAndGet();
            rateData.requestsThisHour.incrementAndGet();
            rateData.lastRequestTime = currentTime;
            
            return true;
        }

        /**
         * Handle rate limit exceeded response.
         */
        private void handleRateLimitExceeded(HttpServletResponse response, String clientId) throws IOException {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60"); // Retry after 60 seconds
            
            String jsonResponse = String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests from client. Please try again later.\",\"correlationId\":\"%s\"}",
                java.util.UUID.randomUUID().toString()
            );
            
            response.getWriter().write(jsonResponse);
            logger.warn("Rate limit exceeded for client: {}", clientId);
        }

        /**
         * Add rate limiting headers to response.
         */
        private void addRateLimitHeaders(HttpServletResponse response, String clientId) {
            ClientRateData rateData = clientRateData.get(clientId);
            if (rateData != null) {
                response.setHeader("X-RateLimit-Limit-Minute", String.valueOf(MAX_REQUESTS_PER_MINUTE));
                response.setHeader("X-RateLimit-Remaining-Minute", 
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - rateData.requestsThisMinute.get())));
                
                response.setHeader("X-RateLimit-Limit-Hour", String.valueOf(MAX_REQUESTS_PER_HOUR));
                response.setHeader("X-RateLimit-Remaining-Hour", 
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_HOUR - rateData.requestsThisHour.get())));
            }
        }

        /**
         * Perform periodic cleanup of old rate limiting data.
         */
        private void performPeriodicCleanup() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
                clientRateData.entrySet().removeIf(entry -> {
                    ClientRateData data = entry.getValue();
                    // Remove entries that haven't been used for more than 2 hours
                    return currentTime - data.lastRequestTime > 7200000;
                });
                lastCleanupTime = currentTime;
                logger.debug("Rate limit cleanup completed - {} active clients", clientRateData.size());
            }
        }

        @Override
        public void destroy() {
            clientRateData.clear();
            logger.info("Rate limiting filter destroyed");
        }

        /**
         * Rate limiting data for each client.
         */
        private static class ClientRateData {
            private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
            private final AtomicInteger requestsThisHour = new AtomicInteger(0);
            private final AtomicLong minuteWindowStart = new AtomicLong(System.currentTimeMillis());
            private final AtomicLong hourWindowStart = new AtomicLong(System.currentTimeMillis());
            private volatile long lastRequestTime = System.currentTimeMillis();

            /**
             * Update counters and reset windows if needed.
             */
            public void updateCounters(long currentTime) {
                // Reset minute window if needed
                if (currentTime - minuteWindowStart.get() >= 60000) {
                    requestsThisMinute.set(0);
                    minuteWindowStart.set(currentTime);
                }

                // Reset hour window if needed
                if (currentTime - hourWindowStart.get() >= 3600000) {
                    requestsThisHour.set(0);
                    hourWindowStart.set(currentTime);
                }
            }
        }
    }
}