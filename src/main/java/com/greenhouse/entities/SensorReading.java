package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historique des lectures de capteurs
 * Collection séparée pour stocker toutes les mesures
 */
@Entity("sensor_readings")
public class SensorReading implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("sensor_id")
    private String sensorId;

    @Column("value")
    private double value;

    @Column("timestamp")
    private LocalDateTime timestamp;

    @Column("greenhouse_id")
    private String greenhouseId;

    // Constructors
    public SensorReading() {
        this.id = UUID.randomUUID().toString();
    }

    public SensorReading(String sensorId, double value, LocalDateTime timestamp, String greenhouseId) {
        this();
        this.sensorId = sensorId;
        this.value = value;
        this.timestamp = timestamp;
        this.greenhouseId = greenhouseId;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getGreenhouseId() {
        return greenhouseId;
    }

    public void setGreenhouseId(String greenhouseId) {
        this.greenhouseId = greenhouseId;
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "sensorId='" + sensorId + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
