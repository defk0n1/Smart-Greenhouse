// mvp.js - Data management and business logic
class GreenhousePresenter {
    constructor() {
        this.sensors = [
            { 
                id: 'temp_int', 
                name: 'Internal Temperature', 
                svg: 'thermometer',
                color: 0xff6b6b,
                position: { x: -0.1, y: 0.3, z: 0.0 },
                unit: '°C',
                min: 15,
                max: 35
            },
            { 
                id: 'temp_ext', 
                name: 'External Temperature', 
                svg: 'thermometer',
                color: 0xff4757,
                position: { x: -0.1, y: 0.7, z: 0.0 },
                unit: '°C',
                min: -5,
                max: 40
            },
            { 
                id: 'humidity_int', 
                name: 'Internal Humidity', 
                svg: 'droplet',
                color: 0x4ecdc4,
                position: { x: 0.2, y: 0.3, z: 0.0 },
                unit: '%',
                min: 30,
                max: 80
            },
            { 
                id: 'humidity_ext', 
                name: 'External Humidity', 
                svg: 'droplet',
                color: 0x3dc4c4,
                position: { x: 0.2, y: 0.7, z: 0.0 },
                unit: '%',
                min: 20,
                max: 100
            },
            { 
                id: 'soil', 
                name: 'Soil Moisture', 
                svg: 'plant',
                color: 0x95e1d3,
                position: { x: 0.4, y: 0.3, z: -0.8 },
                unit: '%',
                min: 0,
                max: 100
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
                id: 'co2', 
                name: 'CO2 Sensor', 
                svg: 'wind',
                color: 0xa8e6cf,
                position: { x: 0.0, y: 0.3, z: 1.0 },
                unit: 'ppm',
                min: 300,
                max: 2000
            },
            { 
                id: 'water', 
                name: 'Water Level', 
                svg: 'water',
                color: 0x74b9ff,
                position: { x: 0.4, y: 0.3, z: 1.4 },
                unit: 'cm',
                min: 0,
                max: 100
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
        
        this.initializeSensorValues();
        this.initPWA();
    }

    initializeSensorValues() {
        this.sensors.forEach(sensor => {
            const value = Math.random() * (sensor.max - sensor.min) + sensor.min;
            this.sensorValues[sensor.id] = {
                current: value,
                unit: sensor.unit,
                lastUpdate: new Date(),
                battery: Math.floor(Math.random() * 30) + 70
            };
        });
    }

    // Popup methods
    showSensorPopup(sensor) {
        this.currentSensor = sensor;
        const sensorData = this.sensorValues[sensor.id];
        
        // Update popup information
        document.getElementById('popupIcon').innerHTML = this.getSVGIcon(sensor.svg, this.toHex(sensor.color));
        document.getElementById('popupTitle').textContent = sensor.name;
        document.getElementById('sensorValue').textContent = `${sensorData.current.toFixed(1)} ${sensor.unit}`;
        document.getElementById('sensorId').textContent = sensor.id;
        document.getElementById('sensorPosition').textContent = `(${sensor.position.x.toFixed(1)}, ${sensor.position.y.toFixed(1)}, ${sensor.position.z.toFixed(1)})`;
        document.getElementById('lastUpdate').textContent = sensorData.lastUpdate.toLocaleTimeString();
        document.getElementById('batteryLevel').textContent = `${sensorData.battery}%`;
        
        // Show popup
        document.getElementById('sensorPopupOverlay').classList.add('active');
        document.getElementById('sensorPopup').classList.add('active');
    }

    closeSensorPopup() {
        document.getElementById('sensorPopupOverlay').classList.remove('active');
        document.getElementById('sensorPopup').classList.remove('active');
        this.currentSensor = null;
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

    generateSensorChart() {
        const ctx = document.getElementById('sensorChart').getContext('2d');
        
        // Generate simulated data for chart
        const timeRange = '1h';
        const data = this.generateSimulatedData(this.currentSensor, timeRange);
        
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

    generateSimulatedData(sensor, timeRange) {
        const now = new Date();
        let labels = [];
        let values = [];
        let dataPoints = 24;
        
        // Determine number of data points and interval based on time range
        switch(timeRange) {
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
            
            switch(timeRange) {
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
            
            // Generate realistic value with some variation
            const baseValue = this.sensorValues[sensor.id].current;
            const variation = (Math.random() - 0.5) * (sensor.max - sensor.min) * 0.1;
            let value = baseValue + variation;
            
            // Ensure value stays within limits
            value = Math.max(sensor.min, Math.min(sensor.max, value));
            
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

    changeTimeRange(range) {
        // Regenerate chart with new time range
        if (this.currentSensor) {
            const data = this.generateSimulatedData(this.currentSensor, range);
            
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

// Create global instance
const mvp = new GreenhousePresenter();