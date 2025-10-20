<!--
Sync Impact Report:
- Version change: 0.0.0 → 1.0.0
- Added principles:
  1. Modular Architecture & Clean Code
  2. Medical Data Security & Reliability
  3. SOAP Client Integration Standards
  4. REST API Design Standards
  5. Test-Driven Development
  6. EESZT Compliance
- Added sections: Security & Compliance, Development Standards
- Templates requiring updates: ⚠ pending validation
- Follow-up TODOs: Ratification date needs to be set
-->

# PUPHAX-service Constitution

## Core Principles

### I. Modular Architecture & Clean Code

All code MUST follow modular architecture principles with clear separation of concerns. Services MUST be organized into distinct layers: presentation (REST controllers), business logic (services), data access (repositories), and external integration (SOAP clients). Each module MUST have a single responsibility and well-defined interfaces. Code MUST be self-documenting with meaningful names and structure that reflects the domain model. Dependencies MUST flow inward toward the business logic core, never outward.

### II. Medical Data Security & Reliability

All patient data MUST be handled with the highest security standards. Data transmission MUST use TLS 1.3 or higher. Data at rest MUST be encrypted using AES-256. Access to patient data MUST be logged and auditable. All medical data operations MUST implement proper error handling, validation, and recovery mechanisms. System MUST maintain 99.9% uptime for critical patient data access. All security vulnerabilities MUST be addressed within 24 hours for critical issues, 7 days for high severity.

### III. SOAP Client Integration Standards

All SOAP client implementations MUST follow WS-Security standards for authentication and encryption. SOAP envelopes MUST be validated against published schemas. Connection pooling and circuit breaker patterns MUST be implemented for external service calls. Timeout configurations MUST be explicit and documented. All SOAP requests/responses MUST be logged (excluding sensitive data) for debugging and audit purposes. Retry logic MUST implement exponential backoff with jitter.

### IV. REST API Design Standards

All REST APIs MUST follow RESTful principles with proper HTTP verb usage. API responses MUST use consistent JSON structure with standardized error formats. All endpoints MUST implement proper HTTP status codes. API versioning MUST use semantic versioning in URL paths (/v1/, /v2/). All APIs MUST implement rate limiting and authentication. OpenAPI 3.0 documentation MUST be maintained and auto-generated from code annotations.

### V. Test-Driven Development

All code changes MUST include corresponding unit tests with minimum 90% code coverage. Integration tests MUST be written for all external service interactions including SOAP clients and database operations. Tests MUST run in isolated environments with no external dependencies. All tests MUST pass before code can be merged to main branch. Test data MUST never include real patient information - only synthetic test data is permitted.

### VI. EESZT Compliance

All implementations MUST adhere to Hungarian e-health interoperability standards (EESZT). Medical data formats MUST comply with HL7 FHIR R4 standards where applicable. Patient identification MUST follow TAJ number validation rules. All data exchanges with EESZT systems MUST be logged and traceable. Compliance validation MUST be performed on every release before deployment to production.

## Security & Compliance

All development MUST follow GDPR requirements for patient data handling. Data retention policies MUST be implemented and enforced automatically. Patient consent MUST be tracked and respected for all data operations. Security audits MUST be performed quarterly. Penetration testing MUST be conducted annually. All developers MUST complete medical data privacy training before accessing production systems.

## Development Standards

Code reviews MUST be performed by at least two developers before merging. All database migrations MUST be reversible and tested. Configuration MUST be externalized and environment-specific. Secrets MUST never be committed to version control. All deployments MUST be automated with rollback capabilities. Performance monitoring MUST be implemented for all critical paths with alerting on degradation.

## Governance

This constitution supersedes all other development practices and standards. Any amendments require approval from the technical lead and project stakeholder. All pull requests MUST verify compliance with these principles. Complexity that violates these principles MUST be justified in writing and approved before implementation. Non-compliance issues MUST be addressed immediately upon discovery.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): needs to be set by project team | **Last Amended**: 2025-10-20