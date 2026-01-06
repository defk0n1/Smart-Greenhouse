import { authManager } from '../js/auth.js';

window.redirectToLogin = () => {
    authManager.login();
};

window.scrollToFeatures = () => {
    document.getElementById('features').scrollIntoView({ behavior: 'smooth' });
};
