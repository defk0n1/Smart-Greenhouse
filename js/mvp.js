// mvp.js - Data management and business logic
import { API_CONFIG } from './config.js';
import { authManager } from './auth.js';

class GreenhousePresenter {
    constructor() {
        this.sensors = [
            {
                id: 'temp_int',
                name: 'Internal Temperature',
                svg: 'thermometer',
                color: 0xff6b6b,
                position: { x: -0.5, y: 0.5, z: 0.5 },
                unit: '°C',
                min: 0,
                max: 50,
                warningHigh: 30,  // Above this = warning (orange)
                criticalHigh: 35  // Above this = critical (red)
            },
            {
                id: 'temp_ext',
                name: 'External Temperature',
                svg: 'thermometer',
                color: 0x4ecdc4,
                position: { x: -0.1, y: 0.7, z: 0.0 },
                unit: '°C',
                min: -10,
                max: 50,
                warningLow: 0,    // Below this = warning
                warningHigh: 35,
                criticalHigh: 40
            },
            {
                id: 'humidity_int',
                name: 'Internal Humidity',
                svg: 'droplet',
                color: 0x95e1d3,
                position: { x: 0.5, y: 0.5, z: -0.5 },
                unit: '%',
                min: 0,
                max: 100,
                warningLow: 30,   // Below 30% = too dry
                warningHigh: 80,  // Above 80% = too humid
                criticalHigh: 90
            },
            {
                id: 'humidity_ext',
                name: 'External Humidity',
                svg: 'droplet',
                color: 0x38ada9,
                position: { x: 0.7, y: 0.6, z: 0.2 },
                unit: '%',
                min: 0,
                max: 100,
                warningHigh: 85,
                criticalHigh: 95
            },
            {
                id: 'soil',
                name: 'Soil Moisture',
                svg: 'droplet',
                color: 0xf9ca24,
                position: { x: 0.0, y: 0.3, z: 0.0 },
                unit: '%',
                min: 0,
                max: 100,
                warningLow: 20,   // Below 20% = needs water
                criticalLow: 10
            },
            {
                id: 'light',
                name: 'Light Sensor',
                svg: 'sun',
                color: 0xffd93d,
                position: { x: -0.3, y: 0.3, z: 0.8 },
                unit: 'lux',
                min: 0,
                max: 100000
            },
            {
                id: 'gas',
                name: 'Gas Sensor',
                svg: 'wind',
                color: 0xa8e6cf,
                position: { x: 0.0, y: 0.3, z: 1.0 },
                unit: 'ppm',
                min: 0,
                max: 5000,
                warningHigh: 1000,  // Above 1000 ppm = warning
                criticalHigh: 2000  // Above 2000 ppm = danger!
            },
            {
                id: 'water',
                name: 'Water Tank Level',
                svg: 'droplet',
                color: 0x74b9ff,
                position: { x: -0.8, y: 0.4, z: -0.3 },
                unit: '%',
                min: 0,
                max: 100,
                warningLow: 20,   // Below 20% = refill soon
                criticalLow: 10   // Below 10% = critical!
            },
            {
                id: 'ph',
                name: 'Soil pH',
                svg: 'flask',
                color: 0xdda15e,
                position: { x: -0.4, y: 0.3, z: 0.0 },
                unit: 'pH',
                min: 0,
                max: 14
            },
            {
                id: 'pressure',
                name: 'Atmospheric Pressure',
                svg: 'gauge',
                color: 0xbc6c25,
                position: { x: 0.0, y: 0.8, z: 1.0 },
                unit: 'hPa',
                min: 950,
                max: 1050
            },
            {
                id: 'fan1',
                name: 'Fan 1',
                svg: 'fan',
                color: 0x00d2ff,
                position: { x: -0.4, y: 0.6, z: -0.8 },
                unit: 'RPM',
                min: 0,
                max: 3000
            },
            {
                id: 'fan2',
                name: 'Fan 2',
                svg: 'fan',
                color: 0x00aeff,
                position: { x: 0.4, y: 0.4, z: 0.8 },
                unit: 'RPM',
                min: 0,
                max: 3000
            },
            {
                id: 'pump',
                name: 'Water Pump',
                svg: 'pump',
                color: 0x5f27cd,
                position: { x: 0.7, y: 0.3, z: 1.0 },
                unit: 'L/h',
                min: 0,
                max: 1000
            },
            {
                id: 'lamp1',
                name: 'LED Lamp 1',
                svg: 'lightbulb',
                color: 0xfeca57,
                position: { x: 0.0, y: 0.5, z: -0.8 },
                unit: 'W',
                min: 0,
                max: 100
            },
            {
                id: 'lamp2',
                name: 'LED Lamp 2',
                svg: 'lightbulb',
                color: 0xffb142,
                position: { x: 0.0, y: 0.5, z: 0.8 },
                unit: 'W',
                min: 0,
                max: 100
            }
        ];

        this.sensorValues = {};
        this.currentSensor = null;
        this.sensorChart = null;
        this.deferredPrompt = null;
        this.gaugeCanvases = {}; // Store gauge canvas contexts

        this.initializeSensorValues();
        this.initializeActuatorStates();
        this.initGauges();
        this.initPWA();
    }


