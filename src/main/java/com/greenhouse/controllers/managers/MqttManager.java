package com.greenhouse.controllers.managers;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.greenhouse.entities.Sensor;
import com.greenhouse.entities.Actuator;
import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.controllers.repositories.ActuatorRepository;

import javax.net.ssl.SSLSocketFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Optional;

@Singleton
@Startup
public class MqttManager {

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    @ConfigProperty(name = "mqtt.uri", defaultValue = "ssl://f7650e29f2d1418ab38a502a12ae2a8e.s1.eu.hivemq.cloud:8883")
    private String uri;

    @Inject
    @ConfigProperty(name = "mqtt.username", defaultValue = "marwen")
    private String username;

    @Inject
    @ConfigProperty(name = "mqtt.password", defaultValue = "M1arwen#")
    private String password;

    private MqttClient client;

    @PostConstruct
    public void start() {
        try {
            System.out.println("🔌 Connecting to MQTT broker: " + uri);

            client = new MqttClient(uri, MqttClient.generateClientId(), new MemoryPersistence());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("⚠️ MQTT Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("📥 Received: " + payload);

                    try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                        JsonObject json = reader.readObject();

                        if (topic.startsWith("iot/sensors/")) {
                            handleSensorMessage(json);
                        } else if (topic.startsWith("iot/actuators/")) {
                            handleActuatorMessage(json);
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Invalid JSON: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            if (uri.startsWith("ssl://")) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setSocketFactory(SSLSocketFactory.getDefault());
            }

            client.connect(options);
            System.out.println("✅ Connected to MQTT broker");

            client.subscribe("iot/sensors/#");
            client.subscribe("iot/actuators/#");

        } catch (Exception e) {
            System.err.println("❌ MQTT init failed: " + e.getMessage());
        }
    }

    private void handleSensorMessage(JsonObject json) {
        try {
            String sensorId = json.getString("sensorId");
            String type = json.getString("type");
            double value = json.getJsonNumber("value").doubleValue();

            LocalDateTime now = LocalDateTime.now();

            Optional<Sensor> existing = sensorRepository.findById(sensorId);

            Sensor sensor;
            if (existing.isEmpty()) {
                sensor = new Sensor(sensorId, type, value, now, "active");
            } else {
                sensor = existing.get();
                sensor.setValue(value);
                sensor.setMeasurementTime(now);
            }

            sensor.setValue(value);
            sensor.setMeasurementTime(now);
            sensor.setStatus("active");

            sensorRepository.save(sensor);

        } catch (Exception e) {
            System.err.println("❌ Sensor error: " + e.getMessage());
        }
    }

    private void handleActuatorMessage(JsonObject json) {
        try {
            String actuatorId = json.getString("actuatorId");
            String type = json.getString("type");
            String state = json.getString("state");
            double value = json.getJsonNumber("value").doubleValue();
            String lastCommand = json.containsKey("lastCommand") ? json.getString("lastCommand") : null;

            LocalDateTime now = LocalDateTime.now();

            Optional<Actuator> existing = actuatorRepository.findById(actuatorId);

            Actuator actuator = existing
                    .orElseGet(() -> new Actuator(actuatorId, type, state, value, lastCommand, now));

            actuator.setState(state);
            actuator.setValue(value);
            actuator.setLastCommand(lastCommand);
            actuator.setTimestamp(now);

            actuatorRepository.save(actuator);

        } catch (Exception e) {
            System.err.println("❌ Actuator error: " + e.getMessage());
        }
    }

    public void sendMessage(String topic, String msg) throws MqttException {
        if (client == null || !client.isConnected()) {
            System.err.println("❌ MQTT not connected!");
            return;
        }

        client.publish(topic, new MqttMessage(msg.getBytes()));
    }
}
