// auth.js - Authentication Manager
import { API_CONFIG } from './config.js';

class AuthManager {
    constructor() {
        this.user = null;
        // We don't store the token anymore
        this.checkSession();
    }

    /**
     * Check if user has a valid session (cookie)
     */
    async checkSession() {
        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/identities/profile`, {
                credentials: 'include'
            });
            if (response.ok) {
                const profile = await response.json();
                this.user = {
                    id: profile.username, // or sub
                    email: profile.username,
                    name: profile.username,
                    roles: JSON.parse(profile.roles || '[]')
                };
                console.log('Session valid for user:', this.user.name);
                return true;
            } else {
                console.log('No valid session');
                this.user = null;
                return false;
            }
        } catch (error) {
            console.error('Session check failed:', error);
            this.user = null;
            return false;
        }
    }

    /**
     * Exchange authorization code for cookie
     */
    async handleCallback(code, codeVerifier) {
        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/oauth/token`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams({
                    grant_type: 'authorization_code',
                    code: code,
                    code_verifier: codeVerifier
                }),
                credentials: 'include' // Important to receive the cookie
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Token exchange failed: ${errorText}`);
            }

            // We don't get the token in the body anymore, but the cookie is set
            await this.checkSession();
            return { success: true };
        } catch (error) {
            console.error('Callback error:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Logout user
     */
    logout() {
        this.user = null;
        // Ideally call a logout endpoint to clear cookie
        // For now, we just clear client state
        document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    }

    /**
     * Redirect to IAM login page
     */
    /**
     * Redirect to IAM login page
     */
    async login() {
        // Get current URL to use as redirect after login
        const currentUrl = encodeURIComponent(window.location.href);

        // Generate OAuth 2.0 PKCE parameters
        const state = Math.random().toString(36).substring(2, 15);
        const codeVerifier = this.generateCodeVerifier();

        // Store code_verifier for later use (when handling callback)
        sessionStorage.setItem('pkce_code_verifier', codeVerifier);
        sessionStorage.setItem('pkce_state', state);
        sessionStorage.setItem('redirect_after_login', window.location.href);

        // Generate code_challenge using SHA-256
        const codeChallenge = await this.generateCodeChallenge(codeVerifier);

        // Redirect to IAM login with OAuth 2.0 parameters
        const iamLoginUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/authorize?` +
            `response_type=code` +
            `&client_id=smartgreenhouse` +
            `&redirect_uri=${currentUrl}` +
            `&state=${state}` +
            `&code_challenge=${codeChallenge}` +
            `&code_challenge_method=S256` +
            `&scope=resource.read resource.write`;

        console.log('Redirecting to IAM login...', iamLoginUrl);
        window.location.href = iamLoginUrl;
    }

    /**
     * Generate a random code verifier
     */
    generateCodeVerifier() {
        const array = new Uint8Array(32);
        window.crypto.getRandomValues(array);
        return this.base64UrlEncode(array);
    }

    /**
     * Generate code challenge from verifier using SHA-256
     */
    async generateCodeChallenge(verifier) {
        const encoder = new TextEncoder();
        const data = encoder.encode(verifier);
        const hash = await window.crypto.subtle.digest('SHA-256', data);
        return this.base64UrlEncode(new Uint8Array(hash));
    }

    /**
     * Base64 URL encode
     */
    base64UrlEncode(buffer) {
        let str = '';
        const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            str += String.fromCharCode(bytes[i]);
        }
        return btoa(str)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
    }

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return this.user !== null;
    }

    /**
     * Get authorization header for API requests
     */
    getAuthHeader() {
        // No header needed, we use cookies
        return {};
    }

    /**
     * Make authenticated API request
     */
    async fetchWithAuth(url, options = {}) {
        const fetchOptions = {
            ...options,
            credentials: 'include' // Send cookies
        };

        try {
            const response = await fetch(url, fetchOptions);

            // If unauthorized, try to refresh or logout
            if (response.status === 401) {
                this.logout();
                throw new Error('Unauthorized - please login again');
            }

            return response;
        } catch (error) {
            console.error('Fetch error:', error);
            throw error;
        }
    }
}

// Create and export auth manager instance
export const authManager = new AuthManager();

// Make available globally for non-module scripts
window.authManager = authManager;
