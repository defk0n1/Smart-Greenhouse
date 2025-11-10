package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurPH extends Capteur {

    @Column
    private double ph;

    // Constructeur
    public CapteurPH() {}

    public CapteurPH(String idEquipement, double ph) {
        setIdEquipement(idEquipement);
        setType("PH");
        this.ph = ph;
        setUnite("pH");
        setValeurConcrete(ph);
    }

    // Getters et Setters
    public double getPh() {
        return ph;
    }

    public void setPh(double ph) {
        this.ph = ph;
        setValeurConcrete(ph);
    }
}

