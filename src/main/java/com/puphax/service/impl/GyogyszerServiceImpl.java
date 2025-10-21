package com.puphax.service.impl;

import com.puphax.model.dto.GyogyszerKeresesiValasz;
import com.puphax.model.dto.DrugSearchResponse;
import com.puphax.model.dto.DrugSummary;
import com.puphax.model.dto.PaginationInfo;
import com.puphax.model.dto.SearchInfo;
import com.puphax.service.GyogyszerService;
import com.puphax.service.DrugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Magyar gyógyszer keresési szolgáltatás implementáció.
 * A meglévő DrugService-t adaptálja magyar terminológiához.
 */
@Service
public class GyogyszerServiceImpl implements GyogyszerService {
    
    private static final Logger logger = LoggerFactory.getLogger(GyogyszerServiceImpl.class);
    
    private final DrugService drugService;
    
    @Autowired
    public GyogyszerServiceImpl(DrugService drugService) {
        this.drugService = drugService;
    }
    
    @Override
    public GyogyszerKeresesiValasz keresesGyogyszerek(
            String keresettkifejezés,
            String gyarto,
            String atcKod,
            int oldal,
            int meret,
            String rendezes,
            String irany) {
        
        logger.debug("Magyar gyógyszer keresés: kifejezés='{}', gyártó='{}', atcKód='{}', oldal={}, méret={}",
                    keresettkifejezés, gyarto, atcKod, oldal, meret);
        
        // Angol mezőnevek fordítása
        String angolRendezes = forditRendezesiMezot(rendezes);
        
        // Angol szolgáltatás hívása
        DrugSearchResponse angolValasz = drugService.searchDrugs(
            keresettkifejezés, gyarto, atcKod, oldal, meret, angolRendezes, irany
        );
        
        // Magyar válasz objektum létrehozása
        return konvertalMagyarValaszra(angolValasz, keresettkifejezés);
    }
    
    /**
     * Magyar rendezési mező angol megfelelőjére fordítása.
     */
    private String forditRendezesiMezot(String magyarMezo) {
        if (magyarMezo == null) {
            return "name";
        }
        
        return switch (magyarMezo) {
            case "nev" -> "name";
            case "gyarto" -> "manufacturer";
            case "atcKod" -> "atcCode";
            default -> "name";
        };
    }
    
    /**
     * Angol DrugSearchResponse konvertálása magyar GyogyszerKeresesiValasz-ra.
     */
    private GyogyszerKeresesiValasz konvertalMagyarValaszra(DrugSearchResponse angolValasz, String keresettkifejezés) {
        
        // Gyógyszerek konvertálása
        List<GyogyszerKeresesiValasz.Gyogyszer> magyarGyogyszerek = angolValasz.drugs().stream()
            .map(this::konvertalGyogyszerre)
            .collect(Collectors.toList());
        
        // Lapozás konvertálása
        GyogyszerKeresesiValasz.Lapozas magyarLapozas = konvertalLapozasra(angolValasz.pagination());
        
        // Keresési info konvertálása
        GyogyszerKeresesiValasz.KeresesiInfo magyarKeresesiInfo = konvertalKeresesiInfora(angolValasz.searchInfo(), keresettkifejezés);
        
        return new GyogyszerKeresesiValasz(
            magyarGyogyszerek,
            magyarLapozas,
            magyarKeresesiInfo,
            angolValasz.getCurrentPageSize()
        );
    }
    
    /**
     * Angol DrugSummary konvertálása magyar Gyogyszer-re.
     */
    private GyogyszerKeresesiValasz.Gyogyszer konvertalGyogyszerre(DrugSummary angolGyogyszer) {
        GyogyszerKeresesiValasz.Gyogyszer magyarGyogyszer = new GyogyszerKeresesiValasz.Gyogyszer();
        
        magyarGyogyszer.setId(angolGyogyszer.id());
        magyarGyogyszer.setNev(angolGyogyszer.name());
        magyarGyogyszer.setGyarto(angolGyogyszer.manufacturer());
        magyarGyogyszer.setAtcKod(angolGyogyszer.atcCode());
        magyarGyogyszer.setHatoanyagok(angolGyogyszer.activeIngredients());
        magyarGyogyszer.setVenykoeteles(angolGyogyszer.prescriptionRequired());
        magyarGyogyszer.setTamogatott(angolGyogyszer.reimbursable());
        magyarGyogyszer.setAllapot(forditAllapotot(angolGyogyszer.status()));
        
        // Forrás beállítása
        magyarGyogyszer.setForras("NEAK PUPHAX Adatbázis");
        
        // Map additional fields if available (these will be null if not present in XML)
        // The DrugService parses these from the XML but doesn't expose them in DrugSummary
        // So we need to get them from the raw XML response or modify the service layer
        
        return magyarGyogyszer;
    }
    
