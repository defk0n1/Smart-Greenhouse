package com.greenhouse.controllers.managers;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLSocketFactory;
import java.io.StringReader;

@Singleton
@Startup
public class MqttManager {

    @Inject
    private GreenhouseManager greenhouseManager;

    @Inject
    private SensorManager sensorManager;

    @Inject
    private ActuatorManager actuatorManager;

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
                    System.out.println("📥 Received on " + topic + ": " + payload);

                    try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                        JsonObject json = reader.readObject();

                        // Basic routing based on topic content or payload
                        if (topic.contains("/sensors/") || topic.contains("iot/sensors/")) {
                            handleSensorMessage(topic, json);
                        } else if (topic.contains("/actuators/") || topic.contains("iot/actuators/")) {
                            handleActuatorMessage(topic, json);
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Invalid JSON or Error processing: " + e.getMessage());
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

            // Subscribe to wildcard topics for auto-discovery
            // This allows any greenhouse (GH1, GH2, etc.) to send data without
            // pre-configuration
            client.subscribe("+/sensors/#");
            System.out.println("✅ Subscribed to: +/sensors/#");

            client.subscribe("+/actuators/#");
            System.out.println("✅ Subscribed to: +/actuators/#");

            // Keep legacy subscriptions for backward compatibility
            client.subscribe("iot/sensors/#");
            client.subscribe("iot/actuators/#");
            System.out.println("✅ Subscribed to legacy topics: iot/sensors/# and iot/actuators/#");

        } catch (Exception e) {
            System.err.println("❌ MQTT init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                // Ensure we don't double slash if user provided trailing slash
                String topicFilter = topic.endsWith("/") ? topic + "#" : topic + "/#";
                client.subscribe(topicFilter);
                System.out.println("✅ Subscribed to: " + topicFilter);
            }
        } catch (MqttException e) {
            System.err.println("❌ Subscribe failed for " + topic + ": " + e.getMessage());
        }
    }

    public void unsubscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                String topicFilter = topic.endsWith("/") ? topic + "#" : topic + "/#";
                client.unsubscribe(topicFilter);
                System.out.println("✅ Unsubscribed from: " + topicFilter);
            }
        } catch (MqttException e) {
            System.err.println("❌ Unsubscribe failed for " + topic + ": " + e.getMessage());
        }
    }

    /**
     * Extract greenhouse name from MQTT topic and auto-create if needed.
     * Topic format: "GH1/sensors/temp1" -> greenhouse name is "GH1"
     */
    private String extractAndEnsureGreenhouse(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        try {
            // Extract first part of topic (greenhouse name)
            // Examples: "GH1/sensors/temp1" -> "GH1", "GH2/actuators/pump" -> "GH2"
            String[] parts = topic.split("/");
            if (parts.length < 2) {
                System.err.println("⚠️ Invalid topic format (expected: <greenhouse>/<type>/<id>): " + topic);
                return null;
            }

            String greenhouseName = parts[0];

            // Skip legacy "iot" topics
            if ("iot".equalsIgnoreCase(greenhouseName)) {
                return null;
            }

            // Auto-create greenhouse if it doesn't exist
            com.greenhouse.entities.Greenhouse greenhouse = greenhouseManager.findOrCreateByName(greenhouseName);
            return greenhouse.getId();

        } catch (Exception e) {
            System.err.println("❌ Error extracting/creating greenhouse from topic " + topic + ": " + e.getMessage());
            return null;
        }
    }

    // Changing signature to accept topic
    private void handleSensorMessage(String topic, JsonObject json) {
        try {
            String sensorId = json.getString("sensorId");
            String type = json.getString("type");
            double value = json.getJsonNumber("value").doubleValue();

            String greenhouseId = extractAndEnsureGreenhouse(topic);

            // Delegate to SensorManager for processing and business rules
            sensorManager.processSensorData(sensorId, type, value, greenhouseId);

        } catch (Exception e) {
            System.err.println("❌ Sensor error: " + e.getMessage());
        }
    }

    private void handleActuatorMessage(String topic, JsonObject json) {
        try {
            // Arduino sends "sensorId" not "actuatorId"
            String actuatorId = json.containsKey("actuatorId")
                    ? json.getString("actuatorId")
                    : json.getString("sensorId");

            String type = json.getString("type");

            // Arduino doesn't send "state", derive it from value
            double value = json.getJsonNumber("value").doubleValue();
            String state = value > 0 ? "ON" : "OFF";

            String lastCommand = json.containsKey("lastCommand") ? json.getString("lastCommand") : null;

            String greenhouseId = extractAndEnsureGreenhouse(topic);

            // Delegate to ActuatorManager for processing
            actuatorManager.processActuatorData(actuatorId, type, state, value, lastCommand, greenhouseId);

        } catch (Exception e) {
            System.err.println("❌ Actuator error: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
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
