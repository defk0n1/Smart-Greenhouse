// config.js - API Configuration
export const API_CONFIG = {
    // Backend URLs
    BASE_URL: window.location.hostname === 'localhost'
        ? 'http://localhost:8080'
        : window.location.origin,

    // API Endpoints
    API_PATH: '/smartgreenhouse/api',
    IAM_PATH: '/iam/rest-iam',

    // Full endpoint URLs
    get SENSORS_URL() {
        return `${this.BASE_URL}${this.API_PATH}/sensors`;
    },

    get ACTUATORS_URL() {
        return `${this.BASE_URL}${this.API_PATH}/actuators`;
    },

    get IAM_LOGIN_URL() {
        return `${this.BASE_URL}${this.IAM_PATH}/identities/login`;
    },

    get IAM_REGISTER_URL() {
        return `${this.BASE_URL}${this.IAM_PATH}/identities`;
    },

    // Polling interval for sensor data (in milliseconds)
    SENSOR_UPDATE_INTERVAL: 5000, // 5 seconds

    // Enable/disable authentication requirement
    AUTH_REQUIRED: true, // Set to true if authentication is mandatory

    // Enable/disable mock data fallback
    USE_MOCK_DATA_ON_ERROR: false
};

// Make available globally for non-module scripts
window.API_CONFIG = API_CONFIG;
