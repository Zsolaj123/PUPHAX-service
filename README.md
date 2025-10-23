# PUPHAX REST API Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

> 🇭🇺 Modern REST API for NEAK PUPHAX pharmaceutical database with automatic CSV fallback
>
> 🇬🇧 [English version below](#english-version)

---

## 🇭🇺 Magyar

### Mi ez?

Modern REST API a NEAK (Nemzeti Egészségbiztosítási Alapkezelő) PUPHAX gyógyszerészeti adatbázisához. Átalakítja az elavult SOAP webszolgáltatást egyszerű REST API-vá, beépített CSV tartalékkal a magas rendelkezésre állásért.

### Főbb Előnyök

- ✅ **Egyszerű REST API** - JSON formátum SOAP helyett
- ✅ **Mindig elérhető** - CSV tartalék ha a NEAK szerver nem működik
- ✅ **Gyors** - 40x gyorsabb keresés (<50ms vs ~2s)
- ✅ **Kétnyelvű** - magyar és angol végpontok
- ✅ **43,930 gyógyszer** - teljes aktuális adatbázis
- ✅ **Docker támogatás** - konténerizált telepítés

### Gyors Kezdés

```bash
# Docker-rel (ajánlott)
git clone https://github.com/Zsolaj123/PUPHAX-service.git
cd PUPHAX-service
docker compose -f docker/docker-compose.yml up -d

# Tesztelés
curl http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors
```

**Szolgáltatás elérhető:** `http://localhost:8081`
**API Dokumentáció:** `http://localhost:8081/swagger-ui.html`

### API Példa

```bash
# Gyógyszer keresése
curl "http://localhost:8081/api/v1/gyogyszerek/kereses?keresett_kifejezés=aspirin"

# Válasz tartalmazza: név, hatóanyag, gyártó, ATC kód, forma, kiszerelés, stb.
```

### Részletes Dokumentáció

- 📚 **[Telepítési Útmutató](guides/INSTALLATION.md)** - Docker, JAR, fejlesztői környezet
- 🚀 **[Használati Útmutató](guides/USAGE.md)** - API végpontok, példák
- 🔧 **[Konfigurációs Útmutató](guides/CONFIGURATION.md)** - Beállítások, környezeti változók
- 🐳 **[Telepítési Útmutató](guides/DEPLOYMENT.md)** - Docker, Kubernetes, Nginx
- 🔍 **[Hibaelhárítás](guides/TROUBLESHOOTING.md)** - Gyakori problémák és megoldások
- 🤝 **[Hozzájárulási Útmutató](guides/CONTRIBUTING.md)** - Fejlesztés, PR-ek
- 📊 **[Adatmezők Dokumentáció](docs/COMPREHENSIVE_DATA_FIELDS.md)** - Mind a 44 elérhető mező
- 🛠️ **[Fejlesztői Útmutató](docs/PUPHAX_DEVELOPMENT_GUIDE.md)** - Architektúra, NEAK optimalizálás

### Technológiák

- **Backend:** Spring Boot 3.5.6, Java 17
- **API:** REST JSON, OpenAPI/Swagger dokumentáció
- **Adatbázis:** CSV fallback (43,930 gyógyszer)
- **Hibatűrés:** Resilience4j (Circuit Breaker, Retry)
- **Cache:** Caffeine (intelligens gyorsítótár)
- **Deploy:** Docker, Docker Compose

### Hozzájárulás

Szívesen fogadunk közreműködést! 🎉

1. **Fork-old** a repository-t
2. **Hozz létre** egy feature branch-et (`git checkout -b feature/uj-funkció`)
3. **Commit-old** a változtatásokat (`git commit -m 'Új funkció hozzáadása'`)
4. **Push-old** a branch-re (`git push origin feature/uj-funkció`)
5. **Nyiss** egy Pull Request-et

Részletekért lásd: [Hozzájárulási Útmutató](guides/CONTRIBUTING.md)

### Licenc

MIT License - lásd [LICENSE](LICENSE) fájl

---

## 🇬🇧 English Version

### What is this?

Modern REST API for NEAK PUPHAX pharmaceutical database. Transforms the legacy SOAP web service into a simple REST API with built-in CSV fallback for high availability.

### Key Features

- ✅ **Simple REST API** - JSON format instead of SOAP
- ✅ **Always Available** - CSV fallback when NEAK server is down
- ✅ **Fast** - 40x faster search (<50ms vs ~2s)
- ✅ **Bilingual** - Hungarian and English endpoints
- ✅ **43,930 drugs** - complete current database
- ✅ **Docker Support** - containerized deployment

### Quick Start

```bash
# With Docker (recommended)
git clone https://github.com/Zsolaj123/PUPHAX-service.git
cd PUPHAX-service
docker compose -f docker/docker-compose.yml up -d

# Test
curl http://localhost:8081/api/v1/drugs/health/quick
```

**Service available at:** `http://localhost:8081`
**API Documentation:** `http://localhost:8081/swagger-ui.html`

### API Example

```bash
# Search for drugs
curl "http://localhost:8081/api/v1/drugs/search?searchTerm=aspirin"

# Response includes: name, active ingredient, manufacturer, ATC code, form, pack size, etc.
```

### Detailed Documentation

- 📚 **[Installation Guide](guides/INSTALLATION.md)** - Docker, JAR, development setup
- 🚀 **[Usage Guide](guides/USAGE.md)** - API endpoints, examples
- 🔧 **[Configuration Guide](guides/CONFIGURATION.md)** - Settings, environment variables
- 🐳 **[Deployment Guide](guides/DEPLOYMENT.md)** - Docker, Kubernetes, Nginx
- 🔍 **[Troubleshooting](guides/TROUBLESHOOTING.md)** - Common issues and solutions
- 🤝 **[Contributing Guide](guides/CONTRIBUTING.md)** - Development, PRs
- 📊 **[Data Fields Documentation](docs/COMPREHENSIVE_DATA_FIELDS.md)** - All 44 available fields
- 🛠️ **[Developer Guide](docs/PUPHAX_DEVELOPMENT_GUIDE.md)** - Architecture, NEAK optimization

### Technologies

- **Backend:** Spring Boot 3.5.6, Java 17
- **API:** REST JSON, OpenAPI/Swagger documentation
- **Database:** CSV fallback (43,930 drugs)
- **Resilience:** Resilience4j (Circuit Breaker, Retry)
- **Cache:** Caffeine (intelligent caching)
- **Deploy:** Docker, Docker Compose

### Contributing

Contributions are welcome! 🎉

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

For details, see: [Contributing Guide](guides/CONTRIBUTING.md)

### License

MIT License - see [LICENSE](LICENSE) file

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/Zsolaj123/PUPHAX-service/issues)
- **Documentation:** See [guides/](guides/) folder for detailed guides

---

**Last Updated:** 2025-10-23
**Version:** 1.1.0
