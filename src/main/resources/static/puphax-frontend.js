/**
 * PUPHAX Gyógyszer Kereső Frontend Alkalmazás
 * 
 * Ez a JavaScript alkalmazás interaktív felületet biztosít a magyar PUPHAX REST API 
 * végpontok teszteléséhez valós idejű kereséssel, szűréssel és lapozással.
 */

class PuphaxGyogyszerKerreso {
    constructor() {
        this.alapUrl = '/api/v1/gyogyszerek';
        this.jelenlegiOldal = 0;
        this.jelenlegiKeresesiParameterek = {};
        this.utolsoKeresesiValasz = null;
        
        this.inizializalasEsemenyFigyelok();
        this.apiEgeszsegEllenorzes();
    }

    /**
     * Az alkalmazás összes eseményfigyelőjének inicializálása.
     */
    inizializalasEsemenyFigyelok() {
        // Keresési űrlap elküldése
        const keresesiUrlap = document.getElementById('gyogyszer-kereses-form');
        keresesiUrlap.addEventListener('submit', (e) => {
            e.preventDefault();
            this.keresesVegrehajtasa();
        });

        // Valós idejű validáció ATC kódra
        const atcKodMezo = document.getElementById('atc-kod');
        atcKodMezo.addEventListener('input', (e) => {
            this.atcKodValidalasa(e.target);
        });

        // Automatikus keresés Enter billentyűre bármely mezőben
        const mezok = keresesiUrlap.querySelectorAll('input, select');
        mezok.forEach(mezo => {
            mezo.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && mezo.type !== 'submit') {
                    e.preventDefault();
                    this.keresesVegrehajtasa();
                }
            });
        });

        // Törlés gomb eseményfigyelő
        const torlesGomb = document.getElementById('torlese-gomb');
        torlesGomb.addEventListener('click', () => {
            this.urlapTorlese();
        });
    }

    /**
     * API egészségének ellenőrzése oldal betöltéskor.
     */
    async apiEgeszsegEllenorzes() {
        try {
            const valasz = await fetch(`${this.alapUrl}/egeszseg`);
            const eredmeny = await valasz.json();
            console.log('API Egészség Ellenőrzés:', eredmeny);
        } catch (hiba) {
            console.warn('API egészség ellenőrzés sikertelen:', hiba);
            this.hibaMutatasa('Figyelem: Nem sikerült kapcsolódni a PUPHAX API-hoz. Kérjük, győződjön meg róla, hogy a backend szolgáltatás fut.');
        }
    }

    /**
     * ATC kód formátum validálása valós időben.
     */
    atcKodValidalasa(mezo) {
        const atcMinta = /^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$/;
        const ertek = mezo.value.toUpperCase();
        
        if (ertek && !atcMinta.test(ertek)) {
            mezo.setCustomValidity('Az ATC kód formátuma: A10AB01 (pl: N02BA01)');
        } else {
            mezo.setCustomValidity('');
        }
        
        // Automatikus formázás nagybetűre
        if (mezo.value !== ertek) {
            mezo.value = ertek;
        }
    }

    /**
     * Űrlap adatok összegyűjtése és gyógyszer keresés végrehajtása.
     */
    async keresesVegrehajtasa(oldal = 0) {
        const urlapAdatok = new FormData(document.getElementById('gyogyszer-kereses-form'));
        const keresesiParameterek = new URLSearchParams();

        // Kötelező kifejezés
        const keresettkifejezés = urlapAdatok.get('keresett_kifejezés')?.trim();
        if (!keresettkifejezés) {
            this.hibaMutatasa('Kérjük, adjon meg egy gyógyszer nevet a kereséshez.');
            return;
        }
        keresesiParameterek.append('keresett_kifejezés', keresettkifejezés);

        // Opcionális szűrők
        const gyarto = urlapAdatok.get('gyarto')?.trim();
        if (gyarto) keresesiParameterek.append('gyarto', gyarto);

        const atcKod = urlapAdatok.get('atc_kod')?.trim();
        if (atcKod) keresesiParameterek.append('atc_kod', atcKod.toUpperCase());

        // Lapozás és rendezés
        keresesiParameterek.append('oldal', oldal.toString());
        keresesiParameterek.append('meret', '20');
        keresesiParameterek.append('rendezes', 'nev');
        keresesiParameterek.append('irany', 'ASC');

        this.jelenlegiOldal = oldal;
        this.jelenlegiKeresesiParameterek = Object.fromEntries(keresesiParameterek.entries());

        await this.keresesVezerles(keresesiParameterek);
    }

    /**
     * A tényleges API hívás végrehajtása gyógyszer keresésre.
     */
    async keresesVezerles(keresesiParameterek) {
        try {
            this.betoltesKijelzes(true);
            this.hibaElrejtes();
            this.eredmenyekElrejtes();

            const url = `${this.alapUrl}/kereses?${keresesiParameterek.toString()}`;
            console.log('API Kérés:', url);

            const kezdoIdo = performance.now();
            const valasz = await fetch(url);
            const valaszIdo = performance.now() - kezdoIdo;

            if (!valasz.ok) {
                throw new Error(`HTTP ${valasz.status}: ${valasz.statusText}`);
            }

            const eredmeny = await valasz.json();
            this.utolsoKeresesiValasz = eredmeny;

            console.log('API Válasz:', eredmeny);
            
            this.eredmenyekMegjelenites(eredmeny, valaszIdo);
            this.keresesiInformacioMutatas(eredmeny, valaszIdo);

        } catch (hiba) {
            console.error('Keresés sikertelen:', hiba);
            this.hibaMutatasa(`Keresés sikertelen: ${hiba.message}`);
        } finally {
            this.betoltesKijelzes(false);
        }
    }

    /**
     * Keresési eredmények megjelenítése a felhasználói felületen.
     */
    eredmenyekMegjelenites(valasz, valaszIdo) {
        const eredmenyekTartalom = document.getElementById('eredmenyek-tartalom');

        if (!valasz.gyogyszerek || valasz.gyogyszerek.length === 0) {
            eredmenyekTartalom.innerHTML = this.uresEredmenyekHtml();
        } else {
            eredmenyekTartalom.innerHTML = valasz.gyogyszerek.map(gyogyszer => 
                this.gyogyszerKartyaHtml(gyogyszer)
            ).join('');
        }

        this.lapozasFreszites(valasz.lapozas);
        this.eredmenyekMutatas();
    }

    /**
     * Egyetlen gyógyszer elem HTML létrehozása.
     */
    gyogyszerKartyaHtml(gyogyszer) {
        const statuszOsztaly = gyogyszer.statusz ? gyogyszer.statusz.toLowerCase() : 'aktiv';
        const statuszSzoveg = this.statuszForditasa(gyogyszer.statusz || 'AKTIV');

        return `
            <div class="gyogyszer-kártya fade-in">
                <div class="gyogyszer-fej">
                    <div>
                        <div class="gyogyszer-neve">${this.htmlEscape(gyogyszer.nev || 'Ismeretlen gyógyszer')}</div>
                        <div class="gyogyszer-azonosito">ID: ${this.htmlEscape(gyogyszer.azonosito || 'N/A')}</div>
                    </div>
                    <div class="gyogyszer-statusz ${statuszOsztaly}">${statuszSzoveg}</div>
                </div>
                
                <div class="gyogyszer-reszletek">
                    <div class="gyogyszer-reszlet">
                        <span class="reszlet-cimke">Gyártó</span>
                        <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.gyarto || 'Nem megadott')}</span>
                    </div>
                    <div class="gyogyszer-reszlet">
                        <span class="reszlet-cimke">ATC Kód</span>
                        <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.atcKod || 'Nem megadott')}</span>
                    </div>
                    ${gyogyszer.hatoanyag ? `
                        <div class="gyogyszer-reszlet">
                            <span class="reszlet-cimke">Hatóanyag</span>
                            <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.hatoanyag)}</span>
                        </div>
                    ` : ''}
                    ${gyogyszer.forma ? `
                        <div class="gyogyszer-reszlet">
                            <span class="reszlet-cimke">Forma</span>
                            <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.forma)}</span>
                        </div>
                    ` : ''}
                </div>

                ${gyogyszer.hatoanyagok && gyogyszer.hatoanyagok.length > 0 ? `
                    <div class="hatoanyagok">
                        <span class="reszlet-cimke">Hatóanyagok</span>
                        <div class="hatoanyagok-lista">
                            ${gyogyszer.hatoanyagok.map(hatoanyag => 
                                `<span class="hatoanyag-cimke">${this.htmlEscape(hatoanyag)}</span>`
                            ).join('')}
                        </div>
                    </div>
                ` : ''}

                <div class="gyogyszer-jelzok">
                    ${gyogyszer.venykoeteles ? '<span class="gyogyszer-jelzo venykoeteles">⚕️ Vényköteles</span>' : ''}
                    ${gyogyszer.tamogatott ? '<span class="gyogyszer-jelzo tamogatott">💰 Támogatott</span>' : ''}
                    ${gyogyszer.generikus ? '<span class="gyogyszer-jelzo generikus">🔄 Generikus</span>' : ''}
                </div>
            </div>
        `;
    }

    /**
     * Üres eredmények HTML létrehozása.
     */
    uresEredmenyekHtml() {
        return `
            <div class="gyogyszer-kártya" style="text-align: center; padding: 60px 40px;">
                <div style="font-size: 4rem; margin-bottom: 20px;">🔍</div>
                <div style="font-size: 1.4rem; font-weight: 600; margin-bottom: 12px; color: var(--text-primary);">
                    Nem találtunk gyógyszereket
                </div>
                <div style="color: var(--text-secondary); font-size: 1.1rem; line-height: 1.6;">
                    Próbálja meg módosítani a keresési feltételeket vagy szűrőket.<br>
                    Ellenőrizze a helyesírást és próbálkozzon egyszerűbb kifejezésekkel.
                </div>
            </div>
        `;
    }

    /**
     * Lapozás vezérlők frissítése.
     */
    lapozasFreszites(lapozas) {
        const lapozasElem = document.getElementById('lapozas');
        
        if (lapozas && lapozas.osszesOldal > 1) {
            lapozasElem.innerHTML = this.lapozasHtml(lapozas);
            lapozasElem.style.display = 'block';
        } else {
            lapozasElem.style.display = 'none';
        }
    }

    /**
     * Lapozás HTML létrehozása.
     */
    lapozasHtml(lapozas) {
        const { jelenlegiOldal, osszesOldal, osszesElem, kovetkezoVan, elozoVan } = lapozas;
        
        return `
            <div class="lapozas-info">
                ${this.formatumTartomany(lapozas)} / ${osszesElem.toLocaleString('hu-HU')} eredmény
            </div>
            <div class="lapozas-gombok">
                <button class="lap-gomb" ${!elozoVan ? 'disabled' : ''} 
                        onclick="puphaxApp.oldalraUgras(${jelenlegiOldal - 1})">
                    ← Előző
                </button>
                
                ${this.oldalszamGombok(jelenlegiOldal, osszesOldal)}
                
                <button class="lap-gomb" ${!kovetkezoVan ? 'disabled' : ''} 
                        onclick="puphaxApp.oldalraUgras(${jelenlegiOldal + 1})">
                    Következő →
                </button>
            </div>
        `;
    }

    /**
     * Oldalszám gombok létrehozása.
     */
    oldalszamGombok(jelenlegiOldal, osszesOldal) {
        const gombok = [];
        const maxLathatoOldal = 5;
        
        let kezdoOldal = Math.max(0, jelenlegiOldal - Math.floor(maxLathatoOldal / 2));
        let vegsoOldal = Math.min(osszesOldal - 1, kezdoOldal + maxLathatoOldal - 1);
        
        // Kezdő oldal módosítása, ha a végéhez közeledünk
        if (vegsoOldal - kezdoOldal < maxLathatoOldal - 1) {
            kezdoOldal = Math.max(0, vegsoOldal - maxLathatoOldal + 1);
        }

        for (let i = kezdoOldal; i <= vegsoOldal; i++) {
            const jelenlegiOldalE = i === jelenlegiOldal;
            gombok.push(`
                <button class="lap-gomb ${jelenlegiOldalE ? 'aktualis' : ''}" 
                        onclick="puphaxApp.oldalraUgras(${i})" ${jelenlegiOldalE ? 'disabled' : ''}>
                    ${i + 1}
                </button>
            `);
        }

        return gombok.join('');
    }

    /**
     * Navigálás egy meghatározott oldalra.
     */
    async oldalraUgras(oldal) {
        if (oldal < 0 || !this.utolsoKeresesiValasz) return;
        
        const osszesOldal = this.utolsoKeresesiValasz.lapozas.osszesOldal;
        if (oldal >= osszesOldal) return;

        await this.keresesVegrehajtasa(oldal);
    }

    /**
     * Keresési információ és statisztikák megjelenítése.
     */
    keresesiInformacioMutatas(valasz, valaszIdo) {
        const eredmenyekInfo = document.getElementById('eredmenyek-info');
        
        const gyogyszerSzam = valasz.gyogyszerek.length;
        const osszesszam = valasz.lapozas.osszesElem;
        
        eredmenyekInfo.innerHTML = `
            <span>Találatok: ${osszesszam.toLocaleString('hu-HU')} (megjelenítve: ${gyogyszerSzam})</span>
            <span>Válaszidő: ${Math.round(valaszIdo)}ms</span>
            <span>${valasz.keresesiInfo?.gyorsitotarHit ? '⚡ Gyorsítótárból' : '🌐 Friss adat'}</span>
        `;
    }

    /**
     * Tartomány formázása a lapozási információhoz.
     */
    formatumTartomany(lapozas) {
        const { jelenlegiOldal, oldalMeret, osszesElem } = lapozas;
        const kezdo = jelenlegiOldal * oldalMeret + 1;
        const veg = Math.min((jelenlegiOldal + 1) * oldalMeret, osszesElem);
        return `${kezdo.toLocaleString('hu-HU')}-${veg.toLocaleString('hu-HU')}`;
    }

    /**
     * Betöltési jelző megjelenítése/elrejtése.
     */
    betoltesKijelzes(mutatas) {
        const keresésGomb = document.getElementById('kereses-gomb');
        const betoltesSpan = keresésGomb.querySelector('.btn-loader');
        const szovegSpan = keresésGomb.querySelector('.btn-text');
        
        if (mutatas) {
            betoltesSpan.style.display = 'inline-flex';
            szovegSpan.style.display = 'none';
            keresésGomb.disabled = true;
        } else {
            betoltesSpan.style.display = 'none';
            szovegSpan.style.display = 'inline-flex';
            keresésGomb.disabled = false;
        }
    }

    /**
     * Eredmények tároló megjelenítése.
     */
    eredmenyekMutatas() {
        const eredmenyekSzekcio = document.getElementById('eredmenyek-szekcio');
        eredmenyekSzekcio.style.display = 'block';
        eredmenyekSzekcio.classList.add('slide-down');
    }

    /**
     * Eredmények elrejtése.
     */
    eredmenyekElrejtes() {
        const eredmenyekSzekcio = document.getElementById('eredmenyek-szekcio');
        eredmenyekSzekcio.style.display = 'none';
    }

    /**
     * Hiba üzenet megjelenítése.
     */
    hibaMutatasa(uzenet) {
        const hibaSzekcio = document.getElementById('hiba-szekcio');
        const hibaUzenet = document.getElementById('hiba-uzenet');
        
        hibaUzenet.textContent = uzenet;
        hibaSzekcio.style.display = 'block';
        hibaSzekcio.classList.add('slide-down');
    }

    /**
     * Hiba üzenet elrejtése.
     */
    hibaElrejtes() {
        const hibaSzekcio = document.getElementById('hiba-szekcio');
        hibaSzekcio.style.display = 'none';
    }

    /**
     * Űrlap törlése.
     */
    urlapTorlese() {
        const urlap = document.getElementById('gyogyszer-kereses-form');
        urlap.reset();
        this.eredmenyekElrejtes();
        this.hibaElrejtes();
        
        // Fókusz visszaállítása a keresési mezőre
        const keresettkifejezesMezo = document.getElementById('keresett-kifejezes');
        keresettkifejezesMezo.focus();
    }

    /**
     * Státusz fordítása magyarra.
     */
    statuszForditasa(statusz) {
        const forditas = {
            'ACTIVE': 'Aktív',
            'SUSPENDED': 'Felfüggesztett', 
            'WITHDRAWN': 'Visszavont',
            'EXPIRED': 'Lejárt',
            'PENDING': 'Függőben'
        };
        return forditas[statusz] || statusz;
    }

    /**
     * HTML escape XSS megelőzésre.
     */
    htmlEscape(szoveg) {
        if (!szoveg) return '';
        const div = document.createElement('div');
        div.textContent = szoveg;
        return div.innerHTML;
    }
}

