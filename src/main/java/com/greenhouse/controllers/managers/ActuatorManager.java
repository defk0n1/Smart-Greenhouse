package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.controllers.repositories.GreenhouseRepository;
import com.greenhouse.entities.Actuator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ActuatorManager {

    private static final Logger LOGGER = Logger.getLogger(ActuatorManager.class.getName());

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    private GreenhouseRepository greenhouseRepository;

    @Inject
    private GreenhouseManager greenhouseManager;

    @Inject
    private Instance<MqttManager> mqttManagerInstance;

    @Inject
    @ConfigProperty(name = "mqtt.topic.actuators", defaultValue = "iot/control")
    private String defaultActuatorTopic;

    // -------------------------------------------------------------------------
    // CONTROL ACTUATOR
    // -------------------------------------------------------------------------
    public void controlActuator(String id, String command, Double value, String greenhouseId) {
        try {
            // Map ID to ESP32 expected format (Capitalized)
            String espId = id;
            if (id.equalsIgnoreCase("fan1"))
                espId = "Fan1";
            else if (id.equalsIgnoreCase("fan2"))
                espId = "Fan2";
            else if (id.equalsIgnoreCase("bulb1"))
                espId = "Bulb1";
            else if (id.equalsIgnoreCase("bulb2"))
                espId = "Bulb2";
            else if (id.equalsIgnoreCase("pump"))
                espId = "Pump";

            // Create NEW actuator record for history
            Actuator actuator = new Actuator();
            actuator.setActuatorId(id); // Logical ID (e.g. "bulb1")
            actuator.setType("unknown"); // Default type

            if (greenhouseId != null) {
                actuator.setGreenhouseId(greenhouseId);
            }

            // Try to find previous type if exists
            actuatorRepository.findByActuatorId(id).ifPresent(prev -> actuator.setType(prev.getType()));

            actuator.setLastCommand(command);
            actuator.setState(command.equalsIgnoreCase("ON") ? "ON" : "OFF");
            actuator.setValue(value != null ? value : 0);
            actuator.setTimestamp(LocalDateTime.now());

            actuatorRepository.save(actuator);

            // Publish MQTT command
            if (greenhouseId != null) {
                String topic = greenhouseRepository.findById(greenhouseId)
                        .map(com.greenhouse.entities.Greenhouse::getMqttTopic)
                        .orElse(null);

                if (topic != null) {
                    // Format for ESP32: {"Fan1": 1} or {"Fan1": 0}
                    int stateValue = command.equalsIgnoreCase("ON") ? 1 : 0;
                    String payload = String.format("{\"%s\":%d}", espId, stateValue);

                    // Topic: GH1/actuators/control (example) or GH1/actuators
                    // Assuming structure: {greenhouseTopic}/actuators
                    String actuatorTopic = topic.endsWith("/") ? topic + "actuators" : topic + "/actuators";

                    // Lazy resolution of MqttManager
                    mqttManagerInstance.get().sendMessage(actuatorTopic, payload);
                    LOGGER.info("Actuator command sent to " + actuatorTopic + ": " + payload);
                } else {
                    LOGGER.warning("No MQTT topic found for greenhouse: " + greenhouseId);
                }
            } else {
                LOGGER.warning("Cannot control actuator without greenhouseId: " + id);
            }

        } catch (Exception e) {
            LOGGER.severe("Error controlling actuator: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void processActuatorData(String actuatorId, String type, String state, double value, String lastCommand,
            String greenhouseId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Create new actuator document for each state change (historical storage)
            Actuator actuator = new Actuator(actuatorId, type);
            actuator.setState(state);
            actuator.setValue(value);
            actuator.setLastCommand(lastCommand);
            actuator.setTimestamp(now);
            if (greenhouseId != null) {
                actuator.setGreenhouseId(greenhouseId);
            }

            // Save new state
            actuatorRepository.save(actuator);

            // Auto-attach actuator to greenhouse (only once per actuatorId)
            if (greenhouseId != null && actuatorId != null) {
                try {
                    greenhouseManager.attachActuator(greenhouseId, actuatorId);
                } catch (Exception e) {
                    // Silently ignore if already attached
                }
            }

        } catch (Exception e) {
            LOGGER.severe("Error processing actuator data: " + e.getMessage());
        }
    }
}
