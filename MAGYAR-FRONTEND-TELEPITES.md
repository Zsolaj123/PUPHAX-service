# PUPHAX Magyar Frontend Telepítési és Tesztutasítások

## Áttekintés

A PUPHAX Magyar Frontend egy teljes körű gyógyszer keresési alkalmazás magyar nyelvre lokalizálva, amely a NEAK PUPHAX webszolgáltatással integrálódik. Az alkalmazás a következő komponenseket tartalmazza:

### Frontend Komponensek

1. **index.html** - Magyar nyelvű HTML felület
   - Teljes UTF-8 karakterkódolás támogatás
   - Magyar nyelvi elemek és címkék
   - Responsive design mobil eszközökre
   - Akadálymentesítési funkciók

2. **puphax-style.css** - Magyar stíluslap
   - Modern, orvosi témájú design
   - Magyar karakterkészlet optimalizálás
   - Animációk és átmenetek
   - Reszponzív elrendezés

3. **puphax-frontend.js** - Magyar JavaScript alkalmazás
   - Magyar API végpontok integráció
   - Valós idejű form validáció magyar hibaüzenetekkel
   - Magyar keresési és eredménykezelő logika
   - UTF-8 karakterek helyes kezelése

### Backend Komponensek

1. **GyogyszerController.java** - Magyar REST API vezérlő
   - `/api/v1/gyogyszerek/kereses` - Gyógyszer keresés
   - `/api/v1/gyogyszerek/egeszseg` - Rendszer állapot
   - Magyar paraméter nevek és validáció

2. **GyogyszerKeresesiValasz.java** - Magyar DTO válaszok
   - Magyar field nevek
   - Hungarian response structure
   - Pagination support

## Előfeltételek

### Java 17 Telepítése
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# macOS
brew install openjdk@17

# Windows
# Java 17 letöltése az Oracle vagy OpenJDK oldaláról
```

### Maven Telepítése
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo yum install maven

# macOS
brew install maven

# Windows
# Maven letöltése és PATH környezeti változó beállítása
```

## Telepítés és Futtatás

### 1. Alkalmazás Építése

```bash
# Projekt könyvtárba lépés
cd /home/zsine/PUPHAX-service

# Maven függőségek letöltése és alkalmazás építése
mvn clean package -DskipTests

# Vagy tesztelésekkel együtt
mvn clean package
```

### 2. Alkalmazás Indítása

```bash
# Spring Boot alkalmazás indítása
java -jar target/puphax-rest-api-1.0.0.jar

# Vagy Maven-nel közvetlenül
mvn spring-boot:run
```

### 3. Magyar Frontend Elérése

Miután az alkalmazás elindult (alapértelmezetten port 8080):

- **Főoldal**: http://localhost:8080
- **API Dokumentáció**: http://localhost:8080/swagger-ui.html
- **Egészség állapot**: http://localhost:8080/api/v1/gyogyszerek/egeszseg

## Tesztelési Útmutató

### 1. Frontend Tesztelés

#### Alapvető Keresési Tesztek:
```javascript
// Browser Developer Console-ban futtatható tesztek

// 1. Egyszerű gyógyszer keresés
tesztKeresések.aspirin();

// 2. Gyártó szűrővel
tesztKeresések.richter();

// 3. ATC kóddal
tesztKeresések.atcKod();

// 4. Teljes keresés minden paraméterrel
tesztKeresések.teljesKereses();
```

#### Form Validáció Tesztelése:
1. **Üres keresés**: Próbálja meg elküldeni üres keresési mezővel
2. **ATC kód formátum**: Írjon be hibás ATC kódot (pl. "ABC123")
3. **Magyar karakterek**: Tesztelje magyar karaktereket (á, é, í, ó, ű, ő, ü, ű)

### 2. API Végpontok Tesztelése

