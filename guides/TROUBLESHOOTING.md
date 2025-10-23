# PUPHAX Service - Troubleshooting Guide

Common issues and solutions for PUPHAX REST API Service.

## 🔍 General Troubleshooting

### Check Service Status

```bash
# Docker
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml logs -f

# Systemd
sudo systemctl status puphax
sudo journalctl -u puphax -f

# Health check
curl http://localhost:8081/api/v1/drugs/health/quick
```

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.puphax: DEBUG
    org.springframework.web: DEBUG
```

Or via environment variable:

```bash
export LOGGING_LEVEL_COM_PUPHAX=DEBUG
```

---

## 🚨 Common Issues

### 1. Port Already in Use

**Error:**
```
Web server failed to start. Port 8081 was already in use.
```

**Solution A:** Find and stop the conflicting process

```bash
# Linux/Mac
lsof -i :8081
kill -9 <PID>

# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

**Solution B:** Change the port

```bash
# Environment variable
export SERVER_PORT=8082

# Or in application.yml
server:
  port: 8082
```

### 2. Cannot Connect to NEAK SOAP Service

**Error in logs:**
```
Failed to connect to PUPHAX SOAP endpoint: Connection timeout
Falling back to CSV data source
```

**Diagnosis:**
- This is expected behavior when NEAK SOAP service is unavailable
- CSV fallback will activate automatically

**Verify CSV fallback:**

```bash
curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"
```

Look for `"source": "CSV Fallback (NEAK Historical Data 2007-2023)"` in response

**If CSV fallback also fails:**

1. Check if CSV file exists:
   ```bash
   ls -lh src/main/resources/data/TERMEK.csv
   ```

2. Check logs for CSV loading errors:
   ```bash
   grep "CSV" logs/application.log
   ```

3. Verify CSV format (44 columns, UTF-8 encoding)

### 3. Empty or Invalid Search Results

**Problem:** Search returns no results or malformed data

**Diagnosis steps:**

1. **Check search term encoding:**
   ```bash
   # Use URL encoding for special characters
   curl "http://localhost:8081/api/v1/drugs/search?searchTerm=paracetam%C3%B3l"
   ```

2. **Try simple search:**
   ```bash
   curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"
   ```

3. **Check logs for parsing errors:**
   ```bash
   docker compose -f docker/docker-compose.yml logs | grep ERROR
   ```

4. **Verify data source:**
   ```bash
   # Response should include "source" field
   curl -s "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin" | grep source
   ```

### 4. Java Out of Memory Error

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:** Increase JVM heap size

**For JAR deployment:**

```bash
java -Xms1024m -Xmx4096m -jar puphax-rest-api-1.0.0.jar
```

**For Docker:**

Edit `docker-compose.yml`:

```yaml
services:
  puphax-service:
    environment:
      - JAVA_OPTS=-Xms1024m -Xmx4096m
```

**For Kubernetes:**

Update `deployment.yaml`:

```yaml
resources:
  limits:
    memory: "4Gi"
  requests:
    memory: "1Gi"
```

### 5. Swagger UI Not Loading

**Problem:** Cannot access `http://localhost:8081/swagger-ui.html`

**Solutions:**

1. **Try alternative URL:**
   ```
   http://localhost:8081/swagger-ui/index.html
   ```

2. **Check if SpringDoc is enabled:**

   Add to `application.yml`:
   ```yaml
   springdoc:
     api-docs:
       enabled: true
     swagger-ui:
       enabled: true
   ```

