package com.greenhouse.dto;

import com.greenhouse.entities.Greenhouse;
import com.greenhouse.entities.Sensor;
import com.greenhouse.entities.Actuator;

import java.util.List;

/**
 * DTO pour retourner une serre avec ses capteurs et actuateurs complets
 */
public class GreenhouseWithDevices {
    private Greenhouse greenhouse;
    private List<Sensor> sensors;
    private List<Actuator> actuators;

    public GreenhouseWithDevices() {
    }

    public GreenhouseWithDevices(Greenhouse greenhouse, List<Sensor> sensors, List<Actuator> actuators) {
        this.greenhouse = greenhouse;
        this.sensors = sensors;
        this.actuators = actuators;
    }

    public Greenhouse getGreenhouse() {
        return greenhouse;
    }

    public void setGreenhouse(Greenhouse greenhouse) {
        this.greenhouse = greenhouse;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    public List<Actuator> getActuators() {
        return actuators;
    }

    public void setActuators(List<Actuator> actuators) {
        this.actuators = actuators;
    }
}
