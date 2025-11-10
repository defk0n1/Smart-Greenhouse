package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class Ventilateur extends Actionneur {

    @Column
    private boolean refroidir;

    @Column
    private int vitesse;

    // Constructeur
    public Ventilateur() {}

    public Ventilateur(int numero, boolean refroidir, int vitesse) {
        super("VENT_" + numero, "VENTILATEUR", "REGULER_TEMPERATURE", "ACTIF", vitesse);
        this.refroidir = refroidir;
        this.vitesse = vitesse;
    }

    // Getters et Setters
    public boolean isRefroidir() { return refroidir; }
    public void setRefroidir(boolean refroidir) { this.refroidir = refroidir; }

    public int getVitesse() { return vitesse; }
    public void setVitesse(int vitesse) {
        this.vitesse = vitesse;
        setPuissance(vitesse);
    }

    // Implémentation des méthodes abstraites
    @Override
    public void donnerOrdre(String ordre) {
        this.setValeur(ordre);
        System.out.println("Ordre donné au ventilateur: " + ordre);
    }

    @Override
    public void delai(int delai) {
        System.out.println("Délai configuré: " + delai + " minutes");
    }

    @Override
    public void activer() {
        this.setStatut("ACTIF");
        System.out.println("Ventilateur activé");
    }

    @Override
    public void desactiver() {
        this.setStatut("INACTIF");
        this.vitesse = 0;
        setPuissance(0);
        System.out.println("Ventilateur désactivé");
    }

    @Override
    public void recevoirSource(String source) {
        System.out.println("Source reçue: " + source);
    }

    // Méthodes spécifiques
    public void augmenterVitesse(int v) {
        this.vitesse += v;
        setPuissance(this.vitesse);
    }

    public void ralentirVitesse(int v) {
        this.vitesse = Math.max(0, this.vitesse - v);
        setPuissance(this.vitesse);
    }
}