    /**
     * Angol PaginationInfo konvertálása magyar Lapozas-ra.
     */
    private GyogyszerKeresesiValasz.Lapozas konvertalLapozasra(PaginationInfo angolLapozas) {
        GyogyszerKeresesiValasz.Lapozas magyarLapozas = new GyogyszerKeresesiValasz.Lapozas();
        
        magyarLapozas.setJelenlegiOldal(angolLapozas.currentPage());
        magyarLapozas.setOldalMeret(angolLapozas.pageSize());
        magyarLapozas.setOsszesOldal(angolLapozas.totalPages());
        magyarLapozas.setOsszesElem(angolLapozas.totalElements());
        magyarLapozas.setVanKovetkezo(angolLapozas.hasNext());
        magyarLapozas.setVanElozo(angolLapozas.hasPrevious());
        
        // Számított mezők
        magyarLapozas.setMegjelenitetOldalSzam(angolLapozas.getDisplayPageNumber());
        magyarLapozas.setKovetkezoOldal(angolLapozas.getNextPage());
        magyarLapozas.setElozoOldal(angolLapozas.getPreviousPage());
        magyarLapozas.setElsoOldal(angolLapozas.isFirstPage());
        magyarLapozas.setUtolsoOldal(angolLapozas.isLastPage());
        
        // Elem tartomány számítása
        magyarLapozas.setElemTartomany(angolLapozas.getItemRange());
        magyarLapozas.setEltolas(angolLapozas.getOffset());
        
        return magyarLapozas;
    }
    
    /**
     * Angol SearchInfo konvertálása magyar KeresesiInfo-ra.
     */
    private GyogyszerKeresesiValasz.KeresesiInfo konvertalKeresesiInfora(SearchInfo angolInfo, String keresettkifejezés) {
        GyogyszerKeresesiValasz.KeresesiInfo magyarInfo = new GyogyszerKeresesiValasz.KeresesiInfo();
        
        magyarInfo.setKeresettkifejezés(keresettkifejezés);
        magyarInfo.setSzurok(new HashMap<>()); // Üres szűrők objektum
        magyarInfo.setValaszIdoMs(angolInfo.responseTimeMs());
        magyarInfo.setCacheTalalat(angolInfo.cacheHit());
        magyarInfo.setIdopecsét(angolInfo.timestamp().toString());
        magyarInfo.setSzurokSzama(angolInfo.getFilterCount());
        magyarInfo.setGyorsValasz(angolInfo.isFastResponse());
        magyarInfo.setSzurokLeiras(forditSzurokLeirast(angolInfo.getFilterDescription()));
        magyarInfo.setTeljesitmenyKategoria(forditTeljesitmenyKategoriat(angolInfo.getPerformanceCategory()));
        
        return magyarInfo;
    }
    
    /**
     * Angol állapot fordítása magyarra.
     */
    private String forditAllapotot(DrugSummary.DrugStatus angolAllapot) {
        if (angolAllapot == null) {
            return "ISMERETLEN";
        }
        
        return switch (angolAllapot) {
            case ACTIVE -> "AKTIV";
            case SUSPENDED -> "FELFUGGESZTETT";
            case WITHDRAWN -> "VISSZAVONT";
            case PENDING -> "FUGGOBEN";
            default -> angolAllapot.toString();
        };
    }
    
    /**
     * Angol szűrők leírás fordítása magyarra.
     */
    private String forditSzurokLeirast(String angolLeiras) {
        if (angolLeiras == null) {
            return "Nincs leírás";
        }
        
        return switch (angolLeiras) {
            case "No filters applied" -> "Nincsenek alkalmazott szűrők";
            default -> angolLeiras;
        };
    }
    
    /**
     * Angol teljesítmény kategória fordítása magyarra.
     */
    private String forditTeljesitmenyKategoriat(String angolKategoria) {
        if (angolKategoria == null) {
            return "Ismeretlen";
        }
        
        return switch (angolKategoria) {
            case "Fast" -> "Gyors";
            case "Normal" -> "Normál";
            case "Slow" -> "Lassú";
            default -> angolKategoria;
        };
    }
}