// mvp.js - Data management and business logic
import { API_CONFIG } from './config.js';
import { authManager } from './auth.js';
import { greenhouseManager } from './greenhouse.js';

class GreenhousePresenter {
    constructor() {
        // Template for sensor/actuator default properties
        this.sensorTemplates = {
            temperature: { svg: 'thermometer', color: 0xff6b6b, unit: '°C', min: 0, max: 50, warningHigh: 30, criticalHigh: 35 },
            humidity: { svg: 'droplet', color: 0x95e1d3, unit: '%', min: 0, max: 100, warningLow: 30, warningHigh: 80, criticalHigh: 90 },
            soil_moisture: { svg: 'droplet', color: 0xf9ca24, unit: '%', min: 0, max: 100, warningLow: 20, criticalLow: 10 },
            light: { svg: 'sun', color: 0xffd93d, unit: '%', min: 0, max: 100 },
            water_level: { svg: 'droplet', color: 0x74b9ff, unit: '%', min: 0, max: 100, warningLow: 20, criticalLow: 10 },
            pump: { svg: 'pump', color: 0x5f27cd, unit: '', min: 0, max: 1, isActuator: true },
            fan: { svg: 'fan', color: 0x00d2ff, unit: '', min: 0, max: 1, isActuator: true },
            led: { svg: 'led', color: 0xfeca57, unit: '', min: 0, max: 1, isActuator: true },
            heater: { svg: 'heater', color: 0xff6348, unit: '', min: 0, max: 1, isActuator: true }
        };

        // Default positions (will be overridden if we have many sensors)
        this.defaultPositions = [
            { x: -0.5, y: 0.5, z: 0.5 },
            { x: 0.5, y: 0.5, z: -0.5 },
            { x: 0.0, y: 0.3, z: 0.0 },
            { x: -0.3, y: 0.3, z: 0.8 },
            { x: -0.8, y: 0.4, z: -0.3 },
            { x: 0.7, y: 0.3, z: 1.0 },
            { x: -0.4, y: 0.6, z: -0.8 },
            { x: 0.0, y: 0.5, z: -0.8 },
            { x: 0.0, y: 0.5, z: 0.8 }
        ];

        // Dynamic sensors array - populated from API
        this.sensors = [];

        this.sensorValues = {};
        this.actuatorStates = {};
        this.currentSensor = null;
        this.sensorChart = null;
        this.deferredPrompt = null;
        this.gaugeCanvases = {}; // Store gauge canvas contexts
        this.gaugeUpdateInterval = null;

        // Note: initGauges() is called after sensors are loaded from API
        this.initPWA();
    }


    async initializeSensorValues() {
        // Try to fetch sensor data specific to this greenhouse from API
        try {
            const greenhouseId = greenhouseManager.getCurrentGreenhouseId();

            if (!greenhouseId) {
                console.warn('⚠️  No greenhouse selected, cannot fetch sensors');
                return;
            }

            // Clear old sensor data and sensors array
            this.sensorValues = {};
            this.sensors = [];

            // Use the new endpoint: GET /greenhouses/{id}/sensors
            const url = `${API_CONFIG.SMART_GREENHOUSE_API_URL}/greenhouses/${greenhouseId}/sensors`;

            const response = await authManager.fetchWithAuth(url);

            if (response.ok) {
                const apiSensors = await response.json();
                console.log('✓ Loaded sensor data for greenhouse', greenhouseId, ':', apiSensors);

                // Create sensors dynamically from API response
                let positionIndex = 0;
                apiSensors.forEach(apiSensor => {
                    // Normalize API data fields
                    const apiId = apiSensor.id || apiSensor._id;
                    const sensorId = apiSensor.sensor_id || apiSensor.sensorId || apiId;
                    const sensorType = apiSensor.type || this.guessSensorType(sensorId);
                    const sensorValue = apiSensor.value !== undefined ? apiSensor.value : (apiSensor.measurement !== undefined ? apiSensor.measurement : 0);
                    const sensorTime = apiSensor.measurementTime || apiSensor.measurement_time || new Date();

                    // Get template based on type
                    const template = this.sensorTemplates[sensorType] || { svg: 'gauge', color: 0xaaaaaa, unit: '', min: 0, max: 100 };

                    // Create sensor object using API ID
                    const sensor = {
                        id: sensorId,  // Use ID from API
                        name: apiSensor.name || this.formatSensorName(sensorId),
                        svg: template.svg,
                        color: template.color,
                        position: this.defaultPositions[positionIndex % this.defaultPositions.length],
                        unit: template.unit,
                        min: template.min,
                        max: template.max,
                        warningLow: template.warningLow,
                        warningHigh: template.warningHigh,
                        criticalLow: template.criticalLow,
                        criticalHigh: template.criticalHigh,
                        isActuator: false
                    };

                    this.sensors.push(sensor);
                    console.log(`✓ Created sensor: ${sensor.id} (${sensor.name})`);

                    // Initialize sensor value
                    this.sensorValues[sensorId] = {
                        current: sensorValue,
                        unit: sensor.unit,
                        lastUpdate: new Date(sensorTime),
                        battery: apiSensor.battery || 100,
                        status: apiSensor.status || 'active',
                        data: apiSensor // Keep original API data
                    };

                    positionIndex++;
                });

                // Start periodic updates
                this.startSensorUpdates();

                // Now load actuators
                await this.initializeActuatorStates();
            } else {
                throw new Error(`API returned ${response.status}`);
            }
        } catch (error) {
            console.warn('⚠ Could not fetch sensor data from API:', error.message);
        }
    }

