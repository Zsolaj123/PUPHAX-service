package com.puphax.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Egészségügyi állapot információk magyar terminológiával.
 * PUPHAX szolgáltatás és komponensek állapotát tartalmazza.
 */
@Schema(description = "Egészségügyi állapot információk")
public class EgeszsegStatu {
    
    @Schema(description = "Szolgáltatás állapota", required = true, 
            allowableValues = {"FEL", "LEALL", "ROMLOTT"}, 
            example = "FEL")
    @NotNull
    private String statu;
    
    @Schema(description = "Állapot üzenet", required = true, 
            example = "Minden szolgáltatás működik")
    @NotNull
    private String uzenet;
    
    @Schema(description = "Ellenőrzés időpecsétje", required = true)
    @NotNull
    private Instant idopecsét;
    
    @Schema(description = "Komponensek állapota")
    private Map<String, KomponensStatu> komponensek;
    
    @Schema(description = "Metrikák")
    private Map<String, Object> metrikak;
    
    @Schema(description = "Válaszidő milliszekundumban", example = "123")
    private Long valaszIdoMs;
    
    @Schema(description = "Verzió információ", example = "1.0.0")
    private String verzio;
    
    // Constructors
    public EgeszsegStatu() {
        this.idopecsét = Instant.now();
    }
    
    public EgeszsegStatu(String statu, String uzenet) {
        this.statu = statu;
        this.uzenet = uzenet;
        this.idopecsét = Instant.now();
    }
    
    public EgeszsegStatu(String statu, String uzenet, Map<String, KomponensStatu> komponensek) {
        this.statu = statu;
        this.uzenet = uzenet;
        this.komponensek = komponensek;
        this.idopecsét = Instant.now();
    }
    
    // Getters és Setters
    public String getStatu() {
        return statu;
    }
    
    public void setStatu(String statu) {
        this.statu = statu;
    }
    
    public String getUzenet() {
        return uzenet;
    }
    
    public void setUzenet(String uzenet) {
        this.uzenet = uzenet;
    }
    
    public Instant getIdopecsét() {
        return idopecsét;
    }
    
    public void setIdopecsét(Instant idopecsét) {
        this.idopecsét = idopecsét;
    }
    
    public Map<String, KomponensStatu> getKomponensek() {
        return komponensek;
    }
    
    public void setKomponensek(Map<String, KomponensStatu> komponensek) {
        this.komponensek = komponensek;
    }
    
    public Map<String, Object> getMetrikak() {
        return metrikak;
    }
    
    public void setMetrikak(Map<String, Object> metrikak) {
        this.metrikak = metrikak;
    }
    
    public Long getValaszIdoMs() {
        return valaszIdoMs;
    }
    
    public void setValaszIdoMs(Long valaszIdoMs) {
        this.valaszIdoMs = valaszIdoMs;
    }
    
    public String getVerzio() {
        return verzio;
    }
    
    public void setVerzio(String verzio) {
        this.verzio = verzio;
    }
    
    /**
     * Komponens állapot információk.
     */
    @Schema(description = "Komponens állapot")
    public static class KomponensStatu {
        
        @Schema(description = "Komponens állapota", 
                allowableValues = {"FEL", "LEALL", "ROMLOTT"}, 
                example = "FEL")
        private String statu;
        
        @Schema(description = "Komponens üzenet", example = "PUPHAX kapcsolat aktív")
        private String uzenet;
        
        @Schema(description = "Válaszidő milliszekundumban", example = "45")
        private Long valaszIdoMs;
        
        @Schema(description = "Hiba részletek")
        private String hibaReszletek;
        
        @Schema(description = "Utolsó sikeres ellenőrzés")
        private Instant utolsoSikeres;
        
        @Schema(description = "Ellenőrzés számlálója", example = "1234")
        private Long ellenorzesekSzama;
        
        @Schema(description = "Sikeres ellenőrzések száma", example = "1200")
        private Long sikeresEllenorzesek;
        
        // Constructors
        public KomponensStatu() {}
        
        public KomponensStatu(String statu, String uzenet) {
            this.statu = statu;
            this.uzenet = uzenet;
        }
        
        public KomponensStatu(String statu, String uzenet, Long valaszIdoMs) {
            this.statu = statu;
            this.uzenet = uzenet;
            this.valaszIdoMs = valaszIdoMs;
        }
        
        // Getters és Setters
        public String getStatu() {
            return statu;
        }
        
        public void setStatu(String statu) {
            this.statu = statu;
        }
        
        public String getUzenet() {
            return uzenet;
        }
        
        public void setUzenet(String uzenet) {
            this.uzenet = uzenet;
        }
        
        public Long getValaszIdoMs() {
            return valaszIdoMs;
        }
        
        public void setValaszIdoMs(Long valaszIdoMs) {
            this.valaszIdoMs = valaszIdoMs;
        }
        
        public String getHibaReszletek() {
            return hibaReszletek;
        }
        
        public void setHibaReszletek(String hibaReszletek) {
            this.hibaReszletek = hibaReszletek;
        }
        
        public Instant getUtolsoSikeres() {
            return utolsoSikeres;
        }
        
        public void setUtolsoSikeres(Instant utolsoSikeres) {
            this.utolsoSikeres = utolsoSikeres;
        }
        
        public Long getEllenorzesekSzama() {
            return ellenorzesekSzama;
        }
        
        public void setEllenorzesekSzama(Long ellenorzesekSzama) {
            this.ellenorzesekSzama = ellenorzesekSzama;
        }
        
        public Long getSikeresEllenorzesek() {
            return sikeresEllenorzesek;
        }
        
        public void setSikeresEllenorzesek(Long sikeresEllenorzesek) {
            this.sikeresEllenorzesek = sikeresEllenorzesek;
        }
    }
}