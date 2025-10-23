package com.puphax.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the PUPHAX REST API.
 * 
 * This configuration implements essential security headers and CORS policy
 * to protect against common web vulnerabilities.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configure security headers filter to protect against common attacks.
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Configure CORS for controlled cross-origin access.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow specific origins (configure based on your frontend domains)
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1:*",
            "https://127.0.0.1:*"
        ));
        
        // Allow specific headers
        config.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "X-Correlation-ID"
        ));
        
        // Allow specific methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/api/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }

    /**
     * Security headers filter implementation.
     */
    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // No initialization needed
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Content Security Policy - Restrict resource loading
            httpResponse.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self' https://puphax.neak.gov.hu; " +
                "frame-ancestors 'none'");

            // HTTP Strict Transport Security - Enforce HTTPS
            httpResponse.setHeader("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");

            // X-Frame-Options - Prevent clickjacking
            httpResponse.setHeader("X-Frame-Options", "DENY");

            // X-Content-Type-Options - Prevent MIME sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // X-XSS-Protection - Enable XSS filtering
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // Referrer Policy - Control referrer information
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy - Control browser features
            httpResponse.setHeader("Permissions-Policy", 
                "camera=(), microphone=(), geolocation=(), payment=()");

            // X-Permitted-Cross-Domain-Policies - Control Flash/PDF cross-domain
            httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");

            // Cache-Control for API responses
            if (httpRequest.getRequestURI().startsWith("/api/")) {
                // API responses should not be cached by default
                httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }

            // Server header removal (hide server information)
            httpResponse.setHeader("Server", "PUPHAX-API");

            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            // No cleanup needed
        }
    }
}