    guessSensorType(sensorId) {
        const id = sensorId.toLowerCase();
        if (id.includes('temp')) return 'temperature';
        if (id.includes('hum')) return 'humidity';
        if (id.includes('soil') || id.includes('moisture')) return 'soil_moisture';
        if (id.includes('light')) return 'light';
        if (id.includes('water')) return 'water_level';
        return 'unknown';
    }

    guessActuatorType(actuatorId) {
        const id = actuatorId.toLowerCase();
        if (id.includes('led') || id.includes('lamp') || id.includes('light')) return 'led';
        if (id.includes('heater')) return 'heater';
        if (id.includes('fan') || id.includes('vent')) return 'fan';
        if (id.includes('pump') || id.includes('water')) return 'pump';
        return 'unknown';
    }

    formatSensorName(sensorId) {
        // Convert 'temp1' to 'Temperature 1', 'hum1' to 'Humidity 1', etc.
        return sensorId
            .replace(/temp/i, 'Température')
            .replace(/hum/i, 'Humidité')
            .replace(/soil/i, 'Sol')
            .replace(/light/i, 'Lumière')
            .replace(/water/i, 'Eau')
            .replace(/(\d+)/, ' $1');
    }
    fillMissingSensorsWithMockData() {
        // Mock data generation disabled to prevent ghost sensors
    }

    async initializeActuatorStates() {
        try {
            const greenhouseId = greenhouseManager.getCurrentGreenhouseId();

            if (!greenhouseId) {
                console.warn('⚠️  No greenhouse selected, cannot fetch actuators');
                return;
            }

            // Clear old actuator data
            this.actuatorStates = {};

            // Use the new endpoint: GET /greenhouses/{id}/actuators
            const url = `${API_CONFIG.SMART_GREENHOUSE_API_URL}/greenhouses/${greenhouseId}/actuators`;

            const response = await authManager.fetchWithAuth(url);
            if (!response.ok) {
                console.warn('Could not fetch actuators list, status:', response.status);
                return;
            }

            const apiActuators = await response.json();
            console.log('✓ Loaded actuators for greenhouse', greenhouseId, ':', apiActuators);

            let positionIndex = this.sensors.length; // Continue from where sensors left off

            for (const actuator of apiActuators) {
                const actuatorId = actuator.actuator_id || actuator.actuatorId || actuator.id;
                const actuatorType = actuator.type || this.guessActuatorType(actuatorId);

                try {
                    // Fetch state
                    const state = actuator.state || actuator.lastCommand || await this.fetchActuatorState(actuatorId);

                    // Get template based on type
                    const template = this.sensorTemplates[actuatorType] || { svg: 'gauge', color: 0xaaaaaa, unit: '', min: 0, max: 1, isActuator: true };

                    // Create actuator sensor object
                    const actuatorSensor = {
                        id: actuatorId,  // Use ID from API
                        name: actuator.name || this.formatActuatorName(actuatorId),
                        svg: template.svg,
                        color: template.color,
                        position: this.defaultPositions[positionIndex % this.defaultPositions.length],
                        unit: template.unit,
                        min: template.min,
                        max: template.max,
                        isActuator: true
                    };

                    this.sensors.push(actuatorSensor);
                    console.log(`✓ Created actuator: ${actuatorSensor.id} (${actuatorSensor.name})`);

                    // Add to actuatorStates
                    this.actuatorStates[actuatorId] = {
                        present: true,
                        state: state,
                        value: state === 'ON' ? 1 : 0,
                        lastUpdate: new Date(),
                        unit: '',
                        battery: 100,
                        status: 'active',
                        data: {
                            state: state,
                            lastCommand: state
                        }
                    };

                    // Also add to sensorValues for unified access
                    this.sensorValues[actuatorId] = {
                        current: state === 'ON' ? 1 : 0,
                        unit: '',
                        lastUpdate: new Date(),
                        battery: 100,
                        status: 'active'
                    };

                    positionIndex++;
                } catch (error) {
                    console.error(`Error initializing actuator ${actuatorId}:`, error);
                }
            }

            // Update gauges after loading all sensors and actuators
            this.initGauges();
            this.updateGauges();

            // Refresh 3D sprites now that sensors are loaded
            if (window.mainApp) {
                window.mainApp.refreshSensors();
            }
        } catch (error) {
            console.error('Error initializing actuators:', error);
        }
    }

