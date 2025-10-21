package com.puphax.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Gyógyszer keresési eredmények válasz objektuma.
 * Magyar PUPHAX gyógyszer adatbázis keresési eredményeket tartalmaz lapozási információkkal.
 */
@Schema(description = "Gyógyszer keresési válasz magyar PUPHAX adatokkal")
public class GyogyszerKeresesiValasz {
    
    @Schema(description = "Gyógyszerek listája", required = true)
    @NotNull
    @Valid
    private List<Gyogyszer> gyogyszerek;
    
    @Schema(description = "Lapozási információk", required = true)
    @NotNull
    @Valid
    private Lapozas lapozas;
    
    @Schema(description = "Keresési információk", required = true)
    @NotNull
    @Valid
    private KeresesiInfo keresesiInfo;
    
    @Schema(description = "Jelenlegi oldal mérete", required = true, example = "5")
    @NotNull
    private Integer jelenlegiOldalMeret;
    
    public GyogyszerKeresesiValasz() {}
    
    public GyogyszerKeresesiValasz(List<Gyogyszer> gyogyszerek, Lapozas lapozas, 
                                  KeresesiInfo keresesiInfo, Integer jelenlegiOldalMeret) {
        this.gyogyszerek = gyogyszerek;
        this.lapozas = lapozas;
        this.keresesiInfo = keresesiInfo;
        this.jelenlegiOldalMeret = jelenlegiOldalMeret;
    }
    
    // Getters és Setters
    public List<Gyogyszer> getGyogyszerek() {
        return gyogyszerek;
    }
    
    public void setGyogyszerek(List<Gyogyszer> gyogyszerek) {
        this.gyogyszerek = gyogyszerek;
    }
    
    public Lapozas getLapozas() {
        return lapozas;
    }
    
    public void setLapozas(Lapozas lapozas) {
        this.lapozas = lapozas;
    }
    
    public KeresesiInfo getKeresesiInfo() {
        return keresesiInfo;
    }
    
    public void setKeresesiInfo(KeresesiInfo keresesiInfo) {
        this.keresesiInfo = keresesiInfo;
    }
    
    public Integer getJelenlegiOldalMeret() {
        return jelenlegiOldalMeret;
    }
    
    public void setJelenlegiOldalMeret(Integer jelenlegiOldalMeret) {
        this.jelenlegiOldalMeret = jelenlegiOldalMeret;
    }
    
    /**
     * Gyógyszer adatok magyar PUPHAX mezőkkel.
     */
    @Schema(description = "Gyógyszer információk")
    public static class Gyogyszer {
        
        @Schema(description = "PUPHAX gyógyszer azonosító", required = true, example = "14714226")
        @NotNull
        private String id;
        
        @Schema(description = "Gyógyszer neve (magyar)", required = true, example = "XANAX 0,25 MG TABLETTA")
        @NotNull
        private String nev;
        
        @Schema(description = "Gyártó neve", required = true, example = "Pfizer Hungary Kft.")
        @NotNull
        private String gyarto;
        
        @Schema(description = "ATC kód", required = true, example = "N05BA12")
        @NotNull
        private String atcKod;
        
        @Schema(description = "Hatóanyagok listája", required = true)
        @NotNull
        private List<String> hatoanyagok;
        
        @Schema(description = "Vényköteles-e", required = true, example = "true")
        @NotNull
        private Boolean venykoeteles;
        
        @Schema(description = "Társadalombiztosítás által támogatott-e", required = true, example = "true")
        @NotNull
        private Boolean tamogatott;
        
        @Schema(description = "Gyógyszer állapota", required = true, example = "AKTIV")
        @NotNull
        private String allapot;
        
        @Schema(description = "Gyógyszerforma", example = "tabletta")
        private String gyogyszerforma;
        
        @Schema(description = "Kiszerelés", example = "30x tabletta")
        private String kiszereles;
        
        @Schema(description = "Forrás", example = "NEAK PUPHAX Adatbázis")
        private String forras;
        
        // Constructors
        public Gyogyszer() {}
        
