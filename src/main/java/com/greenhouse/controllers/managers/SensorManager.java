package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.entities.Sensor;
import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class SensorManager {

    private static final Logger LOGGER = Logger.getLogger(SensorManager.class.getName());

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private GreenhouseManager greenhouseManager;

    @Inject
    private ActuatorManager actuatorManager;

    @Inject
    private Event<SensorAlert> sensorAlertEvent;

    public void processSensorData(String sensorId, String type, double value, String greenhouseId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Create new sensor document for each reading (historical storage)
            Sensor sensor = new Sensor(sensorId, type);
            sensor.setValue(value);
            sensor.setMeasurementTime(now);
            sensor.setStatus("ACTIVE");
            if (greenhouseId != null) {
                sensor.setGreenhouseId(greenhouseId);
            }

            // Save new reading
            sensorRepository.save(sensor);

            // Auto-attach sensor to greenhouse (only once per sensorId)
            if (greenhouseId != null && sensorId != null) {
                try {
                    greenhouseManager.attachSensor(greenhouseId, sensorId);
                } catch (Exception e) {
                    // Silently ignore if already attached
                }
            }

            applyBusinessRules(sensor);

            LOGGER.info("Processed sensor data: " + sensorId + " = " + value + " (Greenhouse: " + greenhouseId + ")");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing sensor data", e);
        }
    }

    // -------------------------------------------------------------------------
    // BUSINESS RULES
    // -------------------------------------------------------------------------
    private void applyBusinessRules(Sensor sensor) {

        switch (sensor.getType().toLowerCase()) {

            case "gas":
                if (sensor.getValue() > 200) {
                    sensorAlertEvent.fire(new SensorAlert(sensor,
                            "DANGER: Gas level critical (" + sensor.getValue() + " ppm)"));
                }
                break;

            case "soil":
                if (sensor.getValue() < 30) {
                    actuatorManager.controlActuator("pump", "ON", 80.0, sensor.getGreenhouseId());
                    sensorAlertEvent.fire(new SensorAlert(sensor,
                            "AUTO: Soil moisture low → Pump ON"));
                }
                break;

            case "ldr":
                if (sensor.getValue() < 100) {
                    actuatorManager.controlActuator("bulb1", "ON", 80.0, sensor.getGreenhouseId());
                    actuatorManager.controlActuator("bulb2", "ON", 80.0, sensor.getGreenhouseId());
                } else if (sensor.getValue() > 300) {
                    actuatorManager.controlActuator("bulb1", "OFF", 0.0, sensor.getGreenhouseId());
                    actuatorManager.controlActuator("bulb2", "OFF", 0.0, sensor.getGreenhouseId());
                }
                break;

            case "ph":
                if (sensor.getValue() < 6.0 || sensor.getValue() > 7.5) {
                    sensorAlertEvent.fire(new SensorAlert(sensor,
                            "ALERT: Soil pH abnormal: " + sensor.getValue()));
                }
                break;

            case "tank_level":
                if (sensor.getValue() < 20) {
                    sensorAlertEvent.fire(new SensorAlert(sensor,
                            "ALERT: Tank level low (" + sensor.getValue() + "%)"));
                }
                break;
        }
    }

    // -------------------------------------------------------------------------
    // ALERT CLASS
    // -------------------------------------------------------------------------
    public static class SensorAlert {
        private final Sensor sensor;
        private final String message;

        public SensorAlert(Sensor sensor, String message) {
            this.sensor = sensor;
            this.message = message;
        }

        public Sensor getSensor() {
            return sensor;
        }

        public String getMessage() {
            return message;
        }
    }
}
