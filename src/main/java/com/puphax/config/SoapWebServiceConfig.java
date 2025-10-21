package com.puphax.config;

import com.puphax.interceptor.CharacterEncodingCorrectionInterceptor;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Web Services configuration for PUPHAX SOAP client.
 * 
 * Based on the official PUPHAX Spring Boot integration documentation,
 * this configuration sets up:
 * - Digest authentication with PUPHAX/puphax credentials
 * - Apache HttpClient for proper authentication support
 * - Character encoding correction interceptor
 * - Proper endpoint URL override
 */
@Configuration
public class SoapWebServiceConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SoapWebServiceConfig.class);
    
    @Value("${puphax.soap.endpoint-url:https://puphax.neak.gov.hu/PUPHAXWS}")
    private String puphaxServiceUrl;
    
    private static final String PUPHAX_USERNAME = "PUPHAX";
    private static final String PUPHAX_PASSWORD = "puphax";
    
    @Bean
    public WebServiceTemplate webServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        
        // Set the message sender with Apache HttpClient for Digest auth support
        webServiceTemplate.setMessageSender(httpComponents5MessageSender());
        
        // Override the incorrect WSDL endpoint URL
        webServiceTemplate.setDefaultUri(puphaxServiceUrl);
        
        // Add the character encoding correction interceptor
        ClientInterceptor[] interceptors = new ClientInterceptor[] {
            new CharacterEncodingCorrectionInterceptor()
        };
        webServiceTemplate.setInterceptors(interceptors);
        
        logger.info("Configured WebServiceTemplate for PUPHAX with Digest auth and encoding correction");
        
        return webServiceTemplate;
    }
    
    @Bean
    public HttpComponents5MessageSender httpComponents5MessageSender() {
        // Simply create and return the message sender with HttpClient
        return new HttpComponents5MessageSender(httpClient());
    }
    
    @Bean
    public CloseableHttpClient httpClient() {
        // Create credentials provider with PUPHAX credentials
        CredentialsProvider credentialsProvider = CredentialsProviderBuilder.create()
            .add(new AuthScope(null, -1), PUPHAX_USERNAME, PUPHAX_PASSWORD.toCharArray())
            .build();
        
        // Build the HTTP client with credentials
        return HttpClients.custom()
            .setDefaultCredentialsProvider(credentialsProvider)
            .build();
    }
}