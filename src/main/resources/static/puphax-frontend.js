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

        // Advanced filters state
        this.filterOptions = null;
        this.selectedFilters = {
            manufacturers: new Set(),
            atcCodes: new Set(),
            productForms: new Set(),
            prescriptionTypes: new Set(),
            brands: new Set(),
            prescriptionRequired: null,
            reimbursable: null,
            inStock: null
        };

        this.inizializalasEsemenyFigyelok();
        this.apiEgeszsegEllenorzes();
        this.fetchFilterOptions();
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

        // Advanced filters toggle
        const toggleFiltersBtn = document.getElementById('toggle-filters-btn');
        toggleFiltersBtn.addEventListener('click', () => {
            this.toggleAdvancedFilters();
        });

        // Apply filters button
        const applyFiltersBtn = document.getElementById('apply-filters');
        applyFiltersBtn.addEventListener('click', () => {
            this.applyAdvancedFilters();
        });

        // Reset filters button
        const resetFiltersBtn = document.getElementById('reset-filters');
        resetFiltersBtn.addEventListener('click', () => {
            this.resetAdvancedFilters();
        });

        // Clear all filters button
        const clearAllFiltersBtn = document.getElementById('clear-all-filters');
        clearAllFiltersBtn.addEventListener('click', () => {
            this.resetAdvancedFilters();
        });

        // Boolean filter checkboxes
        document.getElementById('filter-prescription-required').addEventListener('change', (e) => {
            this.selectedFilters.prescriptionRequired = e.target.checked || null;
            this.updateFilterCounts();
        });
        document.getElementById('filter-reimbursable').addEventListener('change', (e) => {
            this.selectedFilters.reimbursable = e.target.checked || null;
            this.updateFilterCounts();
        });
        document.getElementById('filter-in-stock').addEventListener('change', (e) => {
            this.selectedFilters.inStock = e.target.checked || null;
            this.updateFilterCounts();
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

        // Build DrugSearchFilter JSON object
        const filterCriteria = {};

        // Search term (now optional - can search with filters alone)
        const keresettkifejezés = urlapAdatok.get('keresett_kifejezés')?.trim();
        if (keresettkifejezés) {
            filterCriteria.searchTerm = keresettkifejezés;
        }

        // Opcionális szűrők from form
        const gyarto = urlapAdatok.get('gyarto')?.trim();
        if (gyarto) filterCriteria.manufacturers = [gyarto];

        const atcKod = urlapAdatok.get('atc_kod')?.trim();
        if (atcKod) filterCriteria.atcCodes = [atcKod.toUpperCase()];

        // Advanced filters from multi-select
        if (this.selectedFilters.manufacturers.size > 0) {
            filterCriteria.manufacturers = Array.from(this.selectedFilters.manufacturers);
        }
        if (this.selectedFilters.atcCodes.size > 0) {
            filterCriteria.atcCodes = Array.from(this.selectedFilters.atcCodes);
        }
        if (this.selectedFilters.productForms.size > 0) {
            filterCriteria.productForms = Array.from(this.selectedFilters.productForms);
        }
        if (this.selectedFilters.prescriptionTypes.size > 0) {
            filterCriteria.prescriptionTypes = Array.from(this.selectedFilters.prescriptionTypes);
        }
        if (this.selectedFilters.brands.size > 0) {
            filterCriteria.brands = Array.from(this.selectedFilters.brands);
        }

        // Boolean filters
        if (this.selectedFilters.prescriptionRequired !== null) {
            filterCriteria.prescriptionRequired = this.selectedFilters.prescriptionRequired;
        }
        if (this.selectedFilters.reimbursable !== null) {
            filterCriteria.reimbursable = this.selectedFilters.reimbursable;
        }
        if (this.selectedFilters.inStock !== null) {
            filterCriteria.inStock = this.selectedFilters.inStock;
        }

        // Pagination and sorting
        filterCriteria.page = oldal;
        filterCriteria.size = 10;
        filterCriteria.sortBy = 'name';
        filterCriteria.sortDirection = 'ASC';

        this.jelenlegiOldal = oldal;
        this.jelenlegiKeresesiParameterek = filterCriteria;

        await this.keresesVezerlésAdvanced(filterCriteria);
    }

    /**
     * Advanced search API call using POST with JSON body.
     */
    async keresesVezerlésAdvanced(filterCriteria) {
        try {
            this.betoltesKijelzes(true);
            this.hibaElrejtes();
            this.eredmenyekElrejtes();

            const url = `${this.alapUrl}/kereses/haladó`;
            console.log('Advanced API Request:', url, filterCriteria);

            const kezdoIdo = performance.now();
            const valasz = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(filterCriteria)
            });
            const valaszIdo = performance.now() - kezdoIdo;

            if (!valasz.ok) {
                const hibaReszletek = await valasz.text();
                throw new Error(`HTTP ${valasz.status}: ${valasz.statusText} - ${hibaReszletek}`);
            }

            const adatok = await valasz.json();
            this.utolsoKeresesiValasz = adatok;

            console.log('Advanced search results:', {
                totalResults: adatok.pagination?.osszesElem || 0,
                page: (adatok.pagination?.jelenlegiOldal || 0) + 1,
                totalPages: adatok.pagination?.osszesOldal || 0,
                responseTime: `${valaszIdo.toFixed(2)}ms`,
                filters: adatok.keresesiInfo?.alkalmazottSzurok || {}
            });

            this.eredmenyekMegjelenitese(adatok);

        } catch (error) {
            console.error('Advanced search error:', error);
            this.hibaMutatasa(`Hiba a keresés során: ${error.message}`);
        } finally {
            this.betoltesKijelzes(false);
        }
    }

    /**
     * A tényleges API hívás végrehajtása gyógyszer keresésre (legacy GET method).
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

        // Check if results are from CSV fallback
        const csvFallbackBanner = this.csvFallbackErtesites(valasz);

        if (!valasz.gyogyszerek || valasz.gyogyszerek.length === 0) {
            eredmenyekTartalom.innerHTML = csvFallbackBanner + this.uresEredmenyekHtml();
        } else {
            const drugsHtml = valasz.gyogyszerek.map(gyogyszer =>
                this.gyogyszerKartyaHtml(gyogyszer)
            ).join('');
            eredmenyekTartalom.innerHTML = csvFallbackBanner + drugsHtml;
        }

        this.lapozasFreszites(valasz.lapozas);
        this.eredmenyekMutatas();
    }

    /**
     * CSV fallback értesítés banner létrehozása.
     */
    csvFallbackErtesites(valasz) {
        // Check if using CSV fallback (manufacturer="Unknown" is a good indicator)
        const usingFallback = valasz.gyogyszerek && valasz.gyogyszerek.length > 0 &&
                             valasz.gyogyszerek[0].gyarto === "Unknown";

        if (usingFallback) {
            return `
                <div class="csv-fallback-notice" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                     color: white; padding: 15px 20px; border-radius: 8px; margin-bottom: 20px;
                     border-left: 5px solid #fbbf24; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <span style="font-size: 24px;">ℹ️</span>
                        <div>
                            <strong style="font-size: 16px;">Helyi adatbázis használatban</strong>
                            <p style="margin: 5px 0 0 0; font-size: 14px; opacity: 0.95;">
                                A NEAK PUPHAX webszolgáltatás jelenleg nem elérhető.
                                Az eredmények a helyi CSV adatbázisból származnak (2007-2023 hivatalos NEAK adatok).
                                44,000+ aktuális gyógyszer adatai elérhetők.
                            </p>
                        </div>
                    </div>
                </div>
            `;
        }
        return '';
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
                                ${gyogyszer.hatoanyagNev ? `<span class="alapadat-elem">💊 ${this.htmlEscape(gyogyszer.hatoanyagNev)}</span>` : ''}
                            </div>
                        </div>
                        <div class="gyogyszer-statusz-es-toggle">
                            <div class="gyogyszer-statusz ${statuszOsztaly}">${statuszSzoveg}</div>
                            <span class="toggle-icon" id="${uniqueId}-toggle">▼</span>
                        </div>
                    </div>
                </div>

                <div class="gyogyszer-reszletezett" id="${uniqueId}" style="display: none;">

                    <!-- Core Identification Section -->
                    <div class="reszletek-szekció">
                        <h4 class="szekció-cím">📝 Alapvető azonosítás</h4>
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

    /**
     * Fetch filter options from backend API
     */
    async fetchFilterOptions() {
        try {
            const response = await fetch(`${this.alapUrl}/szurok`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            this.filterOptions = await response.json();
            console.log('Filter options loaded:', this.filterOptions);

            this.renderFilterOptions();
        } catch (error) {
            console.error('Failed to load filter options:', error);
        }
    }

    /**
     * Render filter options in the UI
     */
    renderFilterOptions() {
        if (!this.filterOptions) return;

        // Render manufacturers
        this.renderMultiSelectFilter(
            'manufacturer-options',
            this.filterOptions.gyartok || [],
            'manufacturers',
            'manufacturer-search'
        );

        // Render ATC codes
        this.renderMultiSelectFilter(
            'atc-options',
            (this.filterOptions.atcKodok || []).map(atc => atc.displayName || atc.kod),
            'atcCodes',
            'atc-search'
        );

        // Render product forms
        this.renderMultiSelectFilter(
            'form-options',
            this.filterOptions.gyogyszerformak || [],
            'productForms',
            null
        );

        // Render prescription types
        this.renderMultiSelectFilter(
            'prescription-options',
            (this.filterOptions.venyTipusok || []).map(vt => `${vt.kod} - ${vt.leiras}`),
            'prescriptionTypes',
            null
        );

        // Render brands
        this.renderMultiSelectFilter(
            'brand-options',
            this.filterOptions.markak || [],
            'brands',
            'brand-search'
        );
    }

    /**
     * Render a multi-select filter with checkboxes
     */
    renderMultiSelectFilter(containerId, options, filterKey, searchInputId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        const limit = 50; // Show first 50 options
        const displayOptions = options.slice(0, limit);

        container.innerHTML = displayOptions.map(option => `
            <div class="filter-option" data-filter="${filterKey}" data-value="${this.htmlEscape(option)}">
                <input type="checkbox" id="${filterKey}-${this.generateId(option)}"
                       value="${this.htmlEscape(option)}"
                       onchange="puphaxApp.toggleFilterOption('${filterKey}', this.value, this.checked)">
                <label class="filter-option-label" for="${filterKey}-${this.generateId(option)}">
                    ${this.htmlEscape(option)}
                </label>
            </div>
        `).join('');

        // Add search functionality if searchInputId is provided
        if (searchInputId) {
            const searchInput = document.getElementById(searchInputId);
            if (searchInput) {
                searchInput.addEventListener('input', (e) => {
                    this.filterOptionsList(containerId, e.target.value, filterKey);
                });
            }
        }
    }

    /**
     * Filter options list based on search term
     */
    filterOptionsList(containerId, searchTerm, filterKey) {
        const container = document.getElementById(containerId);
        const options = container.querySelectorAll('.filter-option');
        const lowerSearchTerm = searchTerm.toLowerCase();

        options.forEach(option => {
            const label = option.querySelector('.filter-option-label').textContent.toLowerCase();
            option.style.display = label.includes(lowerSearchTerm) ? 'flex' : 'none';
        });
    }

    /**
     * Toggle filter option selection
     */
    toggleFilterOption(filterKey, value, checked) {
        if (checked) {
            this.selectedFilters[filterKey].add(value);
        } else {
            this.selectedFilters[filterKey].delete(value);
        }

        this.updateFilterCounts();
    }

    /**
     * Update filter counts in UI
     */
    updateFilterCounts() {
        const counts = {
            manufacturers: this.selectedFilters.manufacturers.size,
            atcCodes: this.selectedFilters.atcCodes.size,
            productForms: this.selectedFilters.productForms.size,
            prescriptionTypes: this.selectedFilters.prescriptionTypes.size,
            brands: this.selectedFilters.brands.size
        };

        // Update individual counts
        Object.entries(counts).forEach(([key, count]) => {
            const countElement = document.getElementById(`${key.replace(/([A-Z])/g, '-$1').toLowerCase()}-count`);
            if (countElement) {
                countElement.textContent = count > 0 ? count : '';
                countElement.style.display = count > 0 ? 'inline-block' : 'none';
            }
        });

        // Update total count badge
        const totalCount = Object.values(counts).reduce((sum, count) => sum + count, 0) +
                          (this.selectedFilters.prescriptionRequired ? 1 : 0) +
                          (this.selectedFilters.reimbursable ? 1 : 0) +
                          (this.selectedFilters.inStock ? 1 : 0);

        const badge = document.getElementById('filter-count-badge');
        if (badge) {
            badge.textContent = totalCount;
            badge.style.display = totalCount > 0 ? 'inline-block' : 'none';
        }

        // Update active filters display
        this.renderActiveFilters();
    }

    /**
     * Render active filters as badges
     */
    renderActiveFilters() {
        const activeFiltersContainer = document.getElementById('active-filters');
        const activeFiltersList = document.getElementById('active-filters-list');

        const badges = [];

        // Add multi-select filter badges
        ['manufacturers', 'atcCodes', 'productForms', 'prescriptionTypes', 'brands'].forEach(key => {
            this.selectedFilters[key].forEach(value => {
                badges.push(this.createFilterBadge(key, value));
            });
        });

        // Add boolean filter badges
        if (this.selectedFilters.prescriptionRequired) {
            badges.push(this.createFilterBadge('prescriptionRequired', 'Vényköteles'));
        }
        if (this.selectedFilters.reimbursable) {
            badges.push(this.createFilterBadge('reimbursable', 'Támogatott'));
        }
        if (this.selectedFilters.inStock) {
            badges.push(this.createFilterBadge('inStock', 'Raktáron'));
        }

        activeFiltersList.innerHTML = badges.join('');
        activeFiltersContainer.style.display = badges.length > 0 ? 'block' : 'none';
    }

    /**
     * Create filter badge HTML
     */
    createFilterBadge(filterKey, value) {
        return `
            <div class="active-filter-badge">
                <span>${this.htmlEscape(value)}</span>
                <button class="remove-filter-btn" onclick="puphaxApp.removeFilter('${filterKey}', '${this.htmlEscape(value)}')">
                    ×
                </button>
            </div>
        `;
    }

    /**
     * Remove a single filter
     */
    removeFilter(filterKey, value) {
        if (filterKey === 'prescriptionRequired' || filterKey === 'reimbursable' || filterKey === 'inStock') {
            this.selectedFilters[filterKey] = null;
            document.getElementById(`filter-${filterKey.replace(/([A-Z])/g, '-$1').toLowerCase()}`).checked = false;
        } else {
            this.selectedFilters[filterKey].delete(value);
            const checkbox = document.querySelector(`input[value="${value}"][data-filter="${filterKey}"]`);
            if (checkbox) checkbox.checked = false;
        }

        this.updateFilterCounts();
    }

    /**
     * Toggle advanced filters panel
     */
    toggleAdvancedFilters() {
        const panel = document.getElementById('advanced-filters-panel');
        const icon = document.getElementById('toggle-icon');

        if (panel.style.display === 'none') {
            panel.style.display = 'block';
            icon.classList.add('open');
        } else {
            panel.style.display = 'none';
            icon.classList.remove('open');
        }
    }

    /**
     * Apply advanced filters to search
     */
    async applyAdvancedFilters() {
        await this.keresesVegrehajtasa(0);

        // Close filters panel
        const panel = document.getElementById('advanced-filters-panel');
        const icon = document.getElementById('toggle-icon');
        panel.style.display = 'none';
        icon.classList.remove('open');
    }

    /**
     * Reset all advanced filters
     */
    resetAdvancedFilters() {
        // Clear all selected filters
        this.selectedFilters = {
            manufacturers: new Set(),
            atcCodes: new Set(),
            productForms: new Set(),
            prescriptionTypes: new Set(),
            brands: new Set(),
            prescriptionRequired: null,
            reimbursable: null,
            inStock: null
        };

        // Uncheck all checkboxes
        document.querySelectorAll('.filter-option input[type="checkbox"]').forEach(cb => cb.checked = false);
        document.querySelectorAll('.boolean-filters input[type="checkbox"]').forEach(cb => cb.checked = false);

        // Update counts
        this.updateFilterCounts();
    }

    /**
     * Generate a simple ID from text
     */
    generateId(text) {
        return text.replace(/[^a-z0-9]/gi, '-').toLowerCase().substring(0, 50);
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