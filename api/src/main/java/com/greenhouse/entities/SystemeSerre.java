package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.HashMap;
import java.util.Map;

@Entity
public class SystemeSerre {

    @Id
    @Column
    private String id;

    @Column
    private String nom;

    @Column
    private String login;

    @Column
    private String mdp;

    @Column
    private Map<String, Capteur> captures;

    @Column
    private Map<String, Actionneur> actionneurs;

    // Constructeur
    public SystemeSerre() {
        this.captures = new HashMap<>();
        this.actionneurs = new HashMap<>();
    }

    public SystemeSerre(String id, String nom, String login, String mdp) {
        this.id = id;
        this.nom = nom;
        this.login = login;
        this.mdp = mdp;
        this.captures = new HashMap<>();
        this.actionneurs = new HashMap<>();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getMdp() {
        return mdp;
    }

    public void setMdp(String mdp) {
        this.mdp = mdp;
    }

    public Map<String, Capteur> getCaptures() {
        return captures;
    }

    public void setCaptures(Map<String, Capteur> captures) {
        this.captures = captures;
    }

    public Map<String, Actionneur> getActionneurs() {
        return actionneurs;
    }

    public void setActionneurs(Map<String, Actionneur> actionneurs) {
        this.actionneurs = actionneurs;
    }

    // Méthodes métier
    public void enregistrerCapture(Capteur c) {
        if (c != null && c.getIdEquipement() != null) {
            this.captures.put(c.getIdEquipement(), c);
            System.out.println("Capteur enregistré: " + c.getIdEquipement());
        }
    }

    public void enregistrerActionneur(Actionneur a) {
        if (a != null && a.getIdEquipement() != null) {
            this.actionneurs.put(a.getIdEquipement(), a);
            System.out.println("Actionneur enregistré: " + a.getIdEquipement());
        }
    }

    public void envoyerInfoSysteme() {
        System.out.println("=== Informations du système de serre ===");
        System.out.println("Nom: " + nom);
        System.out.println("Nombre de capteurs: " + captures.size());
        System.out.println("Nombre d'actionneurs: " + actionneurs.size());
    }

    public void regulerConditions() {
        System.out.println("Régulation des conditions de la serre en cours...");
        // Logique de régulation basée sur les capteurs et actionneurs
    }
}