    async initializeSensorValues() {
        // Try to fetch real sensor data from API
        try {
            const response = await fetch(API_CONFIG.SENSORS_URL, { credentials: 'include' });

            if (response.ok) {
                const sensors = await response.json();
                console.log('✓ Loaded sensor data from API:', sensors);

                // Map API sensor data to our sensor values
                sensors.forEach(apiSensor => {
                    // Normalize API data fields
                    const sensorId = apiSensor.id || apiSensor._id;
                    const apiSensorId = apiSensor.sensor_id || apiSensor.sensorId || '';
                    const sensorType = apiSensor.type || '';
                    const sensorValue = apiSensor.value !== undefined ? apiSensor.value : (apiSensor.measurement !== undefined ? apiSensor.measurement : 0);
                    const sensorTime = apiSensor.measurementTime || apiSensor.measurement_time || new Date();

                    // Find matching sensor in our predefined sensors list
                    // Priority: sensorId match (e.g., "temp_int_001" contains "temp_int") > Type exact match
                    let matchingSensor = this.sensors.find(s => {
                        // Check if apiSensorId contains the local sensor id
                        // e.g., "temp_int_001" contains "temp_int"
                        return apiSensorId.includes(s.id) || s.id.includes(apiSensorId);
                    });

                    if (!matchingSensor && sensorType) {
                        // Fallback: Try exact type match
                        matchingSensor = this.sensors.find(s => s.id === sensorType);
                    }

                    if (matchingSensor) {
                        console.log(`Matched API sensor ${sensorId} (${apiSensorId}, ${sensorType}) to local sensor ${matchingSensor.id}`);
                        this.sensorValues[matchingSensor.id] = {
                            current: sensorValue,
                            unit: matchingSensor.unit,
                            lastUpdate: new Date(sensorTime),
                            battery: apiSensor.battery || 100,
                            status: apiSensor.status || 'active',
                            data: apiSensor // Keep original API data
                        };
                    } else {
                        console.log(`No local match found for API sensor ${sensorId} (${apiSensorId}, ${sensorType})`);
                    }
                });

                // Fill in missing sensors with default data (0)
                this.fillMissingSensorsWithMockData();

                // Start periodic updates
                this.startSensorUpdates();
            } else {
                throw new Error(`API returned ${response.status}`);
            }
        } catch (error) {
            console.warn('⚠ Could not fetch sensor data from API:', error.message);
            // Always initialize missing sensors with default values (0) to prevent UI crashes
            this.fillMissingSensorsWithMockData();
        }
    }

    fillMissingSensorsWithMockData() {
        this.sensors.forEach(sensor => {
            if (!this.sensorValues[sensor.id]) {
                // Initialize with default values (0) instead of random data
                this.sensorValues[sensor.id] = {
                    current: 0,
                    unit: sensor.unit,
                    lastUpdate: new Date(),
                    battery: 0,
                    status: 'No Data'
                };
            }
        });
    }

    async initializeActuatorStates() {
        const actuatorIds = ['fan1', 'fan2', 'pump', 'lamp1', 'lamp2'];

        for (const id of actuatorIds) {
            try {
                const state = await this.fetchActuatorState(id);

                // Store actuator state in sensorValues
                if (!this.sensorValues[id]) {
                    this.sensorValues[id] = {
                        current: 0,
                        unit: '',
                        lastUpdate: new Date(),
                        battery: 100,
                        status: 'active'
                    };
                }

                // Store actuator data with state
                this.sensorValues[id].data = {
                    state: state,
                    lastCommand: state
                };

                console.log(`Initialized actuator ${id} with state: ${state}`);
            } catch (error) {
                console.error(`Error initializing actuator ${id}:`, error);
            }
        }
    }

