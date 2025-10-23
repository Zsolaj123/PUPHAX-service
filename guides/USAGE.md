# PUPHAX Service - Usage Guide

API endpoints and usage examples.

## 📍 Base URL

- Local: `http://localhost:8081`
- Production: `https://your-domain.com`

## 🔍 API Endpoints

### Health Check

**Hungarian:**
```bash
GET /api/v1/gyogyszerek/egeszseg/gyors
```

**English:**
```bash
GET /api/v1/drugs/health/quick
```

### Search Drugs

**Hungarian:**
```bash
GET /api/v1/gyogyszerek/kereses?keresett_kifejezés={searchTerm}
```

**English:**
```bash
GET /api/v1/drugs/search?searchTerm={searchTerm}
```

**Response Fields:**
- `id` - Drug ID
- `name` - Drug name
- `manufacturer` - Manufacturer
- `atcCode` - ATC classification code
- `activeIngredients` - List of active substances
- `prescriptionRequired` - Requires prescription (boolean)
- `productForm` - Pharmaceutical form (tablet, capsule, etc.)
- `strength` - Potency/strength
- `packSize` - Package size
- `validFrom` / `validTo` - Validity dates
- `source` - Data source (NEAK SOAP or CSV Fallback)

## 💡 Examples

### Search for Aspirin

```bash
curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"
```

### Search for Active Ingredient

```bash
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=paracetamol"
```

### Pagination

```bash
curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin&page=0&size=10"
```

## 📊 Response Format

```json
{
  "gyogyszerek": [
    {
      "id": "55827054",
      "nev": "ASPIRIN 500 MG TABLETTA",
      "gyarto": "Bayer",
      "atcKod": "N02BA01",
      "hatoanyagok": ["acetilszalicilsav"],
      "venykoeteles": false,
      "productForm": "TABLETTA",
      "strength": "500 MG",
      "packSize": "20 DB",
      "validFrom": "2022-06-01",
      "validTo": "2099-12-31",
      "source": "CSV Fallback (NEAK Historical Data 2007-2023)"
    }
  ]
}
```

## 🔐 Authentication

Currently no authentication required for public endpoints.

## 📖 Interactive Documentation

Swagger UI: `http://localhost:8081/swagger-ui.html`

OpenAPI JSON: `http://localhost:8081/v3/api-docs`

---

**For complete field documentation, see:** [../docs/COMPREHENSIVE_DATA_FIELDS.md](../docs/COMPREHENSIVE_DATA_FIELDS.md)
