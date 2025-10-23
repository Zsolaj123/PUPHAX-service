# PUPHAX Service - Installation Guide

Complete installation instructions for PUPHAX REST API Service.

## 🐳 Docker Installation (Recommended)

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+

### Steps

```bash
# 1. Clone repository
git clone https://github.com/Zsolaj123/PUPHAX-service.git
cd PUPHAX-service

# 2. Build and start
docker compose -f docker/docker-compose.yml up -d

# 3. Verify
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

Service available at: `http://localhost:8081`

### Docker Commands

```bash
# View logs
docker compose -f docker/docker-compose.yml logs -f

# Stop service
docker compose -f docker/docker-compose.yml down

# Restart
docker compose -f docker/docker-compose.yml restart

# Rebuild after code changes
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d
```

---

## ☕ Manual Installation (Development)

### Prerequisites
- Java 17 (JDK)
- Maven 3.9+ (or use included `./mvnw`)

### Steps

```bash
# 1. Clone repository
git clone https://github.com/Zsolaj123/PUPHAX-service.git
cd PUPHAX-service

# 2. Build
./mvnw clean package -DskipTests

# 3. Run
java -jar target/puphax-rest-api-1.0.0.jar

# Or use Maven wrapper
./mvnw spring-boot:run
```

Service will start on port **8081**.

---

## 🔧 Configuration

Default configuration in `src/main/resources/application.yml`:

```yaml
server:
  port: 8081

puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    username: PUPHAX
    password: puphax
```

### Environment Variables

```bash
# Override port
export SERVER_PORT=8082

# Override PUPHAX credentials
export PUPHAX_SOAP_USERNAME=your-username
export PUPHAX_SOAP_PASSWORD=your-password

# Run with custom config
java -jar target/puphax-rest-api-1.0.0.jar
```

See [CONFIGURATION.md](CONFIGURATION.md) for advanced settings.

---

## ✅ Verification

### Health Check

```bash
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

Expected response:
```json
{
  "statu": "FEL",
  "uzenet": "Minden szolgáltatás működik",
  "verzio": "1.0.0"
}
```

### API Documentation

Open in browser: `http://localhost:8081/swagger-ui.html`

### Test Search

```bash
# Hungarian endpoint
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin"

# English endpoint
curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"
```

---

## 🚨 Troubleshooting

**Port already in use:**
```bash
# Check what's using port 8081
lsof -i :8081

# Or change port
export SERVER_PORT=8082
```

**Cannot connect to NEAK SOAP service:**
- CSV fallback will activate automatically
- Check logs for "CSV fallback service initialized"

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for more issues.

---

**For deployment to production, see:** [DEPLOYMENT.md](DEPLOYMENT.md)
**For development setup, see:** [../docs/PUPHAX_DEVELOPMENT_GUIDE.md](../docs/PUPHAX_DEVELOPMENT_GUIDE.md)
