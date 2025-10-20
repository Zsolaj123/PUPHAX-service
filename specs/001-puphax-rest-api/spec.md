# Feature Specification: PUPHAX REST API Service

**Feature Branch**: `001-puphax-rest-api`  
**Created**: 2025-10-20  
**Status**: Draft  
**Input**: User description: "I want to build a Spring Boot 3.5.6 backend service that connects to the NEAK PUPHAX public SOAP drug database (https://puphax.neak.gov.hu/PUPHAXWS). u will fined all documentation of the PUPHA service in reference folder, a starter springboot client called puphax-client and a soapUI demo at https://www.neak.gov.hu/pfile/file?path=/letoltheto/ATFO_dok/gyogyszer/PUPHA/informaciok_a_publikus_gyogyszertorzsrol/PWS-soapui-project_v1.22&inline=true The service should: Load and parse the WSDL automatically using JAX-WS or a Maven plugin. Provide a REST API endpoint (e.g. /api/drugs/search?term=aspirin). Return structured JSON data from the SOAP response. Be production-ready and compatible with future integration into my MediRing/Neuratos health app. No authentication required (public endpoint)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Basic Drug Search (Priority: P1)

A healthcare professional or patient using the MediRing/Neuratos health application needs to search for drug information by entering a medication name or active ingredient. The system should provide immediate access to official Hungarian drug database information through a simple search interface.

**Why this priority**: This is the core functionality that delivers immediate value. Without basic search capability, the service provides no value to users. This represents the minimum viable product that can be independently deployed and tested.

**Independent Test**: Can be fully tested by sending HTTP GET requests to the search endpoint with drug names and verifying that structured drug information is returned in JSON format.

**Acceptance Scenarios**:

1. **Given** the service is running, **When** a user sends GET request to `/api/drugs/search?term=aspirin`, **Then** the system returns a JSON response containing aspirin drug information from the PUPHAX database
2. **Given** the service is running, **When** a user searches for a valid Hungarian drug name, **Then** the system returns matching drug records with complete medication details
3. **Given** the service is running, **When** a user searches for a non-existent drug, **Then** the system returns an empty results array with appropriate status

---

### User Story 2 - Advanced Search and Filtering (Priority: P2)

Healthcare professionals need to perform more sophisticated drug searches using multiple criteria such as therapeutic group, manufacturer, or ATC code to find specific medications or explore treatment alternatives.

**Why this priority**: Enhances the basic search functionality to support professional healthcare workflows. Builds upon the core search capability and provides additional value for clinical decision-making.

**Independent Test**: Can be tested independently by implementing additional query parameters and verifying that filtering and advanced search criteria work correctly.

**Acceptance Scenarios**:

1. **Given** the service supports advanced search, **When** a user provides multiple search parameters, **Then** the system returns drugs matching all criteria
2. **Given** the service supports pagination, **When** search results exceed the page limit, **Then** the system returns paginated results with metadata including total count, page number, and page size

---

### User Story 3 - Service Health and Error Handling (Priority: P3)

System administrators and developers need to monitor the service health and ensure robust error handling when the PUPHAX SOAP service is unavailable or returns errors.

**Why this priority**: Essential for production deployment but not required for basic functionality. Ensures reliability and maintainability in production environments.

**Independent Test**: Can be tested by implementing health check endpoints and simulating various error conditions to verify appropriate error responses.

**Acceptance Scenarios**:

1. **Given** the PUPHAX service is available, **When** a health check is requested, **Then** the system reports healthy status
2. **Given** the PUPHAX service is unavailable, **When** a search is requested, **Then** the system returns appropriate error response with retry suggestions
3. **Given** invalid search parameters are provided, **When** a search is requested, **Then** the system returns validation error with clear messaging

---

### Edge Cases

- What happens when the PUPHAX SOAP service is temporarily unavailable or returns timeout errors?
- How does the system handle special characters, Hungarian accents, or non-standard drug name formats in search terms?
- What occurs when the PUPHAX service returns malformed XML or unexpected response structures?
- How does the system manage large result sets that might cause memory or performance issues?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a REST API endpoint at `/api/drugs/search` that accepts search terms and pagination parameters (page, size) via query parameters
- **FR-002**: System MUST connect to the NEAK PUPHAX SOAP service at https://puphax.neak.gov.hu/PUPHAXWS and retrieve drug information
- **FR-003**: System MUST automatically load and parse the PUPHAX WSDL to generate SOAP client code
- **FR-004**: System MUST convert SOAP XML responses to structured JSON format for REST API responses
- **FR-005**: System MUST handle SOAP service errors gracefully and return appropriate HTTP status codes
- **FR-006**: System MUST support primary search by drug name with optional filters for manufacturer and ATC code
- **FR-007**: System MUST be accessible as a public endpoint without authentication requirements
- **FR-008**: System MUST log all SOAP requests and responses for debugging and audit purposes (excluding sensitive data)
- **FR-009**: System MUST provide health check endpoints for monitoring service availability
- **FR-010**: System MUST validate input parameters and return appropriate error messages for invalid requests
- **FR-011**: System MUST implement short-term caching (5-15 minutes) for SOAP responses to reduce load on PUPHAX service
- **FR-012**: System MUST provide OpenAPI/Swagger documentation auto-generated from code annotations
- **FR-013**: System MUST include unit tests with mocked SOAP responses for all service integration points

### Key Entities

- **Drug Record**: Represents a pharmaceutical product with attributes including name, active ingredients, manufacturer, ATC code, therapeutic group, dosage forms, and regulatory information
- **Search Query**: Represents user search criteria including search terms, filters, pagination parameters, and result formatting preferences
- **SOAP Service Response**: Represents the structured data returned from PUPHAX service including drug records, metadata, and service status information

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Healthcare professionals can find drug information in under 3 seconds for 95% of searches
- **SC-002**: System maintains 99.5% uptime when PUPHAX service is available
- **SC-003**: API responses are returned in under 2 seconds for 90% of requests
- **SC-004**: System handles at least 100 concurrent search requests without performance degradation
- **SC-005**: 95% of valid drug searches return accurate results matching the PUPHAX database
- **SC-006**: Error responses provide actionable information that allows users to correct their requests

## Clarifications

### Session 2025-10-20

- Q: PUPHAX SOAP Methods and Search Capabilities → A: Search by name with filters (manufacturer, ATC)
- Q: Search Result Pagination Strategy → A: Server-side pagination with page/size parameters
- Q: Response Caching Strategy → A: Short-term memory cache (5-15 minutes)
- Q: API Documentation Strategy → A: OpenAPI/Swagger with auto-generation
- Q: Testing Strategy for SOAP Integration → A: Unit tests with mocked SOAP responses