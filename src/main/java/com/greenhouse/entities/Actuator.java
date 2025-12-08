package com.greenhouse.entities;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Actuator {

    @Id
    @Column("_id")
    private String id;

    @Column("actuator_id")
    private String actuatorId;

    @Column
    private String type;

    @Column
    private String state;

    @Column
    private double value;

    @Column("last_command")
    private String lastCommand;

    @Column
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Constructors
    public Actuator() {
        this.id = UUID.randomUUID().toString();
    }

    public Actuator(String actuatorId, String type, String state, double value, String lastCommand,
            LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.actuatorId = actuatorId;
        this.type = type;
        this.state = state;
        this.value = value;
        this.lastCommand = lastCommand;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActuatorId() {
        return actuatorId;
    }

    public void setActuatorId(String actuatorId) {
        this.actuatorId = actuatorId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Actuator actuator = (Actuator) o;
        return Objects.equals(id, actuator.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Actuator{" +
                "id='" + id + '\'' +
                ", actuatorId='" + actuatorId + '\'' +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", value=" + value +
                ", lastCommand='" + lastCommand + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}