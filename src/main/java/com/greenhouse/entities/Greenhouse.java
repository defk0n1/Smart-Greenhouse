package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity("greenhouses")
public class Greenhouse implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("location")
    private String location;

    @Column("owner_id")
    private String ownerId;

    @Column("authorized_users")
    private List<String> authorizedUsers;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("status")
    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @Column("sensors")
    private List<String> sensors; // List of sensor IDs

    @Column("actuators")
    private List<String> actuators; // List of actuator IDs

    @Column("mqtt_topic")
    private String mqttTopic;

    // Constructors
    public Greenhouse() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.authorizedUsers = new ArrayList<>();
        this.sensors = new ArrayList<>();
        this.actuators = new ArrayList<>();
    }

    public Greenhouse(String name, String description, String location, String ownerId) {
        this();
        this.name = name;
        this.description = description;
        this.location = location;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public void setAuthorizedUsers(List<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSensors() {
        return sensors;
    }

    public void setSensors(List<String> sensors) {
        this.sensors = sensors;
    }

    public List<String> getActuators() {
        return actuators;
    }

    public void setActuators(List<String> actuators) {
        this.actuators = actuators;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    // Utility methods for device management (simplified - just store IDs)
    public void attachSensor(String sensorId, String type, boolean active) {
        if (this.sensors == null) {
            this.sensors = new ArrayList<>();
        }
        // Remove if already exists
        this.sensors.remove(sensorId);
        // Add sensor ID
        this.sensors.add(sensorId);
    }

    public void detachSensor(String sensorId) {
        if (this.sensors != null) {
            this.sensors.remove(sensorId);
        }
    }

    public void attachActuator(String actuatorId, String type, boolean active) {
        if (this.actuators == null) {
            this.actuators = new ArrayList<>();
        }
        // Remove if already exists
        this.actuators.remove(actuatorId);
        // Add actuator ID
        this.actuators.add(actuatorId);
    }

    public void detachActuator(String actuatorId) {
        if (this.actuators != null) {
            this.actuators.remove(actuatorId);
        }
    }

    public int getSensorCount() {
        return sensors != null ? sensors.size() : 0;
    }

    public int getActuatorCount() {
        return actuators != null ? actuators.size() : 0;
    }

    @Override
    public String toString() {
        return "Greenhouse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", authorizedUsers=" + authorizedUsers +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }
}
