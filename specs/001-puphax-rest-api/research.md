# Research: PUPHAX REST API Service

**Date**: 2025-10-20  
**Feature**: PUPHAX REST API Service  
**Purpose**: Research technical implementation decisions for Spring Boot 3.5.6 SOAP-to-REST API service

## Technical Research Findings

### Decision: JAX-WS Maven Plugin for WSDL Code Generation

**Rationale**: JAX-WS Maven plugin provides automated SOAP client code generation from WSDL during build process, ensuring type safety and automatic updates when WSDL changes.

**Implementation**:
- Use `jaxws-maven-plugin` version 4.0.2 (compatible with Jakarta namespace in Spring Boot 3.5.6)
- Configure plugin to generate classes in `target/generated-sources/wsimport`
- Package generated classes under `hu.neak.puphax.client` namespace
- Enable UTF-8 encoding for Hungarian character support

**Alternatives considered**:
- Manual SOAP client implementation: Rejected due to complexity and maintenance overhead
- Apache CXF: Rejected due to Spring Boot's native JAX-WS support being sufficient

### Decision: Spring Cache with EhCache for SOAP Response Caching

**Rationale**: Spring Cache abstraction with EhCache provides simple annotation-based caching with configurable TTL (5-15 minutes) and automatic eviction policies.

**Implementation**:
- Use `@Cacheable` annotations on service methods
- Configure EhCache with 15-minute TTL for drug search results
- Implement cache key strategy based on search parameters
- Add cache metrics for monitoring hit/miss ratios

**Alternatives considered**:
- Redis cache: Rejected for MVP due to added infrastructure complexity
- No caching: Rejected due to performance requirements and external service load concerns

### Decision: Resilience4j Circuit Breaker Pattern

**Rationale**: Resilience4j provides comprehensive fault tolerance patterns (circuit breaker, retry, timeout) with Spring Boot 3.x integration and monitoring capabilities.

**Implementation**:
- Configure circuit breaker with 50% failure threshold and 30-second wait time
- Implement exponential backoff retry (3 attempts, 1s initial delay)
- Add 30-second timeout for SOAP calls
- Provide fallback methods returning service unavailable responses

**Alternatives considered**:
- Hystrix: Rejected due to maintenance mode status
- Custom retry logic: Rejected due to Resilience4j's superior features and monitoring

### Decision: SpringDoc OpenAPI for API Documentation

**Rationale**: SpringDoc OpenAPI v2 provides automatic OpenAPI 3.0 documentation generation from Spring annotations, replacing deprecated Springfox.

**Implementation**:
- Use `@Operation`, `@Parameter`, and `@Schema` annotations
- Auto-generate Swagger UI at `/swagger-ui.html`
- Export OpenAPI spec at `/v3/api-docs`
- Include request/response examples for drug search operations

**Alternatives considered**:
- Manual documentation: Rejected due to maintenance overhead
- Springfox: Rejected due to deprecation and Spring Boot 3.x compatibility issues

### Decision: Layered Architecture with Clear Separation

**Rationale**: Clean architecture principles ensure maintainability, testability, and compliance with constitution requirements for modular design.

**Implementation**:
- Controller layer: REST endpoints with validation and error handling
- Service layer: Business logic and SOAP client orchestration
- Configuration layer: Spring beans, cache, and SOAP client setup
- Exception layer: Custom exception hierarchy for different error types

**Alternatives considered**:
- Direct SOAP client usage in controllers: Rejected due to poor separation of concerns
- Single service class: Rejected due to single responsibility principle violations

## Performance and Scalability Considerations

### Connection Pooling Strategy

- Use Apache HttpClient with `PoolingHttpClientConnectionManager`
- Configure 20 total connections, 10 per route maximum
- Set connection timeout: 30 seconds, request timeout: 60 seconds
- Enable connection keep-alive and retry handler (3 attempts)

### Memory Management

- Implement pagination with server-side limits (default 20, max 100 results per page)
- Use streaming JSON serialization for large result sets
- Configure JVM heap sizing for cache overhead (approximately 10MB for 1000 cached entries)

### Monitoring and Observability

- Integrate Micrometer metrics for request duration, cache hit/miss ratios
- Add custom metrics for SOAP service availability and response times
- Configure health checks for SOAP service connectivity
- Implement structured logging with correlation IDs

## Security Considerations

### Data Protection

- No patient data involved (public drug database)
- Use HTTPS for all SOAP communication (TLS 1.3)
- Implement request rate limiting to prevent abuse
- Log SOAP requests/responses (excluding sensitive headers)

### Input Validation

- Validate search parameters (length, character sets, SQL injection prevention)
- Sanitize Hungarian characters and special symbols
- Implement parameter encoding for SOAP requests
- Add CSRF protection for non-GET endpoints

## Testing Strategy

### Unit Testing with Mocked SOAP Responses

- Use Mockito to mock `PuphaxService` SOAP client
- Create test fixtures with realistic Hungarian drug data
- Test error scenarios (timeouts, SOAP faults, malformed responses)
- Achieve 90% code coverage requirement

### Integration Testing Approach

- Use WireMock to simulate PUPHAX SOAP service responses
- Test cache behavior with TTL scenarios
- Verify circuit breaker activation and recovery
- Test pagination and filtering functionality

### Performance Testing Recommendations

- Load test with 100 concurrent users (success criteria requirement)
- Measure 95th percentile response times under load
- Test cache warming and eviction scenarios
- Verify memory usage under sustained load

## Configuration Management

### Environment-Specific Configuration

```yaml
# application-dev.yml
puphax:
  soap:
    endpoint-url: https://dev.neak.gov.hu/puphax/ws
    connect-timeout: 10000
    request-timeout: 30000

# application-prod.yml  
puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    connect-timeout: 30000
    request-timeout: 60000
```

### External Dependencies

- PUPHAX WSDL: Download and version in `src/main/resources/wsdl/`
- Spring Boot 3.5.6 with Java 17 minimum requirement
- Maven 3.8+ for Jakarta namespace support in build plugins

## Deployment Considerations

### Containerization Strategy

- Use official OpenJDK 17 base image
- Multi-stage Docker build to minimize image size
- Health check endpoint configuration for orchestration
- Resource limits: 1GB RAM, 1 CPU core for typical workload

### Operational Requirements

- Graceful shutdown handling for active SOAP requests
- Log aggregation compatibility (JSON structured logging)
- Metrics export for Prometheus/Grafana monitoring
- Backup strategy for cache warming data

## Risk Mitigation

### PUPHAX Service Dependencies

- Implement service discovery for endpoint failover
- Monitor PUPHAX service status and version updates
- Plan for WSDL version migration strategy
- Establish communication channel with NEAK support team

### Performance Degradation Scenarios

- Configure cache warming strategies for popular drugs
- Implement request queuing for burst traffic
- Plan horizontal scaling approach for high availability
- Monitor and alert on response time degradation

This research provides the foundation for implementing a production-ready PUPHAX REST API service that meets all technical requirements and constitution compliance standards.