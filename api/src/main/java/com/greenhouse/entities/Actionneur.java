package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public abstract class Actionneur extends Equipement {

    @Column
    private String action;

    @Column
    private String valeur;

    @Column
    private int puissance;

    // Constructeur
    public Actionneur() {}

    public Actionneur(String id, String type, String action, String valeur, int puissance) {
        setIdEquipement(id);
        setType(type);
        this.action = action;
        this.valeur = valeur;
        this.puissance = puissance;
    }

    // Getters et Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public int getPuissance() {
        return puissance;
    }

    public void setPuissance(int puissance) {
        this.puissance = puissance;
    }

    // Méthodes abstraites
    public abstract void donnerOrdre(String ordre);

    public abstract void delai(int delai);

    public abstract void activer();

    public abstract void desactiver();

    public abstract void recevoirSource(String source);
}
