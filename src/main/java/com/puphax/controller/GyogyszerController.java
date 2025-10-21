package com.puphax.controller;

import com.puphax.model.dto.GyogyszerKeresesiValasz;
import com.puphax.model.dto.HealthStatus;
import com.puphax.model.dto.EgeszsegStatu;
import com.puphax.service.GyogyszerService;
import com.puphax.service.HealthService;
import com.puphax.exception.PuphaxValidationException;
import com.puphax.util.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Magyar REST vezérlő gyógyszer keresési műveletekhez.
 * 
 * Ez a vezérlő végpontokat biztosít a PUPHAX adatbázisban történő gyógyszer kereséshez
 * szűrési, lapozási és rendezési támogatással.
 */
@RestController
@RequestMapping("/api/v1/gyogyszerek")
@Validated
@Tag(name = "Gyógyszer Keresés", description = "API a PUPHAX adatbázisban történő gyógyszer kereséshez")
public class GyogyszerController {
    
    private static final Logger logger = LoggerFactory.getLogger(GyogyszerController.class);
    
    private final GyogyszerService gyogyszerService;
    private final HealthService healthService;
    
    @Autowired
    public GyogyszerController(GyogyszerService gyogyszerService, HealthService healthService) {
        this.gyogyszerService = gyogyszerService;
        this.healthService = healthService;
    }
    
