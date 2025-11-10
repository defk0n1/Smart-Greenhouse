package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class PompeEau extends Actionneur {

    @Column
    private double quantite;

    @Column
    private int tempsFonctionnement;

    // Constructeur
    public PompeEau() {}

    public PompeEau(int numero, double quantite, int tempsFonctionnement) {
        super("POMPE_" + numero, "POMPE_EAU", "ARROSER", "INACTIF", 100);
        this.quantite = quantite;
        this.tempsFonctionnement = tempsFonctionnement;
    }

    // Getters et Setters
    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }

    public int getTempsFonctionnement() {
        return tempsFonctionnement;
    }

    public void setTempsFonctionnement(int tempsFonctionnement) {
        this.tempsFonctionnement = tempsFonctionnement;
    }

    // Implémentation des méthodes abstraites
    @Override
    public void donnerOrdre(String ordre) {
        this.setValeur(ordre);
        System.out.println("Ordre donné à la pompe d'eau: " + ordre);
    }

    @Override
    public void delai(int delai) {
        System.out.println("Délai configuré: " + delai + " minutes");
    }

    @Override
    public void activer() {
        this.setStatut("ACTIF");
        System.out.println("Pompe d'eau activée - Quantité: " + quantite + "L");
    }

    @Override
    public void desactiver() {
        this.setStatut("INACTIF");
        System.out.println("Pompe d'eau désactivée");
    }

    @Override
    public void recevoirSource(String source) {
        System.out.println("Source reçue: " + source);
    }
}

