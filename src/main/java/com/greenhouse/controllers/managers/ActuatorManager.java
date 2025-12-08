package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.entities.Actuator;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@ApplicationScoped
public class ActuatorManager {

    private static final Logger LOGGER = Logger.getLogger(ActuatorManager.class.getName());

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    @ConfigProperty(name = "mqtt.uri")
    private String mqttBrokerUrl;

    @Inject
    @ConfigProperty(name = "mqtt.topic.actuators", defaultValue = "iot/control")
    private String actuatorTopic;

    @Inject
    @ConfigProperty(name = "mqtt.username")
    private String mqttUsername;

    @Inject
    @ConfigProperty(name = "mqtt.password")
    private String mqttPassword;

    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        try {
            String broker = mqttBrokerUrl;
            String clientId = "actuator-controller";

            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setUserName(mqttUsername);
            opts.setPassword(mqttPassword.toCharArray());

            mqttClient.connect(opts);

            LOGGER.info("ActuatorManager MQTT connected to: " + broker);

        } catch (Exception e) {
            LOGGER.severe("Error initializing ActuatorManager MQTT: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // CONTROL ACTUATOR
    // -------------------------------------------------------------------------
    public void controlActuator(String id, String command, Double value) {
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

            // Try to find previous type if exists
            actuatorRepository.findByActuatorId(id).ifPresent(prev -> actuator.setType(prev.getType()));

            actuator.setLastCommand(command);
            actuator.setState(command.equalsIgnoreCase("ON") ? "ON" : "OFF");
            actuator.setValue(value != null ? value : 0);
            actuator.setTimestamp(LocalDateTime.now());

            actuatorRepository.save(actuator);

            // Publish MQTT command
            // Format for ESP32: {"Fan1": 1} or {"Fan1": 0}
            int stateValue = command.equalsIgnoreCase("ON") ? 1 : 0;
            String payload = String.format("{\"%s\":%d}", espId, stateValue);

            // Use the configured topic
            mqttClient.publish(actuatorTopic, new MqttMessage(payload.getBytes()));

            LOGGER.info("Actuator command sent: " + payload);

        } catch (Exception e) {
            LOGGER.severe("Error controlling actuator: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
