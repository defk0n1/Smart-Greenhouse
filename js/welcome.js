// welcome.js - Welcome page logic
import { authManager } from '../js/auth.js';

function redirectToLogin() {
    authManager.login();
}

function scrollToFeatures() {
    document.getElementById('features').scrollIntoView({ behavior: 'smooth' });
}

// Make functions globally available
window.redirectToLogin = redirectToLogin;
window.scrollToFeatures = scrollToFeatures;

// Check if already authenticated, redirect to main app
(async function () {
    const isAuthenticated = await authManager.checkSession();
    if (isAuthenticated) {
        window.location.href = '../index.html';
    }
})();
