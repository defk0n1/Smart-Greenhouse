// auth.js - Authentication Manager
import { API_CONFIG } from './config.js';
import { secureStorage } from './secure-storage.js';

class AuthManager {
    constructor() {
        this.user = null;
        this.secureStorage = secureStorage; // Initialize secureStorage
        // We don't store the token anymore
        this.checkSession();
    }

    /**
     * Check if user has a valid session (cookie)
     */
    async checkSession() {
        try {
            // Check for token in secureStorage
            const token = await this.secureStorage.getItem('access_token');

            if (!token) {
                console.log('No access token found');
                this.user = null;
                return false;
            }

            // Decode token to get user info (simple decode, no verify)
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));

                // Check expiration
                if (payload.exp * 1000 < Date.now()) {
                    console.log('Token expired');
                    this.logout();
                    return false;
                }

                this.user = {
                    username: payload.sub,
                    roles: payload.groups || [] // dependent on your JWT structure
                };

                console.log('Session valid for user:', this.user.username);
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

    /**
     * Exchange authorization code for token
     */
    async handleCallback(code, codeVerifier) {
        try {
            console.log('🔄 Starting token exchange...');
            console.log('  Code:', code ? code.substring(0, 20) + '...' : 'MISSING');
            console.log('  Code Verifier:', codeVerifier ? codeVerifier.substring(0, 20) + '...' : 'MISSING');

            const tokenUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.IAM_PATH}/oauth/token`;
            console.log('  Token URL:', tokenUrl);

            const response = await fetch(tokenUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams({
                    grant_type: 'authorization_code',
                    code: code,
                    code_verifier: codeVerifier
                }),
                credentials: 'include'
            });

            console.log('  Response status:', response.status, response.statusText);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('❌ Token exchange failed:', errorText);
                throw new Error(`Token exchange failed (${response.status}): ${errorText}`);
            }

            const data = await response.json();
            console.log('  Response data:', data);

            if (data.access_token) {
                console.log('✅ Access token received!');
                await this.secureStorage.setItem('access_token', data.access_token, { encrypt: true });
                // Also store refresh token if you want to support refresh
                if (data.refresh_token) {
                    await this.secureStorage.setItem('refresh_token', data.refresh_token, { encrypt: true });
                    console.log('✅ Refresh token stored');
                }

                await this.checkSession();
                return { success: true };
            } else {
                console.error('❌ No access_token in response');
                throw new Error('No access_token received');
            }

        } catch (error) {
            console.error('❌ Callback error:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Logout user
     */
    logout() {
        this.user = null;
        this.secureStorage.removeItem('access_token');
        this.secureStorage.removeItem('refresh_token');
        // document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    }

    /**
     * Redirect to IAM login page
     */
    async login() {
        // Always redirect to index.html after login (not welcome.html)
        // Use root origin to ensure correct path regardless of where login is initiated
        const redirectUri = `${window.location.origin}/index.html`;
        const currentUrl = encodeURIComponent(redirectUri);

        // Generate OAuth 2.0 PKCE parameters
        const state = Math.random().toString(36).substring(2, 15);
        const codeVerifier = this.generateCodeVerifier();

        // Store code_verifier for later use (when handling callback)
        // CRITICAL FIX: Store in secureStorage (sessionStorage)
        await this.secureStorage.setItem('pkce_code_verifier', codeVerifier);
        await this.secureStorage.setItem('pkce_state', state);
        await this.secureStorage.setItem('redirect_after_login', redirectUri);

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
    async getAccessToken() {
        return await this.secureStorage.getValidAccessToken();
    }

    async getAuthHeader() {
        const token = await this.getAccessToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }

    /**
     * Make authenticated API request
     */
    async fetchWithAuth(url, options = {}) {
        const authHeader = await this.getAuthHeader();
        const headers = {
            ...options.headers,
            ...authHeader
        };

        const fetchOptions = {
            ...options,
            headers: headers,
            // credentials: 'include' // Not needed for Token auth if CORS allows it without credentials, but keep if needed for other cookies
        };

        console.log('📡 fetchWithAuth Request:', url);
        if (headers['Authorization']) {
            const t = headers['Authorization'];
            console.log('   🔑 Auth Header:', t.substring(0, 20) + '...' + (t.length > 20 ? ' (length: ' + t.length + ')' : ''));
        } else {
            console.error('   ❌ MISSING AUTH HEADER');
        }

        try {
            const response = await fetch(url, fetchOptions);

            // If unauthorized, try to refresh or logout
            if (response.status === 401) {
                this.logout();
                // Optionally redirect to login or throw error
                // window.location.reload(); 
                throw new Error('Unauthorized - please login again');
            }

            return response;
        } catch (error) {
            console.error('Fetch error:', error);
            throw error;
        }
    }

    // ... rest of class functions can stay but need to ensure no duplication

}

// Create and export auth manager instance
export const authManager = new AuthManager();

// Make available globally for non-module scripts
window.authManager = authManager;
