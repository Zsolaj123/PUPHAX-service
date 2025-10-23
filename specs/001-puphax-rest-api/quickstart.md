# Quickstart: PUPHAX REST API Service

**Date**: 2025-10-20  
**Feature**: PUPHAX REST API Service  
**Purpose**: Quick setup guide for developers to get the service running locally

## Prerequisites

- **Java**: OpenJDK 17 or later
- **Maven**: 3.8+ for Jakarta namespace support
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Internet**: Required to download PUPHAX WSDL and dependencies

## Quick Setup (5 minutes)

### 1. Clone and Setup

```bash
# Navigate to project root
cd PUPHAX-service

# Generate SOAP client classes from WSDL
mvn clean compile

# Run the application
mvn spring-boot:run
```

### 2. Verify Installation

```bash
# Check service health
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP","components":{"puphaxSoap":{"status":"UP"}}}
```

### 3. Test Basic Functionality

```bash
# Search for aspirin
curl "http://localhost:8081/api/v1/drugs/search?term=aspirin"

# Search with filters
curl "http://localhost:8081/api/v1/drugs/search?term=aspirin&manufacturer=Bayer&page=0&size=10"
```

## Project Structure Overview

```
src/main/java/com/puphax/
├── controller/           # REST API endpoints
│   ├── DrugController.java      # Main drug search API
│   └── HealthController.java    # Health check endpoints
├── service/             # Business logic
│   ├── DrugService.java         # Drug search service
│   └── PuphaxSoapClient.java    # SOAP client wrapper
├── model/               # Data models
│   ├── dto/                     # REST API DTOs
│   └── soap/                    # Generated SOAP classes
├── config/              # Spring configuration
│   ├── CacheConfig.java         # Caching configuration
│   ├── SoapConfig.java          # SOAP client config
│   └── OpenApiConfig.java       # Swagger/OpenAPI config
└── exception/           # Custom exceptions
    └── PuphaxServiceException.java
```

## Key Configuration Files

### `application.yml`
```yaml
server:
  port: 8080

puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    connect-timeout: 30000
    request-timeout: 60000
    max-connections: 20

spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=15m

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

### `pom.xml` (Key Dependencies)
```xml
<dependencies>
    <!-- Spring Boot Web Services -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web-services</artifactId>
    </dependency>
    
    <!-- JAX-WS Runtime -->
    <dependency>
        <groupId>com.sun.xml.ws</groupId>
        <artifactId>jaxws-rt</artifactId>
    </dependency>
    
    <!-- Caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- OpenAPI Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>
</dependencies>
```

## Development Workflow

### 1. Run in Development Mode

```bash
# Start with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or with debug enabled
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### 2. API Documentation

Visit `http://localhost:8081/swagger-ui.html` for interactive API documentation.

### 3. Test the API

#### Basic Search
```bash
curl -X GET "http://localhost:8081/api/v1/drugs/search?term=aspirin" \
  -H "accept: application/json"
```

#### Search with Pagination
```bash
curl -X GET "http://localhost:8081/api/v1/drugs/search?term=aspirin&page=0&size=5" \
  -H "accept: application/json"
```

#### Search with Filters
```bash
curl -X GET "http://localhost:8081/api/v1/drugs/search?term=aspirin&manufacturer=Bayer&atcCode=N02BA01" \
  -H "accept: application/json"
```

#### Get Drug Details
```bash
curl -X GET "http://localhost:8081/api/v1/drugs/HU001234" \
  -H "accept: application/json"
```

## Testing

### Run Unit Tests
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Run Integration Tests
```bash
# Run integration tests only
mvn test -Dtest="*IntegrationTest"
```

## Building and Packaging

### Create JAR
```bash
# Build executable JAR
mvn clean package

# Run the JAR
java -jar target/puphax-service-1.0.0.jar
```

### Docker Support
```dockerfile
FROM openjdk:17-jre-slim
COPY target/puphax-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build Docker image
docker build -t puphax-service .

# Run container
docker run -p 8080:8081 puphax-service
```

## Common Issues and Solutions

### Issue: WSDL Generation Fails
**Solution**: Ensure internet connectivity and check PUPHAX service availability:
```bash
curl -I https://puphax.neak.gov.hu/PUPHAXWS?wsdl
```

### Issue: Connection Timeout
**Solution**: Increase timeout values in `application.yml`:
```yaml
puphax:
  soap:
    connect-timeout: 60000
    request-timeout: 120000
```

### Issue: Hungarian Characters Display Incorrectly
**Solution**: Ensure UTF-8 encoding:
```bash
mvn spring-boot:run -Dfile.encoding=UTF-8
```

### Issue: Cache Not Working
**Solution**: Verify cache configuration and check logs:
```yaml
logging:
  level:
    org.springframework.cache: DEBUG
```

## Environment-Specific Configuration

### Development (`application-dev.yml`)
```yaml
puphax:
  soap:
    connect-timeout: 10000
    request-timeout: 30000

logging:
  level:
    com.puphax: DEBUG
    org.springframework.web: DEBUG
```

### Production (`application-prod.yml`)
```yaml
puphax:
  soap:
    connect-timeout: 30000
    request-timeout: 60000

logging:
  level:
    root: WARN
    com.puphax: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,metrics
```

## Performance Testing

### Load Testing with curl
```bash
# Test concurrent requests
for i in {1..100}; do
  curl -s "http://localhost:8081/api/v1/drugs/search?term=aspirin" &
done
wait
```

### Monitor Performance
```bash
# Check JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Check HTTP metrics
curl http://localhost:8081/actuator/metrics/http.server.requests
```

## Next Steps

1. **Customize Configuration**: Update `application.yml` for your environment
2. **Add Custom Endpoints**: Extend `DrugController` with additional features
3. **Implement Caching Strategy**: Configure Redis for distributed caching
4. **Set Up Monitoring**: Integrate with Prometheus/Grafana
5. **Deploy**: Set up CI/CD pipeline for automated deployment

## Useful Commands

```bash
# Generate SOAP classes only
mvn jaxws:wsimport

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests with debug output
mvn test -X

# Check dependency tree
mvn dependency:tree

# Generate project reports
mvn site

# Run with JVM profiling
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=app.jfr"
```

## Support and Documentation

- **API Documentation**: `http://localhost:8081/swagger-ui.html`
- **Health Checks**: `http://localhost:8081/actuator/health`
- **Metrics**: `http://localhost:8081/actuator/metrics`
- **Project Issues**: Use Git repository issue tracker
- **PUPHAX API**: Contact `puphax-request@neak.gov.hu`

This quickstart guide should have you up and running with the PUPHAX REST API service in under 10 minutes!