        public Gyogyszer(String id, String nev, String gyarto, String atcKod, 
                        List<String> hatoanyagok, Boolean venykoeteles, 
                        Boolean tamogatott, String allapot) {
            this.id = id;
            this.nev = nev;
            this.gyarto = gyarto;
            this.atcKod = atcKod;
            this.hatoanyagok = hatoanyagok;
            this.venykoeteles = venykoeteles;
            this.tamogatott = tamogatott;
            this.allapot = allapot;
        }
        
        // Getters és Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getNev() { return nev; }
        public void setNev(String nev) { this.nev = nev; }
        
        public String getGyarto() { return gyarto; }
        public void setGyarto(String gyarto) { this.gyarto = gyarto; }
        
        public String getAtcKod() { return atcKod; }
        public void setAtcKod(String atcKod) { this.atcKod = atcKod; }
        
        public List<String> getHatoanyagok() { return hatoanyagok; }
        public void setHatoanyagok(List<String> hatoanyagok) { this.hatoanyagok = hatoanyagok; }
        
        public Boolean getVenykoeteles() { return venykoeteles; }
        public void setVenykoeteles(Boolean venykoeteles) { this.venykoeteles = venykoeteles; }
        
        public Boolean getTamogatott() { return tamogatott; }
        public void setTamogatott(Boolean tamogatott) { this.tamogatott = tamogatott; }
        
        public String getAllapot() { return allapot; }
        public void setAllapot(String allapot) { this.allapot = allapot; }
        
        public String getGyogyszerforma() { return gyogyszerforma; }
        public void setGyogyszerforma(String gyogyszerforma) { this.gyogyszerforma = gyogyszerforma; }
        
        public String getKiszereles() { return kiszereles; }
        public void setKiszereles(String kiszereles) { this.kiszereles = kiszereles; }
        
        public String getForras() { return forras; }
        public void setForras(String forras) { this.forras = forras; }
    }
    
    /**
     * Lapozási információk.
     */
    @Schema(description = "Lapozási információk")
    public static class Lapozas {
        
        @Schema(description = "Jelenlegi oldal (0-tól kezdődik)", example = "0")
        private Integer jelenlegiOldal;
        
        @Schema(description = "Oldal mérete", example = "20")
        private Integer oldalMeret;
        
        @Schema(description = "Összes oldal száma", example = "5")
        private Integer osszesOldal;
        
        @Schema(description = "Összes elem száma", example = "98")
        private Long osszesElem;
        
        @Schema(description = "Van következő oldal", example = "true")
        private Boolean vanKovetkezo;
        
        @Schema(description = "Van előző oldal", example = "false")
        private Boolean vanElozo;
        
        @Schema(description = "Megjelenített oldal szám (1-től kezdődik)", example = "1")
        private Integer megjelenitetOldalSzam;
        
        @Schema(description = "Következő oldal száma", example = "1")
        private Integer kovetkezoOldal;
        
        @Schema(description = "Előző oldal száma")
        private Integer elozoOldal;
        
        @Schema(description = "Első oldal-e", example = "true")
        private Boolean elsoOldal;
        
        @Schema(description = "Utolsó oldal-e", example = "false")
        private Boolean utolsoOldal;
        
        @Schema(description = "Elem tartomány", example = "1-20 / 98")
        private String elemTartomany;
        
        @Schema(description = "Eltolás", example = "0")
        private Integer eltolas;
        
        // Constructors
        public Lapozas() {}
        
        // Getters és Setters
        public Integer getJelenlegiOldal() { return jelenlegiOldal; }
        public void setJelenlegiOldal(Integer jelenlegiOldal) { this.jelenlegiOldal = jelenlegiOldal; }
        
        public Integer getOldalMeret() { return oldalMeret; }
        public void setOldalMeret(Integer oldalMeret) { this.oldalMeret = oldalMeret; }
        
        public Integer getOsszesOldal() { return osszesOldal; }
        public void setOsszesOldal(Integer osszesOldal) { this.osszesOldal = osszesOldal; }
        
        public Long getOsszesElem() { return osszesElem; }
        public void setOsszesElem(Long osszesElem) { this.osszesElem = osszesElem; }
        
        public Boolean getVanKovetkezo() { return vanKovetkezo; }
        public void setVanKovetkezo(Boolean vanKovetkezo) { this.vanKovetkezo = vanKovetkezo; }
        
