// app-loader.js - Application loader with authentication check and PWA install
import { authManager } from './auth.js';
import { API_CONFIG } from './config.js';

// Make available globally
window.authManager = authManager;
window.API_CONFIG = API_CONFIG;

// Authentication check (after modules are loaded)
async function checkAuthentication() {
    console.log('=== AUTHENTICATION CHECK START ===');

    // 1. Check for OAuth callback (code) FIRST
    const urlParams = new URLSearchParams(window.location.search);
    console.log('📍 Current URL:', window.location.href);
    console.log('📍 Query params:', Array.from(urlParams.entries()));

    if (urlParams.has('code')) {
        console.log('✅ OAuth code detected in URL!');
        const code = urlParams.get('code');
        const state = urlParams.get('state');
        const storedState = await authManager.secureStorage.getItem('pkce_state');
        const codeVerifier = await authManager.secureStorage.getItem('pkce_code_verifier');

        console.log('  Code:', code ? code.substring(0, 30) + '...' : 'NULL');
        console.log('  State from URL:', state);
        console.log('  Stored state:', storedState);
        console.log('  Code verifier:', codeVerifier ? codeVerifier.substring(0, 30) + '...' : 'NULL/MISSING');

        // En dev local, on peut être plus souple si le state est perdu
        if (state === storedState || window.location.hostname === 'localhost') {
            console.log('✅ State validation passed (or skipped for localhost)');

            if (!codeVerifier) {
                console.error('❌ CRITICAL: code_verifier is missing from sessionStorage!');
                alert('Authentication error: Session lost. Please try logging in again.');
                window.location.href = 'pages/welcome.html';
                return;
            }

            // Exchange authorization code for token
            console.log('🔄 Calling handleCallback...');
            const result = await authManager.handleCallback(code, codeVerifier);
            console.log('  Callback result:', result);

            if (result.success) {
                console.log('✅ Token exchange successful!');
                // Clean up sessionStorage
                authManager.secureStorage.removeItem('pkce_code_verifier');
                authManager.secureStorage.removeItem('pkce_state');
                authManager.secureStorage.removeItem('redirect_after_login');

                // Clean up URL and reload without query parameters
                const cleanUrl = window.location.origin + window.location.pathname;
                window.history.replaceState({}, document.title, cleanUrl);

                console.log('🔄 Reloading app...');
                window.location.reload();
                return; // Stop execution here
            } else {
                console.error('❌ Token exchange failed:', result.error);
                alert('Authentication failed: ' + result.error);
                // Clear URL and redirect to welcome page
                window.location.href = 'pages/welcome.html';
                return;
            }
        } else {
            console.error('❌ State mismatch! Possible CSRF attack.');
            console.error('  Received:', state);
            console.error('  Stored:', storedState);
            alert('Security error: Invalid redirect.');
            window.location.href = 'pages/welcome.html';
            return;
        }
    } else {
        console.log('ℹ️ No OAuth code in URL (normal page load)');
    }

    // 2. Check authentication before loading the main application
    console.log('🔍 Checking session...');
    const isAuthenticated = await authManager.checkSession();
    console.log('  Is authenticated:', isAuthenticated);

    if (API_CONFIG.AUTH_REQUIRED && !isAuthenticated) {
        // Redirect to welcome page instead of showing error
        console.log('⚠️ User not authenticated, redirecting to welcome page...');
        window.location.href = 'pages/welcome.html';

        // Stop script execution to prevent loading the app
        throw new Error('Authentication required - redirecting to welcome');
    } else {
        console.log('✅ User is authenticated, loading app...');
        // Authenticated - reset loop counter (kept for backward compatibility)
        sessionStorage.removeItem('auth_loop_count');

        // Load application scripts dynamically to avoid race conditions
        await loadApplication();
    }

    console.log('=== AUTHENTICATION CHECK END ===');
}

async function loadApplication() {
    try {
        console.log('🚀 Loading application scripts...');
        await import('./mvp.js');
        await import('./app-init.js');
        await import('./main.js');
        console.log('✅ Application scripts loaded');
    } catch (error) {
        console.error('❌ Failed to load application scripts:', error);
    }
}

// Run authentication check
checkAuthentication();

// Service Worker Registration
if ('serviceWorker' in navigator) {
    window.addEventListener('load', async () => {
        try {
            const registration = await navigator.serviceWorker.register('sw.js');
            console.log('✓ Service Worker enregistré avec succès:', registration.scope);
        } catch (error) {
            console.error('✗ Échec de l\'enregistrement du Service Worker:', error);
        }
    });
}

// PWA Install Prompt
let deferredPrompt;
const installPrompt = document.getElementById('installPrompt');
const installButton = document.getElementById('installButton');
const dismissButton = document.getElementById('dismissButton');

window.addEventListener('beforeinstallprompt', (e) => {
    // Empêcher la mini-barre d'information de s'afficher automatiquement
    e.preventDefault();
    // Stocker l'événement pour pouvoir le déclencher plus tard
    deferredPrompt = e;
    // Afficher notre propre UI d'installation
    if (installPrompt) {
        installPrompt.style.display = 'flex';
    }
});

if (installButton) {
    installButton.addEventListener('click', async () => {
        if (!deferredPrompt) return;

        // Afficher la boîte de dialogue d'installation
        deferredPrompt.prompt();

        // Attendre la réponse de l'utilisateur
        const { outcome } = await deferredPrompt.userChoice;
        console.log(outcome === 'accepted' ? '✓ PWA installée' : '✗ Installation refusée');

        // Reset la variable car elle ne peut être utilisée qu'une fois
        deferredPrompt = null;
        if (installPrompt) {
            installPrompt.style.display = 'none';
        }
    });
}

if (dismissButton) {
    dismissButton.addEventListener('click', () => {
        if (installPrompt) {
            installPrompt.style.display = 'none';
        }
    });
}

// Détecter si l'app est déjà installée
window.addEventListener('appinstalled', () => {
    console.log('✓ PWA a été installée avec succès!');
    if (installPrompt) {
        installPrompt.style.display = 'none';
    }
});
