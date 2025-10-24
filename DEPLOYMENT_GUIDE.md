# PUPHAX Service - Deployment Guide

**Version**: 1.0.0 (Enhanced)
**Date**: 2025-10-24
**Target Environment**: Production

## Overview

This guide covers the deployment of the enhanced PUPHAX drug database search system with advanced filtering, modern UI, and comprehensive data display.

## Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or later
- **Memory**: Minimum 2GB RAM (4GB recommended)
- **Disk Space**: 500MB minimum
- **Operating System**: Linux, macOS, or Windows

### Software Dependencies
- Maven 3.8+
- Git
- Docker (optional, for containerized deployment)

## Build Process

### Step 1: Clean Build
```bash
# Navigate to project directory
cd /home/zsine/PUPHAX-service

# Set JAVA_HOME
export JAVA_HOME=/home/zsine/PUPHAX-service/jdk-17.0.8.1+1
export PATH=$JAVA_HOME/bin:$PATH

# Clean and build
./mvnw clean package -DskipTests
```

**Expected Output**:
- JAR file: `target/puphax-rest-api-1.0.0.jar`
- Size: ~35-40 MB
- Build time: ~30-60 seconds

### Step 2: Verify Build
```bash
# Check JAR exists
ls -lh target/puphax-rest-api-1.0.0.jar

# Verify manifest
jar -xf target/puphax-rest-api-1.0.0.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
```

## Deployment Options

### Option 1: Standalone JAR (Recommended)

#### Start Application
```bash
# Kill any existing instances
killall -9 java 2>/dev/null

# Start application
export JAVA_HOME=/home/zsine/PUPHAX-service/jdk-17.0.8.1+1
java -jar target/puphax-rest-api-1.0.0.jar

# Or with custom port
java -jar target/puphax-rest-api-1.0.0.jar --server.port=8080
```

#### Run in Background
```bash
nohup java -jar target/puphax-rest-api-1.0.0.jar > puphax.log 2>&1 &
echo $! > puphax.pid
```

#### Check Status
```bash
# Check if running
curl http://localhost:8081/actuator/health

# View logs
tail -f puphax.log

# Stop application
kill $(cat puphax.pid)
```

### Option 2: Docker Deployment

#### Build Docker Image
```bash
# Create Dockerfile (if not exists)
cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/puphax-rest-api-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build image
docker build -t puphax-service:1.0.0 .
```

#### Run Container
```bash
# Run container
docker run -d \
  --name puphax-service \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  puphax-service:1.0.0

# Check logs
docker logs -f puphax-service

# Stop container
docker stop puphax-service
docker rm puphax-service
```

### Option 3: Docker Compose

#### Create docker-compose.yml
```yaml
version: '3.8'

services:
  puphax:
    image: puphax-service:1.0.0
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx2g -Xms512m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped
```

#### Deploy
```bash
docker-compose up -d
docker-compose logs -f
```

## Configuration

### Application Properties

#### Production Configuration (application-prod.properties)
```properties
# Server
server.port=8081
server.compression.enabled=true

# Logging
logging.level.root=INFO
logging.level.com.puphax=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Cache
spring.cache.type=simple

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

### Environment Variables
```bash
# Application
export SERVER_PORT=8081
export SPRING_PROFILES_ACTIVE=prod

# JVM Options
export JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseG1GC"

# PUPHAX Credentials (if using live service)
export PUPHAX_USERNAME=your_username
export PUPHAX_PASSWORD=your_password
```

## Post-Deployment Verification

### Health Check
```bash
# Application health
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP"}
```

### API Endpoints Test
```bash
# Test filter options endpoint
curl http://localhost:8081/api/v1/drugs/filters | python3 -m json.tool

# Test advanced search
curl -X POST http://localhost:8081/api/v1/drugs/search/advanced \
  -H "Content-Type: application/json" \
  -d '{"searchTerm": "aspirin", "size": 5}' | python3 -m json.tool
```

### Frontend Access
```bash
# Open in browser
# http://localhost:8081

# Expected: PUPHAX search interface with advanced filters
```

### Performance Check
```bash
# Check response times
time curl -s http://localhost:8081/api/v1/drugs/filters > /dev/null

# Expected: < 500ms
```

## Monitoring

### Application Metrics
- **Health**: http://localhost:8081/actuator/health
- **Info**: http://localhost:8081/actuator/info
- **Metrics**: http://localhost:8081/actuator/metrics

### Log Files
```bash
# View logs
tail -f puphax.log

# Search for errors
grep ERROR puphax.log

# Search for correlation ID
grep "req-" puphax.log | grep "your-correlation-id"
```

### Resource Usage
```bash
# Check Java process
ps aux | grep java

# Check memory
free -h

