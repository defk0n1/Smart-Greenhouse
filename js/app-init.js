// app-init.js - Application Initialization for Greenhouse Selection
import { greenhouseManager } from './greenhouse.js';
import { greenhousePresenter } from './mvp.js';

// -------------------------------------------------------------------------
// INITIALIZATION
// -------------------------------------------------------------------------
async function initializeGreenhouseApp() {
    try {
        console.log('🚀 Initializing Greenhouse Selection...');

        // Initialize greenhouse manager
        await greenhouseManager.initialize();
        console.log('✓ Greenhouse manager initialized');

        // Populate the selector dropdown
        const selectElement = document.getElementById('greenhouseSelect');
        if (selectElement) {
            greenhouseManager.populateGreenhouseSelector(selectElement);
            console.log('✓ Greenhouse selector populated');
        }

        // Setup change listener to refresh data when greenhouse changes
        greenhouseManager.onGreenhouseChange((greenhouse) => {
            console.log('🏡 Greenhouse changed to:', greenhouse.name);
            // Reload sensor AND actuator data for the new greenhouse
            greenhousePresenter.initializeSensorValues();
        });

        // Trigger initial load if we already have a selected greenhouse
        const currentGreenhouse = greenhouseManager.getCurrentGreenhouse();
        if (currentGreenhouse) {
            console.log('🔄 Triggering initial data load for:', currentGreenhouse.name);
            greenhousePresenter.initializeSensorValues();
        }

        console.log('✓ Greenhouse app initialization complete');
    } catch (error) {
        console.error('Failed to initialize greenhouse app:', error);
    }
}

// Run initialization when DOM is loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeGreenhouseApp);
} else {
    // DOM already loaded
    initializeGreenhouseApp();
}
