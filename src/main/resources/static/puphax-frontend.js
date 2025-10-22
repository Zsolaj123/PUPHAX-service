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
        keresesiParameterek.append('meret', '10'); // Show 10 results per page as requested
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
        const uniqueId = `drug-${gyogyszer.id || Math.random().toString(36).substr(2, 9)}`;

        return `
            <div class="gyogyszer-kártya fade-in" data-drug-id="${gyogyszer.id || 'N/A'}">
                <div class="gyogyszer-osszefoglalo" onclick="puphaxApp.toggleDrugDetails('${uniqueId}')">
                    <div class="gyogyszer-fej">
                        <div class="gyogyszer-fo-info">
                            <div class="gyogyszer-neve">${this.htmlEscape(gyogyszer.nev || 'Ismeretlen gyógyszer')}</div>
                            <div class="gyogyszer-alapadatok">
                                ${gyogyszer.gyarto ? `<span class="alapadat-elem">🏭 ${this.htmlEscape(gyogyszer.gyarto)}</span>` : ''}
                                ${gyogyszer.atcKod ? `<span class="alapadat-elem">📋 ${this.htmlEscape(gyogyszer.atcKod)}</span>` : ''}
                                ${gyogyszer.hatoanyagok && gyogyszer.hatoanyagok.length > 0 ? 
                                    `<span class="alapadat-elem">💊 ${this.htmlEscape(gyogyszer.hatoanyagok[0])}</span>` : ''}
                            </div>
                        </div>
                        <div class="gyogyszer-statusz-es-toggle">
                            <div class="gyogyszer-statusz ${statuszOsztaly}">${statuszSzoveg}</div>
                            <span class="toggle-icon" id="${uniqueId}-toggle">▼</span>
                        </div>
                    </div>
                </div>
                
                <div class="gyogyszer-reszletezett" id="${uniqueId}" style="display: none;">
                    <div class="reszletek-grid">
                        <div class="gyogyszer-reszlet">
                            <span class="reszlet-cimke">Azonosító</span>
                            <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.id || 'N/A')}</span>
                        </div>
                        <div class="gyogyszer-reszlet">
                            <span class="reszlet-cimke">Gyártó</span>
                            <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.gyarto || 'Nem megadott')}</span>
                        </div>
                        <div class="gyogyszer-reszlet">
                            <span class="reszlet-cimke">ATC Kód</span>
                            <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.atcKod || 'Nem megadott')}</span>
                        </div>
                        ${gyogyszer.tttKod ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">TTT Kód</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.tttKod)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.kiszereles ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Kiszerelés</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.kiszereles)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.torzskonyvSzam ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Törzskönyvi szám</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.torzskonyvSzam)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.venyStatus ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Vény státusz</span>
                                <span class="reszlet-ertek">${this.htmlEscape(this.venyStatusLeiras(gyogyszer.venyStatus))}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.gyogyszerforma ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Gyógyszerforma</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.gyogyszerforma)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.hatarossag ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Hatáserősség</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.hatarossag)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.ar ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Ár</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.ar)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.tamogatasSzazalek ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Támogatás</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.tamogatasSzazalek)}%</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.ervenyessegKezdete ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Érvényes ettől</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.ervenyessegKezdete)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.ervenyessegVege ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Érvényes eddig</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.ervenyessegVege)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.normativitas ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Normativitás</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.normativitas)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.tamogatasTipus ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Támogatás típusa</span>
                                <span class="reszlet-ertek">${this.htmlEscape(gyogyszer.tamogatasTipus)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.bruttoFogyasztarAr ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Bruttó fogyasztói ár</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.bruttoFogyasztarAr)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.nettoFogyasztarAr ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Nettó fogyasztói ár</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.nettoFogyasztarAr)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.termelesAr ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Termelői ár</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.termelesAr)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.nagykerAr ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Nagykereskedelmi ár</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.nagykerAr)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.tamogatottAr ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Támogatott ár</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.tamogatottAr)}</span>
                            </div>
                        ` : ''}
                        ${gyogyszer.teritesiDij ? `
                            <div class="gyogyszer-reszlet">
                                <span class="reszlet-cimke">Térítési díj</span>
                                <span class="reszlet-ertek">${this.formatPrice(gyogyszer.teritesiDij)}</span>
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
        const jelenlegiOldal = lapozas.jelenlegiOldal;
        const osszesOldal = lapozas.osszesOldal;
        const osszesElem = lapozas.osszesElem;
        const kovetkezoVan = lapozas.kovetkezoOldal !== null && lapozas.kovetkezoOldal !== undefined;
        const elozoVan = lapozas.elozoOldal !== null && lapozas.elozoOldal !== undefined;
        
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
     * Get prescription status description
     */
    venyStatusLeiras(status) {
        const statusDescriptions = {
            'VN': 'Vényköteles (normál)',
            'V5': 'Vényköteles (5x ismételhető)',
            'V1': 'Vényköteles (1x ismételhető)',
            'J': 'Különleges rendelvényen',
            'VK': 'Vény nélkül kapható',
            'SZK': 'Szakorvosi javaslat'
        };
        return statusDescriptions[status] || status;
    }

    /**
     * Toggle drug details visibility
     */
    toggleDrugDetails(drugId) {
        const detailsElement = document.getElementById(drugId);
        const toggleIcon = document.getElementById(drugId + '-toggle');
        
        if (detailsElement && toggleIcon) {
            if (detailsElement.style.display === 'none') {
                detailsElement.style.display = 'block';
                toggleIcon.textContent = '▲';
                // Add slide down animation
                detailsElement.classList.add('slide-down');
            } else {
                detailsElement.style.display = 'none';
                toggleIcon.textContent = '▼';
                detailsElement.classList.remove('slide-down');
            }
        }
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
    
    /**
     * Format price values with Ft suffix if not already present.
     */
    formatPrice(price) {
        if (!price || price === 'N/A') return price;
        
        // If price already has Ft, return as is
        if (price.includes('Ft')) {
            return price;
        }
        
        // Try to parse as number and format
        const numPrice = parseFloat(price);
        if (!isNaN(numPrice)) {
            return numPrice.toLocaleString('hu-HU') + ' Ft';
        }
        
        return price + ' Ft';
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