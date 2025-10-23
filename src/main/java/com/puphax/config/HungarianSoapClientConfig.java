package com.puphax.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.xml.ws.BindingProvider;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Configuration for SOAP client with Hungarian character encoding support.
 */
@Configuration
public class HungarianSoapClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(HungarianSoapClientConfig.class);
    
    @Value("${puphax.soap.endpoint-url}")
    private String endpointUrl;
    
    @Value("${puphax.soap.username:PUPHAX}")
    private String puphaxUsername;
    
    @Value("${puphax.soap.password:puphax}")
    private String puphaxPassword;
    
    /**
     * Configure global digest authentication for PUPHAX.
     */
    @Bean
    public Authenticator puphaxAuthenticator() {
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost() != null && 
                    (getRequestingHost().equals("puphax.neak.gov.hu") || 
                     getRequestingHost().contains("puphax"))) {
                    logger.debug("Providing digest authentication for PUPHAX host: {}", getRequestingHost());
                    return new PasswordAuthentication(puphaxUsername, puphaxPassword.toCharArray());
                }
                return null;
            }
        };
        
        Authenticator.setDefault(authenticator);
        logger.info("Configured global digest authenticator for PUPHAX");
        
        return authenticator;
    }
    
    /**
     * Configure system properties for Hungarian character support.
     */
    @Bean
    public String hungarianEncodingConfig() {
        // Set system properties for Hungarian character encoding
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
        
        // Configure HTTP client to handle encoding issues
        System.setProperty("http.agent", "PUPHAX-Hungarian-Client/1.0");
        System.setProperty("sun.net.useExclusiveBind", "false");
        
        // Configure XML processing for Hungarian content
        System.setProperty("javax.xml.transform.TransformerFactory", 
                          "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        
        logger.info("Configured system properties for Hungarian character encoding support");
        
        return "configured";
    }
}