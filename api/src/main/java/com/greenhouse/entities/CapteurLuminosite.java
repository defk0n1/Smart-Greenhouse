package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurLuminosite extends Capteur {

    @Column
    private String localisation;

    @Column
    private double luminosite;

    // Constructeur
    public CapteurLuminosite() {}

    public CapteurLuminosite(String idEquipement, String localisation, double luminosite) {
        setIdEquipement(idEquipement);
        setType("LUMINOSITE");
        this.localisation = localisation;
        this.luminosite = luminosite;
        setUnite("lux");
    }

    // Getters et Setters
    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public double getLuminosite() {
        return luminosite;
    }

    public void setLuminosite(double luminosite) {
        this.luminosite = luminosite;
        setValeurConcrete(luminosite);
    }
}