        public Boolean getVanElozo() { return vanElozo; }
        public void setVanElozo(Boolean vanElozo) { this.vanElozo = vanElozo; }
        
        public Integer getMegjelenitetOldalSzam() { return megjelenitetOldalSzam; }
        public void setMegjelenitetOldalSzam(Integer megjelenitetOldalSzam) { this.megjelenitetOldalSzam = megjelenitetOldalSzam; }
        
        public Integer getKovetkezoOldal() { return kovetkezoOldal; }
        public void setKovetkezoOldal(Integer kovetkezoOldal) { this.kovetkezoOldal = kovetkezoOldal; }
        
        public Integer getElozoOldal() { return elozoOldal; }
        public void setElozoOldal(Integer elozoOldal) { this.elozoOldal = elozoOldal; }
        
        public Boolean getElsoOldal() { return elsoOldal; }
        public void setElsoOldal(Boolean elsoOldal) { this.elsoOldal = elsoOldal; }
        
        public Boolean getUtolsoOldal() { return utolsoOldal; }
        public void setUtolsoOldal(Boolean utolsoOldal) { this.utolsoOldal = utolsoOldal; }
        
        public String getElemTartomany() { return elemTartomany; }
        public void setElemTartomany(String elemTartomany) { this.elemTartomany = elemTartomany; }
        
        public Integer getEltolas() { return eltolas; }
        public void setEltolas(Integer eltolas) { this.eltolas = eltolas; }
    }
    
    /**
     * Keresési információk.
     */
    @Schema(description = "Keresési információk")
    public static class KeresesiInfo {
        
        @Schema(description = "Keresett kifejezés", example = "aspirin")
        private String keresettkifejezés;
        
        @Schema(description = "Alkalmazott szűrők")
        private Object szurok;
        
        @Schema(description = "Válaszidő milliszekundumban", example = "345")
        private Long valaszIdoMs;
        
        @Schema(description = "Cache találat volt-e", example = "false")
        private Boolean cacheTalalat;
        
        @Schema(description = "Időpecsét", example = "2025-10-20T22:24:26.440Z")
        private String idopecsét;
        
        @Schema(description = "Szűrők száma", example = "0")
        private Integer szurokSzama;
        
        @Schema(description = "Gyors válasz-e", example = "true")
        private Boolean gyorsValasz;
        
        @Schema(description = "Szűrők leírása", example = "Nincsenek alkalmazott szűrők")
        private String szurokLeiras;
        
        @Schema(description = "Teljesítmény kategória", example = "Normál")
        private String teljesitmenyKategoria;
        
        // Constructors
        public KeresesiInfo() {}
        
        // Getters és Setters
        public String getKeresettkifejezés() { return keresettkifejezés; }
        public void setKeresettkifejezés(String keresettkifejezés) { this.keresettkifejezés = keresettkifejezés; }
        
        public Object getSzurok() { return szurok; }
        public void setSzurok(Object szurok) { this.szurok = szurok; }
        
        public Long getValaszIdoMs() { return valaszIdoMs; }
        public void setValaszIdoMs(Long valaszIdoMs) { this.valaszIdoMs = valaszIdoMs; }
        
        public Boolean getCacheTalalat() { return cacheTalalat; }
        public void setCacheTalalat(Boolean cacheTalalat) { this.cacheTalalat = cacheTalalat; }
        
        public String getIdopecsét() { return idopecsét; }
        public void setIdopecsét(String idopecsét) { this.idopecsét = idopecsét; }
        
        public Integer getSzurokSzama() { return szurokSzama; }
        public void setSzurokSzama(Integer szurokSzama) { this.szurokSzama = szurokSzama; }
        
        public Boolean getGyorsValasz() { return gyorsValasz; }
        public void setGyorsValasz(Boolean gyorsValasz) { this.gyorsValasz = gyorsValasz; }
        
        public String getSzurokLeiras() { return szurokLeiras; }
        public void setSzurokLeiras(String szurokLeiras) { this.szurokLeiras = szurokLeiras; }
        
        public String getTeljesitmenyKategoria() { return teljesitmenyKategoria; }
        public void setTeljesitmenyKategoria(String teljesitmenyKategoria) { this.teljesitmenyKategoria = teljesitmenyKategoria; }
    }
}