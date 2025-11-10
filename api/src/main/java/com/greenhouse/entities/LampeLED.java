package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class LampeLED extends Actionneur {

    @Column
    private int intensite;

    @Column
    private String couleur;

    // Constructeur
    public LampeLED() {}

    public LampeLED(int numero, int intensite, String couleur) {
        super("LED_" + numero, "LAMPE_LED", "ECLAIRER", "ACTIF", intensite);
        this.intensite = intensite;
        this.couleur = couleur;
    }

    // Getters et Setters
    public int getIntensite() {
        return intensite;
    }

    public void setIntensite(int intensite) {
        this.intensite = intensite;
        setPuissance(intensite);
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    // Implémentation des méthodes abstraites
    @Override
    public void donnerOrdre(String ordre) {
        this.setValeur(ordre);
        System.out.println("Ordre donné à la lampe LED: " + ordre);
    }

    @Override
    public void delai(int delai) {
        System.out.println("Délai configuré: " + delai + " minutes");
    }

    @Override
    public void activer() {
        this.setStatut("ACTIF");
        System.out.println("Lampe LED activée");
    }

    @Override
    public void desactiver() {
        this.setStatut("INACTIF");
        this.intensite = 0;
        setPuissance(0);
        System.out.println("Lampe LED désactivée");
    }

    @Override
    public void recevoirSource(String source) {
        System.out.println("Source reçue: " + source);
    }

    // Méthodes spécifiques
    public void augmenterIntensite(int i) {
        this.intensite += i;
        setPuissance(this.intensite);
    }

    public void reduireIntensite(int i) {
        this.intensite = Math.max(0, this.intensite - i);
        setPuissance(this.intensite);
    }
}

