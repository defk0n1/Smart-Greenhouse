import { API_CONFIG } from './config.js';

export class IamService {
    constructor(authManager) {
        this.authManager = authManager;
        this.baseUrl = API_CONFIG.IAM_PATH; // /iam/rest-iam
    }

    async getAllUsers() {
        // GET /iam/rest-iam/identities
        const response = await this.authManager.fetchWithAuth(`${API_CONFIG.BASE_URL}${this.baseUrl}/identities`);
        if (!response.ok) throw new Error('Failed to fetch users');
        return await response.json();
    }

    async updateUserStatus(userId, isActive) {
        // PUT /iam/rest-iam/identities/{id}/status?activate=true/false
        const response = await this.authManager.fetchWithAuth(
            `${API_CONFIG.BASE_URL}${this.baseUrl}/identities/${userId}/status?activate=${isActive}`,
            { method: 'PUT' }
        );
        if (!response.ok) throw new Error('Failed to update user status');
        return true;
    }
}
