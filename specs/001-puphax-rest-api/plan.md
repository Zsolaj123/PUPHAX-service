# Implementation Plan: PUPHAX REST API Service

**Branch**: `001-puphax-rest-api` | **Date**: 2025-10-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-puphax-rest-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Build a Spring Boot 3.5.6 REST API service that connects to the NEAK PUPHAX public SOAP drug database. The service will provide a clean REST API for drug search functionality, converting SOAP XML responses to structured JSON. Key features include automatic WSDL parsing, caching for performance, pagination for large result sets, and comprehensive testing with mocked SOAP responses.

## Technical Context

**Language/Version**: Java 17 with Spring Boot 3.5.6  
**Primary Dependencies**: Spring Web, JAX-WS, Jackson, SpringDoc OpenAPI, Spring Cache, JUnit 5, Mockito  
**Storage**: In-memory cache for SOAP responses (no persistent storage required)  
**Testing**: JUnit 5 with Mockito for unit tests, mocked SOAP responses  
**Target Platform**: Linux server/containerized deployment  
**Project Type**: Single backend service (REST API)  
**Performance Goals**: <2s response time for 90% of requests, 100+ concurrent users  
**Constraints**: <200ms p95 for cached responses, 5-15 minute cache TTL, public endpoint (no auth)  
**Scale/Scope**: Healthcare application integration, Hungarian drug database queries

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**✅ I. Modular Architecture & Clean Code**: Plan follows layered architecture with REST controllers, service layer, and SOAP client integration. Clear separation of concerns maintained.

**✅ II. Medical Data Security & Reliability**: Public drug database (no patient data), TLS encryption for SOAP calls, comprehensive error handling and logging planned.

**✅ III. SOAP Client Integration Standards**: JAX-WS with schema validation, connection pooling via Spring, timeout configuration, request/response logging, retry logic to be implemented.

**✅ IV. REST API Design Standards**: RESTful design, JSON responses, proper HTTP status codes, OpenAPI documentation, pagination support.

**✅ V. Test-Driven Development**: Unit tests with mocked SOAP responses planned, 90% coverage target, isolated test environment.

**⚠️ VI. EESZT Compliance**: Not directly applicable (public drug database, not patient data), but logging and traceability requirements will be met.

**Overall Status**: ✅ PASSED - All applicable constitution principles satisfied.

### Post-Design Re-evaluation

After completing Phase 1 design work (data model, API contracts, quickstart guide):

**✅ I. Modular Architecture & Clean Code**: Confirmed - Clear layered architecture with REST controllers, service layer, DTOs, and SOAP integration. Well-defined interfaces and separation of concerns maintained throughout design.

**✅ II. Medical Data Security & Reliability**: Confirmed - Comprehensive error handling patterns defined, input validation specifications, HTTPS enforcement, and structured logging design. No patient data involved (public drug database).

**✅ III. SOAP Client Integration Standards**: Confirmed - JAX-WS implementation with schema validation, connection pooling via Spring HTTP client, explicit timeout configurations, comprehensive request/response logging, and retry logic with exponential backoff.

**✅ IV. REST API Design Standards**: Confirmed - RESTful design with proper HTTP verbs, consistent JSON response structures, comprehensive OpenAPI 3.0 documentation, semantic URL paths with versioning (/api/v1/), and detailed error response formats.

**✅ V. Test-Driven Development**: Confirmed - Unit testing strategy with Mockito for SOAP client mocking, 90% coverage target specified, isolated test environment design, and comprehensive test scenarios including error conditions.

**⚠️ VI. EESZT Compliance**: Confirmed N/A - Public drug database service, no patient data handling. Logging and traceability requirements addressed through structured logging and correlation IDs.

**Final Status**: ✅ PASSED - Design maintains full compliance with constitution principles.

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```
src/main/java/com/puphax/
├── controller/           # REST API controllers
│   ├── DrugController.java
│   └── HealthController.java
├── service/             # Business logic layer
│   ├── DrugService.java
│   └── PuphaxSoapClient.java
├── model/               # Data transfer objects
│   ├── dto/
│   │   ├── DrugSearchRequest.java
│   │   ├── DrugSearchResponse.java
│   │   └── DrugRecord.java
│   └── soap/            # Generated SOAP classes
├── config/              # Spring configuration
│   ├── CacheConfig.java
│   ├── SoapConfig.java
│   └── OpenApiConfig.java
└── exception/           # Custom exceptions
    └── PuphaxServiceException.java

src/test/java/com/puphax/
├── controller/          # Controller unit tests
├── service/             # Service unit tests
└── integration/         # Integration tests

pom.xml                  # Maven dependencies
target/generated-sources/# JAX-WS generated classes
```

**Structure Decision**: Single Spring Boot project with standard Maven structure. JAX-WS plugin will generate SOAP client classes in target/generated-sources during build. Clean separation between REST controllers, business services, and SOAP integration.

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

