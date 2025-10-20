# Data Model: PUPHAX REST API Service

**Date**: 2025-10-20  
**Feature**: PUPHAX REST API Service  
**Purpose**: Define data entities, DTOs, and relationships for drug search functionality

## Core Entities

### DrugRecord

**Purpose**: Represents a pharmaceutical product from the PUPHAX database with all relevant medication information.

**Attributes**:
- `id`: String - Unique drug identifier (PUPHAX internal ID)
- `name`: String - Commercial drug name
- `activeIngredients`: List<ActiveIngredient> - Active pharmaceutical ingredients
- `manufacturer`: String - Pharmaceutical company name
- `atcCode`: String - Anatomical Therapeutic Chemical classification code
- `therapeuticGroup`: String - Therapeutic category
- `dosageForms`: List<DosageForm> - Available dosage forms (tablet, capsule, injection, etc.)
- `strength`: String - Drug strength/concentration
- `packagingSize`: String - Package size information
- `prescriptionRequired`: Boolean - Whether prescription is required
- `reimbursable`: Boolean - Whether covered by insurance
- `registrationNumber`: String - Official registration number
- `registrationDate`: LocalDate - Date of registration
- `expiryDate`: LocalDate - Registration expiry date (if applicable)
- `status`: DrugStatus - Current status (ACTIVE, SUSPENDED, WITHDRAWN)
- `additionalInfo`: String - Additional regulatory information

**Validation Rules**:
- `id` must not be null or empty
- `name` must not be null or empty, max 255 characters
- `atcCode` must follow ATC format (letter-number pattern)
- `activeIngredients` must contain at least one ingredient
- `registrationDate` must be in the past
- `expiryDate` must be after registration date if present

**Relationships**:
- Contains multiple `ActiveIngredient` entities
- Contains multiple `DosageForm` entities

### ActiveIngredient

**Purpose**: Represents an active pharmaceutical ingredient within a drug.

**Attributes**:
- `name`: String - Chemical name of the active ingredient
- `concentration`: String - Concentration amount and unit
- `role`: IngredientRole - Role in the formulation (ACTIVE, EXCIPIENT)

**Validation Rules**:
- `name` must not be null or empty, max 255 characters
- `concentration` must follow standard pharmaceutical notation
- `role` must be a valid enum value

### DosageForm

**Purpose**: Represents available dosage forms for a drug.

**Attributes**:
- `form`: String - Dosage form (tablet, capsule, injection, cream, etc.)
- `route`: String - Route of administration (oral, topical, intravenous, etc.)
- `description`: String - Additional form description

**Validation Rules**:
- `form` must not be null or empty
- `route` must be from predefined list of valid routes

## Data Transfer Objects (DTOs)

### DrugSearchRequest

**Purpose**: Encapsulates search criteria for drug queries.

**Attributes**:
- `term`: String - Primary search term (drug name or active ingredient)
- `manufacturer`: String - Optional manufacturer filter
- `atcCode`: String - Optional ATC code filter
- `page`: Integer - Page number for pagination (default: 0)
- `size`: Integer - Page size (default: 20, max: 100)
- `sortBy`: String - Sort field (name, manufacturer, atcCode)
- `sortDirection`: SortDirection - Sort direction (ASC, DESC)

**Validation Rules**:
- `term` must not be null or empty, min 2 characters, max 100 characters
- `manufacturer` if provided, max 100 characters
- `atcCode` if provided, must match ATC format
- `page` must be >= 0
- `size` must be between 1 and 100
- `sortBy` must be from allowed fields list
- `sortDirection` must be valid enum value

### DrugSearchResponse

**Purpose**: Contains paginated search results with metadata.

**Attributes**:
- `drugs`: List<DrugSummary> - List of drug records matching search criteria
- `pagination`: PaginationInfo - Pagination metadata
- `searchInfo`: SearchInfo - Information about the search performed

**Validation Rules**:
- `drugs` list must not be null (can be empty)
- `pagination` must not be null
- `searchInfo` must not be null

### DrugSummary

**Purpose**: Simplified drug information for search results.

**Attributes**:
- `id`: String - Drug identifier
- `name`: String - Drug name
- `manufacturer`: String - Manufacturer name
- `atcCode`: String - ATC code
- `activeIngredients`: List<String> - List of active ingredient names
- `prescriptionRequired`: Boolean - Prescription requirement
- `reimbursable`: Boolean - Reimbursement status
- `status`: DrugStatus - Current status

### PaginationInfo

**Purpose**: Metadata for paginated responses.

