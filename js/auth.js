// auth.js - Authentication Manager
import { API_CONFIG } from './config.js';
import { secureStorage } from './secure-storage.js';

class AuthManager {
    constructor() {
        this.user = null;
        this.secureStorage = secureStorage;
        this.checkSession();
    }

    async checkSession() {
        try {
            const token = await this.secureStorage.getItem('access_token');
            if (!token) {
                this.user = null;
                return false;
            }

            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                if (payload.exp * 1000 < Date.now()) {
                    console.log('Token expired');
                    this.logout();
                    return false;
                }

                this.user = {
                    username: payload.sub,
                    roles: payload.groups || [] // "groups": ["ROOT", ...]
                };

                return true;
            } catch (e) {
                console.error('Invalid token format', e);
                this.logout();
                return false;
            }
        } catch (error) {
            console.error('Session check failed:', error);
            this.logout();
            return false;
        }
    }

    isAdmin() {
        if (!this.user || !this.user.roles) return false;
        // Check "groups" from JWT claiming to be ROOT or ADMIN
        // IAM returns role names like "ROOT", "R_P01", etc.
        const roles = this.user.roles.map(r => r.toUpperCase());
        return roles.includes('ROOT') || roles.includes('ADMIN') || roles.includes('ADMINISTRATOR');
    }

    async handleCallback(code, codeVerifier) {
        try {
            const tokenUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/oauth/token`;

            const response = await fetch(tokenUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    grant_type: 'authorization_code',
                    code: code,
                    code_verifier: codeVerifier
                }),
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`Token exchange failed (${response.status})`);
            }

            const data = await response.json();
            if (data.access_token) {
                await this.secureStorage.setItem('access_token', data.access_token, { encrypt: true });
                if (data.refresh_token) {
                    await this.secureStorage.setItem('refresh_token', data.refresh_token, { encrypt: true });
                }
                await this.checkSession();
                return { success: true };
            } else {
                return { success: false, error: 'No access_token received' };
            }
        } catch (error) {
            console.error('Callback error:', error);
            return { success: false, error: error.message };
        }
    }

    logout() {
        this.user = null;
        this.secureStorage.removeItem('access_token');
        this.secureStorage.removeItem('refresh_token');
        window.location.reload();
    }

    async login() {
        const redirectUri = `${window.location.origin}/index.html`;
        const currentUrl = encodeURIComponent(redirectUri);

        const state = Math.random().toString(36).substring(2, 15);
        const codeVerifier = this.generateCodeVerifier();

        await this.secureStorage.setItem('pkce_code_verifier', codeVerifier);
        await this.secureStorage.setItem('pkce_state', state);

        const codeChallenge = await this.generateCodeChallenge(codeVerifier);

        const iamLoginUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/authorize?` +
            `response_type=code` +
            `&client_id=smartgreenhouse` +
            `&redirect_uri=${currentUrl}` +
            `&state=${state}` +
            `&code_challenge=${codeChallenge}` +
            `&code_challenge_method=S256` +
            `&scope=resource.read resource.write`;

        window.location.href = iamLoginUrl;
    }

    generateCodeVerifier() {
        const array = new Uint8Array(32);
        window.crypto.getRandomValues(array);
        return this.base64UrlEncode(array);
    }

    async generateCodeChallenge(verifier) {
        const encoder = new TextEncoder();
        const data = encoder.encode(verifier);
        const hash = await window.crypto.subtle.digest('SHA-256', data);
        return this.base64UrlEncode(new Uint8Array(hash));
    }

    base64UrlEncode(buffer) {
        let str = '';
        const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            str += String.fromCharCode(bytes[i]);
        }
        return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    isAuthenticated() {
        return this.user !== null;
    }

    async fetchWithAuth(url, options = {}) {
        const token = await this.secureStorage.getValidAccessToken();
        const headers = {
            ...options.headers,
            'Authorization': token ? `Bearer ${token}` : ''
        };

        const response = await fetch(url, { ...options, headers });
        if (response.status === 401) {
            this.logout();
            throw new Error('Unauthorized');
        }
        return response;
    }
}

export const authManager = new AuthManager();
window.authManager = authManager;
