package com.puphax.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI/Swagger documentation.
 * 
 * This configuration sets up comprehensive API documentation
 * that is auto-generated from code annotations, providing
 * interactive documentation for the PUPHAX REST API.
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8081}")
    private int serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PUPHAX REST API Service")
                .description("""
                    REST API service that provides access to the Hungarian NEAK PUPHAX drug database.
                    
                    This service converts SOAP responses from the PUPHAX public drug database into 
                    structured JSON format, providing healthcare applications with easy access to 
                    official drug information including active ingredients, manufacturers, ATC codes,
                    and regulatory details.
                    
                    **Features:**
                    - Drug search by name, manufacturer, or ATC code
                    - Paginated results with configurable page sizes
                    - Response caching for improved performance
                    - Comprehensive error handling
                    - Production-ready monitoring and health checks
                    
                    **Data Source:** NEAK PUPHAX Public Drug Database  
                    **Update Frequency:** Real-time via SOAP service
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("PUPHAX REST API Support")
                    .email("support@example.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development server"),
                new Server()
                    .url("https://api.example.com")
                    .description("Production server")
            ));
    }
}