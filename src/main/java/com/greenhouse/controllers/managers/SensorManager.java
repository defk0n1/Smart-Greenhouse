package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.entities.Sensor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class SensorManager implements MqttCallback {

    private static final Logger LOGGER = Logger.getLogger(SensorManager.class.getName());

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ActuatorManager actuatorManager;

    @Inject
    private Event<SensorAlert> sensorAlertEvent;

    @Inject
    @ConfigProperty(name = "mqtt.broker.url", defaultValue = "ssl://f7650e29f2d1418ab38a502a12ae2a8e.s1.eu.hivemq.cloud:8883")
    private String brokerUrl;

    @Inject
    @ConfigProperty(name = "mqtt.client.id", defaultValue = "greenhouse-sensor-manager")
    private String clientId;

    @Inject
    @ConfigProperty(name = "mqtt.username", defaultValue = "marwen")
    private String username;

    @Inject
    @ConfigProperty(name = "mqtt.password", defaultValue = "M1arwen#")
    private String password;

    @Inject
    @ConfigProperty(name = "mqtt.topic.sensors", defaultValue = "iot/sensors/#")
    private String topic;

    private MqttClient mqttClient;

    // -------------------------------------------------------------------------
    // INITIALISATION
    // -------------------------------------------------------------------------
    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setUserName(username);
            opts.setPassword(password.toCharArray());
            opts.setSocketFactory(javax.net.ssl.SSLSocketFactory.getDefault());

            mqttClient.connect(opts);

            mqttClient.subscribe(topic);

            LOGGER.info("SensorManager MQTT connected → subscribed to: " + topic);

        } catch (MqttException e) {
            LOGGER.log(Level.SEVERE, "Error initializing SensorManager MQTT", e);
        }
    }

    // -------------------------------------------------------------------------
    // MQTT CALLBACKS
    // -------------------------------------------------------------------------
    @Override
    public void connectionLost(Throwable cause) {
        LOGGER.warning("MQTT connection lost: " + cause.getMessage());
        try {
            Thread.sleep(3000);
            init(); // attempt reconnect
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            LOGGER.info("MQTT → Sensor received: " + payload);

            JSONObject json = new JSONObject(payload);

            // ESP32 Format: All sensors in one message
            // {"TempIndoor":24.00,"HumIndoor":40.00,"Gas":3951, ...}

            // Map of MQTT field names to sensor types
            String[][] sensorMappings = {
                    { "TempIndoor", "temp_int" },
                    { "TempOutdoor", "temp_ext" },
                    { "HumIndoor", "humidity_int" },
                    { "HumOutdoor", "humidity_ext" },
                    { "Soil", "soil" },
                    { "Tank", "water" },
                    { "pH", "ph" },
                    { "Light", "light" },
                    { "Gas", "gas" },
                    { "Pressure", "pressure" }
            };

            for (String[] mapping : sensorMappings) {
                String mqttKey = mapping[0];
                String sensorType = mapping[1];

                if (json.has(mqttKey)) {
                    double value = json.optDouble(mqttKey, 0);

                    Sensor sensor = new Sensor();
                    sensor.setSensorId(sensorType + "_001");
                    sensor.setType(sensorType);
                    sensor.setValue(value);
                    sensor.setMeasurementTime(LocalDateTime.now());
                    sensor.setStatus("ACTIVE");

                    sensorRepository.save(sensor);
                    applyBusinessRules(sensor);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling sensor MQTT message", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // nothing: this is only a subscriber
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
                    actuatorManager.controlActuator("pump", "ON", 80.0);
                    sensorAlertEvent.fire(new SensorAlert(sensor,
                            "AUTO: Soil moisture low → Pump ON"));
                }
                break;

            case "ldr":
                if (sensor.getValue() < 100) {
                    actuatorManager.controlActuator("bulb1", "ON", 80.0);
                    actuatorManager.controlActuator("bulb2", "ON", 80.0);
                } else if (sensor.getValue() > 300) {
                    actuatorManager.controlActuator("bulb1", "OFF", 0.0);
                    actuatorManager.controlActuator("bulb2", "OFF", 0.0);
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
    // SHUTDOWN
    // -------------------------------------------------------------------------
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            LOGGER.warning("Error disconnecting MQTT client");
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