# Check disk
df -h
```

## Troubleshooting

### Port Already in Use
```bash
# Find process on port 8081
lsof -ti :8081

# Kill process
kill -9 $(lsof -ti :8081)
```

### Application Won't Start
```bash
# Check Java version
java -version

# Check JAVA_HOME
echo $JAVA_HOME

# Check JAR integrity
jar -tf target/puphax-rest-api-1.0.0.jar | head

# Run with debug logging
java -jar target/puphax-rest-api-1.0.0.jar --debug
```

### CSV Data Not Loading
```bash
# Check CSV file exists
ls -lh src/main/resources/neak-data/

# Check logs for "CSV fallback service initialized"
grep "CSV fallback service" puphax.log
```

### 500 Internal Server Errors
```bash
# Check application logs
tail -100 puphax.log

# Check for stack traces
grep -A 10 "Exception" puphax.log

# Enable debug logging
export LOGGING_LEVEL_COM_PUPHAX=DEBUG
```

## Performance Tuning

### JVM Tuning
```bash
# For production (4GB server)
java -Xmx2g -Xms1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails \
     -jar target/puphax-rest-api-1.0.0.jar
```

### Cache Configuration
- Filter options are cached in memory
- CSV data loaded once on startup
- Consider adding Redis for distributed caching

### Database Connection Pool
- Default: No database (CSV fallback)
- If using PostgreSQL, configure HikariCP:
  ```properties
  spring.datasource.hikari.maximum-pool-size=10
  spring.datasource.hikari.minimum-idle=5
  ```

## Security Considerations

### SSL/TLS
```bash
# Generate self-signed certificate (dev/test only)
keytool -genkeypair -alias puphax \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 \
  -validity 3650

# Configure in application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

### Rate Limiting
- Built-in: 60 requests/min per IP
- 1000 requests/hour per IP
- Configure in RateLimitConfig.java

### CORS
```properties
# Allow specific origins
cors.allowed-origins=https://yourdomain.com
```

## Backup and Restore

### Backup
```bash
# Backup CSV data
tar -czf neak-data-backup-$(date +%Y%m%d).tar.gz \
  src/main/resources/neak-data/

# Backup configuration
cp application.properties application-backup.properties
```

### Restore
```bash
# Restore CSV data
tar -xzf neak-data-backup-20251024.tar.gz
```

## Rollback Procedure

### Rollback to Previous Version
```bash
# Stop current version
kill $(cat puphax.pid)

# Checkout previous version
git checkout <previous-commit-hash>

# Rebuild
./mvnw clean package -DskipTests

# Start
java -jar target/puphax-rest-api-1.0.0.jar
```

### Rollback Specific Features
```bash
# Rollback to before Phase 5
git checkout e9ad7e8^

# Rebuild and deploy
./mvnw clean package -DskipTests
```

## Scaling

### Horizontal Scaling
```bash
# Run multiple instances behind load balancer
# Instance 1
java -jar target/puphax-rest-api-1.0.0.jar --server.port=8081

# Instance 2
java -jar target/puphax-rest-api-1.0.0.jar --server.port=8082

# Configure nginx/HAProxy to load balance
```

### Vertical Scaling
```bash
# Increase memory
java -Xmx4g -Xms2g -jar target/puphax-rest-api-1.0.0.jar
```

## Maintenance

### Regular Tasks
- **Daily**: Check logs for errors
- **Weekly**: Monitor disk space, restart if memory leak suspected
- **Monthly**: Update dependencies, security patches
- **Quarterly**: Review and optimize database/cache

### Updates
```bash
# Pull latest changes
git pull origin main

# Rebuild
./mvnw clean package -DskipTests

# Rolling restart (zero downtime)
# Start new instance on different port
# Update load balancer
# Stop old instance
```

## Support

### Logs Location
- Application logs: `puphax.log`
- Access logs: `access.log` (if configured)
- Error logs: Check `puphax.log` for ERROR level

### Contact
- GitHub Issues: https://github.com/your-org/puphax-service/issues
- Email: support@yourdomain.com

## Appendix

### Quick Command Reference
```bash
# Build
./mvnw clean package -DskipTests

# Run
java -jar target/puphax-rest-api-1.0.0.jar

# Health check
curl http://localhost:8081/actuator/health

# Stop
kill $(cat puphax.pid)

# Logs
tail -f puphax.log
```

### Environment Checklist
- [ ] Java 17+ installed
- [ ] JAVA_HOME set correctly
- [ ] Port 8081 available
- [ ] CSV data files present
- [ ] Sufficient memory (2GB+)
- [ ] Disk space available (500MB+)
- [ ] Network connectivity
- [ ] Firewall rules configured

---

**Last Updated**: 2025-10-24
**Version**: 1.0.0
**Maintained By**: PUPHAX Development Team
