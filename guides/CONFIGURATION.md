# PUPHAX Service - Configuration Guide

Advanced configuration options for PUPHAX REST API Service.

## 📄 Configuration File

Default configuration in `src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: puphax-rest-api

puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    username: PUPHAX
    password: puphax
    connection-timeout: 30000
    read-timeout: 30000

  cache:
    enabled: true
    ttl-minutes: 60
    max-size: 1000
```

---

## 🔧 Environment Variables

Override configuration using environment variables:

### Server Configuration

```bash
# Change server port
export SERVER_PORT=8082

# Enable/disable SSL
export SERVER_SSL_ENABLED=true
```

### PUPHAX SOAP Configuration

```bash
# Override PUPHAX endpoint
export PUPHAX_SOAP_ENDPOINT_URL=https://custom-puphax-endpoint.com

# Override credentials
export PUPHAX_SOAP_USERNAME=your-username
export PUPHAX_SOAP_PASSWORD=your-password

# Timeout settings (milliseconds)
export PUPHAX_SOAP_CONNECTION_TIMEOUT=60000
export PUPHAX_SOAP_READ_TIMEOUT=60000
```

### Cache Configuration

```bash
# Disable cache
export PUPHAX_CACHE_ENABLED=false

# Cache TTL in minutes
export PUPHAX_CACHE_TTL_MINUTES=120

# Max cache size
export PUPHAX_CACHE_MAX_SIZE=5000
```

---

## 🐳 Docker Configuration

### Docker Compose

Edit `docker/docker-compose.yml`:

```yaml
services:
  puphax-service:
    environment:
      - SERVER_PORT=8081
      - PUPHAX_SOAP_USERNAME=PUPHAX
      - PUPHAX_SOAP_PASSWORD=puphax
      - PUPHAX_CACHE_ENABLED=true
      - PUPHAX_CACHE_TTL_MINUTES=60
    ports:
      - "8081:8081"
```

### Docker Run

```bash
docker run -d \
  -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e PUPHAX_SOAP_USERNAME=PUPHAX \
  -e PUPHAX_SOAP_PASSWORD=puphax \
  -e PUPHAX_CACHE_ENABLED=true \
  puphax-service:latest
```

---

## 📊 Logging Configuration

### Log Levels

Add to `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.puphax: DEBUG
    org.springframework: INFO
    org.springframework.web: DEBUG
```

### Log to File

```yaml
logging:
  file:
    name: logs/puphax-service.log
    max-size: 10MB
    max-history: 30
```

### Environment Variable

```bash
export LOGGING_LEVEL_COM_PUPHAX=DEBUG
```

---

## 🔄 CSV Fallback Configuration

CSV fallback is automatically enabled when SOAP service is unavailable.

### CSV Data Location

Default: `src/main/resources/data/TERMEK.csv`

To use custom CSV:

1. Place your CSV in `src/main/resources/data/`
2. Update `PuphaxCsvFallbackService.java`:

```java
private static final String CSV_FILE_PATH = "data/YOUR_CUSTOM_FILE.csv";
```

### CSV Format

The CSV should have 44 columns matching NEAK TERMEK structure:

```
ID,PARENT_ID,ERV_KEZD,ERV_VEGE,TERMEKKOD,KOZHID,TTT,...
55827054,,2022-06-01,2099-12-31,7612345678901,123456789012,7612345678901,...
```

---

## 🎯 API Configuration

### CORS Configuration

Add to `application.yml`:

```yaml
spring:
  web:
    cors:
      allowed-origins: "*"
      allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
      allowed-headers: "*"
      max-age: 3600
```

Or via Java configuration in `CorsConfig.java`:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
```

### Pagination Defaults

Modify in controller or configuration:

```java
@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "20") int size  // Change default page size
```

---

## 🔒 Security Configuration (Optional)

### Basic Authentication

Add Spring Security dependency and configure:

```yaml
spring:
  security:
    user:
      name: admin
      password: secret123
```

### API Key Authentication

Implement custom filter:

```java
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    // Custom implementation
}
```

---

## 📈 Monitoring Configuration

### Spring Actuator

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configure in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

Access at: `http://localhost:8081/actuator/health`

---

## 🧪 Profile-Specific Configuration

### Development Profile

`application-dev.yml`:

```yaml
logging:
  level:
    com.puphax: DEBUG

puphax:
  cache:
    enabled: false
```

Run with profile:

```bash
java -jar target/puphax-rest-api-1.0.0.jar --spring.profiles.active=dev
```

### Production Profile

`application-prod.yml`:

```yaml
logging:
  level:
    com.puphax: WARN

puphax:
  cache:
    enabled: true
    ttl-minutes: 120
```

---

## 🔍 Advanced Tuning

### JVM Settings

```bash
export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
java $JAVA_OPTS -jar target/puphax-rest-api-1.0.0.jar
```

### Connection Pool

Add HikariCP configuration (if using database):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

---

## 📝 Configuration Reference

**Complete list of properties**:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8081 | Server port |
| `puphax.soap.endpoint-url` | https://puphax.neak.gov.hu/PUPHAXWS | SOAP endpoint |
| `puphax.soap.username` | PUPHAX | SOAP username |
| `puphax.soap.password` | puphax | SOAP password |
| `puphax.soap.connection-timeout` | 30000 | Connection timeout (ms) |
| `puphax.soap.read-timeout` | 30000 | Read timeout (ms) |
| `puphax.cache.enabled` | true | Enable cache |
| `puphax.cache.ttl-minutes` | 60 | Cache TTL |
| `puphax.cache.max-size` | 1000 | Max cache entries |

---

**For deployment configurations, see:** [DEPLOYMENT.md](DEPLOYMENT.md)
**For troubleshooting, see:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