**Attributes**:
- `currentPage`: Integer - Current page number (0-based)
- `pageSize`: Integer - Number of items per page
- `totalPages`: Integer - Total number of pages
- `totalElements`: Long - Total number of matching records
- `hasNext`: Boolean - Whether there are more pages
- `hasPrevious`: Boolean - Whether there are previous pages

### SearchInfo

**Purpose**: Metadata about the search operation performed.

**Attributes**:
- `searchTerm`: String - Original search term
- `filters`: Map<String, String> - Applied filters
- `executionTime`: Long - Search execution time in milliseconds
- `cacheHit`: Boolean - Whether result was served from cache
- `timestamp`: Instant - When search was performed

## SOAP Integration Models

### PuphaxSoapRequest

**Purpose**: Maps REST API requests to SOAP service calls.

**Attributes**:
- `operation`: String - SOAP operation name
- `parameters`: Map<String, Object> - Operation parameters
- `timeout`: Integer - Request timeout in milliseconds

### PuphaxSoapResponse

**Purpose**: Raw SOAP service response wrapper.

**Attributes**:
- `successful`: Boolean - Whether SOAP call succeeded
- `data`: Object - Response data from SOAP service
- `errorCode`: String - Error code if call failed
- `errorMessage`: String - Error message if call failed
- `executionTime`: Long - SOAP call duration

## Enumerations

### DrugStatus
- `ACTIVE` - Drug is currently available
- `SUSPENDED` - Temporarily unavailable
- `WITHDRAWN` - Permanently withdrawn from market

### IngredientRole
- `ACTIVE` - Active pharmaceutical ingredient
- `EXCIPIENT` - Inactive ingredient

### SortDirection
- `ASC` - Ascending order
- `DESC` - Descending order

## Error Response Models

### ApiErrorResponse

**Purpose**: Standardized error response format.

**Attributes**:
- `timestamp`: Instant - When error occurred
- `status`: Integer - HTTP status code
- `error`: String - Error type/category
- `message`: String - Human-readable error message
- `path`: String - Request path that caused error
- `correlationId`: String - Unique identifier for request tracing

### ValidationErrorResponse

**Purpose**: Detailed validation error information.

**Attributes**:
- `timestamp`: Instant - When validation failed
- `status`: Integer - HTTP status code (400)
- `error`: String - "Validation Failed"
- `message`: String - General validation error message
- `path`: String - Request path
- `correlationId`: String - Request correlation ID
- `fieldErrors`: List<FieldError> - Specific field validation errors

### FieldError

**Purpose**: Individual field validation error details.

**Attributes**:
- `field`: String - Name of the field that failed validation
- `rejectedValue`: Object - Value that was rejected
- `message`: String - Validation error message for this field

## Cache Models

### CacheEntry

**Purpose**: Wrapper for cached SOAP responses.

**Attributes**:
- `key`: String - Cache key
- `value`: Object - Cached response data
- `timestamp`: Instant - When cached
- `ttl`: Duration - Time to live
- `accessCount`: Integer - Number of times accessed

## Database Schema (Future Enhancement)

*Note: Current implementation uses in-memory caching only. Future enhancements may require persistent storage.*

### Potential Tables:
- `drug_search_log` - Search history and analytics
- `cache_statistics` - Cache performance metrics
- `api_usage_metrics` - API usage tracking

## Data Flow Patterns

### Search Request Flow:
1. `DrugSearchRequest` → Validation
2. Check cache for `CacheEntry`
3. If cache miss → Transform to `PuphaxSoapRequest`
4. SOAP call → `PuphaxSoapResponse`
5. Transform SOAP response → `DrugRecord` entities
6. Create `DrugSummary` for response
7. Build `DrugSearchResponse` with pagination
8. Cache results as `CacheEntry`

### Error Handling Flow:
1. Validation errors → `ValidationErrorResponse`
2. SOAP service errors → `ApiErrorResponse`
3. Timeout/connection errors → `ApiErrorResponse` with retry suggestion
4. Unknown errors → `ApiErrorResponse` with correlation ID

## Serialization Considerations

### JSON Serialization:
- Use Jackson with default Spring Boot configuration
- Configure date/time serialization to ISO-8601 format
- Handle Hungarian characters (UTF-8 encoding)
- Exclude null values from JSON output
- Use camelCase for field names

### SOAP Serialization:
- JAX-WS handles XML serialization automatically
- Configure character encoding for Hungarian text
- Handle special characters in drug names
- Validate XML schema compliance

This data model provides a comprehensive foundation for the PUPHAX REST API service, ensuring type safety, proper validation, and clear separation between REST and SOAP concerns.