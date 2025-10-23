# PUPHAX Magyar Frontend Docker Telepítési Útmutató

## Áttekintés

Ez az útmutató lépésről lépésre bemutatja, hogyan telepítheti és futtathatja a PUPHAX Magyar Gyógyszer Kereső alkalmazást Docker használatával. Az alkalmazás teljes mértékben magyar nyelvű és a NEAK PUPHAX webszolgáltatással integrálódik.

## 🚀 Gyors Telepítés

### Előfeltételek

1. **Docker telepítése**
   ```bash
   # Ubuntu/Debian
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   
   # Windows/macOS
   # Docker Desktop letöltése: https://www.docker.com/products/docker-desktop
   ```

2. **Docker Compose telepítése**
   ```bash
   # Linux
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   
   # Windows/macOS - Docker Desktop tartalmazza
   ```

### Automatikus Telepítés

1. **Projekt klónozása és telepítés**
   ```bash
   # Ha még nincs meg a projekt
   git clone <repository-url>
   cd PUPHAX-service
   
   # Automatikus telepítés
   ./docker-deploy.sh
   ```

2. **Frontend elérése**
   - **Magyar Frontend**: http://localhost:8081
   - **API Dokumentáció**: http://localhost:8081/swagger-ui.html
   - **Egészség állapot**: http://localhost:8081/api/v1/gyogyszerek/egeszseg

## 📝 Manuális Telepítés

### 1. Docker Image Építése

```bash
# Alapértelmezett build
docker-compose build

# Gyorsítótár nélküli build
docker-compose build --no-cache

# Vagy csak a Docker image
docker build -t puphax-hungarian:latest .
```

### 2. Alkalmazás Indítása

```bash
# Háttérben futtatás
docker-compose up -d

# Logokkal együtt
docker-compose up

# Csak egy példány futtatása
docker run -d -p 8080:8081 --name puphax-hungarian puphax-hungarian:latest
```

### 3. Állapot Ellenőrzése

```bash
# Konténerek állapota
docker-compose ps

# Logok megtekintése
docker-compose logs -f

# Egészség ellenőrzés
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

## ⚙️ Konfigurációs Lehetőségek

### Környezeti Változók

A `docker-compose.yml` fájlban módosíthatók:

```yaml
environment:
  # Port beállítás
  - SERVER_PORT=8080
  
  # Magyar karakterkódolás
  - LANG=hu_HU.UTF-8
  - LC_ALL=hu_HU.UTF-8
  
  # PUPHAX endpoint
  - PUPHAX_SOAP_ENDPOINT_URL=https://puphax.neak.gov.hu/PUPHAXWS
  
  # Cache beállítások
  - SPRING_CACHE_CAFFEINE_SPEC=maximumSize=1000,expireAfterWrite=10m
  
  # JVM optimalizáció
  - JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

### Port Módosítása

```yaml
# docker-compose.yml
ports:
  - "9090:8081"  # Külső port 9090, belső 8080
```

### Memória Korlátok

```yaml
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 2G
      cpus: '2.0'
    reservations:
      memory: 1G
      cpus: '1.0'
```

## 🛠️ Fejlesztői Parancsok

### Konténer Kezelés

```bash
# Leállítás
docker-compose down

# Újraindítás
docker-compose restart

# Konténerbe belépés
docker-compose exec puphax-hungarian bash

# Frissítés
docker-compose pull
docker-compose up -d
```

### Logok és Hibakeresés

```bash
# Valós idejű logok
docker-compose logs -f

# Csak az utolsó 100 sor
docker-compose logs --tail=100

# Hiba logok szűrése
docker-compose logs | grep ERROR

# Konténer erőforrás használat
docker stats puphax-hungarian-frontend
```

### Adatok Mentése

```bash
# Logok mentése
docker-compose logs > puphax-logs-$(date +%Y%m%d).txt

# Adatbázis backup (ha van)
docker-compose exec puphax-hungarian pg_dump > backup.sql
```

## 🔧 Hibaelhárítás

### Gyakori Problémák

1. **Port már használatban**
   ```bash
   # Ellenőrzés: mi fut a 8080 porton
   sudo netstat -tlnp | grep :8081
   
   # Megoldás: másik port használata
   # docker-compose.yml módosítása: "8081:8081"
   ```

2. **Memória hiba**
   ```bash
   # JVM memória növelése
   # docker-compose.yml JAVA_OPTS módosítása:
   - JAVA_OPTS=-Xms1g -Xmx2g
   ```

3. **PUPHAX kapcsolódási hiba**
   ```bash
   # Hálózat ellenőrzése
   docker-compose exec puphax-hungarian curl -I https://puphax.neak.gov.hu/PUPHAXWS
   
   # DNS ellenőrzése
   docker-compose exec puphax-hungarian nslookup puphax.neak.gov.hu
   ```

4. **Magyar karakterek nem jelennek meg**
   ```bash
   # Karakterkódolás ellenőrzése
   docker-compose exec puphax-hungarian locale
   
   # Várható eredmény: LANG=hu_HU.UTF-8
   ```

### Log Szintek

```yaml
environment:
  # Részletes hibakeresés
  - LOGGING_LEVEL_COM_PUPHAX=TRACE
  - LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
  
  # Teljesítmény monitoring
  - LOGGING_LEVEL_COM_PUPHAX_UTIL_LOGGINGUTILS=INFO
```

## 📊 Monitoring és Teljesítmény

### Metrikák Elérése

```bash
# Alapvető metrikák
curl http://localhost:8081/actuator/health

# Részletes információk
curl http://localhost:8081/actuator/info

# JVM metrikák
curl http://localhost:8081/actuator/metrics
```

### Prometheus Integráció

```yaml
# docker-compose.yml kiegészítés
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
```

## 🚀 Produkciós Telepítés

### SSL/HTTPS Beállítás

```yaml
# Nginx proxy-val
services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl/certs
```

### Backup Stratégia

```bash
# Automatikus backup script
#!/bin/bash
docker-compose exec puphax-hungarian pg_dump > backup-$(date +%Y%m%d-%H%M%S).sql
docker-compose logs > logs-$(date +%Y%m%d-%H%M%S).txt
```

### Frissítési Folyamat

```bash
# 1. Új verzió letöltése
git pull origin main

# 2. Alkalmazás leállítása
docker-compose down

# 3. Új image építése
docker-compose build --no-cache

# 4. Alkalmazás indítása
docker-compose up -d

# 5. Egészség ellenőrzés
curl -f http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

## 📞 Támogatás

### Hasznos Parancsok Összefoglalása

```bash
# Gyors állapot ellenőrzés
./docker-deploy.sh --clean  # Teljes újratelepítés

# Alapvető műveletek
docker-compose up -d         # Indítás
docker-compose down          # Leállítás
docker-compose restart       # Újraindítás
docker-compose logs -f       # Logok követése

# Hibaelhárítás
docker-compose exec puphax-hungarian bash  # Konténerbe belépés
docker system df                            # Disk használat
docker system prune                         # Cleanup

# Monitoring
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg  # Egészség
docker stats puphax-hungarian-frontend                  # Erőforrások
```

### Kapcsolat

- **Frontend URL**: http://localhost:8081
- **API Docs**: http://localhost:8081/swagger-ui.html
- **Health Check**: http://localhost:8081/api/v1/gyogyszerek/egeszseg

---

**Megjegyzés**: Ez a Docker setup teljes mértékben támogatja a magyar karakterkódolást és a NEAK PUPHAX webszolgáltatással való integrációt. Az alkalmazás production-ready és skálázható környezetben is futtatható.