# PUPHAX Service - Deployment Guide

Production deployment instructions for PUPHAX REST API Service.

## 🐳 Docker Deployment (Recommended)

### Quick Deploy with Docker Compose

```bash
# 1. Clone repository
git clone https://github.com/Zsolaj123/PUPHAX-service.git
cd PUPHAX-service

# 2. Build and deploy
docker compose -f docker/docker-compose.yml up -d

# 3. Verify
curl http://localhost:8081/api/v1/drugs/health/quick
```

### Custom Docker Compose Configuration

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  puphax-service:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: puphax-rest-api
    ports:
      - "8081:8081"
    environment:
      - SERVER_PORT=8081
      - PUPHAX_SOAP_USERNAME=${PUPHAX_USERNAME}
      - PUPHAX_SOAP_PASSWORD=${PUPHAX_PASSWORD}
      - PUPHAX_CACHE_ENABLED=true
      - PUPHAX_CACHE_TTL_MINUTES=120
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/v1/drugs/health/quick"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

Deploy:

```bash
docker compose -f docker-compose.prod.yml up -d
```

---

## 🚀 Kubernetes Deployment

### Deployment Configuration

`k8s/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: puphax-service
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: puphax-service
  template:
    metadata:
      labels:
        app: puphax-service
    spec:
      containers:
      - name: puphax-service
        image: your-registry/puphax-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SERVER_PORT
          value: "8081"
        - name: PUPHAX_SOAP_USERNAME
          valueFrom:
            secretKeyRef:
              name: puphax-credentials
              key: username
        - name: PUPHAX_SOAP_PASSWORD
          valueFrom:
            secretKeyRef:
              name: puphax-credentials
              key: password
        - name: PUPHAX_CACHE_ENABLED
          value: "true"
        - name: PUPHAX_CACHE_TTL_MINUTES
          value: "120"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/v1/drugs/health/quick
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/drugs/health/quick
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
```

### Service Configuration

`k8s/service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: puphax-service
  namespace: default
spec:
  type: LoadBalancer
  selector:
    app: puphax-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8081
```

### Secret Configuration

```bash
# Create secret
kubectl create secret generic puphax-credentials \
  --from-literal=username=PUPHAX \
  --from-literal=password=puphax
```

### Deploy to Kubernetes

```bash
# Apply configurations
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Verify deployment
kubectl get pods
kubectl get services

# Check logs
kubectl logs -f deployment/puphax-service
```

---

## 🌐 Reverse Proxy Configuration

### Nginx

`/etc/nginx/sites-available/puphax`:

```nginx
upstream puphax_backend {
    server 127.0.0.1:8081;
    keepalive 32;
}

server {
    listen 80;
    server_name api.your-domain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.your-domain.com;

    # SSL configuration
    ssl_certificate /etc/letsencrypt/live/api.your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Logging
    access_log /var/log/nginx/puphax-access.log;
    error_log /var/log/nginx/puphax-error.log;

    # Proxy settings
    location /api/ {
        proxy_pass http://puphax_backend/api/;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Swagger UI
    location /swagger-ui/ {
        proxy_pass http://puphax_backend/swagger-ui/;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
    }

    # Health check
    location /health {
        proxy_pass http://puphax_backend/api/v1/drugs/health/quick;
        access_log off;
    }
}
```

Enable and restart:

```bash
sudo ln -s /etc/nginx/sites-available/puphax /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Apache

`/etc/apache2/sites-available/puphax.conf`:

```apache
<VirtualHost *:80>
    ServerName api.your-domain.com
    Redirect permanent / https://api.your-domain.com/
</VirtualHost>

<VirtualHost *:443>
    ServerName api.your-domain.com

    SSLEngine on
    SSLCertificateFile /etc/letsencrypt/live/api.your-domain.com/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/api.your-domain.com/privkey.pem

    ProxyPreserveHost On
    ProxyPass /api/ http://127.0.0.1:8081/api/
    ProxyPassReverse /api/ http://127.0.0.1:8081/api/

    ProxyPass /swagger-ui/ http://127.0.0.1:8081/swagger-ui/
    ProxyPassReverse /swagger-ui/ http://127.0.0.1:8081/swagger-ui/

    ErrorLog ${APACHE_LOG_DIR}/puphax-error.log
    CustomLog ${APACHE_LOG_DIR}/puphax-access.log combined