    /**
     * Gyógyszerek keresése a megadott kritériumok alapján.
     * 
     * @param keresett_kifejezés Gyógyszer neve vagy részleges neve (kötelező)
     * @param gyarto Opcionális gyártó szűrő
     * @param atcKod Opcionális ATC kód szűrő (formátum: A10AB01)
     * @param oldal Oldal szám, 0-tól kezdődik (alapértelmezett: 0)
     * @param meret Oldal méret, maximum 100 (alapértelmezett: 20)
     * @param rendezes Rendezési mező: nev, gyarto, vagy atcKod (alapértelmezett: nev)
     * @param irany Rendezési irány: ASC vagy DESC (alapértelmezett: ASC)
     * @return GyogyszerKeresesiValasz lapozott eredményekkel
     */
    @GetMapping("/kereses")
    @Operation(
        summary = "Gyógyszerek keresése",
        description = "Gyógyszerek keresése név alapján, opcionális szűrési lehetőségekkel gyártó és ATC kód szerint. Támogatja a lapozást és rendezést."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Keresés sikeresen végrehajtva",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = GyogyszerKeresesiValasz.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Érvénytelen kérés paraméterek",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Belső szerver hiba",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "503",
            description = "PUPHAX szolgáltatás nem elérhető",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    public ResponseEntity<GyogyszerKeresesiValasz> keresesGyogyszerek(
        
        @Parameter(
            description = "Gyógyszer neve vagy részleges neve kereséshez",
            required = true,
            example = "aspirin"
        )
        @RequestParam("keresett_kifejezés")
        @NotBlank(message = "A keresett kifejezés nem lehet üres")
        @Size(min = 2, max = 100, message = "A keresett kifejezésnek 2 és 100 karakter között kell lennie")
        String keresettkifejezés,
        
        @Parameter(
            description = "Szűrés gyártó név szerint",
            required = false,
            example = "Bayer"
        )
        @RequestParam(value = "gyarto", required = false)
        @Size(max = 100, message = "A gyártó szűrő nem lehet hosszabb 100 karakternél")
        String gyarto,
        
        @Parameter(
            description = "Szűrés ATC kód szerint (formátum: A10AB01)",
            required = false,
            example = "N02BA01"
        )
        @RequestParam(value = "atc_kod", required = false)
        @Pattern(
            regexp = "^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$",
            message = "Az ATC kódnak a következő formátumot kell követnie: A10AB01"
        )
        String atcKod,
        
        @Parameter(
            description = "Oldal szám (0-tól kezdődik)",
            required = false,
            example = "0"
        )
        @RequestParam(value = "oldal", defaultValue = "0")
        @Min(value = 0, message = "Az oldal számnak 0-nál nagyobbnak vagy egyenlőnek kell lennie")
        int oldal,
        
        @Parameter(
            description = "Oldal méret (1-100)",
            required = false,
            example = "20"
        )
        @RequestParam(value = "meret", defaultValue = "20")
        @Min(value = 1, message = "Az oldal méretnek legalább 1-nek kell lennie")
        @Max(value = 100, message = "Az oldal méret nem lehet nagyobb 100-nál")
        int meret,
        
        @Parameter(
            description = "Rendezési mező",
            required = false,
            example = "nev"
        )
        @RequestParam(value = "rendezes", defaultValue = "nev")
        @Pattern(
            regexp = "^(nev|gyarto|atcKod)$",
            message = "A rendezési mezőnek a következők egyikének kell lennie: nev, gyarto, atcKod"
        )
        String rendezes,
        
        @Parameter(
            description = "Rendezési irány",
            required = false,
            example = "ASC"
        )
        @RequestParam(value = "irany", defaultValue = "ASC")
        @Pattern(
            regexp = "^(ASC|DESC)$",
            message = "A rendezési iránynak ASC vagy DESC-nek kell lennie"
        )
        String irany,
        HttpServletRequest request
    ) {
        
        String korrelaciosId = LoggingUtils.generateCorrelationId();
        long kezdesIdeje = System.currentTimeMillis();
        
        try {
            // Naplózási kontextus beállítása
            LoggingUtils.setupSearchContext(korrelaciosId, keresettkifejezés, gyarto, atcKod, oldal, meret);
            LoggingUtils.setClientIp(getClientIpAddress(request));
            
            logger.info("Gyógyszer keresési kérés indítva: kifejezés='{}', gyártó='{}', atcKód='{}', oldal={}, méret={}, rendezés={}, irány={}",
                       keresettkifejezés, gyarto, atcKod, oldal, meret, rendezes, irany);
            
            // Bemeneti paraméterek validálása
            validaljaKeresesiParametereket(keresettkifejezés, gyarto, atcKod, oldal, meret, rendezes, irany);
            
            // Keresés végrehajtása
            GyogyszerKeresesiValasz valasz = gyogyszerService.keresesGyogyszerek(
                keresettkifejezés, gyarto, atcKod, oldal, meret, rendezes, irany
            );
            
            // Sikeres metrikák naplózása
            long valaszIdo = System.currentTimeMillis() - kezdesIdeje;
            LoggingUtils.setResponseTime(valaszIdo);
            LoggingUtils.setResultCount(valasz.getJelenlegiOldalMeret());
            
            logger.info("Gyógyszer keresés sikeresen befejezve: {} eredmény található a(z) '{}' kifejezésre, összes elem: {}, válaszidő: {}ms",
                       valasz.getJelenlegiOldalMeret(), keresettkifejezés, valasz.getLapozas().getOsszesElem(), valaszIdo);
            
            return ResponseEntity.ok(valasz);
            
        } catch (Exception e) {
            long valaszIdo = System.currentTimeMillis() - kezdesIdeje;
            LoggingUtils.setResponseTime(valaszIdo);
            LoggingUtils.setupErrorContext(korrelaciosId, 
                e instanceof PuphaxValidationException ? "VALIDACIOS_HIBA" : "KERESESI_HIBA", 
                "gyogyszer-kereses");
            
            logger.error("Gyógyszer keresés sikertelen a(z) '{}' kifejezésre: {} (válaszidő: {}ms)", 
                        keresettkifejezés, e.getMessage(), valaszIdo);
            throw e;
            
        } finally {
            LoggingUtils.clearContext();
        }
    }
    
    /**
     * Keresési paraméterek validálása és megfelelő kivételek dobása érvénytelen bemenet esetén.
     */
    private void validaljaKeresesiParametereket(String keresettkifejezés, String gyarto, String atcKod,
                                              int oldal, int meret, String rendezes, String irany) {
        
        // Kifejezés validáció
        if (keresettkifejezés == null || keresettkifejezés.trim().isEmpty()) {
            throw new PuphaxValidationException("keresett_kifejezés", keresettkifejezés, "A keresett kifejezés nem lehet üres");
        }
        
        if (keresettkifejezés.length() < 2 || keresettkifejezés.length() > 100) {
            throw new PuphaxValidationException("keresett_kifejezés", keresettkifejezés, "A keresett kifejezésnek 2 és 100 karakter között kell lennie");
        }
        
        // Gyártó validáció
        if (gyarto != null && gyarto.length() > 100) {
            throw new PuphaxValidationException("gyarto", gyarto, "A gyártó szűrő nem lehet hosszabb 100 karakternél");
        }
        
        // ATC kód validáció
        if (atcKod != null && !atcKod.matches("^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$")) {
            throw new PuphaxValidationException("atc_kod", atcKod, "Az ATC kódnak a következő formátumot kell követnie: A10AB01");
        }
        
        // Lapozás validáció
        if (oldal < 0) {
            throw new PuphaxValidationException("oldal", oldal, "Az oldal számnak 0-nál nagyobbnak vagy egyenlőnek kell lennie");
        }
        
        if (meret < 1 || meret > 100) {
            throw new PuphaxValidationException("meret", meret, "Az oldal méretnek 1 és 100 között kell lennie");
        }
        
        // Rendezés validáció
        if (!"nev".equals(rendezes) && !"gyarto".equals(rendezes) && !"atcKod".equals(rendezes)) {
            throw new PuphaxValidationException("rendezes", rendezes, "A rendezési mezőnek a következők egyikének kell lennie: nev, gyarto, atcKod");
        }
        
        if (!"ASC".equals(irany) && !"DESC".equals(irany)) {
            throw new PuphaxValidationException("irany", irany, "A rendezési iránynak ASC vagy DESC-nek kell lennie");
        }
    }
    
    /**
     * Átfogó egészségügyi ellenőrzési végpont a gyógyszer keresési szolgáltatáshoz.
     * 
     * @return Részletes egészségügyi állapot beleértve a PUPHAX szolgáltatás kapcsolódást
     */
    @GetMapping("/egeszseg")
    @Operation(
        summary = "Gyógyszer keresési szolgáltatás egészségügyi ellenőrzése",
        description = "Átfogó egészségügyi ellenőrzési végpont, amely ellenőrzi a PUPHAX szolgáltatás kapcsolódást és a komponensek állapotát"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "A szolgáltatás egészséges vagy részben egészséges",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthStatus.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "A szolgáltatás leállt vagy nem elérhető",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthStatus.class)
            )
        )
    })
    public ResponseEntity<EgeszsegStatu> egeszseg(HttpServletRequest request) {
        String korrelaciosId = LoggingUtils.generateCorrelationId();
        long kezdesIdeje = System.currentTimeMillis();
        
        try {
            LoggingUtils.setupHealthCheckContext(korrelaciosId);
            LoggingUtils.setClientIp(getClientIpAddress(request));
            
            logger.debug("Egészségügyi ellenőrzés kérve a gyógyszer keresési szolgáltatáshoz");
            
            HealthStatus angolStatu = healthService.checkHealth();
            EgeszsegStatu egeszsegStatu = konvertalEgeszsegStatusra(angolStatu);
            
            long valaszIdo = System.currentTimeMillis() - kezdesIdeje;
            LoggingUtils.setResponseTime(valaszIdo);
            
            // 503 Service Unavailable visszaadása, ha a szolgáltatás leállt
            if ("LEALL".equals(egeszsegStatu.getStatu())) {
                logger.warn("Egészségügyi ellenőrzés sikertelen: a szolgáltatás LEÁLLT (válaszidő: {}ms)", valaszIdo);
                return ResponseEntity.status(503).body(egeszsegStatu);
            }
            
            // 200 OK visszaadása FEL vagy ROMLOTT állapot esetén
            logger.info("Egészségügyi ellenőrzés befejezve: a szolgáltatás állapota {} (válaszidő: {}ms)", 
                       egeszsegStatu.getStatu(), valaszIdo);
            return ResponseEntity.ok(egeszsegStatu);
            
        } finally {
            LoggingUtils.clearContext();
        }
    }
    
    /**
     * Gyors egészségügyi ellenőrzési végpont terheléselosztókhoz és monitorozáshoz.
     * 
     * @return Egyszerű egészségügyi állapot
     */
    @GetMapping("/egeszseg/gyors")
    @Operation(
        summary = "Gyors egészségügyi ellenőrzés",
        description = "Könnyű egészségügyi ellenőrzési végpont terheléselosztókhoz és monitorozó rendszerekhez"
    )
    @ApiResponse(
        responseCode = "200",
        description = "A szolgáltatás működőképes",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
    )
    public ResponseEntity<EgeszsegStatu> gyorsEgeszseg() {
        logger.debug("Gyors egészségügyi ellenőrzés kérve");
        
        HealthStatus angolStatu = healthService.checkHealthQuick();
        EgeszsegStatu egeszsegStatu = konvertalEgeszsegStatusra(angolStatu);
        
        if ("LEALL".equals(egeszsegStatu.getStatu())) {
            return ResponseEntity.status(503).body(egeszsegStatu);
        }
        
        return ResponseEntity.ok(egeszsegStatu);
    }
    
    /**
     * Egyszerű teszt végpont a szolgáltatás működésének ellenőrzéséhez.
     */
    @GetMapping("/teszt")
    public ResponseEntity<String> teszt() {
        logger.info("Teszt végpont meghívva");
        return ResponseEntity.ok("{\"uzenet\":\"GyogyszerController működik!\",\"idopecsét\":\"" + java.time.Instant.now() + "\"}");
    }
    
    /**
     * Egyszerű keresési végpont teszteléshez összetett logika nélkül.
     */
    @GetMapping("/kereses-egyszeru")
    public ResponseEntity<String> egyszeruKereses(@RequestParam("kifejezés") String kifejezés) {
        logger.info("Egyszerű keresés meghívva a következő kifejezéssel: {}", kifejezés);
        
        String valasz = String.format("""
            {
                "keresettkifejezés": "%s",
                "gyógyszerek": [
                    {
                        "id": "HU001234",
                        "nev": "%s 100mg tabletta",
                        "gyarto": "Bayer Hungary Kft.",
                        "atcKod": "N02BA01",
                        "hatoanyagok": ["Acetilszalicilsav"],
                        "venykoeteles": false,
                        "tamogatott": true,
                        "allapot": "AKTIV"
                    }
                ],
                "lapozas": {
                    "jelenlegiOldal": 0,
                    "oldalMeret": 20,
                    "osszesOldal": 1,
                    "osszesElem": 1
                }
            }
            """, kifejezés, kifejezés);
        
        return ResponseEntity.ok(valasz);
    }
    
    /**
     * HealthStatus konvertálása magyar EgeszsegStatu-ra.
     */
    private EgeszsegStatu konvertalEgeszsegStatusra(HealthStatus angolStatu) {
        String magyarAllapot = switch (angolStatu.status()) {
            case "UP" -> "FEL";
            case "DOWN" -> "LEALL";
            case "DEGRADED" -> "ROMLOTT";
            default -> angolStatu.status();
        };
        
        String uzenet = switch (magyarAllapot) {
            case "FEL" -> "Minden szolgáltatás működik";
            case "LEALL" -> "Szolgáltatás nem elérhető";
            case "ROMLOTT" -> "Néhány szolgáltatás problémával küzd";
            default -> "Ismeretlen állapot";
        };
        
        EgeszsegStatu magyarStatu = new EgeszsegStatu(magyarAllapot, uzenet);
        magyarStatu.setIdopecsét(angolStatu.timestamp());
        magyarStatu.setVerzio(angolStatu.version());
        
        // Convert components if needed
        // TODO: Convert HealthStatus.ComponentHealth to EgeszsegStatu.KomponensStatu
        
        return magyarStatu;
    }
    
    /**
     * Kliens IP cím kinyerése a HTTP kérésből.
     * 
     * @param request HTTP servlet kérés
     * @return Kliens IP cím
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}