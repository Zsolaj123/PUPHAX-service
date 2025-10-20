# PUPHAX Service Development Guide

## Overview

PUPHAX (PUblikus PHArma Xml) is the Hungarian National Health Insurance Fund (NEAK) pharmaceutical database webservice that provides access to drug information, pricing, and reimbursement data. This guide provides comprehensive information for developers working with the PUPHAX REST API service.

## Table of Contents

1. [PUPHAX Webservice Basics](#puphax-webservice-basics)
2. [Authentication](#authentication)
3. [API Architecture](#api-architecture)
4. [Data Structure](#data-structure)
5. [Common Operations](#common-operations)
6. [Recurring Issues & Solutions](#recurring-issues--solutions)
7. [Implementation Notes](#implementation-notes)
8. [Testing & Debugging](#testing--debugging)

## PUPHAX Webservice Basics

### Service Information
- **Service Name**: PUPHAXWS
- **Version**: 1.33 (Current)
- **Base URL**: `https://puphax.neak.gov.hu/PUPHAXWS`
- **WSDL**: `https://puphax.neak.gov.hu/PUPHAXWS?wsdl`
- **Protocol**: HTTPS with SOAP/XML
- **Character Encoding**: UTF-8 response, EE8ISO8859P2 (ISO-8859-2) for internal content

### Key Features
- Drug database with historical data from 2007-04-01
- Time-series data with version management
- Support for incremental updates
- Comprehensive filtering system
- Support for both master data tables and time-dependent data

## Authentication

### Digest Authentication
PUPHAX uses **digest authentication** (not basic auth):

```
Username: PUPHAX
Password: puphax
```

### Implementation in Java
```java
// Configure digest authentication for PUPHAX service
Authenticator.setDefault(new Authenticator() {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestingHost().equals("puphax.neak.gov.hu")) {
            return new PasswordAuthentication("PUPHAX", "puphax".toCharArray());
        }
        return null;
    }
});
```

### SOAP Client Configuration
```java
BindingProvider bindingProvider = (BindingProvider) puphaxPort;
bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
bindingProvider.getRequestContext().put("com.sun.xml.ws.connect.timeout", 30000);
bindingProvider.getRequestContext().put("com.sun.xml.ws.request.timeout", 60000);
```

## API Architecture

### Our REST API Structure
```
/api/v1/drugs/
├── search          # Search drugs with filters
├── {id}            # Get drug details by ID
└── health          # Service health check
```

### SOAP to REST Mapping
| SOAP Operation | REST Endpoint | Description |
|----------------|---------------|-------------|
| TERMEKLISTA | `GET /api/v1/drugs/search` | Search drugs |
| TERMEKADAT | `GET /api/v1/drugs/{id}` | Get drug details |
| TAMOGATDAT | Included in drug details | Support information |

## Data Structure

### Two-Level Time Series
PUPHAX uses a dual time-series approach:

1. **TERMEK Table**: Product master data changes
2. **TAMALAP Table**: Price and support changes

Each record has validity periods:
- `ERV_KEZD`: Validity start date
- `ERV_VEGE`: Validity end date

### Key Tables
- **TERMEK**: Product master data
- **TAMALAP**: Price and support data  
- **KATEGTAM**: Support categories
- **EUPONTOK**: EU points (indications)
- **ATC/ISO/BNO**: Classification tables
- **CEGEK**: Companies (manufacturers/distributors)

### Data Types & Null Handling
| Type | Null Value | Example |
|------|------------|---------|
| String | `-/-` | Empty manufacturer |
| Date | `2099-12-31` | No deletion date |
| Double | `999999999.999999` | No price limit |

## Common Operations

### 1. Drug Search (TERMEKLISTA)
**SOAP Request Structure:**
```xml
<COBJIDLISTA-TERMEKLISTAInput>
    <DSP-DATE-IN>2024-10-20</DSP-DATE-IN>
    <SXFILTER-VARCHAR2-IN>
        <![CDATA[
            <alapfilter>
                <ATC>N05BA%</ATC>
                <TNEV>XANAX%</TNEV>
                <TERMKOD>G7</TERMKOD>
            </alapfilter>
        ]]>
    </SXFILTER-VARCHAR2-IN>
</COBJIDLISTA-TERMEKLISTAInput>
```

### 2. Drug Details (TERMEKADAT)
```xml
<COBJTERMEKADAT-TERMEKADATInput>
    <NID-NUMBER-IN>14714226</NID-NUMBER-IN>
</COBJTERMEKADAT-TERMEKADATInput>
```

### 3. Support Data (TAMOGATDAT)
```xml
<COBJTAMOGAT-TAMOGATADATInput>
    <DSP-DATE-IN>2024-10-20</DSP-DATE-IN>
    <NID-NUMBER-IN>14714226</NID-NUMBER-IN>
</COBJTAMOGAT-TAMOGATADATInput>
```

## Recurring Issues & Solutions

### 1. Authentication Issues (401 Unauthorized)

**Problem**: Getting 401 errors when calling PUPHAX service

**Common Causes:**
- Using basic auth instead of digest auth
- Incorrect username/password
- Not setting up Authenticator properly

**Solution:**
```java
// Use Java's built-in Authenticator for digest auth
Authenticator.setDefault(new Authenticator() {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestingHost().equals("puphax.neak.gov.hu")) {
            return new PasswordAuthentication("PUPHAX", "puphax".toCharArray());
        }
        return null;
    }
});
```

### 2. WSDL Address Issues

**Problem**: WSDL contains incorrect internal address

**Issue**: WSDL shows `<soap:address location="http://puphax:1958/orawsv/PUPHAX/PUPHAXWS"/>`

**Solution**: Always use the public endpoint: `https://puphax.neak.gov.hu/PUPHAXWS`

### 3. Character Encoding Issues

**Problem**: Hungarian characters appear corrupted

**Solution**: 
- Response is UTF-8 
- Internal content uses ISO-8859-2
- Convert on client side if needed

### 4. Rate Limiting

**Current Limits:**
- Maximum 3 concurrent threads per client
- 256KB bandwidth limit per client
- Avoid continuous polling

**Best Practices:**
- Use incremental updates
- Cache responses appropriately
- Don't query unchanged data repeatedly

### 5. Large Dataset Handling

**Problem**: Timeouts on large queries

**Solutions:**
- Use pagination with `LAPOZAS` filter: `<LAPOZAS>126:25</LAPOZAS>` 
- Download historical data from CSV files (2007-2023)
- Use incremental change queries (`INKVALT`)

### 6. Version Management Issues

**Problem**: Confusing work versions vs. live versions

**Status Values:**
- `A` (alapozás): Base work in progress - don't use
- `M` (munkaverzió): Work version - preview only  
- `E` (éles verzió): Live version - use this

**Solution**: Always check `STATUS` field in `KIHIRD` response

## Implementation Notes

### Spring Boot Configuration
```yaml
puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    connect-timeout: 30000
    request-timeout: 60000
    
resilience4j:
  circuitbreaker:
    instances:
      puphax-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

### Error Handling Strategy
```java
try {
    TERMEKLISTAOutput result = puphaxPort.termeklista(input);
    return convertPuphaxResponseToXml(result, searchTerm, manufacturer, atcCode);
} catch (Exception e) {
    logger.error("PUPHAX SOAP call failed: {}", e.getMessage(), e);
    
    // Circuit breaker will handle fallback
    throw handleSoapException(e);
}
```

### XML Response Parsing
```java
private List<DrugSummary> parseSearchResponse(String xmlResponse) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        xmlResponse.getBytes(StandardCharsets.UTF_8)
    );
    Document document = builder.parse(inputStream);
    
    NodeList drugNodes = document.getElementsByTagName("drug");
    // Process nodes...
}
```

## Testing & Debugging

### Local Testing
1. Use SoapUI with basic auth (it treats digest same as basic)
2. Test with simple drug searches first
3. Verify WSDL accessibility

### Common Test Cases
```bash
# Test drug search
curl "http://localhost:8080/api/v1/drugs/search?term=aspirin&page=0&size=5"

# Test health endpoint  
curl "http://localhost:8080/api/v1/drugs/health"
```

### Debugging Checklist
1. ✅ Verify PUPHAX service is accessible
2. ✅ Check authentication configuration
3. ✅ Validate SOAP request format
4. ✅ Monitor for rate limiting
5. ✅ Check character encoding
6. ✅ Verify date formats (yyyy-MM-dd)

### Logging Configuration
```yaml
logging:
  level:
    com.puphax: DEBUG
    jakarta.xml.ws: DEBUG
```

## Performance Optimization

### Caching Strategy
```java
@Cacheable(value = "drugSearchCache", 
          key = "#searchTerm + '_' + #manufacturer + '_' + #atcCode + '_' + #page")
public DrugSearchResponse searchDrugs(String searchTerm, String manufacturer, String atcCode, int page, int size) {
    // Implementation
}
```

### Async Processing
```java
@Async
public CompletableFuture<String> searchDrugsAsync(String searchTerm, String manufacturer, String atcCode) {
    return CompletableFuture.supplyAsync(() -> {
        // SOAP call implementation
    });
}
```

## Security Considerations

1. **Never log credentials** - The digest auth credentials are in code
2. **Use HTTPS only** - PUPHAX enforces HTTPS
3. **Validate all inputs** - Prevent XML injection in filters
4. **Rate limiting compliance** - Respect NEAK's limits
5. **Error handling** - Don't expose internal errors to API users

## Emergency Procedures

### If PUPHAX is Down
1. Circuit breaker activates automatically
2. Fallback responses provided 
3. Monitor NEAK status page
4. Enable mock mode if needed

### High Traffic Periods
- Typically 24th of month to 5th of next month
- NEAK may restrict historical data queries
- Use cached data during peak times

## References

- **PUPHAX Documentation**: NEAK_PUPHAX_WS_v1.33.pdf
- **Sample Calls**: PUPHAXWS mintahívások v1.21.txt
- **Database Schema**: PUPHAX terméktörzs documentation
- **NEAK Website**: For latest updates and CSV downloads

---

*Last Updated: 2025-10-20*
*Version: 1.0*