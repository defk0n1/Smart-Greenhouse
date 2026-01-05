import { API_CONFIG } from './config.js'; // Assuming we can reuse the main config

export class IamService {
    constructor(authManager) {
        this.authManager = authManager;
        this.baseUrl = API_CONFIG.IAM_API_URL;
    }

    async getAllUsers() {
        const token = this.authManager.getAccessToken(); // You'll need to ensure AuthManager exposes this or gets from cookie
        try {
            const response = await fetch(`${this.baseUrl}/identities`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    throw new Error('Unauthorized');
                }
                throw new Error(`Error fetching users: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error(error);
            throw error;
        }
    }

    async updateUserStatus(userId, isActive) {
        const token = this.authManager.getAccessToken();
        try {
            const response = await fetch(`${this.baseUrl}/identities/${userId}/status?activate=${isActive}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Error updating user status: ${response.statusText}`);
            }
            return true;
        } catch (error) {
            console.error(error);
            throw error;
        }
    }
}