    guessActuatorType(actuatorId) {
        const id = actuatorId.toLowerCase();
        if (id.includes('pump')) return 'pump';
        if (id.includes('fan')) return 'fan';
        if (id.includes('led') || id.includes('light') || id.includes('lamp')) return 'led';
        if (id.includes('heat')) return 'heater';
        return 'unknown';
    }

    formatActuatorName(actuatorId) {
        // Convert 'pump1' to 'Pompe 1', 'fan1' to 'Ventilateur 1', etc.
        return actuatorId
            .replace(/pump/i, 'Pompe')
            .replace(/fan/i, 'Ventilateur')
            .replace(/led/i, 'LED')
            .replace(/heater/i, 'Chauffage')
            .replace(/(\d+)/, ' $1');
    }

    startSensorUpdates() {
        // Update sensor data periodically
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }

        this.updateInterval = setInterval(async () => {
            try {
                const greenhouseId = greenhouseManager.getCurrentGreenhouseId();

                if (!greenhouseId) {
                    console.warn('⚠️  No greenhouse selected, skipping sensor update');
                    return;
                }

                // Use the correct greenhouse-specific endpoint
                const url = `${API_CONFIG.SMART_GREENHOUSE_API_URL}/greenhouses/${greenhouseId}/sensors`;

                const response = await authManager.fetchWithAuth(url);
                if (response.ok) {
                    const sensors = await response.json();

                    sensors.forEach(apiSensor => {
                        // Normalize API data fields
                        const sensorId = apiSensor.id || apiSensor._id;
                        const apiSensorId = apiSensor.sensor_id || apiSensor.sensorId || '';
                        const sensorType = apiSensor.type || '';
                        const sensorValue = apiSensor.value !== undefined ? apiSensor.value : (apiSensor.measurement !== undefined ? apiSensor.measurement : 0);
                        const sensorTime = apiSensor.measurementTime || apiSensor.measurement_time || new Date();

                        // Try direct ID match first (e.g., temp1 === temp1)
                        let matchingSensor = this.sensors.find(s => s.id === apiSensorId);

                        // Fallback: fuzzy matching
                        if (!matchingSensor) {
                            matchingSensor = this.sensors.find(s => {
                                return apiSensorId.includes(s.id) || s.id.includes(apiSensorId);
                            });
                        }

                        // Fallback: type-based matching
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
        // Gauges will be populated after sensors are loaded from API
        // This is now called after initializeSensorValues completes

        if (this.sensors.length === 0) {
            console.warn('No sensors loaded yet, gauges will be empty');
            return;
        }

        // Use first 6 sensors/actuators for the 6 gauge slots
        const gaugeSlots = [
            { canvasId: 'gaugeTemp' },
            { canvasId: 'gaugeHumidity' },
            { canvasId: 'gaugeSoil' },
            { canvasId: 'gaugeGas' },
            { canvasId: 'gaugeWater' },
            { canvasId: 'gaugePH' }
        ];

        for (let i = 0; i < gaugeSlots.length && i < this.sensors.length; i++) {
            const { canvasId } = gaugeSlots[i];
            const sensor = this.sensors[i];

            const canvas = document.getElementById(canvasId);
            if (canvas) {
                const ctx = canvas.getContext('2d');
                this.gaugeCanvases[canvasId] = { ctx, sensorId: sensor.id };

                // Initial draw
                this.drawGauge(canvas, ctx, sensor.id);

                // Add click event to open sensor popup
                canvas.addEventListener('click', () => {
                    this.showSensorPopup(sensor);
                });
            }
        }

        // Update gauges periodically (synchronized with sensor updates)
        if (!this.gaugeUpdateInterval) {
            this.gaugeUpdateInterval = setInterval(() => {
                this.updateGauges();
            }, 1000);
        }
    }

    // Draw a circular gauge
    drawGauge(canvas, ctx, sensorId) {
        // Always clear canvas first to prevent stale data
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const sensor = this.sensors.find(s => s.id === sensorId);

        // If sensor doesn't exist or has no data, show "No Data" or blank
        const sensorData = this.sensorValues[sensorId];
        if (!sensor || !sensorData) {
            // Draw placeholder/empty state
            const centerX = canvas.width / 2;
            const centerY = 90;
            const radius = 60;

            // Draw faint background circle
            ctx.beginPath();
            ctx.arc(centerX, centerY, radius, 0.75 * Math.PI, 2.25 * Math.PI);
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
            ctx.lineWidth = 12;
            ctx.lineCap = 'round';
            ctx.stroke();

            // Text "No Data"
            ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
            ctx.font = '14px -apple-system, BlinkMacSystemFont, sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText('Waiting...', centerX, centerY);

            // Draw label if possible (from ID guess)
            if (sensorId) {
                ctx.font = 'bold 12px -apple-system, BlinkMacSystemFont, sans-serif';
                ctx.fillText(sensorId.toUpperCase(), centerX, centerY + radius + 18);
            }
            return;
        }

        const centerX = canvas.width / 2;
        const centerY = 90; // Position fixed for better text placement
        const radius = 60;
        const lineWidth = 12;

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
        const name = sensor.id;
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
    // Update all gauges - Dynamic Generation
    updateGauges() {
        const leftPanel = document.getElementById('gaugesLeft');
        const rightPanel = document.getElementById('gaugesRight');

        if (!leftPanel || !rightPanel) return;

        // User request: "les gauges soient utiliser seulement pour les capteurs et non pas les actuateurs"
        // So we filter for gauges ONLY. 
        // Note: this.sensors still contains everything for the 3D view (sprites).
        const sensorsForGauges = this.sensors.filter(s => !s.isActuator);

        // Iterate through sensors meant for gauges
        sensorsForGauges.forEach((sensor, index) => {
            const canvasId = `gauge_${sensor.id}`;
            let canvas = document.getElementById(canvasId);

            if (!canvas) {
                // Create new canvas if it doesn't exist
                canvas = document.createElement('canvas');
                canvas.id = canvasId;
                canvas.className = 'gauge-canvas';
                canvas.width = 180;
                canvas.height = 200;

                // Add click listener for popup
                canvas.addEventListener('click', () => {
                    this.showSensorPopup(sensor);
                });

                // append to panels alternately or based on index
                if (index % 2 === 0) {
                    leftPanel.appendChild(canvas);
                } else {
                    rightPanel.appendChild(canvas);
                }
            }

            // Get or create context
            let ctx = this.gaugeCanvases[canvasId]?.ctx;
            if (!ctx) {
                ctx = canvas.getContext('2d');
                this.gaugeCanvases[canvasId] = { ctx, sensorId: sensor.id };
            } else {
                // Update mapping to use current sensor ID
                this.gaugeCanvases[canvasId].sensorId = sensor.id;
            }

            // Draw gauge with the correct sensor data
            this.drawGauge(canvas, ctx, sensor.id);
        });

        // Cleanup: Remove any canvases that no longer have a corresponding sensor in the gauge list
        const validGaugeIds = new Set(sensorsForGauges.map(s => `gauge_${s.id}`));
        [...leftPanel.children, ...rightPanel.children].forEach(child => {
            if (child.tagName === 'CANVAS' && !validGaugeIds.has(child.id)) {
                child.remove();
                delete this.gaugeCanvases[child.id];
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

        // Check if this is an actuator using the isActuator flag
        const isActuator = sensor.isActuator === true;

        // Update popup information
        document.getElementById('popupIcon').innerHTML = this.getSVGIcon(sensor.svg, this.toHex(sensor.color));
        document.getElementById('popupTitle').textContent = sensor.id;

        // For actuators, hide detailed sensor info
        const sensorInfoDiv = document.querySelector('.sensor-info');
        const sensorValueDiv = document.querySelector('.sensor-value');
        const sensorStatusDiv = document.querySelector('.sensor-status');

        if (isActuator) {
            // Hide sensor details for actuators
            sensorInfoDiv.style.display = 'none';
            sensorValueDiv.style.display = 'none';
            sensorStatusDiv.style.display = 'none';
        } else if (sensorData) {
            // Show sensor details for regular sensors
            sensorInfoDiv.style.display = 'grid';
            sensorValueDiv.style.display = 'block';
            sensorStatusDiv.style.display = 'flex';

            document.getElementById('sensorValue').textContent = `${sensorData.current.toFixed(1)} ${sensor.unit}`;
            document.getElementById('sensorId').textContent = sensor.id;
            document.getElementById('sensorPosition').textContent = `(${sensor.position.x.toFixed(1)}, ${sensor.position.y.toFixed(1)}, ${sensor.position.z.toFixed(1)})`;
            document.getElementById('lastUpdate').textContent = sensorData.lastUpdate.toLocaleTimeString();
            document.getElementById('batteryLevel').textContent = `${sensorData.battery}%`;
        } else {
            // No data available yet
            sensorInfoDiv.style.display = 'grid';
            sensorValueDiv.style.display = 'block';
            sensorStatusDiv.style.display = 'flex';

            document.getElementById('sensorValue').textContent = `-- ${sensor.unit}`;
            document.getElementById('sensorId').textContent = sensor.id;
            document.getElementById('sensorPosition').textContent = `(${sensor.position.x.toFixed(1)}, ${sensor.position.y.toFixed(1)}, ${sensor.position.z.toFixed(1)})`;
            document.getElementById('lastUpdate').textContent = 'Waiting for data...';
            document.getElementById('batteryLevel').textContent = '--';
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

            // Map actuator IDs to proper names - REMOVED: Using direct IDs from discovery
            // We use the ID as provided by the API/Discovery
            const actuatorName = actuatorId;

            // Send control command to API (using Query Params as expected by ActuatorResource)
            const response = await authManager.fetchWithAuth(`${API_CONFIG.ACTUATORS_URL}/${actuatorName}/command?command=${command}`, {
                method: 'POST'
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
            // Map actuator IDs - REMOVED: Using direct IDs
            const actuatorName = actuatorId;

            // Fetch actuator data from API
            const response = await authManager.fetchWithAuth(`${API_CONFIG.ACTUATORS_URL}/${actuatorName}`);

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
            temperature: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/>
            </svg>`,
            humidity: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
            </svg>`,
            soil_moisture: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
                <path d="M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" />
            </svg>`,
            light: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <circle cx="12" cy="12" r="5"/>
                <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>`,
            water_level: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M12 2v20M2 12h20"/>
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
            </svg>`,
            heater: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <path d="M5 12h14M5 16h14M5 8h14M9 4v2M15 4v2M9 18v2M15 18v2"/>
            </svg>`,
            led: `<svg viewBox="0 0 24 24" fill="none" stroke="${colorHex}" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <circle cx="12" cy="12" r="4" fill="${colorHex}"/>
                <path d="M18 6l2-2M6 18l-2 2M6 6L4 4M18 18l2 2"/>
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
                console.log(`User response to the install prompt: ${outcome} `);
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
export const greenhousePresenter = mvp; // Alias for app-init.js

// Make available globally for non-module scripts
window.mvp = mvp;