/**
 * Újra keresés függvény globális használatra.
 */
function ujraKereses() {
    if (window.puphaxApp) {
        puphaxApp.keresesVegrehajtasa();
    }
}

// Az alkalmazás inicializálása amikor a DOM betöltődött
document.addEventListener('DOMContentLoaded', () => {
    window.puphaxApp = new PuphaxGyogyszerKerreso();
});

// Teszt keresések példákhoz
window.tesztKeresések = {
    aspirin: () => {
        document.getElementById('keresett-kifejezes').value = 'aspirin';
        puphaxApp.keresesVegrehajtasa();
    },
    
    richter: () => {
        document.getElementById('keresett-kifejezes').value = 'aspirin';
        document.getElementById('gyarto').value = 'Richter';
        puphaxApp.keresesVegrehajtasa();
    },
    
    atcKod: () => {
        document.getElementById('keresett-kifejezes').value = 'aspirin';
        document.getElementById('atc-kod').value = 'N02BA01';
        puphaxApp.keresesVegrehajtasa();
    },
    
    teljesKereses: () => {
        document.getElementById('keresett-kifejezes').value = 'paracetamol';
        document.getElementById('gyarto').value = 'TEVA';
        document.getElementById('atc-kod').value = 'N02BE01';
        puphaxApp.keresesVegrehajtasa();
    }
};

console.log('🏥 PUPHAX Gyógyszer Kereső Felület betöltve!');
console.log('💡 Próbálja ki ezeket a teszt kereséseket:', Object.keys(window.tesztKeresések));
console.log('📋 Példa: tesztKeresések.aspirin()');