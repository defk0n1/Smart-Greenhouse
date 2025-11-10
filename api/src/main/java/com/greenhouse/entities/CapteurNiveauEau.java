package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class CapteurNiveauEau extends Capteur {

    @Column
    private double capaciteMax;

    @Column
    private double niveauEau;

    // Constructeur
    public CapteurNiveauEau() {}

    public CapteurNiveauEau(String idEquipement, double capaciteMax, double niveauEau) {
        setIdEquipement(idEquipement);
        setType("NIVEAU_EAU");
        this.capaciteMax = capaciteMax;
        this.niveauEau = niveauEau;
        setUnite("L");
        setValeurConcrete(niveauEau);
    }

    // Getters et Setters
    public double getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(double capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public double getNiveauEau() {
        return niveauEau;
    }

    public void setNiveauEau(double niveauEau) {
        this.niveauEau = niveauEau;
        setValeurConcrete(niveauEau);
    }
}

