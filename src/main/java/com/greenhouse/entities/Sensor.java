package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity("sensors")
public class Sensor implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("sensor_id")
    private String sensorId;

    @Column("type")
    private String type;

    @Column("value")
    private double value;

    @Column("measurement_time")
    private LocalDateTime measurementTime;

    @Column("status")
    private String status;

    // Constructors
    public Sensor() {
        this.id = UUID.randomUUID().toString();
    }

    public Sensor(String sensorId, String type, double value, LocalDateTime measurementTime, String status) {
        this.id = UUID.randomUUID().toString();
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.measurementTime = measurementTime;
        this.status = status;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public LocalDateTime getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(LocalDateTime measurementTime) {
        this.measurementTime = measurementTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // toString
    @Override
    public String toString() {
        return "Sensor{" +
                "id='" + id + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", measurementTime=" + measurementTime +
                ", status='" + status + '\'' +
                '}';
    }
}
