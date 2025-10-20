/**
 * PUPHAX Drug Search Frontend Application
 * 
 * This JavaScript application provides an interactive interface for testing
 * the PUPHAX REST API endpoints with real-time search, filtering, and pagination.
 */

class PuphaxApp {
    constructor() {
        this.baseUrl = '/api/v1/drugs';
        this.currentPage = 0;
        this.currentSearchParams = {};
        this.lastSearchResponse = null;
        
        this.initializeEventListeners();
        this.checkApiHealth();
    }

    /**
     * Initialize all event listeners for the application.
     */
    initializeEventListeners() {
        // Search form submission
        const searchForm = document.getElementById('searchForm');
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.performSearch();
        });

        // Real-time validation for ATC code
        const atcCodeInput = document.getElementById('atcCode');
        atcCodeInput.addEventListener('input', (e) => {
            this.validateAtcCode(e.target);
        });

        // Auto-search on Enter in any input field
        const inputs = searchForm.querySelectorAll('input, select');
        inputs.forEach(input => {
            input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && input.type !== 'submit') {
                    e.preventDefault();
                    this.performSearch();
                }
            });
        });
    }

    /**
     * Check API health status on page load.
     */
    async checkApiHealth() {
        try {
            const response = await fetch(`${this.baseUrl}/health`);
            const result = await response.json();
            console.log('API Health Check:', result);
        } catch (error) {
            console.warn('API health check failed:', error);
            this.showError('Warning: Could not connect to PUPHAX API. Please ensure the backend service is running.');
        }
    }

    /**
     * Validate ATC code format in real-time.
     */
    validateAtcCode(input) {
        const atcPattern = /^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$/;
        const value = input.value.toUpperCase();
        
        if (value && !atcPattern.test(value)) {
            input.setCustomValidity('ATC code must follow format: A10AB01 (e.g., N02BA01)');
        } else {
            input.setCustomValidity('');
        }
        
        // Auto-format to uppercase
        if (input.value !== value) {
            input.value = value;
        }
    }

    /**
     * Collect form data and perform drug search.
     */
    async performSearch(page = 0) {
        const formData = new FormData(document.getElementById('searchForm'));
        const searchParams = new URLSearchParams();

        // Required term
        const term = formData.get('term')?.trim();
        if (!term) {
            this.showError('Please enter a drug name to search.');
            return;
        }
        searchParams.append('term', term);

        // Optional filters
        const manufacturer = formData.get('manufacturer')?.trim();
        if (manufacturer) searchParams.append('manufacturer', manufacturer);

        const atcCode = formData.get('atcCode')?.trim();
        if (atcCode) searchParams.append('atcCode', atcCode.toUpperCase());

        // Pagination and sorting
        searchParams.append('page', page.toString());
        searchParams.append('size', formData.get('size') || '20');
        searchParams.append('sortBy', formData.get('sortBy') || 'name');
        searchParams.append('sortDirection', formData.get('sortDirection') || 'ASC');

        this.currentPage = page;
        this.currentSearchParams = Object.fromEntries(searchParams.entries());

        await this.executeSearch(searchParams);
    }

    /**
     * Execute the actual API call to search for drugs.
     */
    async executeSearch(searchParams) {
        try {
            this.showLoading(true);
            this.hideError();
            this.hideResults();

            const url = `${this.baseUrl}/search?${searchParams.toString()}`;
            console.log('API Request:', url);

            const startTime = performance.now();
            const response = await fetch(url);
            const responseTime = performance.now() - startTime;

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();
            this.lastSearchResponse = result;

            console.log('API Response:', result);
            
            this.displayResults(result, responseTime);
            this.showSearchInfo(result, responseTime);

        } catch (error) {
            console.error('Search failed:', error);
            this.showError(`Search failed: ${error.message}`);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Display search results in the UI.
     */
    displayResults(response, responseTime) {
        const resultsContainer = document.getElementById('resultsContainer');
        const drugsList = document.getElementById('drugsList');

        if (!response.drugs || response.drugs.length === 0) {
            drugsList.innerHTML = this.createEmptyResultsHtml();
        } else {
            drugsList.innerHTML = response.drugs.map(drug => this.createDrugItemHtml(drug)).join('');
        }

        this.updatePagination(response.pagination);
        this.showResults();
    }

    /**
     * Create HTML for a single drug item.
     */
    createDrugItemHtml(drug) {
        const statusClass = drug.status ? drug.status.toLowerCase() : 'active';
        const statusText = drug.status || 'ACTIVE';

        return `
            <div class="drug-item">
                <div class="drug-header">
                    <div>
                        <div class="drug-name">${this.escapeHtml(drug.name || 'Unknown Drug')}</div>
                        <div class="drug-id">ID: ${this.escapeHtml(drug.id || 'N/A')}</div>
                    </div>
                    <div class="drug-status ${statusClass}">${statusText}</div>
                </div>
                
                <div class="drug-details">
                    <div class="drug-detail">
                        <span class="drug-detail-label">Manufacturer</span>
                        <span class="drug-detail-value">${this.escapeHtml(drug.manufacturer || 'Not specified')}</span>
                    </div>
                    <div class="drug-detail">
                        <span class="drug-detail-label">ATC Code</span>
                        <span class="drug-detail-value">${this.escapeHtml(drug.atcCode || 'Not specified')}</span>
                    </div>
                </div>

                ${drug.activeIngredients && drug.activeIngredients.length > 0 ? `
                    <div class="drug-ingredients">
                        <span class="drug-detail-label">Active Ingredients</span>
                        <div class="ingredients-list">
                            ${drug.activeIngredients.map(ingredient => 
                                `<span class="ingredient-tag">${this.escapeHtml(ingredient)}</span>`
                            ).join('')}
                        </div>
                    </div>
                ` : ''}

                <div class="drug-flags">
                    ${drug.prescriptionRequired ? '<span class="drug-flag prescription">⚕️ Prescription Required</span>' : ''}
                    ${drug.reimbursable ? '<span class="drug-flag reimbursable">💰 Reimbursable</span>' : ''}
                </div>
            </div>
        `;
    }

    /**
     * Create HTML for empty results.
     */
    createEmptyResultsHtml() {
        return `
            <div class="drug-item" style="text-align: center; padding: 40px;">
                <div style="font-size: 3rem; margin-bottom: 15px;">🔍</div>
                <div style="font-size: 1.2rem; font-weight: 600; margin-bottom: 8px;">No drugs found</div>
                <div style="color: var(--text-secondary);">Try adjusting your search terms or filters</div>
            </div>
        `;
    }

    /**
     * Update pagination controls.
     */
    updatePagination(pagination) {
        const topPagination = document.getElementById('paginationTop');
        const bottomPagination = document.getElementById('paginationBottom');
        
        const paginationHtml = this.createPaginationHtml(pagination);
        topPagination.innerHTML = paginationHtml;
        bottomPagination.innerHTML = paginationHtml;
    }

    /**
     * Create pagination HTML.
     */
    createPaginationHtml(pagination) {
        const { currentPage, totalPages, totalElements, hasNext, hasPrevious } = pagination;
        
        return `
            <div class="page-info">
                Showing ${this.formatRange(pagination)} of ${totalElements.toLocaleString()} results
            </div>
            <div class="page-buttons">
                <button class="page-button" ${!hasPrevious ? 'disabled' : ''} 
                        onclick="app.goToPage(${currentPage - 1})">
                    ← Previous
                </button>
                
                ${this.createPageNumberButtons(currentPage, totalPages)}
                
                <button class="page-button" ${!hasNext ? 'disabled' : ''} 
                        onclick="app.goToPage(${currentPage + 1})">
                    Next →
                </button>
            </div>
        `;
    }

    /**
     * Create page number buttons.
     */
    createPageNumberButtons(currentPage, totalPages) {
        const buttons = [];
        const maxVisiblePages = 5;
        
        let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
        
        // Adjust start if we're near the end
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(0, endPage - maxVisiblePages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            const isCurrentPage = i === currentPage;
            buttons.push(`
                <button class="page-button ${isCurrentPage ? 'current' : ''}" 
                        onclick="app.goToPage(${i})" ${isCurrentPage ? 'disabled' : ''}>
                    ${i + 1}
                </button>
            `);
        }

        return buttons.join('');
    }

    /**
     * Navigate to a specific page.
     */
    async goToPage(page) {
        if (page < 0 || !this.lastSearchResponse) return;
        
        const totalPages = this.lastSearchResponse.pagination.totalPages;
        if (page >= totalPages) return;

        await this.performSearch(page);
    }

    /**
     * Show search information and statistics.
     */
    showSearchInfo(response, responseTime) {
        const searchInfo = document.getElementById('searchInfo');
        const searchStats = document.getElementById('searchStats');
        const searchTime = document.getElementById('searchTime');
        const cacheStatus = document.getElementById('cacheStatus');

        const drugCount = response.drugs.length;
        const totalCount = response.pagination.totalElements;
        
        searchStats.textContent = `Found ${totalCount.toLocaleString()} drugs (showing ${drugCount})`;
        searchTime.textContent = `Response time: ${Math.round(responseTime)}ms`;
        cacheStatus.textContent = response.searchInfo.cacheHit ? '⚡ Cache hit' : '🌐 Fresh data';

        searchInfo.classList.remove('hidden');
    }

    /**
     * Format range display for pagination info.
     */
    formatRange(pagination) {
        const { currentPage, pageSize, totalElements } = pagination;
        const start = currentPage * pageSize + 1;
        const end = Math.min((currentPage + 1) * pageSize, totalElements);
        return `${start.toLocaleString()}-${end.toLocaleString()}`;
    }

    /**
     * Show/hide loading spinner.
     */
    showLoading(show) {
        const loading = document.getElementById('loadingSpinner');
        loading.classList.toggle('hidden', !show);
    }

    /**
     * Show/hide results container.
     */
    showResults() {
        const resultsContainer = document.getElementById('resultsContainer');
        resultsContainer.classList.remove('hidden');
    }

    hideResults() {
        const resultsContainer = document.getElementById('resultsContainer');
        resultsContainer.classList.add('hidden');
    }

    /**
     * Show error message.
     */
    showError(message) {
        const errorElement = document.getElementById('errorMessage');
        errorElement.textContent = message;
        errorElement.classList.remove('hidden');
    }

    /**
     * Hide error message.
     */
    hideError() {
        const errorElement = document.getElementById('errorMessage');
        errorElement.classList.add('hidden');
    }

    /**
     * Escape HTML to prevent XSS.
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new PuphaxApp();
});

// Example searches for testing
window.testSearches = {
    aspirin: () => {
        document.getElementById('searchTerm').value = 'aspirin';
        app.performSearch();
    },
    
    bayer: () => {
        document.getElementById('searchTerm').value = 'aspirin';
        document.getElementById('manufacturer').value = 'Bayer';
        app.performSearch();
    },
    
    atcCode: () => {
        document.getElementById('searchTerm').value = 'aspirin';
        document.getElementById('atcCode').value = 'N02BA01';
        app.performSearch();
    },
    
    fullSearch: () => {
        document.getElementById('searchTerm').value = 'paracetamol';
        document.getElementById('manufacturer').value = 'TEVA';
        document.getElementById('atcCode').value = 'N02BE01';
        document.getElementById('pageSize').value = '10';
        document.getElementById('sortBy').value = 'manufacturer';
        document.getElementById('sortDirection').value = 'DESC';
        app.performSearch();
    }
};

console.log('🏥 PUPHAX Drug Search Interface loaded!');
console.log('💡 Try these test searches:', Object.keys(window.testSearches));
console.log('📋 Example: testSearches.aspirin()');