</VirtualHost>
```

Enable and restart:

```bash
sudo a2enmod ssl proxy proxy_http
sudo a2ensite puphax
sudo apachectl configtest
sudo systemctl restart apache2
```

---

## 📦 Standalone JAR Deployment

### Build JAR

```bash
# Build with Maven
./mvnw clean package -DskipTests

# JAR location
ls -lh target/puphax-rest-api-1.0.0.jar
```

### Deploy as Systemd Service

Create `/etc/systemd/system/puphax.service`:

```ini
[Unit]
Description=PUPHAX REST API Service
After=network.target

[Service]
Type=simple
User=puphax
WorkingDirectory=/opt/puphax
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -jar /opt/puphax/puphax-rest-api-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=puphax

Environment="SERVER_PORT=8081"
Environment="PUPHAX_SOAP_USERNAME=PUPHAX"
Environment="PUPHAX_SOAP_PASSWORD=puphax"

[Install]
WantedBy=multi-user.target
```

Deploy:

```bash
# Create user and directory
sudo useradd -r -s /bin/false puphax
sudo mkdir -p /opt/puphax
sudo cp target/puphax-rest-api-1.0.0.jar /opt/puphax/
sudo chown -R puphax:puphax /opt/puphax

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable puphax
sudo systemctl start puphax

# Check status
sudo systemctl status puphax
sudo journalctl -u puphax -f
```

---

## 🔒 SSL/TLS Configuration

### Generate Self-Signed Certificate (Development)

```bash
keytool -genkeypair -alias puphax -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore puphax-keystore.p12 -validity 3650 \
  -dname "CN=localhost, OU=Development, O=PUPHAX, L=Budapest, ST=Hungary, C=HU"
```

Configure in `application.yml`:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:puphax-keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: puphax
```

### Let's Encrypt (Production)

```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate
sudo certbot certonly --standalone -d api.your-domain.com

# Certificates will be at:
# /etc/letsencrypt/live/api.your-domain.com/fullchain.pem
# /etc/letsencrypt/live/api.your-domain.com/privkey.pem

# Use with Nginx/Apache (see reverse proxy section above)
```

---

## 📊 Monitoring and Logging

### Centralized Logging with ELK

Docker Compose with Elasticsearch/Logstash/Kibana:

```yaml
version: '3.8'

services:
  puphax-service:
    # ... existing config
    logging:
      driver: "gelf"
      options:
        gelf-address: "udp://logstash:12201"
        tag: "puphax-service"

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.10.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "12201:12201/udp"

  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    ports:
      - "5601:5601"
```

### Prometheus Metrics

Add Spring Boot Actuator + Micrometer:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Configure:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Scrape endpoint: `http://localhost:8081/actuator/prometheus`

---

## 🔄 CI/CD Pipeline

### GitHub Actions

`.github/workflows/deploy.yml`:

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build with Maven
      run: ./mvnw clean package -DskipTests

    - name: Build Docker image
      run: docker build -f docker/Dockerfile -t puphax-service:${{ github.sha }} .

    - name: Push to registry
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker tag puphax-service:${{ github.sha }} your-registry/puphax-service:latest
        docker push your-registry/puphax-service:latest

    - name: Deploy to server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        script: |
          cd /opt/puphax
          docker compose pull
          docker compose up -d
```

---

## ✅ Post-Deployment Checklist

- [ ] Service is running and accessible
- [ ] Health check endpoint responds: `/api/v1/drugs/health/quick`
- [ ] Swagger UI accessible: `/swagger-ui.html`
- [ ] Search endpoint works: `/api/v1/drugs/search?searchTerm=aspirin`
- [ ] CSV fallback working (if SOAP unavailable)
- [ ] Logs are being collected
- [ ] SSL/TLS configured (for production)
- [ ] Monitoring/alerting configured
- [ ] Backup strategy in place

---

**For configuration details, see:** [CONFIGURATION.md](CONFIGURATION.md)
**For troubleshooting, see:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
