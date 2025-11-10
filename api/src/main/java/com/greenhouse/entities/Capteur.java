package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public abstract class Capteur extends Equipement {

    @Column
    private String unite;

    @Column
    private double valeurConcrete;

    @Column
    private String etat;

    // Constructeur par défaut
    public Capteur() {}

    // Getters et Setters
    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public double getValeurConcrete() {
        return valeurConcrete;
    }

    public void setValeurConcrete(double valeurConcrete) {
        this.valeurConcrete = valeurConcrete;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }
}