3. **Verify dependency in pom.xml:**
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
   </dependency>
   ```

### 6. 404 Not Found on API Endpoints

**Problem:** Endpoints return 404

**Common causes:**

1. **Wrong base path:**
   ```
   ❌ http://localhost:8081/drugs/search
   ✅ http://localhost:8081/api/v1/drugs/search
   ```

2. **Wrong HTTP method:**
   ```bash
   # Use GET, not POST
   curl -X GET "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"
   ```

3. **Check available endpoints:**
   ```bash
   curl http://localhost:8081/v3/api-docs
   ```

### 7. Slow Response Times

**Problem:** API responses take > 5 seconds

**Diagnosis:**

1. **Check if cache is enabled:**
   ```yaml
   puphax:
     cache:
       enabled: true  # Should be true
   ```

2. **Monitor logs for SOAP timeout:**
   ```bash
   grep "timeout" logs/application.log
   ```

3. **Increase SOAP timeout:**
   ```yaml
   puphax:
     soap:
       connection-timeout: 60000  # 60 seconds
       read-timeout: 60000
   ```

4. **Check system resources:**
   ```bash
   # CPU and memory usage
   docker stats puphax-rest-api

   # Or for systemd
   systemctl status puphax
   ```

### 8. Docker Build Fails

**Error:**
```
failed to solve: process "/bin/sh -c ./mvnw clean package -DskipTests" did not complete successfully
```

**Solutions:**

1. **Check Docker daemon:**
   ```bash
   docker info
   ```

2. **Increase Docker memory:**

   Docker Desktop → Settings → Resources → Memory (set to 4GB+)

3. **Clean and rebuild:**
   ```bash
   docker compose -f docker/docker-compose.yml down
   docker system prune -af
   docker compose -f docker/docker-compose.yml build --no-cache
   docker compose -f docker/docker-compose.yml up -d
   ```

4. **Build JAR locally first:**
   ```bash
   ./mvnw clean package -DskipTests
   docker compose -f docker/docker-compose.yml build
   ```

### 9. Maven Build Fails

**Error:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solutions:**

1. **Verify Java version:**
   ```bash
   java -version  # Should be Java 17
   ```

2. **Clean Maven cache:**
   ```bash
   ./mvnw clean
   rm -rf ~/.m2/repository
   ./mvnw package -DskipTests
   ```

3. **Skip tests if failing:**
   ```bash
   ./mvnw package -Dmaven.test.skip=true
   ```

4. **Check for network issues:**
   ```bash
   # Test Maven Central connectivity
   curl -I https://repo.maven.apache.org/maven2/
   ```

### 10. Character Encoding Issues

**Problem:** Hungarian characters (ő, ű, á, etc.) display incorrectly

**Solution:**

1. **Ensure UTF-8 encoding in application.yml:**
   ```yaml
   spring:
     http:
       encoding:
         charset: UTF-8
         enabled: true
         force: true
   ```

2. **Set JVM encoding:**
   ```bash
   java -Dfile.encoding=UTF-8 -jar puphax-rest-api-1.0.0.jar
   ```

3. **For Docker:**
   ```yaml
   environment:
     - JAVA_OPTS=-Dfile.encoding=UTF-8
   ```

4. **Check CSV encoding:**
   ```bash
   file -i src/main/resources/data/TERMEK.csv
   # Should show: charset=utf-8
   ```

---

## 🔧 Advanced Debugging

### Enable Request/Response Logging

Add to `application.yml`:

```yaml
logging:
  level:
    org.springframework.web.servlet.mvc.method.annotation: TRACE
    org.springframework.web.client.RestTemplate: DEBUG
```

### Thread Dump

```bash
# Get process ID
ps aux | grep puphax

# Create thread dump
jstack <PID> > thread-dump.txt
```

### Heap Dump

```bash
# Create heap dump
jmap -dump:format=b,file=heap-dump.hprof <PID>

# Analyze with jhat (built-in)
jhat heap-dump.hprof
# Open http://localhost:7000
```

### Network Debugging

```bash
# Test SOAP endpoint connectivity
curl -v https://puphax.neak.gov.hu/PUPHAXWS

# Test with specific timeout
curl --max-time 10 http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin

# Monitor network traffic
tcpdump -i any -s 0 -A 'port 8081'
```

---

## 📝 Collecting Diagnostic Information

When reporting issues, collect:

1. **Application logs:**
   ```bash
   docker compose -f docker/docker-compose.yml logs --tail=200 > logs.txt
   ```

2. **System information:**
   ```bash
   docker version
   docker compose version
   java -version
   uname -a
   ```

3. **Configuration:**
   ```bash
   cat src/main/resources/application.yml
   env | grep PUPHAX
   ```

4. **Health check:**
   ```bash
   curl -v http://localhost:8081/api/v1/drugs/health/quick > health.txt
   ```

5. **Sample request:**
   ```bash
   curl -v "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin" > response.txt
   ```

---

## 🆘 Getting Help

**Still having issues?**

1. Check [GitHub Issues](https://github.com/Zsolaj123/PUPHAX-service/issues)
2. Search for similar problems in closed issues
3. Create a new issue with:
   - Detailed problem description
   - Steps to reproduce
   - Diagnostic information (see above)
   - Environment details (OS, Docker version, Java version)

**Useful resources:**

- [Installation Guide](INSTALLATION.md)
- [Configuration Guide](CONFIGURATION.md)
- [Deployment Guide](DEPLOYMENT.md)
- [Developer Guide](../docs/PUPHAX_DEVELOPMENT_GUIDE.md)

---

**Last Updated:** 2025-10-23
