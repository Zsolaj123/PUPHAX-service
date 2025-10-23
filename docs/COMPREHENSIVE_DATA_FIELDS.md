# Comprehensive Drug Data Fields

## Overview

The PUPHAX REST API Service now provides access to ALL 44 fields available in the NEAK PUPHAX database, ensuring comprehensive pharmaceutical data for both SOAP web service responses and CSV fallback data.

## Data Source Dual Functionality

The service operates in two modes with identical data fields:

1. **NEAK SOAP Web Service** (Primary): When available, fetches real-time data from NEAK
2. **CSV Fallback** (Secondary): When SOAP is unavailable, uses historical data (2007-2023) with 43,930 current drug products

Both sources provide the same comprehensive field set for consistency.

## Available Data Fields (44 Total)

### Core Identification (12 fields)
- `id` - Unique product identifier
- `parentId` - Parent product reference
- `productCode` - NEAK product code (TERMEKKOD)
- `eanCode` - European Article Number barcode
- `validFrom` - Validity start date
- `validTo` - Validity end date
- `tttCode` - Prescription category code (TTT)
- `tkCode` - Subsidy category code (TK)
- `tkDeletion` - Subsidy deletion flag
- `tkDeletionDate` - Subsidy deletion date
- `brandId` - Brand/manufacturer ID
- `publicationId` - Official publication reference (KIHIRDETES_ID)

### Names and Descriptions (2 fields)
- `name` - Full product name (NEV)
- `shortName` - Abbreviated name (KISZNEV)

### Classification (3 fields)
- `atcCode` - Anatomical Therapeutic Chemical code
- `atcDescription` - ATC classification description
- `isoCode` - ISO standard code

### Active Ingredients (1 field)
- `activeIngredients` - List of active substances (HATOANYAG)

### Pharmaceutical Form and Administration (2 fields)
- `pharmaceuticalForm` - Dosage form (tablet, capsule, etc.) (GYFORMA)
- `administrationMethod` - Route of administration (ADAGMOD)

### Strength and Dosage (12 fields)
- `strength` - Product potency (POTENCIA)
- `activeSubstanceAmount` - Active ingredient quantity (HATO_MENNY)
- `activeSubstanceUnit` - Unit of measurement (HATO_EGYS)
- `originalActiveAmount` - Original active amount (OHATO_MENNY)
- `packageSize` - Package contents quantity (KISZ_MENNY)
- `packageSizeUnit` - Package unit (KISZ_EGYS)
- `dddAmount` - Defined Daily Dose amount (DDD_MENNY)
- `dddUnit` - DDD unit of measurement (DDD_EGYS)
- `dddFactor` - DDD calculation factor (DDD_FAKTOR)
- `dotCode` - DOT classification
- `dosageAmount` - Single dose amount (ADAG_MENNY)
- `dosageUnit` - Dosage unit (ADAG_EGYS)

### Regulatory and Prescription (4 fields)
- `prescriptionRequired` - Boolean: requires prescription
- `prescribable` - Prescription eligibility (RENDELHET)
- `reimbursable` - Boolean: eligible for reimbursement
- `substitutable` - Generic substitution allowed (HELYETTESITH)

### Special Attributes (6 fields)
- `specialIndication` - Special use indicator (EGYEDI)
- `laterality` - Left/right specification (OLDALISAG)
- `multipleWarranty` - Multiple warranty flag (TOBBLGAR)
- `pharmacyOnly` - Pharmacy-only distribution (PATIKA)
- `boxIdentifier` - Box identification (DOBAZON)
- `crossReference` - Cross-reference code (KERESZTJELZES)

### Distribution (2 fields)
- `distributorAuthorizationId` - Distribution authorization (FORGENGT_ID)
- `distributorId` - Distributor company ID (FORGALMAZ_ID)
- `inStock` - Boolean: currently in stock (FORGALOMBAN)

## CSV Data Statistics

When using CSV fallback, the service provides:
- **43,930 current drug products** (only valid/recent products)
- **8,905 brand names** from BRAND table
- **6,828 ATC classification codes** from ATCKONYV table  
- **2,314 pharmaceutical companies** from CEGEK table

## API Response Format

### Hungarian Endpoint
```
GET /api/v1/gyogyszerek/kereses?keresett_kifejezés={searchTerm}
```

### English Endpoint  
```
GET /api/v1/drugs/search?searchTerm={searchTerm}
```

### Response includes
- All 44 fields where available
- Data source indicator (NEAK SOAP or CSV Fallback)
- Search metadata (total count, response time)

## Performance

- **CSV Fallback**: <50ms average response time
- **NEAK SOAP**: ~2s average response time (when available)
- **Search Index**: 28,947 searchable keywords
- **Memory footprint**: ~150MB for full CSV dataset

## Data Freshness

- **SOAP Service**: Real-time data from NEAK
- **CSV Fallback**: Historical snapshot (2007-2023)
  - Contains products valid until 2099
  - Filtered to include only current/recent products (within 2 years)
  - Automatically updated when NEAK releases new historical dumps

## Usage Example

```bash
# Search for aspirin with comprehensive data
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin"

# Returns all 44 fields for each matching product
# Including: names, codes, classifications, dosages, regulatory info, etc.
```

## Notes

- Fields may be null/empty if not applicable to specific product
- CSV fallback provides same field structure as SOAP for consistency
- All Hungarian characters properly encoded (UTF-8)
- Brand and manufacturer names resolved from lookup tables
- ATC descriptions provided in both Hungarian and English where available

---

**Last Updated**: 2025-10-23  
**Version**: 1.1.0
