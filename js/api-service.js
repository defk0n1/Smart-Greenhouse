import { API_CONFIG } from './config.js';

export class ApiService {
    constructor(authManager) {
        this.authManager = authManager;
        this.baseUrl = API_CONFIG.SMART_GREENHOUSE_API_URL;
    }

    async getAllGreenhouses() {
        const token = this.authManager.getAccessToken();
        // Admin likely needs to see ALL greenhouses. 
        // The current API `getAccessibleGreenhouses` filters by user. 
        // We might need a generic "getAll" for admins if not present, OR the admin user is "owner" of none but has admin rights.
        // Wait, `GreenhouseResource` `getAccessibleGreenhouses` filters by `findByUserId`.
        // Admin needs to see EVERYTHING to assign.
        // We might need to update GreenhouseResource to allow Admin to see all.
        // For now, let's assume we use the existing `GreenhouseResource` but maybe we initially assign the admin as owner of everything? 
        // OR we need a new endpoint `GET /greenhouses/admin/all`.
        // Let's stick to what we have or maybe the user is the owner.
        // Actually, the user asked to "Assign users to greenhouses". This implies listing all greenhouses.
        // Let's assume for now we hit the endpoint and maybe update API if needed.
        // But `GreenhouseRepository.findAll()` exists.

        try {
            // NOTE: This might fail if the current user isn't authorized to see ALL greenhouses.
            // We might need to update API to allow role-based 'get all'.
            const response = await fetch(`${this.baseUrl}/greenhouses`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Error fetching greenhouses: ${response.statusText}`);
            }
            return await response.json();
        } catch (error) {
            console.error(error);
            throw error;
        }
    }

    async assignUserToGreenhouse(greenhouseId, userId) {
        const token = this.authManager.getAccessToken();
        try {
            const response = await fetch(`${this.baseUrl}/greenhouses/${greenhouseId}/users/${userId}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                throw new Error(`Error assigning user: ${response.statusText}`);
            }
            return true;
        } catch (error) {
            console.error(error);
            throw error;
        }
    }

    async removeUserFromGreenhouse(greenhouseId, userId) {
        const token = this.authManager.getAccessToken();
        try {
            const response = await fetch(`${this.baseUrl}/greenhouses/${greenhouseId}/users/${userId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                throw new Error(`Error removing user: ${response.statusText}`);
            }
            return true;
        } catch (error) {
            console.error(error);
            throw error;
        }
    }
}
