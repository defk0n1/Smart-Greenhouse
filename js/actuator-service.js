import { API_CONFIG } from './config.js';
import { authManager } from './auth.js';

export class ActuatorService {
    constructor() {
        this.baseUrl = API_CONFIG.ACTUATORS_URL;
    }

    async getAllActuators(filter = null) {
        const response = await authManager.fetchWithAuth(this.baseUrl);
        if (!response.ok) throw new Error('Failed to fetch actuators');
        return await response.json();
    }

    async createActuator(data) {
        const response = await authManager.fetchWithAuth(this.baseUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Failed to create actuator');
        return await response.json();
    }

    async updateActuator(id, data) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Failed to update actuator');
        return await response.json();
    }

    async deleteActuator(id) {
        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete actuator');
        return true;
    }

    async sendCommand(id, command, value) {
        // CRITICAL: Updated based on API analysis
        // Endpoint: POST /api/actuators/{id}/command?command={command}&value={value}

        const params = new URLSearchParams();
        if (command) params.append('command', command);
        if (value !== null && value !== undefined) params.append('value', value);

        const response = await authManager.fetchWithAuth(`${this.baseUrl}/${id}/command?${params.toString()}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) {
            const txt = await response.text(); // Capture error message
            throw new Error('Failed to send command: ' + txt);
        }
        // Endpoint returns string message
        return await response.text();
    }
}
