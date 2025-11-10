package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurTemperature extends Capteur {

    @Column
    private String localisation;

    @Column
    private double temperature;

    // Constructeur
    public CapteurTemperature() {}

    public CapteurTemperature(String idEquipement, String localisation, double temperature) {
        setIdEquipement(idEquipement);
        setType("TEMPERATURE");
        this.localisation = localisation;
        this.temperature = temperature;
        setUnite("°C");
    }

    // Getters et Setters
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}