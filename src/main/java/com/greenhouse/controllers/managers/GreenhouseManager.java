package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.GreenhouseRepository;
import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.entities.Greenhouse;
import com.greenhouse.entities.Sensor;
import com.greenhouse.entities.Actuator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Manager for handling greenhouses and their associated devices.
 * Handles business logic and ensures data consistency between greenhouses
 * and devices.
 */
@ApplicationScoped
public class GreenhouseManager {

    private static final Logger LOGGER = Logger.getLogger(GreenhouseManager.class.getName());

    @Inject
    private GreenhouseRepository greenhouseRepository;

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ActuatorRepository actuatorRepository;

    /**
     * Finds or automatically creates a greenhouse by its name (for MQTT
     * auto-discovery).
     * Used when an MQTT message arrives on a topic like "GH1/sensors/temp1"
     */
    public Greenhouse findOrCreateByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Greenhouse name cannot be null or empty");
        }

        // Search first if it already exists
        Optional<Greenhouse> existing = greenhouseRepository.findByName(name);
        if (existing.isPresent()) {
            LOGGER.info("Found existing greenhouse: " + name);
            return existing.get();
        }

        // Create if it doesn't exist
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setName(name);
        greenhouse.setMqttTopic(name); // Auto-configure the MQTT topic
        greenhouse.setStatus("ACTIVE"); // ACTIVE for visibility to all users
        greenhouse.setDescription("Auto-created from MQTT topic");
        greenhouse.setOwnerId("marwen"); // Default Owner

        Greenhouse saved = greenhouseRepository.save(greenhouse);
        LOGGER.info("✨ Auto-created ACTIVE greenhouse: " + name + " (ID: " + saved.getId() + ")");

        return saved;
    }

    /**
     * Attach a sensor to a greenhouse with bidirectional validation
     */
    public void attachSensor(String greenhouseId, String sensorId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            throw new Exception("Greenhouse not found: " + greenhouseId);
        }

        Greenhouse greenhouse = ghOpt.get();

        // Check if already attached - if so, return early (idempotent operation)
        if (greenhouse.getSensors() != null && greenhouse.getSensors().contains(sensorId)) {
            LOGGER.fine("Sensor " + sensorId + " already attached to greenhouse " + greenhouseId);
            return;
        }

        // Try to find the sensor - but don't fail if not found yet (might be in
        // creation)
        // The sensor will be created by SensorManager before this is called
        Optional<Sensor> sensorOpt = sensorRepository.findAll()
                .filter(s -> s.getSensorId() != null && s.getSensorId().equals(sensorId))
                .findFirst();

        // Update the greenhouse immediately - even if sensor details not available yet
        // This creates the reference from greenhouse -> sensor
        if (sensorOpt.isPresent()) {
            Sensor sensor = sensorOpt.get();

            // If the sensor is already attached to another greenhouse, detach it first
            if (sensor.getGreenhouseId() != null && !sensor.getGreenhouseId().equals(greenhouseId)) {
                detachSensor(sensor.getGreenhouseId(), sensorId);
            }

            // Update the sensor to point to this greenhouse
            sensor.setGreenhouseId(greenhouseId);
            sensorRepository.update(sensor);

            // Update the greenhouse (add reference)
            boolean active = sensor.getStatus() != null && sensor.getStatus().equalsIgnoreCase("ACTIVE");
            greenhouse.attachSensor(sensorId, sensor.getType(), active);
        } else {
            // Sensor not found in repository yet (race condition during creation)
            // Still attach the reference - sensor details will sync later
            LOGGER.info("Sensor " + sensorId + " not yet in repository, attaching reference only");
            greenhouse.attachSensor(sensorId, "unknown", true);
        }

        greenhouseRepository.update(greenhouse);

        LOGGER.info("✅ Attached sensor " + sensorId + " to greenhouse " + greenhouseId);
    }

    /**
     * Detach a sensor from a greenhouse
     */
    public void detachSensor(String greenhouseId, String sensorId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            LOGGER.warning("Greenhouse not found for detach: " + greenhouseId);
            return; // Silent fail if greenhouse does not exist
        }

        Greenhouse greenhouse = ghOpt.get();

        // Remove the reference from the greenhouse
        greenhouse.detachSensor(sensorId);
        greenhouseRepository.update(greenhouse);

        // Update the sensor to remove the greenhouseId
        sensorRepository.findAll()
                .filter(s -> s.getSensorId() != null && s.getSensorId().equals(sensorId))
                .findFirst()
                .ifPresent(sensor -> {
                    sensor.setGreenhouseId(null);
                    sensorRepository.update(sensor);
                });

        LOGGER.info("🔓 Detached sensor " + sensorId + " from greenhouse " + greenhouseId);
    }

    /**
     * Attach an actuator to a greenhouse with bidirectional validation
     */
    public void attachActuator(String greenhouseId, String actuatorId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            throw new Exception("Greenhouse not found: " + greenhouseId);
        }

        Greenhouse greenhouse = ghOpt.get();

        // Check if already attached - if so, return early (idempotent operation)
        if (greenhouse.getActuators() != null && greenhouse.getActuators().contains(actuatorId)) {
            LOGGER.fine("Actuator " + actuatorId + " already attached to greenhouse " + greenhouseId);
            return;
        }

        // Try to find the actuator - but don't fail if not found yet (might be in
        // creation)
        Optional<Actuator> actOpt = actuatorRepository.findByActuatorId(actuatorId);

        // Update the greenhouse immediately - even if actuator details not available
        // yet
        if (actOpt.isPresent()) {
            Actuator actuator = actOpt.get();

            // If the actuator is already attached to another greenhouse, detach it first
            if (actuator.getGreenhouseId() != null && !actuator.getGreenhouseId().equals(greenhouseId)) {
                detachActuator(actuator.getGreenhouseId(), actuatorId);
            }

            // Update the actuator
            actuator.setGreenhouseId(greenhouseId);
            actuatorRepository.update(actuator);

            // Update the greenhouse (add reference)
            boolean active = actuator.getState() != null && actuator.getState().equalsIgnoreCase("ON");
            greenhouse.attachActuator(actuatorId, actuator.getType(), active);
        } else {
            // Actuator not found in repository yet (race condition during creation)
            // Still attach the reference - actuator details will sync later
            LOGGER.info("Actuator " + actuatorId + " not yet in repository, attaching reference only");
            greenhouse.attachActuator(actuatorId, "unknown", true);
        }

        greenhouseRepository.update(greenhouse);

        LOGGER.info("✅ Attached actuator " + actuatorId + " to greenhouse " + greenhouseId);
    }

    /**
     * Detach an actuator from a greenhouse
     */
    public void detachActuator(String greenhouseId, String actuatorId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            LOGGER.warning("Greenhouse not found for detach: " + greenhouseId);
            return;
        }

        Greenhouse greenhouse = ghOpt.get();

        // Remove the reference from the greenhouse
        greenhouse.detachActuator(actuatorId);
        greenhouseRepository.update(greenhouse);

        // Update the actuator to remove the greenhouseId
        actuatorRepository.findByActuatorId(actuatorId)
                .ifPresent(actuator -> {
                    actuator.setGreenhouseId(null);
                    actuatorRepository.update(actuator);
                });

        LOGGER.info("🔓 Detached actuator " + actuatorId + " from greenhouse " + greenhouseId);
    }

    /**
     * Delete a greenhouse and detach all its devices
     * Ensures data consistency before deletion
     */
    @Inject
    private MqttManager mqttManager;

    /**
     * Create a new greenhouse and subscribe to its MQTT topic
     */
    public Greenhouse createGreenhouse(Greenhouse greenhouse) {
        Greenhouse saved = greenhouseRepository.save(greenhouse);

        if (saved.getMqttTopic() != null && !saved.getMqttTopic().trim().isEmpty()) {
            mqttManager.subscribe(saved.getMqttTopic());
        }

        return saved;
    }

    /**
     * Update a greenhouse and manage MQTT subscriptions
     */
    public Greenhouse updateGreenhouse(Greenhouse greenhouse) {
        // Retrieve the old version to compare topics
        Optional<Greenhouse> existingOpt = greenhouseRepository.findById(greenhouse.getId());
        String oldTopic = existingOpt.map(Greenhouse::getMqttTopic).orElse(null);

        Greenhouse updated = greenhouseRepository.update(greenhouse);
        String newTopic = updated.getMqttTopic();

        // If the topic has changed, unsubscribe from the old one
        if (oldTopic != null && !oldTopic.equals(newTopic)) {
            mqttManager.unsubscribe(oldTopic);
        }

        // If new topic (and different from old), subscribe
        if (newTopic != null && !newTopic.trim().isEmpty() && !newTopic.equals(oldTopic)) {
            mqttManager.subscribe(newTopic);
        }

        return updated;
    }

    /**
     * Delete a greenhouse, detach all its devices and unsubscribe from the MQTT topic
     */
    public void deleteGreenhouse(String greenhouseId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            throw new Exception("Greenhouse not found: " + greenhouseId);
        }

        Greenhouse gh = ghOpt.get();

        // Unsubscribe from MQTT topic
        if (gh.getMqttTopic() != null && !gh.getMqttTopic().trim().isEmpty()) {
            mqttManager.unsubscribe(gh.getMqttTopic());
        }

        int detachedSensors = 0;
        int detachedActuators = 0;

        // Detach all sensors
        if (gh.getSensors() != null && !gh.getSensors().isEmpty()) {
            for (String sensorId : gh.getSensors()) {
                try {
                    detachSensor(greenhouseId, sensorId);
                    detachedSensors++;
                } catch (Exception e) {
                    LOGGER.warning("Error detaching sensor " + sensorId + ": " + e.getMessage());
                }
            }
        }

        // Detach all actuators
        if (gh.getActuators() != null && !gh.getActuators().isEmpty()) {
            for (String actuatorId : gh.getActuators()) {
                try {
                    detachActuator(greenhouseId, actuatorId);
                    detachedActuators++;
                } catch (Exception e) {
                    LOGGER.warning("Error detaching actuator " + actuatorId + ": " + e.getMessage());
                }
            }
        }

        // Delete the greenhouse
        greenhouseRepository.delete(greenhouseId);

        LOGGER.info("🗑️ Deleted greenhouse " + greenhouseId +
                " (detached " + detachedSensors + " sensors, " + detachedActuators + " actuators)");
    }

    /**
     * Synchronize device references in a greenhouse
     * Useful for correcting data inconsistencies
     */
    public void syncDeviceReferences(String greenhouseId) throws Exception {
        Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
        if (ghOpt.isEmpty()) {
            throw new Exception("Greenhouse not found: " + greenhouseId);
        }

        Greenhouse greenhouse = ghOpt.get();

        // Rebuild the sensor list from database
        greenhouse.setSensors(new java.util.ArrayList<>());
        sensorRepository.findByGreenhouseId(greenhouseId)
                .forEach(sensor -> {
                    if (sensor.getSensorId() != null) {
                        boolean active = sensor.getStatus() != null && sensor.getStatus().equalsIgnoreCase("ACTIVE");
                        greenhouse.attachSensor(sensor.getSensorId(), sensor.getType(), active);
                    }
                });

        // Rebuild the actuator list from database
        greenhouse.setActuators(new java.util.ArrayList<>());
        actuatorRepository.findByGreenhouseId(greenhouseId)
                .forEach(actuator -> {
                    if (actuator.getActuatorId() != null) {
                        boolean active = actuator.getState() != null && actuator.getState().equalsIgnoreCase("ON");
                        greenhouse.attachActuator(actuator.getActuatorId(), actuator.getType(), active);
                    }
                });

        greenhouseRepository.update(greenhouse);

        LOGGER.info("🔄 Synced device references for greenhouse " + greenhouseId +
                " (" + greenhouse.getSensorCount() + " sensors, " + greenhouse.getActuatorCount() + " actuators)");
    }
}
