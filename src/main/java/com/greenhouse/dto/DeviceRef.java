package com.greenhouse.dto;

import java.io.Serializable;

/**
 * Référence légère pour un device (sensor ou actuator) attaché à une
 * greenhouse.
 * Évite la duplication de données en ne stockant que les informations
 * essentielles.
 */
public class DeviceRef implements Serializable {

    private String deviceId; // sensorId ou actuatorId
    private String type; // "SENSOR" ou "ACTUATOR"
    private String subType; // "temperature", "humidity", "fan", "pump", etc.
    private boolean active; // État actif/inactif

    // Constructeurs
    public DeviceRef() {
    }

    public DeviceRef(String deviceId, String type, String subType, boolean active) {
        this.deviceId = deviceId;
        this.type = type;
        this.subType = subType;
        this.active = active;
    }

    // Getters & Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "DeviceRef{" +
                "deviceId='" + deviceId + '\'' +
                ", type='" + type + '\'' +
                ", subType='" + subType + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DeviceRef deviceRef = (DeviceRef) o;
        return deviceId != null && deviceId.equals(deviceRef.deviceId);
    }

    @Override
    public int hashCode() {
        return deviceId != null ? deviceId.hashCode() : 0;
    }
}
