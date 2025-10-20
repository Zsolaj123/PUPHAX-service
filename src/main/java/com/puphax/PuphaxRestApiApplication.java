package com.puphax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main application class for PUPHAX REST API Service.
 * 
 * This service provides a REST API interface to the Hungarian NEAK PUPHAX
 * drug database by converting SOAP responses to structured JSON format.
 */
@SpringBootApplication
@EnableCaching
public class PuphaxRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuphaxRestApiApplication.class, args);
    }
}