    startSensorUpdates() {
        // Update sensor data periodically
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }

        this.updateInterval = setInterval(async () => {
            try {
                const response = await fetch(API_CONFIG.SENSORS_URL, { credentials: 'include' });
                if (response.ok) {
                    const sensors = await response.json();

                    sensors.forEach(apiSensor => {
                        // Normalize API data fields
                        const sensorId = apiSensor.id || apiSensor._id;
                        const apiSensorId = apiSensor.sensor_id || apiSensor.sensorId || '';
                        const sensorType = apiSensor.type || '';
                        const sensorValue = apiSensor.value !== undefined ? apiSensor.value : (apiSensor.measurement !== undefined ? apiSensor.measurement : 0);
                        const sensorTime = apiSensor.measurementTime || apiSensor.measurement_time || new Date();

                        let matchingSensor = this.sensors.find(s => {
                            return apiSensorId.includes(s.id) || s.id.includes(apiSensorId);
                        });

                        if (!matchingSensor && sensorType) {
                            matchingSensor = this.sensors.find(s => s.id === sensorType);
                        }

                        if (matchingSensor && this.sensorValues[matchingSensor.id]) {
                            this.sensorValues[matchingSensor.id].current = sensorValue;
                            this.sensorValues[matchingSensor.id].lastUpdate = new Date(sensorTime);
                            this.sensorValues[matchingSensor.id].status = apiSensor.status || 'active';

                            // Update popup in real-time if it's currently open
                            if (this.currentSensor && this.currentSensor.id === matchingSensor.id) {
                                this.updateSensorPopupValues();

                                // Also update chart if chart popup is open
                                if (this.sensorChart && document.getElementById('chartPopup').classList.contains('active')) {
                                    this.refreshChart();
                                }
                            }
                        }
                    });
                }
            } catch (error) {
                console.error('Error updating sensor data:', error);
            }
        }, API_CONFIG.SENSOR_UPDATE_INTERVAL);
    }

    // Initialize Gauges
    initGauges() {
        // Define which sensors to display on each gauge panel
        const leftGauges = [
            { canvasId: 'gaugeTemp', sensorId: 'temp_int' },
            { canvasId: 'gaugeHumidity', sensorId: 'humidity_int' },
            { canvasId: 'gaugeSoil', sensorId: 'soil' }
        ];

        const rightGauges = [
            { canvasId: 'gaugeGas', sensorId: 'gas' },
            { canvasId: 'gaugeWater', sensorId: 'water' },
            { canvasId: 'gaugePH', sensorId: 'ph' }
        ];

        // Initialize all gauges
        [...leftGauges, ...rightGauges].forEach(({ canvasId, sensorId }) => {
            const canvas = document.getElementById(canvasId);
            if (canvas) {
                const ctx = canvas.getContext('2d');
                this.gaugeCanvases[canvasId] = { ctx, sensorId };

                // Initial draw
                this.drawGauge(canvas, ctx, sensorId);

                // Add click event to open sensor popup
                canvas.addEventListener('click', () => {
                    const sensor = this.sensors.find(s => s.id === sensorId);
                    if (sensor) {
                        this.showSensorPopup(sensor);
                    }
                });
            }
        });

        // Update gauges periodically (synchronized with sensor updates)
        setInterval(() => {
            this.updateGauges();
        }, 1000);
    }

    // Draw a circular gauge
    drawGauge(canvas, ctx, sensorId) {
        const sensor = this.sensors.find(s => s.id === sensorId);
        if (!sensor) return;

        const sensorData = this.sensorValues[sensorId];
        if (!sensorData) return;

        const centerX = canvas.width / 2;
        const centerY = 90; // Position fixed for better text placement
        const radius = 60;
        const lineWidth = 12;

        // Clear canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Calculate gauge properties
        const value = sensorData.current;
        const min = sensor.min;
        const max = sensor.max;
        const percentage = Math.max(0, Math.min(1, (value - min) / (max - min)));

        // Draw background circle
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, 0.75 * Math.PI, 2.25 * Math.PI);
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
        ctx.lineWidth = lineWidth;
        ctx.lineCap = 'round';
        ctx.stroke();

        // Draw value arc with gradient
        const startAngle = 0.75 * Math.PI;
        const endAngle = startAngle + (percentage * 1.5 * Math.PI);

        const gradient = ctx.createLinearGradient(centerX - radius, centerY, centerX + radius, centerY);
        const colorHex = this.toHex(sensor.color);
        gradient.addColorStop(0, colorHex);
        gradient.addColorStop(1, this.adjustBrightness(colorHex, 40));

        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, endAngle);
        ctx.strokeStyle = gradient;
        ctx.lineWidth = lineWidth;
        ctx.lineCap = 'round';
        ctx.stroke();

        // Draw center circle (completely transparent)
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius - lineWidth / 2 - 5, 0, 2 * Math.PI);
        ctx.fillStyle = 'rgba(30, 41, 59, 0)'; // Complètement transparent
        ctx.fill();

        // Text shadow for better readability
        ctx.shadowColor = 'rgba(0, 0, 0, 0.8)';
        ctx.shadowBlur = 4;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 2;

        // Draw value text
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 22px -apple-system, BlinkMacSystemFont, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        const displayValue = value.toFixed(1);
        ctx.fillText(displayValue, centerX, centerY - 8);

        // Draw unit text
        ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
        ctx.font = '12px -apple-system, BlinkMacSystemFont, sans-serif';
        ctx.fillText(sensor.unit, centerX, centerY + 12);

        // Draw sensor name below gauge
        ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
        ctx.font = 'bold 12px -apple-system, BlinkMacSystemFont, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';

        // Split long names into multiple lines
        const name = sensor.name;
        const words = name.split(' ');
        if (words.length > 2) {
            ctx.fillText(words.slice(0, 2).join(' '), centerX, centerY + radius + 18);
            ctx.fillText(words.slice(2).join(' '), centerX, centerY + radius + 35);
        } else {
            ctx.fillText(name, centerX, centerY + radius + 18);
        }

        // Draw min/max labels
        ctx.fillStyle = 'rgba(255, 255, 255, 0.5)';
        ctx.font = '10px -apple-system, BlinkMacSystemFont, sans-serif';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(min.toString(), centerX - radius - 8, centerY + 20);
        ctx.textAlign = 'right';
        ctx.fillText(max.toString(), centerX + radius + 8, centerY + 20);

        // Reset shadow
        ctx.shadowColor = 'transparent';
        ctx.shadowBlur = 0;
    }

    // Update all gauges
    updateGauges() {
        Object.entries(this.gaugeCanvases).forEach(([canvasId, { ctx, sensorId }]) => {
            const canvas = document.getElementById(canvasId);
            if (canvas && ctx) {
                this.drawGauge(canvas, ctx, sensorId);
            }
        });
    }

    // Utility: Adjust color brightness
    adjustBrightness(hex, percent) {
        // Remove # if present
        hex = hex.replace('#', '');

        // Convert to RGB
        const r = parseInt(hex.substring(0, 2), 16);
        const g = parseInt(hex.substring(2, 4), 16);
        const b = parseInt(hex.substring(4, 6), 16);

        // Adjust brightness
        const newR = Math.min(255, Math.max(0, r + percent));
        const newG = Math.min(255, Math.max(0, g + percent));
        const newB = Math.min(255, Math.max(0, b + percent));

        // Convert back to hex
        return '#' +
            newR.toString(16).padStart(2, '0') +
            newG.toString(16).padStart(2, '0') +
            newB.toString(16).padStart(2, '0');
    }


    showSensorPopup(sensor) {
        this.currentSensor = sensor;
        const sensorData = this.sensorValues[sensor.id];

        // Check if this is an actuator
        const actuatorIds = ['fan1', 'fan2', 'pump', 'lamp1', 'lamp2'];
        const isActuator = actuatorIds.includes(sensor.id);

        // Update popup information
        document.getElementById('popupIcon').innerHTML = this.getSVGIcon(sensor.svg, this.toHex(sensor.color));
        document.getElementById('popupTitle').textContent = sensor.name;

        // For actuators, hide detailed info
        const sensorInfoDiv = document.querySelector('.sensor-info');
        const sensorValueDiv = document.querySelector('.sensor-value');
        const sensorStatusDiv = document.querySelector('.sensor-status');

        if (isActuator) {
            // Hide sensor details for actuators
            sensorInfoDiv.style.display = 'none';
            sensorValueDiv.style.display = 'none';
            sensorStatusDiv.style.display = 'none';
        } else {
            // Show sensor details for regular sensors
            sensorInfoDiv.style.display = 'grid';
            sensorValueDiv.style.display = 'block';
            sensorStatusDiv.style.display = 'flex';

            document.getElementById('sensorValue').textContent = `${sensorData.current.toFixed(1)} ${sensor.unit}`;
            document.getElementById('sensorId').textContent = sensor.id;
            document.getElementById('sensorPosition').textContent = `(${sensor.position.x.toFixed(1)}, ${sensor.position.y.toFixed(1)}, ${sensor.position.z.toFixed(1)})`;
            document.getElementById('lastUpdate').textContent = sensorData.lastUpdate.toLocaleTimeString();
            document.getElementById('batteryLevel').textContent = `${sensorData.battery}%`;
        }

        // Show/hide actuator controls
        const actuatorControls = document.getElementById('actuatorControlsInPopup');
        if (isActuator) {
            actuatorControls.style.display = 'block';

            // Setup button handlers
            const onBtn = document.getElementById('actuatorOnBtn');
            const offBtn = document.getElementById('actuatorOffBtn');

            // Remove previous listeners
            onBtn.replaceWith(onBtn.cloneNode(true));
            offBtn.replaceWith(offBtn.cloneNode(true));

            // Get fresh references
            const newOnBtn = document.getElementById('actuatorOnBtn');
            const newOffBtn = document.getElementById('actuatorOffBtn');

            // Fetch current actuator state from API
            this.fetchActuatorState(sensor.id).then(state => {
                if (state === 'ON') {
                    newOnBtn.classList.add('active');
                    newOffBtn.classList.remove('active');
                } else {
                    newOffBtn.classList.add('active');
                    newOnBtn.classList.remove('active');
                }
            });

            // Add new listeners
            newOnBtn.addEventListener('click', async () => {
                const result = await this.controlActuator(sensor.id, 'ON');
                if (result.success) {
                    newOffBtn.classList.remove('active');
                    newOnBtn.classList.add('active');
                }
            });

            newOffBtn.addEventListener('click', async () => {
                const result = await this.controlActuator(sensor.id, 'OFF');
                if (result.success) {
                    newOnBtn.classList.remove('active');
                    newOffBtn.classList.add('active');
                }
            });
        } else {
            actuatorControls.style.display = 'none';
        }

        // Show popup
        document.getElementById('sensorPopupOverlay').classList.add('active');
        document.getElementById('sensorPopup').classList.add('active');
    }

    closeSensorPopup() {
        document.getElementById('sensorPopupOverlay').classList.remove('active');
        document.getElementById('sensorPopup').classList.remove('active');
        this.currentSensor = null;
    }

    updateSensorPopupValues() {
        if (!this.currentSensor) return;

        const sensorData = this.sensorValues[this.currentSensor.id];
        if (!sensorData) return;

        // Update displayed values
        const sensorValueEl = document.getElementById('sensorValue');
        const lastUpdateEl = document.getElementById('lastUpdate');

        if (sensorValueEl) {
            sensorValueEl.textContent = `${sensorData.current.toFixed(1)} ${this.currentSensor.unit}`;
        }

        if (lastUpdateEl) {
            lastUpdateEl.textContent = sensorData.lastUpdate.toLocaleTimeString();
        }
    }

    showSensorChart() {
        if (!this.currentSensor) return;

        // Update chart popup information
        document.getElementById('chartPopupIcon').innerHTML = this.getSVGIcon(this.currentSensor.svg, this.toHex(this.currentSensor.color));
        document.getElementById('chartPopupTitle').textContent = `History - ${this.currentSensor.name}`;

        // Generate and display chart
        this.generateSensorChart();

        // Switch to chart popup
        document.getElementById('sensorPopup').classList.remove('active');
        document.getElementById('chartPopup').classList.add('active');
    }

    closeChartPopup() {
        document.getElementById('chartPopup').classList.remove('active');
        document.getElementById('sensorPopup').classList.add('active');

        // Destroy chart to free resources
        if (this.sensorChart) {
            this.sensorChart.destroy();
            this.sensorChart = null;
        }
    }

    async controlActuator(actuatorId, command) {
        try {
            console.log(`Controlling actuator ${actuatorId}: ${command}`);

            // Get authentication token
            const token = window.authManager ? window.authManager.token : sessionStorage.getItem('jwt_token');

            // Map actuator IDs to proper names
            const actuatorMap = {
                'fan1': 'Fan1',
                'fan2': 'Fan2',
                'pump': 'Pump',
                'lamp1': 'Bulb1',
                'lamp2': 'Bulb2'
            };

            const actuatorName = actuatorMap[actuatorId] || actuatorId;

            // Send control command to API (using Query Params as expected by ActuatorResource)
            const response = await fetch(`${API_CONFIG.ACTUATORS_URL}/${actuatorName}/command?command=${command}`, {
                method: 'POST',
                headers: {
                    // 'Content-Type': 'application/json' // Not needed for query params
                },
                credentials: 'include'
            });

            if (response.ok) {
                const result = await response.text();
                console.log(`✓ Actuator ${actuatorName} ${command} successful:`, result);

                // Update stored actuator state
                if (this.sensorValues[actuatorId]) {
                    this.sensorValues[actuatorId].data = {
                        state: command,
                        lastCommand: command
                    };
                }

                return { success: true, data: result };
            } else {
                const errorText = await response.text();
                console.error(`✗ Failed to control actuator: ${response.status} - ${errorText}`);
                return { success: false, error: `HTTP ${response.status}` };
            }
        } catch (error) {
            console.error('Error controlling actuator:', error);
            return { success: false, error: error.message };
        }
    }

    async fetchActuatorState(actuatorId) {
        try {
            // Map actuator IDs to proper names
            const actuatorMap = {
                'fan1': 'Fan1',
                'fan2': 'Fan2',
                'pump': 'Pump',
                'lamp1': 'Bulb1',
                'lamp2': 'Bulb2'
            };

            const actuatorName = actuatorMap[actuatorId] || actuatorId;

            // Fetch actuator data from API
            const response = await fetch(`${API_CONFIG.ACTUATORS_URL}/${actuatorName}`, {
                credentials: 'include'
            });

            if (response.ok) {
                const actuator = await response.json();
                return actuator.state || actuator.lastCommand || 'OFF';
            }

            return 'OFF'; // Default to OFF if not found
        } catch (error) {
            console.error('Error fetching actuator state:', error);
            return 'OFF';
        }
    }


    async generateSensorChart() {
        const ctx = document.getElementById('sensorChart').getContext('2d');

        // Get historical data for chart
        const timeRange = '1h';
        const data = await this.generateHistoricalData(this.currentSensor, timeRange);

        // Destroy existing chart
        if (this.sensorChart) {
            this.sensorChart.destroy();
        }

        // Create new chart
        this.sensorChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [{
                    label: this.currentSensor.name,
                    data: data.values,
                    borderColor: this.toHex(this.currentSensor.color),
                    backgroundColor: this.toHex(this.currentSensor.color) + '20',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointBackgroundColor: this.toHex(this.currentSensor.color),
                    pointBorderColor: '#ffffff',
                    pointBorderWidth: 2,
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: false,
                        grid: {
                            color: 'rgba(255, 255, 255, 0.1)'
                        },
                        ticks: {
                            color: 'white',
                            font: {
                                size: 11
                            }
                        }
                    },
                    x: {
                        grid: {
                            color: 'rgba(255, 255, 255, 0.1)'
                        },
                        ticks: {
                            color: 'white',
                            font: {
                                size: 11
                            }
                        }
                    }
                },
                plugins: {
                    legend: {
                        labels: {
                            color: 'white',
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(45, 45, 45, 0.9)',
                        titleColor: 'white',
                        bodyColor: 'white',
                        borderColor: this.toHex(this.currentSensor.color),
                        borderWidth: 1
                    }
                }
            }
        });

        // Update statistics
        this.updateChartStats(data.values);
    }

    async generateHistoricalData(sensor, timeRange) {
        try {
            // Fetch historical data from API
            const apiSensorId = sensor.id;
            const response = await fetch(`${API_CONFIG.SENSORS_URL}?type=${apiSensorId}`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('Failed to fetch historical data');
            }

            const allSensors = await response.json();

            // Filter sensors matching this type and sort by time
            const historicalData = allSensors
                .filter(s => {
                    const sensorId = s.sensor_id || s.sensorId || '';
                    return sensorId.includes(sensor.id) || s.type === sensor.id;
                })
                .sort((a, b) => {
                    const timeA = new Date(a.measurement_time || a.measurementTime);
                    const timeB = new Date(b.measurement_time || b.measurementTime);
                    return timeA - timeB;
                });

            if (historicalData.length === 0) {
                // No historical data, use current value
                return this.generateSimulatedData(sensor, timeRange);
            }

            // Determine time window based on range
            const now = new Date();
            let startTime;
            switch (timeRange) {
                case '1h':
                    startTime = new Date(now.getTime() - 60 * 60 * 1000);
                    break;
                case '6h':
                    startTime = new Date(now.getTime() - 6 * 60 * 60 * 1000);
                    break;
                case '24h':
                    startTime = new Date(now.getTime() - 24 * 60 * 60 * 1000);
                    break;
                case '7d':
                    startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                    break;
                default:
                    startTime = new Date(now.getTime() - 60 * 60 * 1000);
            }

            // Filter data within time range
            const filteredData = historicalData.filter(s => {
                const time = new Date(s.measurement_time || s.measurementTime);
                return time >= startTime;
            });

            const labels = [];
            const values = [];

            filteredData.forEach(s => {
                const time = new Date(s.measurement_time || s.measurementTime);
                const value = s.value !== undefined ? s.value : 0;

                labels.push(time.toLocaleTimeString());
                values.push(value);
            });

            // If we have very little data, supplement with simulated
            if (labels.length < 3) {
                return this.generateSimulatedData(sensor, timeRange);
            }

            return { labels, values };

        } catch (error) {
            console.error('Error fetching historical data:', error);
            // Fallback to simulated data
            return this.generateSimulatedData(sensor, timeRange);
        }
    }

    generateSimulatedData(sensor, timeRange) {
        const now = new Date();
        let labels = [];
        let values = [];
        let dataPoints = 24;

        // Determine number of data points and interval based on time range
        switch (timeRange) {
            case '1h':
                dataPoints = 12;
                break;
            case '6h':
                dataPoints = 24;
                break;
            case '24h':
                dataPoints = 48;
                break;
            case '7d':
                dataPoints = 28;
                break;
        }

        // Generate time labels
        for (let i = dataPoints - 1; i >= 0; i--) {
            const time = new Date(now);

            switch (timeRange) {
                case '1h':
                    time.setMinutes(now.getMinutes() - i * 5);
                    labels.push(time.getMinutes() + 'min');
                    break;
                case '6h':
                    time.setMinutes(now.getMinutes() - i * 15);
                    labels.push(time.getHours() + ':' + time.getMinutes().toString().padStart(2, '0'));
                    break;
                case '24h':
                    time.setHours(now.getHours() - i * 0.5);
                    labels.push(time.getHours() + 'h');
                    break;
                case '7d':
                    time.setDate(now.getDate() - i);
                    labels.push(time.getDate() + '/' + (time.getMonth() + 1));
                    break;
            }

            // Use current value without random variation
            const value = this.sensorValues[sensor.id].current;
            values.push(value);
        }

        return { labels, values };
    }

    updateChartStats(values) {
        const sum = values.reduce((a, b) => a + b, 0);
        const avg = sum / values.length;
        const max = Math.max(...values);
        const min = Math.min(...values);

        // Calculate trend (comparison of first and last points)
        const trend = values[values.length - 1] - values[0];

        document.getElementById('averageValue').textContent = avg.toFixed(1);
        document.getElementById('maxValue').textContent = max.toFixed(1);
        document.getElementById('minValue').textContent = min.toFixed(1);
        document.getElementById('trendValue').textContent = trend >= 0 ? '↗ Rising' : '↘ Falling';
        document.getElementById('trendValue').style.color = trend >= 0 ? '#10b981' : '#ef4444';
    }

    async changeTimeRange(range) {
        // Regenerate chart with new time range
        if (this.currentSensor) {
            const data = await this.generateHistoricalData(this.currentSensor, range);

            this.sensorChart.data.labels = data.labels;
            this.sensorChart.data.datasets[0].data = data.values;
            this.sensorChart.update();

            this.updateChartStats(data.values);
        }
    }

    async refreshChart() {
        // Refresh chart with latest data (keep current time range)
        if (this.currentSensor && this.sensorChart) {
            // Get current time range from active button
            const activeBtn = document.querySelector('.time-btn.active');
            const timeRange = activeBtn ? activeBtn.dataset.timeRange : '1h';

            const data = await this.generateHistoricalData(this.currentSensor, timeRange);

            this.sensorChart.data.labels = data.labels;
            this.sensorChart.data.datasets[0].data = data.values;
            this.sensorChart.update();

            this.updateChartStats(data.values);
        }
    }

    // Utility methods
    getSVGIcon(type, colorHex) {
        const icons = {
            thermometer: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M14 4v10a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0z"/>
                <path d="M12 14v4"/>
            </svg>`,
            droplet: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
            </svg>`,
            plant: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 22v-8m0 0c-2-3-5-5-5-8a5 5 0 0 1 10 0c0 3-3 5-5 8z"/>
                <path d="M7 12c0-3 2-5 5-5s5 2 5 5"/>
            </svg>`,
            sun: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <circle cx="12" cy="12" r="4"/>
                <path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32l1.41-1.41"/>
            </svg>`,
            wind: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M9 10h6a3 3 0 0 1 0 6H9m0-6a3 3 0 0 0 0-6h6m-6 6v6"/>
            </svg>`,
            water: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M2 12h4l3-9 4 18 3-9h4"/>
            </svg>`,
            flask: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M10 2v8L6 18a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l-4-8V2"/>
                <path d="M8 2h8"/>
            </svg>`,
            gauge: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"/>
                <path d="M12 8V2m0 20v-6M8 12H2m20 0h-6"/>
            </svg>`,
            fan: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <circle cx="12" cy="12" r="2"/>
                <path d="M12 10V2m0 20v-8M14 12h8M2 12h8m1-5l-7-2m18 2l-7 2m-1 3l2 7m-2-18l2 7m3 1l7 2m-18-2l7-2"/>
            </svg>`,
            pump: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 2v4m0 12v4M20 12h-4M8 12H4m13.66-5.66l-2.83 2.83M8.17 15.83l-2.83 2.83m11.32 0l-2.83-2.83M8.17 8.17L5.34 5.34"/>
            </svg>`,
            lightbulb: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M9 18h6m-3-5v5m0-13a5 5 0 0 1 5 5c0 3-2 4-2 7H9c0-3-2-4-2-7a5 5 0 0 1 5-5z"/>
                <path d="M9 21h6"/>
            </svg>`
        };

        return icons[type] || icons.gauge;
    }

    toHex(color) {
        return '#' + color.toString(16).padStart(6, '0');
    }

    // 3D control methods
    resetCamera() {
        if (window.mainApp) {
            window.mainApp.resetCamera();
        }
    }

    toggleAutoRotate() {
        if (window.mainApp) {
            window.mainApp.toggleAutoRotate();
        }
    }

    // PWA methods
    initPWA() {
        // Service Worker registration
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('/sw.js')
                    .then((registration) => {
                        console.log('SW registered: ', registration);
                    })
                    .catch((registrationError) => {
                        console.log('SW registration failed: ', registrationError);
                    });
            });
        }

        // App installation handling
        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            this.deferredPrompt = e;
            this.showInstallPromotion();
        });

        window.addEventListener('appinstalled', () => {
            this.hideInstallPromotion();
            this.deferredPrompt = null;
            console.log('PWA was installed');
        });
    }

    showInstallPromotion() {
        const installPrompt = document.getElementById('installPrompt');
        installPrompt.classList.add('active');

        document.getElementById('installButton').addEventListener('click', async () => {
            if (this.deferredPrompt) {
                this.deferredPrompt.prompt();
                const { outcome } = await this.deferredPrompt.userChoice;
                console.log(`User response to the install prompt: ${outcome}`);
                this.deferredPrompt = null;
                this.hideInstallPromotion();
            }
        });

        document.getElementById('dismissButton').addEventListener('click', () => {
            this.hideInstallPromotion();
        });
    }

    hideInstallPromotion() {
        const installPrompt = document.getElementById('installPrompt');
        installPrompt.classList.remove('active');
    }
}

// Create and export global instance
export const mvp = new GreenhousePresenter();

// Make available globally for non-module scripts
window.mvp = mvp;