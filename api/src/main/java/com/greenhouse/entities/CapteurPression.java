package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurPression extends Capteur {

    @Column
    private double pression;

    // Constructeur
    public CapteurPression() {}

    public CapteurPression(String idEquipement, double pression) {
        setIdEquipement(idEquipement);
        setType("PRESSION");
        this.pression = pression;
        setUnite("hPa");
        setValeurConcrete(pression);
    }

    // Getters et Setters
    public double getPression() {
        return pression;
    }

    public void setPression(double pression) {
        this.pression = pression;
        setValeurConcrete(pression);
    }
}

