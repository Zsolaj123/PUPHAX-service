---
description: "Task list template for feature implementation"
---

# Tasks: PUPHAX REST API Service

**Input**: Design documents from `/specs/001-puphax-rest-api/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions
- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create Maven project structure with Spring Boot 3.5.6 starter
- [x] T002 Configure pom.xml with JAX-WS Maven plugin and dependencies in pom.xml
- [x] T003 [P] Download and store PUPHAX WSDL in src/main/resources/wsdl/PUPHAXWS.wsdl
- [x] T004 [P] Configure JAX-WS wsimport plugin to generate SOAP client classes in pom.xml
- [x] T005 [P] Set up application.yml with PUPHAX endpoint configuration and cache settings

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Generate SOAP client classes by running Maven wsimport plugin
- [x] T007 [P] Create PuphaxServiceException hierarchy in src/main/java/com/puphax/exception/
- [x] T008 [P] Implement SoapConfig for SOAP client configuration in src/main/java/com/puphax/config/SoapConfig.java
- [x] T009 [P] Implement CacheConfig for Spring Cache configuration in src/main/java/com/puphax/config/CacheConfig.java
- [x] T010 [P] Create OpenApiConfig for Swagger documentation in src/main/java/com/puphax/config/OpenApiConfig.java
- [x] T011 Create PuphaxSoapClient service wrapper in src/main/java/com/puphax/service/PuphaxSoapClient.java
- [x] T012 [P] Create base error handling and validation infrastructure in src/main/java/com/puphax/exception/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Basic Drug Search (Priority: P1) 🎯 MVP

**Goal**: Implement core drug search functionality with REST API endpoint for searching by drug name

**Independent Test**: Can be tested by sending GET requests to `/api/v1/drugs/search?term=aspirin` and verifying JSON response with drug information

### Tests for User Story 1 (REQUIRED per FR-013) ⚠️

**NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T013 [P] [US1] Create unit test for DrugController search endpoint in src/test/java/com/puphax/controller/DrugControllerTest.java
- [ ] T014 [P] [US1] Create unit test for DrugService with mocked SOAP responses in src/test/java/com/puphax/service/DrugServiceTest.java
- [ ] T015 [P] [US1] Create unit test for PuphaxSoapClient with timeout scenarios in src/test/java/com/puphax/service/PuphaxSoapClientTest.java

### Implementation for User Story 1

- [ ] T016 [P] [US1] Create DrugSearchRequest DTO in src/main/java/com/puphax/model/dto/DrugSearchRequest.java
- [ ] T017 [P] [US1] Create DrugSearchResponse DTO in src/main/java/com/puphax/model/dto/DrugSearchResponse.java
- [ ] T018 [P] [US1] Create DrugSummary DTO in src/main/java/com/puphax/model/dto/DrugSummary.java
- [ ] T019 [P] [US1] Create PaginationInfo DTO in src/main/java/com/puphax/model/dto/PaginationInfo.java
- [ ] T020 [P] [US1] Create SearchInfo DTO in src/main/java/com/puphax/model/dto/SearchInfo.java
- [ ] T021 [US1] Implement DrugService with basic search logic in src/main/java/com/puphax/service/DrugService.java (depends on T011)
- [ ] T022 [US1] Implement REST endpoint `/api/v1/drugs/search` in src/main/java/com/puphax/controller/DrugController.java
- [ ] T023 [US1] Add input validation and error handling for search parameters
- [ ] T024 [US1] Implement SOAP to JSON response mapping in DrugService
- [ ] T025 [US1] Add basic caching for search results using @Cacheable annotation

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Advanced Search and Filtering (Priority: P2)

**Goal**: Enhance search functionality with filtering by manufacturer, ATC code, and pagination support

**Independent Test**: Can be tested by using query parameters like `?term=aspirin&manufacturer=Bayer&page=0&size=10` and verifying filtered results with pagination metadata

### Tests for User Story 2 (REQUIRED per FR-013) ⚠️

- [ ] T026 [P] [US2] Create unit test for advanced search parameters in src/test/java/com/puphax/controller/DrugControllerAdvancedTest.java
- [ ] T027 [P] [US2] Create unit test for pagination logic in src/test/java/com/puphax/service/DrugServicePaginationTest.java

### Implementation for User Story 2

- [ ] T028 [P] [US2] Extend DrugSearchRequest with manufacturer and atcCode filters in src/main/java/com/puphax/model/dto/DrugSearchRequest.java
- [ ] T029 [P] [US2] Add pagination parameters (page, size, sortBy, sortDirection) to DrugSearchRequest
- [ ] T030 [US2] Implement filtering logic in DrugService search method (depends on T021)
- [ ] T031 [US2] Implement server-side pagination in DrugService
- [ ] T032 [US2] Add sorting functionality for search results by name, manufacturer, atcCode
- [ ] T033 [US2] Update REST endpoint to handle additional query parameters
- [ ] T034 [US2] Add validation for ATC code format and pagination parameters

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Service Health and Error Handling (Priority: P3)

**Goal**: Implement comprehensive health monitoring and robust error handling for production deployment

**Independent Test**: Can be tested by calling `/actuator/health` endpoint and simulating SOAP service failures to verify error responses

### Tests for User Story 3 (REQUIRED per FR-013) ⚠️

- [ ] T035 [P] [US3] Create integration test for health check endpoints in src/test/java/com/puphax/integration/HealthCheckIntegrationTest.java
- [ ] T036 [P] [US3] Create unit test for error handling scenarios in src/test/java/com/puphax/service/ErrorHandlingTest.java

### Implementation for User Story 3

- [ ] T037 [P] [US3] Create HealthController with custom health indicators in src/main/java/com/puphax/controller/HealthController.java
- [ ] T038 [P] [US3] Create DrugRecord entity for detailed drug information in src/main/java/com/puphax/model/dto/DrugRecord.java
- [ ] T039 [P] [US3] Create ActiveIngredient and DosageForm DTOs in src/main/java/com/puphax/model/dto/
- [ ] T040 [US3] Implement `/api/v1/drugs/{drugId}` endpoint for detailed drug information
- [ ] T041 [US3] Add comprehensive error handling with proper HTTP status codes
- [ ] T042 [US3] Implement circuit breaker pattern using @CircuitBreaker annotation
- [ ] T043 [US3] Add retry logic with exponential backoff for SOAP service calls
- [ ] T044 [US3] Configure Actuator health check endpoints for PUPHAX service connectivity
- [ ] T045 [US3] Add custom error response DTOs (ApiErrorResponse, ValidationErrorResponse)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T046 [P] Add comprehensive OpenAPI documentation annotations to all REST endpoints
- [ ] T047 [P] Implement request/response logging with correlation IDs in src/main/java/com/puphax/config/LoggingConfig.java
- [ ] T048 [P] Add Micrometer metrics for cache hit/miss ratios and response times
- [ ] T049 [P] Configure application.yml for different environments (dev, prod)
- [ ] T050 [P] Add Hungarian character encoding support and validation
- [ ] T051 [P] Create integration tests for complete user workflows in src/test/java/com/puphax/integration/
- [ ] T052 [P] Add performance testing scenarios for 100 concurrent requests
- [ ] T053 [P] Implement graceful shutdown handling for active SOAP requests
- [ ] T054 Run quickstart.md validation to ensure setup instructions work
- [ ] T055 Create Docker configuration with health checks and resource limits

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Extends US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Adds production features, independently testable

### Within Each User Story

- Tests (per FR-013) MUST be written and FAIL before implementation
- DTOs before services
- Services before controllers
- Core implementation before error handling
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- DTOs within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Create unit test for DrugController search endpoint in src/test/java/com/puphax/controller/DrugControllerTest.java"
Task: "Create unit test for DrugService with mocked SOAP responses in src/test/java/com/puphax/service/DrugServiceTest.java"
Task: "Create unit test for PuphaxSoapClient with timeout scenarios in src/test/java/com/puphax/service/PuphaxSoapClientTest.java"

# Launch all DTOs for User Story 1 together:
Task: "Create DrugSearchRequest DTO in src/main/java/com/puphax/model/dto/DrugSearchRequest.java"
Task: "Create DrugSearchResponse DTO in src/main/java/com/puphax/model/dto/DrugSearchResponse.java"
Task: "Create DrugSummary DTO in src/main/java/com/puphax/model/dto/DrugSummary.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach per FR-013)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- FR-013 requires unit tests with mocked SOAP responses - tests are REQUIRED for this feature
- Follow Spring Boot 3.5.6 conventions and Java 17 syntax
- Use JAX-WS generated classes for SOAP client integration
- Implement proper cache TTL of 5-15 minutes as specified in clarifications