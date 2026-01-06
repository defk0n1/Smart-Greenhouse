// init.js - Application initialization logic
import { authManager } from './auth.js';

// Helper to get query params
function getQueryParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

async function init() {
    const loading = document.getElementById('loading');
    const loadingText = document.getElementById('loading-text');

    // 1. Check for OAuth Callback
    const code = getQueryParam('code');

    if (code) {
        loadingText.textContent = 'Authenticating...';
        const codeVerifier = await authManager.secureStorage.getItem('pkce_code_verifier');
        const result = await authManager.handleCallback(code, codeVerifier);

        if (result.success) {
            loadingText.textContent = 'Success! Redirecting...';
            window.history.replaceState({}, document.title, window.location.pathname);
            loadAdminApp();
        } else {
            showAccessDenied("Authentication Failed: " + (result.error || 'Unknown error'));
        }
        return;
    }

    // 2. Check Session
    loadingText.textContent = 'Checking session...';
    try {
        const isAuthenticated = await authManager.checkSession();

        if (isAuthenticated) {
            loadAdminApp();
        } else {
            // Not authenticated, just go to welcome page cleanly
            window.location.href = 'pages/welcome.html';
        }
    } catch (e) {
        console.error("Session check error:", e);
        window.location.href = 'pages/welcome.html';
    }
}

async function loadAdminApp() {
    const loading = document.getElementById('loading');
    const loadingText = document.getElementById('loading-text');
    const app = document.getElementById('app');

    loadingText.textContent = 'Verifying admin access...';

    if (!authManager.isAdmin()) {
        showAccessDenied("You do not have the required permissions to access this dashboard.");
        authManager.secureStorage.removeItem('access_token');
        authManager.secureStorage.removeItem('refresh_token');
        return;
    }

    console.log('✅ Admin access granted for user:', authManager.user.username);
    loadingText.textContent = 'Loading Admin Panel...';

    try {
        await import('./admin.js');
        // Smooth fade out
        loading.style.opacity = '0';
        setTimeout(() => {
            loading.style.display = 'none';
            app.style.display = 'block';
        }, 500);
    } catch (e) {
        console.error("Failed to load admin app:", e);
        loadingText.textContent = "Failed to load application.";
    }
}

function showAccessDenied(message) {
    // Ensure CSS is loaded
    if (!document.querySelector('link[href="css/access-denied.css"]')) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'css/access-denied.css';
        document.head.appendChild(link);
    }

    const loading = document.getElementById('loading');

    // Clear existing loading spinner styles if needed, or just overwrite innerHTML which we do below.
    // We remove the 'loading' ID style constraints by just replacing content, 
    // but 'loading' div likely has styles. 
    // Ideally we append the overlay to body and hide loading.

    loading.style.display = 'none'; // Hide default loader

    const overlay = document.createElement('div');
    overlay.className = 'access-denied-overlay';

    overlay.innerHTML = `
        <div class="access-denied-card">
            <div class="access-denied-icon">⛔</div>
            <h2 class="access-denied-title">Access Denied</h2>
            <p class="access-denied-message">${message}</p>
            <button class="access-denied-btn" id="redirBtn">Return to Safe Zone</button>
        </div>
    `;

    document.body.appendChild(overlay);

    // Auto-redirect or manual
    document.getElementById('redirBtn').onclick = () => {
        window.location.href = 'pages/welcome.html';
    };
}

init();

if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js').catch(console.error);
    });
}
