// greenhouse.js - Greenhouse Selection and Management
import { API_CONFIG } from './config.js';
import { authManager } from './auth.js';

class GreenhouseManager {
    constructor() {
        this.greenhouses = [];
        this.currentGreenhouseId = null;
        this.onGreenhouseChangeCallback = null;
    }

    // -------------------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------------------
    async initialize() {
        // Load selected greenhouse from localStorage
        this.currentGreenhouseId = localStorage.getItem('selectedGreenhouseId');

        // Fetch list of accessible greenhouses
        await this.fetchGreenhouses();

        // If no greenhouse is selected or selected one doesn't exist, select the first one
        if (!this.currentGreenhouseId || !this.greenhouses.find(g => g.id === this.currentGreenhouseId)) {
            if (this.greenhouses.length > 0) {
                this.selectGreenhouse(this.greenhouses[0].id);
            }
        }
    }

    // -------------------------------------------------------------------------
    // FETCH GREENHOUSES
    // -------------------------------------------------------------------------
    async fetchGreenhouses() {
        try {
            const response = await authManager.fetchWithAuth(API_CONFIG.GREENHOUSES_URL);

            if (response.ok) {
                this.greenhouses = await response.json();
                console.log('✓ Loaded greenhouses:', this.greenhouses);
                return this.greenhouses;
            } else if (response.status === 401) {
                console.warn('⚠ Not authenticated to fetch greenhouses');
                return [];
            } else {
                console.error('✗ Failed to fetch greenhouses:', response.status);
                return [];
            }
        } catch (error) {
            console.error('Error fetching greenhouses:', error);
            return [];
        }
    }

    // -------------------------------------------------------------------------
    // GREENHOUSE SELECTION
    // -------------------------------------------------------------------------
    selectGreenhouse(greenhouseId) {
        const greenhouse = this.greenhouses.find(g => g.id === greenhouseId);
        if (!greenhouse) {
            console.error('Greenhouse not found:', greenhouseId);
            return;
        }

        this.currentGreenhouseId = greenhouseId;
        localStorage.setItem('selectedGreenhouseId', greenhouseId);

        console.log('Selected greenhouse:', greenhouse.name);

        // Notify listeners
        if (this.onGreenhouseChangeCallback) {
            this.onGreenhouseChangeCallback(greenhouse);
        }
    }

    getCurrentGreenhouseId() {
        return this.currentGreenhouseId;
    }

    getCurrentGreenhouse() {
        return this.greenhouses.find(g => g.id === this.currentGreenhouseId);
    }

    // -------------------------------------------------------------------------
    // EVENT HANDLER
    // -------------------------------------------------------------------------
    onGreenhouseChange(callback) {
        this.onGreenhouseChangeCallback = callback;
    }

    // -------------------------------------------------------------------------
    // CREATE NEW GREENHOUSE
    // -------------------------------------------------------------------------
    async createGreenhouse(name, description, location) {
        try {
            const response = await authManager.fetchWithAuth(API_CONFIG.GREENHOUSES_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    name: name,
                    description: description,
                    location: location
                })
            });

            if (response.ok) {
                const newGreenhouse = await response.json();
                console.log('✓ Created greenhouse:', newGreenhouse);

                // Refresh the list
                await this.fetchGreenhouses();

                // Automatically select the new greenhouse
                this.selectGreenhouse(newGreenhouse.id);

                return newGreenhouse;
            } else {
                const errorText = await response.text();
                console.error('✗ Failed to create greenhouse:', errorText);
                return null;
            }
        } catch (error) {
            console.error('Error creating greenhouse:', error);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // UI HELPERS
    // -------------------------------------------------------------------------
    populateGreenhouseSelector(selectElement) {
        if (!selectElement) return;

        // Clear existing options
        selectElement.innerHTML = '';

        // Add greenhouses as options
        this.greenhouses.forEach(greenhouse => {
            const option = document.createElement('option');
            option.value = greenhouse.id;
            option.textContent = greenhouse.name;
            if (greenhouse.id === this.currentGreenhouseId) {
                option.selected = true;
            }
            selectElement.appendChild(option);
        });

        // Add event listener for change
        selectElement.addEventListener('change', (e) => {
            this.selectGreenhouse(e.target.value);
        });
    }
}

// Create singleton instance
export const greenhouseManager = new GreenhouseManager();

// Make available globally for debugging
window.greenhouseManager = greenhouseManager;
