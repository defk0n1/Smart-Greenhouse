package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurCO2 extends Capteur {

    @Column
    private double co2;

    // Constructeur
    public CapteurCO2() {}

    public CapteurCO2(String idEquipement, double co2) {
        setIdEquipement(idEquipement);
        setType("CO2");
        this.co2 = co2;
        setUnite("ppm");
        setValeurConcrete(co2);
    }

    // Getters et Setters
    public double getCo2() {
        return co2;
    }

    public void setCo2(double co2) {
        this.co2 = co2;
        setValeurConcrete(co2);
    }
}