#### cURL parancsokkal:
```bash
# 1. Egészség állapot ellenőrzés
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/egeszseg" \
     -H "Accept: application/json"

# 2. Egyszerű gyógyszer keresés
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin" \
     -H "Accept: application/json"

# 3. Szűrt keresés gyártóval
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin&gyarto=Bayer" \
     -H "Accept: application/json"

# 4. ATC kóddal történő keresés
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin&atc_kod=N02BA01" \
     -H "Accept: application/json"

# 5. Lapozásos keresés
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=paracetamol&oldal=1&meret=10" \
     -H "Accept: application/json"
```

### 3. PUPHAX Integráció Tesztelése

#### Valós PUPHAX adatok tesztelése:
```bash
# Magyar gyógyszerek keresése
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=algopyrin" \
     -H "Accept: application/json"

curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=kalmopyrin" \
     -H "Accept: application/json"

curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin" \
     -H "Accept: application/json"
```

## Karakterkódolás Tesztelése

### Magyar Karakterek Tesztelése:
1. **Frontend-en keresztül**:
   - Írjon be magyar karaktereket a keresési mezőbe: "gyógyszer", "paracetamol"
   - Ellenőrizze, hogy a válaszban helyesen jelennek meg a magyar karakterek

2. **API-n keresztül**:
```bash
# URL encoding magyar karakterekkel
curl -X GET "http://localhost:8080/api/v1/gyogyszerek/kereses?keresett_kifejezés=gy%C3%B3gyszer" \
     -H "Accept: application/json; charset=UTF-8"
```

## Hibaelhárítás

### Gyakori Problémák:

1. **"Connection refused" hiba**:
   - Ellenőrizze, hogy az alkalmazás fut-e a 8080 porton
   - Nézze meg a konzol log-okat hibákért

2. **"PUPHAX szolgáltatás nem elérhető"**:
   - Ellenőrizze az internet kapcsolatot
   - A PUPHAX szerver (https://puphax.neak.gov.hu/PUPHAXWS) elérhető-e

3. **Magyar karakterek nem jelennek meg helyesen**:
   - Ellenőrizze a browser charset beállítását (UTF-8)
   - Nézze meg a HTTP response headereket

4. **Form validáció nem működik**:
   - Ellenőrizze a browser JavaScript console-t hibákért
   - Győződjön meg róla, hogy a puphax-frontend.js betöltődött

### Log Fájlok Ellenőrzése:
```bash
# Alkalmazás log fájl ellenőrzése
tail -f logs/puphax-service.log

# Szűrés csak hibákra
grep "ERROR" logs/puphax-service.log

# Keresési műveletek követése
grep "kereses" logs/puphax-service.log
```

## Teljesítmény Optimalizálás

### Gyorsítótár Beállítások:
- Alapértelmezett cache: 10 perc
- Maximum 1000 keresési eredmény tárolása
- Cache hit率 monitorozása a log fájlokban

### Javasolt Produkciós Beállítások:
```yaml
# application-prod.yml
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=30m

puphax:
  soap:
    connect-timeout: 15000
    request-timeout: 30000
    max-connections: 50
```

## Támogatás és Fejlesztés

### További fejlesztési lehetőségek:
1. **Autocompletare** - Gyógyszer nevek automatikus kiegészítése
2. **Kedvencek** - Felhasználói kedvenc gyógyszerek mentése
3. **Export funkció** - Eredmények exportálása CSV/PDF formátumban
4. **Részletes gyógyszer információ** - Kattintható gyógyszer kártyák
5. **Mobil app** - React Native vagy Flutter mobile app

### API bővítési lehetőségek:
1. **Gyógyszer részletek** - `/api/v1/gyogyszerek/{id}/reszletek`
2. **Kategória keresés** - `/api/v1/gyogyszerek/kategoriak`
3. **Statisztikák** - `/api/v1/gyogyszerek/statisztikak`

---

**Megjegyzés**: Ez a frontend teljes mértékben kompatibilis a valós NEAK PUPHAX webszolgáltatással és a magyar egészségügyi környezettel. Az alkalmazás UTF-8 karakterkódolást használ és támogatja a magyar nyelvű kereséseket és válaszokat.