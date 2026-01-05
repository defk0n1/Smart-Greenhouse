package com.greenhouse.entities;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity("actuators")
public class Actuator implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("actuator_id")
    private String actuatorId;

    @Column("type")
    private String type;

    @Column("current_state")
    private String currentState;

    @Column("current_value")
    private double currentValue;

    @Column("last_command")
    private String lastCommand;

    @Column("last_update")
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdate;

    @Column("greenhouse_id")
    private String greenhouseId;

    @Column("state_history")
    private List<StateChange> stateHistory;

    // Inner class for state history
    public static class StateChange implements Serializable {
        @Column("state")
        private String state;

        @Column("value")
        private double value;

        @Column("command")
        private String command;

        @Column("timestamp")
        private LocalDateTime timestamp;

        public StateChange() {
        }

        public StateChange(String state, double value, String command, LocalDateTime timestamp) {
            this.state = state;
            this.value = value;
            this.command = command;
            this.timestamp = timestamp;
        }

        // Getters & Setters
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

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    // Constructors
    public Actuator() {
        this.id = UUID.randomUUID().toString();
        this.stateHistory = new ArrayList<>();
    }

    public Actuator(String actuatorId, String type) {
        this();
        this.actuatorId = actuatorId;
        this.type = type;
    }

    // Add state change (keep last 100)
    public void addStateChange(String state, double value, String command, LocalDateTime timestamp) {
        this.currentState = state;
        this.currentValue = value;
        this.lastCommand = command;
        this.lastUpdate = timestamp;

        if (this.stateHistory == null) {
            this.stateHistory = new ArrayList<>();
        }

        this.stateHistory.add(new StateChange(state, value, command, timestamp));

        // Keep only last 100 state changes
        if (this.stateHistory.size() > 100) {
            this.stateHistory.remove(0);
        }
    }

    // Getters & Setters
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

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    // Backward compatibility
    public String getState() {
        return currentState;
    }

    public void setState(String state) {
        this.currentState = state;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    // Backward compatibility
    public double getValue() {
        return currentValue;
    }

    public void setValue(double value) {
        this.currentValue = value;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    // Backward compatibility
    public LocalDateTime getTimestamp() {
        return lastUpdate;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.lastUpdate = timestamp;
    }

    public String getGreenhouseId() {
        return greenhouseId;
    }

    public void setGreenhouseId(String greenhouseId) {
        this.greenhouseId = greenhouseId;
    }

    public List<StateChange> getStateHistory() {
        return stateHistory;
    }

    public void setStateHistory(List<StateChange> stateHistory) {
        this.stateHistory = stateHistory;
    }

    @Override
    public String toString() {
        return "Actuator{" +
                "id='" + id + '\'' +
                ", actuatorId='" + actuatorId + '\'' +
                ", type='" + type + '\'' +
                ", currentState='" + currentState + '\'' +
                ", currentValue=" + currentValue +
                ", lastUpdate=" + lastUpdate +
                ", stateHistoryCount=" + (stateHistory != null ? stateHistory.size() : 0) +
                '}';
    }
}