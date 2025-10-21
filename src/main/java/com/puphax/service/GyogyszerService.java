package com.puphax.service;

import com.puphax.model.dto.GyogyszerKeresesiValasz;

/**
 * Magyar gyógyszer keresési szolgáltatás interface.
 * PUPHAX adatbázisban történő gyógyszer keresési műveleteket definiál.
 */
public interface GyogyszerService {
    
    /**
     * Gyógyszerek keresése a megadott kritériumok alapján.
     * 
     * @param keresettkifejezés Gyógyszer neve vagy részleges neve
     * @param gyarto Opcionális gyártó szűrő
     * @param atcKod Opcionális ATC kód szűrő
     * @param oldal Oldal szám (0-tól kezdődik)
     * @param meret Oldal méret
     * @param rendezes Rendezési mező (nev, gyarto, atcKod)
     * @param irany Rendezési irány (ASC, DESC)
     * @return GyogyszerKeresesiValasz a keresési eredményekkel
     */
    GyogyszerKeresesiValasz keresesGyogyszerek(
        String keresettkifejezés,
        String gyarto,
        String atcKod,
        int oldal,
        int meret,
        String rendezes,
        String irany
    );
}