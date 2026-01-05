// secure-storage.js - Secure storage wrapper with encryption and sessionStorage
class SecureStorage {
    constructor() {
        // Use sessionStorage instead of localStorage (cleared on tab close)
        this.storage = sessionStorage;
        this.encryptionKey = null;
    }

    /**
     * Generate a simple encryption key from browser fingerprint
     * Note: This is basic obfuscation, not cryptographic security
     */
    async generateKey() {
        const fingerprint = navigator.userAgent + navigator.language + screen.width + screen.height;
        const encoder = new TextEncoder();
        const data = encoder.encode(fingerprint);
        const hashBuffer = await crypto.subtle.digest('SHA-256', data);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        this.encryptionKey = hashArray.slice(0, 16); // Use first 16 bytes
    }

    /**
     * Simple XOR encryption (obfuscation)
     */
    encrypt(text) {
        if (!this.encryptionKey) return text;

        const encoder = new TextEncoder();
        const data = encoder.encode(text);
        const encrypted = new Uint8Array(data.length);

        for (let i = 0; i < data.length; i++) {
            encrypted[i] = data[i] ^ this.encryptionKey[i % this.encryptionKey.length];
        }

        // Convert to base64
        return btoa(String.fromCharCode(...encrypted));
    }

    /**
     * Simple XOR decryption
     */
    decrypt(encryptedText) {
        if (!this.encryptionKey) return encryptedText;

        try {
            // Decode from base64
            const encrypted = Uint8Array.from(atob(encryptedText), c => c.charCodeAt(0));
            const decrypted = new Uint8Array(encrypted.length);

            for (let i = 0; i < encrypted.length; i++) {
                decrypted[i] = encrypted[i] ^ this.encryptionKey[i % this.encryptionKey.length];
            }

            const decoder = new TextDecoder();
            return decoder.decode(decrypted);
        } catch (e) {
            console.warn('Decryption failed, returning original');
            return encryptedText;
        }
    }

    /**
     * Set item with optional encryption
     */
    async setItem(key, value, options = {}) {
        const { encrypt = false } = options;

        if (!this.encryptionKey) {
            await this.generateKey();
        }

        const dataToStore = encrypt ? this.encrypt(value) : value;

        // Store with metadata
        const storageData = {
            value: dataToStore,
            encrypted: encrypt,
            timestamp: Date.now()
        };

        this.storage.setItem(key, JSON.stringify(storageData));
    }

    /**
     * Get item with automatic decryption
     */
    async getItem(key) {
        const stored = this.storage.getItem(key);

        if (!stored) return null;

        try {
            const storageData = JSON.parse(stored);

            if (!this.encryptionKey) {
                await this.generateKey();
            }

            return storageData.encrypted
                ? this.decrypt(storageData.value)
                : storageData.value;
        } catch (e) {
            // Fallback for non-JSON data (backward compatibility)
            return stored;
        }
    }

    /**
     * Remove item
     */
    removeItem(key) {
        this.storage.removeItem(key);
    }

    /**
     * Clear all items
     */
    clear() {
        this.storage.clear();
    }

    /**
     * Check if token is expired based on JWT
     */
    isTokenExpired(token) {
        if (!token) return true;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.exp * 1000 < Date.now();
        } catch (e) {
            return true;
        }
    }

    /**
     * Get access token if not expired
     */
    async getValidAccessToken() {
        const token = await this.getItem('access_token');

        if (!token || this.isTokenExpired(token)) {
            this.removeItem('access_token');
            return null;
        }

        return token;
    }
}

// Export singleton instance
export const secureStorage = new SecureStorage();
window.secureStorage = secureStorage; // For debugging
