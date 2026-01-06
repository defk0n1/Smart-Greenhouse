export const API_CONFIG = {
    // Backend URLs - Direct to backend (CORS enabled)
    BASE_URL: 'http://localhost:8080',

    IAM_PATH: '/iam/rest-iam',

    // API Endpoints - Backend uses /smartgreenhouse/api as base path
    get GREENHOUSES_URL() { return `${this.BASE_URL}/smartgreenhouse/api/greenhouses`; },
    get SENSORS_URL() { return `${this.BASE_URL}/smartgreenhouse/api/sensors/latest`; },
    get ACTUATORS_URL() { return `${this.BASE_URL}/smartgreenhouse/api/actuators`; },

    AUTH_REQUIRED: true
};
