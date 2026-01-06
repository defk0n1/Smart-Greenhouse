import { API_CONFIG } from './config.js';
import { authManager } from './auth.js';

export class GreenhouseService {
    constructor() {
        this.baseUrl = API_CONFIG.GREENHOUSES_URL;
    }

    async getAllGreenhouses() {
        const response = await authManager.fetchWithAuth(this.baseUrl);
        if (!response.ok) throw new Error('Failed to fetch greenhouses');
        return await response.json();
    }

    async createGreenhouse(data) {
        const response = await authManager.fetchWithAuth(this.baseUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Failed to create greenhouse');
        return await response.json();
    }

    async updateGreenhouse(id, data) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Failed to update greenhouse');
        return await response.json();
    }

    async deleteGreenhouse(id) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete greenhouse');
        return true;
    }

    async getGreenhouseSensors(id) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}/sensors`);
        if (!response.ok) return [];
        return await response.json();
    }

    async getGreenhouseActuators(id) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}/actuators`);
        if (!response.ok) return [];
        return await response.json();
    }

    async assignUserToGreenhouse(greenhouseId, username) {
        // Endpoint: POST /api/greenhouses/{id}/users/{username}
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/users/${username}`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error('Failed to assign user');
        return true;
    }

    async removeUserFromGreenhouse(greenhouseId, username) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/users/${username}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to remove user');
        return true;
    }

    async attachSensor(greenhouseId, deviceId) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/sensors/${deviceId}`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error('Failed to attach sensor');
        return true;
    }

    async detachSensor(greenhouseId, deviceId) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/sensors/${deviceId}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to detach sensor');
        return true;
    }

    async attachActuator(greenhouseId, deviceId) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/actuators/${deviceId}`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error('Failed to attach actuator');
        return true;
    }

    async detachActuator(greenhouseId, deviceId) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${greenhouseId}/actuators/${deviceId}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to detach actuator');
        return true;
    }
}
