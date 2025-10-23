# PUPHAX REST API Szolgáltatás / Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/Licenc-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Kész-blue.svg)](https://www.docker.com/)

> 🇭🇺 **Magyar dokumentáció lent** | 🇬🇧 **[English version below](#english-version)**

---

# 🇭🇺 Magyar Dokumentáció

## Tartalomjegyzék

- [Áttekintés](#áttekintés)
- [Főbb Jellemzők](#főbb-jellemzők)
- [Gyors Kezdés](#gyors-kezdés)
- [API Dokumentáció](#api-dokumentáció)
- [CSV Tartalék Rendszer](#csv-tartalék-rendszer)
- [Integráció Meglévő Projektbe](#integráció-meglévő-projektbe)
- [Hozzájárulás a Projekthez](#hozzájárulás-a-projekthez)
- [Konfiguráció](#konfiguráció)
- [Fejlesztés](#fejlesztés)
- [Telepítés](#telepítés)
- [Hibaelhárítás](#hibaelhárítás)

## Áttekintés

A PUPHAX REST API Szolgáltatás egy modern REST API, amely hozzáférést biztosít a NEAK (Nemzeti Egészségbiztosítási Alapkezelő) PUPHAX gyógyszerészeti adatbázisához. Ez a szolgáltatás hidat képez a modern alkalmazások és az elavult SOAP-alapú PUPHAX webszolgáltatás között, beépített CSV tartalék rendszerrel a magas rendelkezésre állásért.

### Miért használd?

- ✅ **Egyszerű REST API** a bonyolult SOAP helyett
- ✅ **Mindig elérhető** - működik akkor is, ha a NEAK szervere nem elérhető
- ✅ **Gyors** - 40x gyorsabb keresés a CSV tartalékkal (<50ms vs ~2s)
- ✅ **Kétnyelvű** - magyar és angol végpontok
- ✅ **Termelésre kész** - Docker, monitoring, health check-ek
- ✅ **Jól dokumentált** - teljes magyar és angol dokumentáció

## Főbb Jellemzők

### 🚀 Modern Technológiák

- **Spring Boot 3.5.6** - Java 17 alapokon
- **REST API** - Tiszta JSON formátum
- **Docker támogatás** - Konténerizált telepítés
- **Resilience4j** - Hibatűrés (Circuit Breaker, Retry)
- **Caffeine Cache** - Intelligens gyorsítótár
- **OpenAPI/Swagger** - Automatikus API dokumentáció

### 💾 CSV Tartalék Rendszer

A szolgáltatás tartalmazza a NEAK hivatalos történeti adatait (2007-2023):

- **43,930 aktuális gyógyszer** - csak az érvényes termékek
- **8,905 brand név** - gyártók és márkák
- **6,828 ATC kód** - gyógyszer klasszifikáció
- **2,314 cég** - gyártók adatai

**Automatikus átállás amikor:**
- A NEAK szervere nem elérhető
- A NEAK HTML hibaüzenetet küld SOAP helyett
- A válasz feldolgozása sikertelen

### 🔍 Keresési Képességek

- **Keresés gyógyszernév alapján**: pl. "Aspirin"
- **Keresés hatóanyag alapján**: pl. "acetilszalicilsav"
- **Kombinált keresés**: mindkét szempontot egyszerre vizsgálja
- **Gyors válasz**: <50ms átlagos válaszidő

## Gyors Kezdés

### Előfeltételek

- **Java 17** vagy újabb
- **Maven 3.9+** vagy használd a mellékelt wrapper-t
- **Docker** (opcionális, konténerizált telepítéshez)

### Docker-rel (Ajánlott)

```bash
# Repository klónozása
git clone https://github.com/felhasznalonev/puphax-service.git
cd puphax-service

# Szolgáltatás építése és indítása
docker-compose up -d

# Állapot ellenőrzése
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

A szolgáltatás elérhető lesz: `http://localhost:8081`

### Helyi Futtatás (Fejlesztéshez)

```bash
# Projekt build-elése
./mvnw clean package

# Szolgáltatás indítása
java -jar target/puphax-rest-api-1.0.0.jar

# Vagy közvetlenül Maven-nel
./mvnw spring-boot:run
```

### Első API Hívás

```bash
# Gyógyszer keresése (magyar végpont)
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin&meret=5"

# Gyógyszer keresése hatóanyag alapján
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=acetilszalicilsav&meret=5"

# Egészség ellenőrzés
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

## API Dokumentáció

### Swagger UI

Az API teljes dokumentációja elérhető:
```
http://localhost:8081/swagger-ui.html
```

### Magyar Végpontok

#### Gyógyszer Keresés

```http
GET /api/v1/gyogyszerek/kereses
```

**Query paraméterek:**
- `keresett_kifejezés` (kötelező): Keresett szöveg (gyógyszernév vagy hatóanyag)
- `gyarto` (opcionális): Szűrés gyártó szerint
- `atc_kod` (opcionális): Szűrés ATC kód szerint
- `oldal` (opcionális, alapértelmezett: 0): Oldal száma
- `meret` (opcionális, alapértelmezett: 10): Találatok száma oldalanként
- `rendezes` (opcionális): Rendezési mező (nev, gyarto, atcKod)
- `irany` (opcionális): Rendezési irány (ASC, DESC)

**Példa válasz:**
```json
{
  "gyogyszerek": [
    {
      "id": "55827054",
      "nev": "ASPIRIN 500 MG TABLETTA",
      "gyarto": "Bayer",
      "atcKod": "N02BA01",
      "hatoanyagok": ["acetilszalicilsav"],
      "venyKoteles": false,
      "tamogatott": true,
      "statusz": "AKTIV"
    }
  ],
  "lapozas": {
    "jelenlegiOldal": 0,
    "oldalMeret": 10,
    "osszesElem": 25,
    "osszesOldal": 3
  },
  "keresesiAdatok": {
    "keresettKifejezes": "aspirin",
    "valaszIdoMs": 45,
    "gyorsitotarTalalat": false
  }
}
```

#### További Végpontok

```http
# Gyógyszer részletes adatai
GET /api/v1/gyogyszerek/{id}

# Gyors egészség ellenőrzés
GET /api/v1/gyogyszerek/egeszseg/gyors

# Részletes egészség ellenőrzés
GET /api/v1/gyogyszerek/egeszseg
```

### Angol Végpontok

Az angol végpontok ugyanazokat a funkciókat nyújtják angol névvel:

```http
GET /api/v1/drugs/search?term=aspirin&size=10
GET /api/v1/drugs/{id}
GET /api/v1/drugs/health
```

## CSV Tartalék Rendszer

### Működés

A szolgáltatás automatikusan átáll a helyi CSV adatbázisra, ha:

1. **NEAK szerver nem elérhető** - hálózati hiba, timeout
2. **HTML hibaüzenet** - NEAK HTML-t küld SOAP válasz helyett
3. **Feldolgozási hiba** - a SOAP válasz nem dolgozható fel
4. **Üres eredmény** - nem található termék ID a válaszban

### Felhasználói Élmény

Amikor a CSV tartalék aktív:
- **Frontend értesítés**: Lila banner jelenik meg "Helyi adatbázis használatban" üzenettel
- **Gyors válasz**: <50ms válaszidő (40x gyorsabb mint a SOAP)
- **Teljes funkcionalitás**: Keresés név és hatóanyag szerint is működik

### Adatfrissítés

A CSV adatok frissítése:

```bash
# 1. Töltsd le a legfrissebb adatokat a NEAK weboldaláról
# https://www.neak.gov.hu/.../puphax

# 2. Szűrd az aktuális termékekre (utolsó 2 év)
python3 scripts/filter-termek.py

# 3. Másold az új fájlokat
cp filtered/*.csv src/main/resources/puphax-data/

# 4. Újraépítés és telepítés
./mvnw clean package
docker-compose build
docker-compose up -d
```

## Integráció Meglévő Projektbe

### 1. Mint Microservice (Ajánlott)

A legegyszerűbb módszer: futtasd a PUPHAX szolgáltatást külön konténerként.

#### Docker Compose-zal

```yaml
# A saját docker-compose.yml fájlodban
version: '3.8'

services:
  # A saját alkalmazásod
  my-backend:
    image: my-app:latest
    environment:
      PUPHAX_API_URL: http://puphax-service:8081
    depends_on:
      - puphax-service

  # PUPHAX szolgáltatás
  puphax-service:
    image: puphax-hungarian:latest
    ports:
      - "8081:8081"
    environment:
      PUPHAX_USERNAME: ${PUPHAX_USERNAME}
      PUPHAX_PASSWORD: ${PUPHAX_PASSWORD}
```

#### Használat a Backend-edben (Java/Spring Boot)

```java
@Service
public class PharmacyService {
    
    @Value("${puphax.api.url}")
    private String puphaxApiUrl;
    
    private final RestTemplate restTemplate;
    
    public List<Drug> searchDrugs(String searchTerm) {
        String url = puphaxApiUrl + "/api/v1/drugs/search?term=" + searchTerm;
        
        DrugSearchResponse response = restTemplate.getForObject(
            url, 
            DrugSearchResponse.class
        );
        
        return response.getDrugs();
    }
}
```

### 2. Közvetlen Integráció (Maven Modul)

Ha közvetlenül szeretnéd használni a kódot a projektedben:

#### 2.1. Klónozd a Repository-t

```bash
cd /path/to/your/project
mkdir -p modules
git clone https://github.com/felhasznalonev/puphax-service.git modules/puphax-service
```

#### 2.2. Add hozzá Maven Modulként

A projekt gyökér `pom.xml` fájljában:

```xml
<project>
    <packaging>pom</packaging>
    
    <modules>
        <module>your-existing-module</module>
        <module>modules/puphax-service</module>
    </modules>
</project>
```

#### 2.3. Add hozzá Függőségként

A saját modulodban (`your-module/pom.xml`):

```xml
<dependencies>
    <dependency>
        <groupId>com.puphax</groupId>
        <artifactId>puphax-rest-api</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### 2.4. Konfiguráld az Application Properties-t

`application.yml`:

```yaml
puphax:
  soap:
    endpoint-url: https://puphax.neak.gov.hu/PUPHAXWS
    username: ${PUPHAX_USERNAME}
    password: ${PUPHAX_PASSWORD}
  query:
    snapshot-date-offset-months: 1
    use-current-snapshot: true
```

#### 2.5. Használd a Szolgáltatásokat

```java
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private DrugService drugService;

    @GetMapping("/drugs/search")
    public ResponseEntity<DrugSearchResponse> searchDrugs(
        @RequestParam String term,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        
        DrugSearchResponse response = drugService.searchDrugs(
            term, null, null, page, size, "name", "ASC"
        );
        
        return ResponseEntity.ok(response);
    }
}
```

### 3. Frontend Integráció

#### 3.1. Vanilla JavaScript

```javascript
// Gyógyszer keresés funkció
async function searchDrugs(searchTerm) {
    try {
        const response = await fetch(
            `http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=${encodeURIComponent(searchTerm)}&meret=20`
        );
        
        if (!response.ok) {
            throw new Error('Keresés sikertelen');
        }
        
        const data = await response.json();
        return data.gyogyszerek;
        
    } catch (error) {
        console.error('Hiba a keresés során:', error);
        throw error;
    }
}

// Használat
searchDrugs('aspirin')
    .then(gyogyszerek => {
        console.log(`${gyogyszerek.length} gyógyszer találva`);
        gyogyszerek.forEach(gy => {
            console.log(`${gy.nev} - ${gy.atcKod}`);
        });
    })
    .catch(error => {
        alert('Hiba történt a keresés során');
    });
```

#### 3.2. React Hook

```javascript
import { useState, useEffect } from 'react';

// Custom hook gyógyszer kereséshez
function useDrugSearch(searchTerm) {
    const [drugs, setDrugs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!searchTerm || searchTerm.length < 3) {
            setDrugs([]);
            return;
        }

        setLoading(true);
        setError(null);

        const apiUrl = process.env.REACT_APP_PUPHAX_API || 'http://localhost:8081';
        
        fetch(`${apiUrl}/api/v1/drugs/search?term=${encodeURIComponent(searchTerm)}`)
            .then(res => {
                if (!res.ok) throw new Error('Keresés sikertelen');
                return res.json();
            })
            .then(data => {
                setDrugs(data.drugs || []);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, [searchTerm]);

    return { drugs, loading, error };
}

// Használat komponensben
function DrugSearchComponent() {
    const [searchTerm, setSearchTerm] = useState('');
    const { drugs, loading, error } = useDrugSearch(searchTerm);

    return (
        <div>
            <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Gyógyszernév vagy hatóanyag..."
            />
            
            {loading && <p>Keresés...</p>}
            {error && <p>Hiba: {error}</p>}
            
            <ul>
                {drugs.map(drug => (
                    <li key={drug.id}>
                        <strong>{drug.name}</strong> - {drug.atcCode}
                        <br />
                        Hatóanyag: {drug.activeIngredients.join(', ')}
                    </li>
                ))}
            </ul>
        </div>
    );
}
```

#### 3.3. Vue.js Composition API

```javascript
import { ref, watch } from 'vue';

export function useDrugSearch() {
    const searchTerm = ref('');
    const drugs = ref([]);
    const loading = ref(false);
    const error = ref(null);

    watch(searchTerm, async (newTerm) => {
        if (!newTerm || newTerm.length < 3) {
            drugs.value = [];
            return;
        }

        loading.value = true;
        error.value = null;

        try {
            const response = await fetch(
                `http://localhost:8081/api/v1/drugs/search?term=${encodeURIComponent(newTerm)}`
            );
            
            if (!response.ok) throw new Error('Keresés sikertelen');
            
            const data = await response.json();
            drugs.value = data.drugs || [];
            
        } catch (err) {
            error.value = err.message;
        } finally {
            loading.value = false;
        }
    });

    return {
        searchTerm,
        drugs,
        loading,
        error
    };
}
```

### 4. Példa: Teljes Backend Integráció

```java
@Configuration
public class PuphaxConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class DrugPharmacyService {
    
    private final RestTemplate restTemplate;
    private final String puphaxApiUrl;
    
    public DrugPharmacyService(
        RestTemplate restTemplate,
        @Value("${puphax.api.url:http://localhost:8081}") String puphaxApiUrl) {
        this.restTemplate = restTemplate;
        this.puphaxApiUrl = puphaxApiUrl;
    }
    
    public List<DrugDTO> searchDrugs(String searchTerm) {
        String url = String.format(
            "%s/api/v1/drugs/search?term=%s&size=50",
            puphaxApiUrl,
            URLEncoder.encode(searchTerm, StandardCharsets.UTF_8)
        );
        
        try {
            ResponseEntity<DrugSearchResponse> response = 
                restTemplate.getForEntity(url, DrugSearchResponse.class);
            
            return response.getBody().getDrugs();
            
        } catch (HttpClientErrorException e) {
            log.error("PUPHAX API hiba: {}", e.getMessage());
            throw new DrugSearchException("Keresés sikertelen", e);
        }
    }
    
    public DrugDTO getDrugDetails(String drugId) {
        String url = String.format("%s/api/v1/drugs/%s", puphaxApiUrl, drugId);
        
        try {
            return restTemplate.getForObject(url, DrugDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new DrugNotFoundException("Gyógyszer nem található: " + drugId);
        }
    }
    
    public boolean isServiceHealthy() {
        String url = puphaxApiUrl + "/api/v1/drugs/health";

        try {
            ResponseEntity<HealthResponse> response =
                restTemplate.getForEntity(url, HealthResponse.class);

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Hozzájárulás a Projekthez

Örömmel fogadunk minden hozzájárulást! Ez a rész segít eligazodni, hogyan járulhatsz hozzá a projekthez.

### Fork és Clone

```bash
# 1. Fork-old a repository-t a GitHub webes felületén (jobb felső sarok)

# 2. Klónozd a saját fork-olt repository-det
git clone https://github.com/sajat-felhasznalonev/puphax-service.git
cd puphax-service

# 3. Add hozzá az eredeti repository-t upstream-ként
git remote add upstream https://github.com/eredeti-tulajdonos/puphax-service.git

# 4. Ellenőrizd a remote-okat
git remote -v
```

### Branch Létrehozása

Mindig hozz létre új branch-et a változtatásaidhoz:

```bash
# Frissítsd a main branch-et
git checkout main
git pull upstream main

# Hozz létre új feature branch-et
git checkout -b feature/gyogyszer-szures-bovites

# Vagy bugfix branch-et
git checkout -b fix/cache-timeout-hiba
```

### Branch Elnevezési Konvenciók

- `feature/nev` - új funkciók (pl. `feature/fuzzy-search`)
- `fix/nev` - hibajavítások (pl. `fix/csv-encoding`)
- `docs/nev` - dokumentáció frissítések (pl. `docs/api-examples`)
- `refactor/nev` - kód újrastrukturálás (pl. `refactor/service-layer`)
- `test/nev` - tesztek hozzáadása (pl. `test/integration-tests`)

### Fejlesztés és Commit

```bash
# Végezd el a változtatásokat

# Ellenőrizd a változásokat
git status
git diff

# Add hozzá a fájlokat
git add .

# Commit üzenet írása
git commit -m "feat: fuzzy search hozzáadása gyógyszer kereséshez

- Implementált Levenshtein távolság számítás
- Javított keresési pontosság elírások esetén
- Hozzáadott konfiguráció a tolerancia beállításához"
```

### Commit Üzenet Formátum

Használj strukturált commit üzeneteket:

```
<típus>: <rövid leírás>

<részletes leírás - opcionális>

<footer - opcionális>
```

**Típusok:**
- `feat`: Új funkció
- `fix`: Hibajavítás
- `docs`: Dokumentáció
- `style`: Formázás (nincs kód változás)
- `refactor`: Kód újrastrukturálás
- `test`: Tesztek hozzáadása
- `chore`: Build vagy config változások

**Példák:**
```
feat: ATC kód alapú szűrés hozzáadása

fix: CSV encoding probléma magyar karakterekkel

docs: REST API példák frissítése README-ben

refactor: Cache konfiguráció külön osztályba szervezése
```

### Tesztelés

Mielőtt pull request-et nyitasz, győződj meg róla, hogy minden teszt lefut:

```bash
# Unit tesztek futtatása
./mvnw test

# Integrációs tesztek
./mvnw verify

# Docker build tesztelése
docker-compose build
docker-compose up -d

# Manuális API tesztelés
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin"

# Docker leállítása
docker-compose down
```

### Pull Request Nyitása

```bash
# Push-old a branch-et a saját fork-odba
git push origin feature/gyogyszer-szures-bovites

# Menj a GitHub webes felületére
# Kattints a "Compare & pull request" gombra
```

**Pull Request Sablon:**

```markdown
## Leírás
Rövid összefoglaló a változtatásokról.

## Változtatások típusa
- [ ] Új funkció (feature)
- [ ] Hibajavítás (fix)
- [ ] Dokumentáció frissítés (docs)
- [ ] Kód újrastrukturálás (refactor)
- [ ] Tesztek (test)

## Tesztelés
Hogyan teszteltél? Milyen teszteket adtál hozzá?

- [ ] Unit tesztek írva/frissítve
- [ ] Integrációs tesztek lefuttatva
- [ ] Manuális tesztelés elvégezve
- [ ] Docker build sikeres

## Checklist
- [ ] A kód követi a projekt code style-ját
- [ ] Self-review elvégezve
- [ ] Kommentek hozzáadva ahol szükséges
- [ ] Dokumentáció frissítve
- [ ] Minden teszt lefut sikeresen
- [ ] Nincs új warning
```

### Kód Stílus Útmutató

#### Java Konvenciók

```java
// 1. Package nevek: kisbetűs, ponttal elválasztva
package com.puphax.service;

// 2. Class nevek: PascalCase
public class DrugSearchService {

    // 3. Konstansok: UPPER_SNAKE_CASE
    private static final int MAX_SEARCH_RESULTS = 100;

    // 4. Field nevek: camelCase
    private final RestTemplate restTemplate;

    // 5. Method nevek: camelCase, action verb-bel kezdve
    public List<Drug> searchDrugsByName(String name) {
        // ...
    }

    // 6. Használj Javadoc-ot public API-khoz
    /**
     * Searches for drugs by name or active ingredient.
     *
     * @param searchTerm the search term
     * @param pageSize maximum number of results
     * @return list of matching drugs
     */
    public List<Drug> search(String searchTerm, int pageSize) {
        // ...
    }
}
```

#### Logging Best Practices

```java
// SLF4J használata
private static final Logger logger = LoggerFactory.getLogger(DrugService.class);

// Megfelelő log szintek
logger.debug("Searching for drug: {}", searchTerm);
logger.info("CSV fallback service initialized: {} products", count);
logger.warn("NEAK service unavailable, using CSV fallback");
logger.error("Failed to parse drug data: {}", e.getMessage(), e);
```

#### Exception Handling

```java
// Specifikus exception-ök használata
try {
    return drugService.search(term);
} catch (HttpClientErrorException.NotFound e) {
    throw new DrugNotFoundException("Drug not found: " + id);
} catch (HttpServerErrorException e) {
    logger.error("NEAK server error: {}", e.getMessage());
    return fallbackService.search(term);
} catch (Exception e) {
    logger.error("Unexpected error during drug search", e);
    throw new DrugSearchException("Search failed", e);
}
```

### Új Funkció Hozzáadása

Ha új funkciót szeretnél hozzáadni:

1. **Nyiss issue-t** - Írd le a funkciót, hogy megbeszélhessük
2. **Várj feedback-re** - A maintainerek reagálnak
3. **Implementálj** - Kövessd a fenti guide-okat
4. **Tesztelj** - Írj unit és integrációs teszteket
5. **Dokumentálj** - Frissítsd a README-t és Javadoc-ot
6. **Pull Request** - Nyiss PR-t részletes leírással

### Hibajavítás Flow

```bash
# 1. Reprodukáld a hibát
# 2. Írj egy failing tesztet
# 3. Javítsd a hibát
# 4. Ellenőrizd, hogy a teszt zöld
# 5. Push és PR
```

### Kérdések?

- **GitHub Issues**: Technikai kérdések, hibák
- **GitHub Discussions**: Általános megbeszélések, feature ötletek
- **Pull Requests**: Kód review, implementációs kérdések

---

## Konfiguráció

### Környezeti Változók

A szolgáltatás testreszabható környezeti változókkal:

```bash
# NEAK PUPHAX kapcsolat
export PUPHAX_ENDPOINT_URL=https://puphax.neak.gov.hu/PUPHAXWS
export PUPHAX_USERNAME=your_username
export PUPHAX_PASSWORD=your_password

# Szerver konfiguráció
export SERVER_PORT=8081
export SPRING_PROFILES_ACTIVE=production

# Logging
export LOGGING_LEVEL_COM_PUPHAX=DEBUG
export LOGGING_FILE_NAME=/var/log/puphax/app.log

# CSV fallback beállítások
export PUPHAX_CSV_ENABLED=true
export PUPHAX_CSV_AUTO_FALLBACK=true
```

### Application.yml Teljes Konfiguráció

```yaml
server:
  port: 8081
  compression:
    enabled: true
    mime-types: application/json,application/xml

spring:
  application:
    name: puphax-rest-api

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30m

puphax:
  soap:
    endpoint-url: ${PUPHAX_ENDPOINT_URL:https://puphax.neak.gov.hu/PUPHAXWS}
    username: ${PUPHAX_USERNAME:PUPHAX}
    password: ${PUPHAX_PASSWORD:puphax}
    connection-timeout: 30000
    read-timeout: 60000

  query:
    # NEAK szerver terhelés csökkentése
    # Csak az elmúlt 1 hónap adatait kérdezi le (nem 15 év)
    snapshot-date-offset-months: 1
    use-current-snapshot: true

  csv:
    enabled: true
    auto-fallback: true
    data-path: classpath:puphax-data/

  cache:
    drug-search-ttl: 30m
    product-details-ttl: 2h
    support-data-ttl: 4h
    companies-ttl: 24h

resilience4j:
  circuitbreaker:
    instances:
      puphaxService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60000
        permittedNumberOfCallsInHalfOpenState: 3

  retry:
    instances:
      puphaxService:
        maxAttempts: 3
        waitDuration: 1000
        exponentialBackoffMultiplier: 2

  timelimiter:
    instances:
      puphaxService:
        timeoutDuration: 15s

logging:
  level:
    com.puphax: INFO
    org.springframework.web: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/puphax-service.log
    max-size: 10MB
    max-history: 30
```

### Docker Környezeti Változók

`docker-compose.yml`:

```yaml
version: '3.8'

services:
  puphax-service:
    image: puphax-hungarian:latest
    ports:
      - "8081:8081"
    environment:
      # NEAK Kapcsolat
      PUPHAX_ENDPOINT_URL: https://puphax.neak.gov.hu/PUPHAXWS
      PUPHAX_USERNAME: ${PUPHAX_USERNAME}
      PUPHAX_PASSWORD: ${PUPHAX_PASSWORD}

      # Szerver
      SERVER_PORT: 8081
      SPRING_PROFILES_ACTIVE: production

      # Cache
      PUPHAX_CACHE_DRUG_SEARCH_TTL: 30m

      # CSV Fallback
      PUPHAX_CSV_ENABLED: true
      PUPHAX_CSV_AUTO_FALLBACK: true

      # Logging
      LOGGING_LEVEL_COM_PUPHAX: INFO

    volumes:
      - ./logs:/app/logs

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

    restart: unless-stopped
```

---

## Fejlesztés

### Helyi Fejlesztői Környezet

```bash
# Repository klónozása
git clone https://github.com/felhasznalonev/puphax-service.git
cd puphax-service

# Java verzió ellenőrzése
java -version  # Kell: Java 17+

# Build dependencies letöltése
./mvnw dependency:resolve

# Alkalmazás futtatása development módban
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Vagy JAR build és futtatás
./mvnw clean package
java -jar target/puphax-rest-api-1.0.0.jar
```

### IDE Setup

#### IntelliJ IDEA

```bash
# 1. Open Project
File → Open → válaszd ki a puphax-service mappát

# 2. Maven projekt automatikusan importálódik

# 3. Java 17 SDK beállítása
File → Project Structure → Project SDK → Java 17

# 4. Alkalmazás futtatása
Run → Edit Configurations → + → Spring Boot
Main class: com.puphax.PuphaxRestApiApplication

# 5. Environment variables hozzáadása (opcionális)
PUPHAX_USERNAME=your_user;PUPHAX_PASSWORD=your_pass
```

#### VS Code

```bash
# 1. Telepítsd az Extension Pack for Java-t
# 2. Nyisd meg a projektet
# 3. A .vscode/launch.json automatikusan generálódik

# 4. Debug futtatásához: F5
```

### Hot Reload Development

Spring Boot DevTools használata:

```xml
<!-- pom.xml-ben már bent van -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Most a kód változtatások után automatikusan újraindul az alkalmazás.

### Tesztelés

```bash
# Összes teszt futtatása
./mvnw test

# Csak egy teszt osztály
./mvnw test -Dtest=DrugServiceTest

# Csak egy teszt metódus
./mvnw test -Dtest=DrugServiceTest#testSearchDrugs

# Integrációs tesztek
./mvnw verify

# Tesztek átugrása build során (NEM ajánlott!)
./mvnw package -DskipTests
```

### Új Teszt Írása

```java
@SpringBootTest
class DrugSearchServiceTest {

    @Autowired
    private DrugService drugService;

    @MockBean
    private PuphaxCsvFallbackService csvFallbackService;

    @Test
    void testSearchDrugsByName() {
        // Given
        String searchTerm = "aspirin";

        // When
        List<Drug> results = drugService.searchDrugs(searchTerm, 0, 10);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream()
            .anyMatch(drug -> drug.getName().toLowerCase().contains("aspirin")));
    }

    @Test
    void testCsvFallbackWhenNeakDown() {
        // Given
        when(csvFallbackService.isInitialized()).thenReturn(true);
        when(csvFallbackService.searchDrugs("aspirin"))
            .thenReturn(createMockXmlResponse());

        // When - simulate NEAK failure
        // Then - should use CSV fallback
        // ...
    }
}
```

---

## Telepítés

### Production Deployment Docker-rel

```bash
# 1. Klónozd a repository-t
git clone https://github.com/felhasznalonev/puphax-service.git
cd puphax-service

# 2. Hozz létre .env fájlt
cat > .env << EOF
PUPHAX_USERNAME=your_production_username
PUPHAX_PASSWORD=your_production_password
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=production
LOGGING_LEVEL_COM_PUPHAX=INFO
EOF

# 3. Build és indítás
docker-compose up -d

# 4. Ellenőrzés
docker-compose logs -f
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors

# 5. Auto-restart beállítása
# A docker-compose.yml már tartalmazza: restart: unless-stopped
```

### Production Deployment JAR-ral

```bash
# 1. Build production JAR
./mvnw clean package -Pprod

# 2. Futtasd a JAR-t
java -jar target/puphax-rest-api-1.0.0.jar \
  --server.port=8081 \
  --spring.profiles.active=production \
  --puphax.soap.username=your_user \
  --puphax.soap.password=your_pass

# 3. Systemd service létrehozása (Linux)
sudo cat > /etc/systemd/system/puphax-service.service << EOF
[Unit]
Description=PUPHAX REST API Service
After=network.target

[Service]
Type=simple
User=puphax
WorkingDirectory=/opt/puphax-service
ExecStart=/usr/bin/java -jar /opt/puphax-service/puphax-rest-api-1.0.0.jar
Restart=on-failure
RestartSec=10

Environment="SERVER_PORT=8081"
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="PUPHAX_USERNAME=your_user"
Environment="PUPHAX_PASSWORD=your_pass"

[Install]
WantedBy=multi-user.target
EOF

# 4. Indítás
sudo systemctl daemon-reload
sudo systemctl enable puphax-service
sudo systemctl start puphax-service

# 5. Státusz ellenőrzés
sudo systemctl status puphax-service
```

### Kubernetes Deployment

```yaml
# puphax-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: puphax-service
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
      - name: puphax
        image: puphax-hungarian:latest
        ports:
        - containerPort: 8081
        env:
        - name: PUPHAX_USERNAME
          valueFrom:
            secretKeyRef:
              name: puphax-credentials
              key: username
        - name: PUPHAX_PASSWORD
          valueFrom:
            secretKeyRef:
              name: puphax-credentials
              key: password
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        livenessProbe:
          httpGet:
            path: /api/v1/gyogyszerek/egeszseg/gyors
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/gyogyszerek/egeszseg
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: puphax-service
spec:
  selector:
    app: puphax-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8081
  type: LoadBalancer
```

Telepítés:

```bash
# Secret létrehozása
kubectl create secret generic puphax-credentials \
  --from-literal=username=your_user \
  --from-literal=password=your_pass

# Deployment
kubectl apply -f puphax-deployment.yaml

# Státusz
kubectl get pods
kubectl get services
kubectl logs -f deployment/puphax-service
```

### Nginx Reverse Proxy

```nginx
# /etc/nginx/sites-available/puphax-api
upstream puphax_backend {
    server localhost:8081;
}

server {
    listen 80;
    server_name api.yourdomain.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;

    location /api/ {
        proxy_pass http://puphax_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /swagger-ui/ {
        proxy_pass http://puphax_backend;
    }

    location /v3/api-docs/ {
        proxy_pass http://puphax_backend;
    }
}
```

Aktiválás:

```bash
sudo ln -s /etc/nginx/sites-available/puphax-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## Hibaelhárítás

### Docker Build Hibák

#### Maven Wrapper JAR hiányzik

**Hiba:**
```
Error: Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain
```

**Megoldás:**
```bash
curl -o .mvn/wrapper/maven-wrapper.jar \
  https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
```

#### WSDL nem található

**Hiba:**
```
Failed to read WSDL from URL
```

**Megoldás:**
A szolgáltatás most automatikusan fallback-el a classpath WSDL-re ha a NEAK szerver nem elérhető. Ellenőrizd hogy a `src/main/resources/wsdl/PUPHAXWS.wsdl` létezik.

### Runtime Hibák

#### "Kapcsolódási Hiba" üzenet

**Ok:** NEAK PUPHAX szerver nem elérhető vagy HTML hibát küld.

**Megoldás:**
A szolgáltatás automatikusan átáll CSV fallback-re. Ellenőrizd a logokat:

```bash
# Docker
docker-compose logs puphax-service | grep -i "csv fallback"

# Helyi
tail -f logs/puphax-service.log | grep -i "csv"
```

Látnod kell:
```
CSV fallback service initialized successfully: 43930 products
Using CSV fallback service for search term: aspirin
```

#### CSV adatok nem töltődnek be

**Hiba:**
```
Failed to initialize CSV fallback service
```

**Ellenőrzés:**
```bash
# Ellenőrizd hogy a CSV fájlok léteznek
ls -lh src/main/resources/puphax-data/
# Kell látni: TERMEK.csv, BRAND.csv, ATCKONYV.csv, CEGEK.csv

# Ellenőrizd az encoding-ot
file src/main/resources/puphax-data/TERMEK.csv
# Kell: UTF-8 Unicode text
```

**Megoldás:**
Ha a fájlok hibásak, futtasd újra a filter szkriptet:
```bash
python3 scripts/filter-termek.py
```

#### Memory OutOfMemory hiba

**Hiba:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Megoldás Docker-ben:**
```yaml
# docker-compose.yml
services:
  puphax-service:
    environment:
      JAVA_OPTS: "-Xmx1g -Xms512m"
```

**Megoldás JAR futtatásban:**
```bash
java -Xmx1g -Xms512m -jar target/puphax-rest-api-1.0.0.jar
```

### NEAK Kapcsolat Problémák

#### Lassú keresés (>2s)

**Ok:** NEAK PUPHAX szerver lassú, 15 év adatot dolgoz fel.

**Megoldás:**
Ellenőrizd hogy a snapshot date konfiguráció aktív:

```yaml
# application.yml
puphax:
  query:
    snapshot-date-offset-months: 1  # Csak 1 hónap, nem 15 év
    use-current-snapshot: true
```

Logban látnod kell:
```
Making direct HTTP call to PUPHAX for search term: aspirin (snapshot date: 2025-09-23)
```

#### NEAK visszautasítja a kérést

**Hiba:**
```
Kérését később tudjuk kiszolgálni
```

**Ok:** NEAK szerver túlterhelt vagy védelem alatt.

**Megoldás:**
A CSV fallback automatikusan aktiválódik. Ha állandóan ezt látod:

1. Csökkentsd a cache TTL-t `application.yml`-ben
2. Használd a CSV fallback-et elsődleges forrásként development során
3. Implementálj rate limiting-et:

```java
@RateLimiter(name = "puphaxService", fallbackMethod = "useCsvFallback")
public String searchDrugs(String term) {
    // ...
}
```

### API Hibák

#### 404 Not Found végpontokon

**Ellenőrzés:**
```bash
# Ellenőrizd hogy a service fut
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors

# Nézd meg az elérhető végpontokat
curl http://localhost:8081/swagger-ui.html
```

#### JSON parsing hiba

**Hiba:**
```
HttpMessageNotReadableException: JSON parse error
```

**Ellenőrzés:**
```bash
# Helyes formátum
curl -X POST http://localhost:8081/api/v1/gyogyszerek/kereses \
  -H "Content-Type: application/json" \
  -d '{"keresett_kifejezés": "aspirin", "meret": 10}'
```

### Teljesítmény Problémák

#### Szolgáltatás lassan indul (>1 perc)

**Ok:** CSV fájlok betöltése időigényes.

**Optimalizálás:**
```yaml
# application.yml - csak szükséges táblák betöltése
puphax:
  csv:
    tables:
      - TERMEK
      - BRAND
    # ATCKONYV és CEGEK opcionális
```

#### Magas memóriahasználat

**Monitorozás:**
```bash
# Docker
docker stats puphax-service

# Heap dump elemzése
jcmd <pid> GC.heap_dump /tmp/heap.hprof
```

**Optimalizálás:**
- Növeld a cache eviction rate-et
- Csökkentsd a betöltött termékek számát (pl. csak utolsó 1 év)
- Kapcsold ki a nem használt CSV táblákat

### Log Elemzés

```bash
# Hibák keresése
docker-compose logs puphax-service | grep -i error

# CSV fallback használat
docker-compose logs puphax-service | grep -i "csv fallback"

# NEAK hívások
docker-compose logs puphax-service | grep -i "making direct http call"

# Cache statisztikák
docker-compose logs puphax-service | grep -i cache

# Válaszidők elemzése
docker-compose logs puphax-service | grep "valaszIdoMs"
```

### Support

Ha nem találod a megoldást:

1. **GitHub Issues**: https://github.com/felhasznalonev/puphax-service/issues
2. **Dokumentáció**: Olvasd el a `PUPHAX_DEVELOPMENT_GUIDE.md`-t
3. **NEAK Hivatalos**: https://www.neak.gov.hu

---

# 🇬🇧 English Version {#english-version}

## Table of Contents

- [Overview](#overview-en)
- [Key Features](#key-features-en)
- [Quick Start](#quick-start-en)
- [API Documentation](#api-documentation-en)
- [CSV Fallback System](#csv-fallback-system-en)
- [Integration Guide](#integration-guide-en)
- [Contributing](#contributing-en)
- [Configuration](#configuration-en)
- [Development](#development-en)
- [Deployment](#deployment-en)
- [Troubleshooting](#troubleshooting-en)

## Overview {#overview-en}

The PUPHAX REST API Service is a modern REST API providing access to the NEAK (National Health Insurance Fund of Hungary) PUPHAX pharmaceutical database. This service bridges modern applications with the legacy SOAP-based PUPHAX webservice, featuring a built-in CSV fallback system for high availability.

### Why Use This?

- ✅ **Simple REST API** instead of complex SOAP
- ✅ **Always available** - works even when NEAK server is down
- ✅ **Fast** - 40x faster searches with CSV fallback (<50ms vs ~2s)
- ✅ **Bilingual** - Hungarian and English endpoints
- ✅ **Production-ready** - Docker, monitoring, health checks
- ✅ **Well-documented** - Complete Hungarian and English docs

## Key Features {#key-features-en}

### 🚀 Modern Technologies

- **Spring Boot 3.5.6** - Built on Java 17
- **REST API** - Clean JSON format
- **Docker support** - Containerized deployment
- **Resilience4j** - Fault tolerance (Circuit Breaker, Retry)
- **Caffeine Cache** - Intelligent caching
- **OpenAPI/Swagger** - Automatic API documentation

### 💾 CSV Fallback System

The service includes official NEAK historical data (2007-2023):

- **43,930 current drugs** - only valid products
- **8,905 brand names** - manufacturers and brands
- **6,828 ATC codes** - drug classification
- **2,314 companies** - manufacturer data

**Automatic fallback when:**
- NEAK server is unavailable
- NEAK sends HTML error instead of SOAP response
- Response processing fails

### 🔍 Search Capabilities

- **Search by drug name**: e.g. "Aspirin"
- **Search by active ingredient**: e.g. "acetylsalicylic acid"
- **Combined search**: examines both criteria simultaneously
- **Fast response**: <50ms average response time

## Quick Start {#quick-start-en}

### Prerequisites

- **Java 17** or newer
- **Maven 3.9+** or use the included wrapper
- **Docker** (optional, for containerized deployment)

### With Docker (Recommended)

```bash
# Clone repository
git clone https://github.com/username/puphax-service.git
cd puphax-service

# Build and start service
docker-compose up -d

# Check health
curl http://localhost:8081/api/v1/drugs/health/quick
```

Service will be available at: `http://localhost:8081`

### Local Development

```bash
# Build project
./mvnw clean package

# Start service
java -jar target/puphax-rest-api-1.0.0.jar

# Or directly with Maven
./mvnw spring-boot:run
```

### First API Call

```bash
# Search drugs (English endpoint)
curl "http://localhost:8081/api/v1/drugs/search?term=aspirin&size=5"

# Search by active ingredient
curl "http://localhost:8081/api/v1/drugs/search?term=acetylsalicylic&size=5"

# Health check
curl http://localhost:8081/api/v1/drugs/health/quick
```

## API Documentation {#api-documentation-en}

### Swagger UI

Full API documentation available at:
```
http://localhost:8081/swagger-ui.html
```

### English Endpoints

#### Drug Search

```http
GET /api/v1/drugs/search
```

**Query parameters:**
- `term` (required): Search text (drug name or active ingredient)
- `manufacturer` (optional): Filter by manufacturer
- `atc_code` (optional): Filter by ATC code
- `page` (optional, default: 0): Page number
- `size` (optional, default: 10): Results per page
- `sort` (optional): Sort field (name, manufacturer, atcCode)
- `direction` (optional): Sort direction (ASC, DESC)

**Example response:**
```json
{
  "drugs": [
    {
      "id": "55827054",
      "name": "ASPIRIN 500 MG TABLET",
      "manufacturer": "Bayer",
      "atcCode": "N02BA01",
      "activeIngredients": ["acetylsalicylic acid"],
      "prescriptionRequired": false,
      "reimbursable": true,
      "status": "ACTIVE"
    }
  ],
  "pagination": {
    "currentPage": 0,
    "pageSize": 10,
    "totalElements": 25,
    "totalPages": 3
  },
  "searchMetadata": {
    "searchTerm": "aspirin",
    "responseTimeMs": 45,
    "cacheHit": false
  }
}
```

#### Additional Endpoints

```http
# Drug details
GET /api/v1/drugs/{id}

# Quick health check
GET /api/v1/drugs/health/quick

# Detailed health check
GET /api/v1/drugs/health
```

## CSV Fallback System {#csv-fallback-system-en}

### How It Works

The service automatically switches to the local CSV database when:

1. **NEAK server unavailable** - network error, timeout
2. **HTML error message** - NEAK sends HTML instead of SOAP response
3. **Processing error** - SOAP response cannot be processed
4. **Empty result** - no product ID found in response

### User Experience

When CSV fallback is active:
- **Frontend notification**: Purple banner appears with "Local database in use" message
- **Fast response**: <50ms response time (40x faster than SOAP)
- **Full functionality**: Search by name and active ingredient both work

### Data Updates

To update CSV data:

```bash
# 1. Download latest data from NEAK website
# https://www.neak.gov.hu/.../puphax

# 2. Filter to current products (last 2 years)
python3 scripts/filter-termek.py

# 3. Copy new files
cp filtered/*.csv src/main/resources/puphax-data/

# 4. Rebuild and deploy
./mvnw clean package
docker-compose build
docker-compose up -d
```

## Integration Guide {#integration-guide-en}

### 1. As a Microservice (Recommended)

Simplest method: run PUPHAX service as a separate container.

#### With Docker Compose

```yaml
# In your docker-compose.yml
version: '3.8'

services:
  # Your application
  my-backend:
    image: my-app:latest
    environment:
      PUPHAX_API_URL: http://puphax-service:8081
    depends_on:
      - puphax-service

  # PUPHAX service
  puphax-service:
    image: puphax-hungarian:latest
    ports:
      - "8081:8081"
    environment:
      PUPHAX_USERNAME: ${PUPHAX_USERNAME}
      PUPHAX_PASSWORD: ${PUPHAX_PASSWORD}
      # CSV fallback enabled by default
      PUPHAX_CSV_AUTO_FALLBACK: true
      # Query only recent data (not full 15-year history)
      PUPHAX_QUERY_SNAPSHOT_DATE_OFFSET_MONTHS: 1
```

#### Usage in Backend (Java/Spring Boot)

```java
@Service
public class PharmacyService {

    @Value("${puphax.api.url}")
    private String puphaxApiUrl;

    private final RestTemplate restTemplate;

    public List<Drug> searchDrugs(String searchTerm) {
        String url = puphaxApiUrl + "/api/v1/drugs/search?term=" + searchTerm;

        DrugSearchResponse response = restTemplate.getForObject(
            url,
            DrugSearchResponse.class
        );

        return response.getDrugs();
    }
}
```

### 2. Direct Integration (Maven Module)

For direct code integration in your project, follow the same steps as in the Hungarian section above.

### 3. Frontend Integration

Frontend integration examples (Vanilla JS, React, Vue.js) are identical to the Hungarian section - just use English endpoint `/api/v1/drugs/search` instead of `/api/v1/gyogyszerek/kereses`.

## Contributing {#contributing-en}

We welcome all contributions! This section helps you get started.

### Fork and Clone

```bash
# 1. Fork the repository on GitHub (top right corner)

# 2. Clone your forked repository
git clone https://github.com/your-username/puphax-service.git
cd puphax-service

# 3. Add the original repository as upstream
git remote add upstream https://github.com/original-owner/puphax-service.git

# 4. Verify remotes
git remote -v
```

### Creating a Branch

Always create a new branch for your changes:

```bash
# Update main branch
git checkout main
git pull upstream main

# Create new feature branch
git checkout -b feature/drug-filtering-enhancement

# Or bugfix branch
git checkout -b fix/cache-timeout-bug
```

### Branch Naming Conventions

- `feature/name` - new features (e.g. `feature/fuzzy-search`)
- `fix/name` - bug fixes (e.g. `fix/csv-encoding`)
- `docs/name` - documentation updates (e.g. `docs/api-examples`)
- `refactor/name` - code restructuring (e.g. `refactor/service-layer`)
- `test/name` - adding tests (e.g. `test/integration-tests`)

### Development and Commit

```bash
# Make your changes

# Check changes
git status
git diff

# Stage files
git add .

# Write commit message
git commit -m "feat: add fuzzy search to drug search

- Implemented Levenshtein distance calculation
- Improved search accuracy for typos
- Added configuration for tolerance settings"
```

### Commit Message Format

Use structured commit messages:

```
<type>: <short description>

<detailed description - optional>

<footer - optional>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting (no code change)
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Build or config changes

**Examples:**
```
feat: add ATC code filtering

fix: CSV encoding issue with Hungarian characters

docs: update REST API examples in README

refactor: organize cache configuration into separate class
```

### Testing

Before opening a pull request, ensure all tests pass:

```bash
# Run unit tests
./mvnw test

# Integration tests
./mvnw verify

# Test Docker build
docker-compose build
docker-compose up -d

# Manual API testing
curl http://localhost:8081/api/v1/drugs/health/quick
curl "http://localhost:8081/api/v1/drugs/search?term=aspirin"

# Stop Docker
docker-compose down
```

### Opening a Pull Request

```bash
# Push branch to your fork
git push origin feature/drug-filtering-enhancement

# Go to GitHub web interface
# Click "Compare & pull request" button
```

**Pull Request Template:**

```markdown
## Description
Brief summary of changes.

## Type of Change
- [ ] New feature (feat)
- [ ] Bug fix (fix)
- [ ] Documentation update (docs)
- [ ] Code refactoring (refactor)
- [ ] Tests (test)

## Testing
How did you test? What tests did you add?

- [ ] Unit tests written/updated
- [ ] Integration tests run
- [ ] Manual testing completed
- [ ] Docker build successful

## Checklist
- [ ] Code follows project style
- [ ] Self-review completed
- [ ] Comments added where necessary
- [ ] Documentation updated
- [ ] All tests pass
- [ ] No new warnings
```

For detailed code style guidelines, development setup, deployment instructions, and troubleshooting - please refer to the corresponding Hungarian sections above, as they contain identical technical information.

---

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Acknowledgments

- **NEAK**: For PUPHAX webservice and historical data
- **Spring Team**: For excellent Spring Boot framework
- **Community**: For testing and feedback

---

**Made with ❤️ for the Hungarian healthcare community**

