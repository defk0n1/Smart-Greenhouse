package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.List;

@Entity
public class Administrateur extends Personne {

    @Id
    @Column
    private String idAdministrateur;  // Identifiant principal pour JNoSQL

    @Column
    private List<String> permissions;

    // Constructeur par défaut
    public Administrateur() {}

    // Constructeur complet
    public Administrateur(String id, String nom, String prenom, String email, String role,
                          String idAdministrateur, List<String> permissions) {
        super(id, nom, prenom, email, role);
        this.idAdministrateur = idAdministrateur;
        this.permissions = permissions;
    }

    // Getters et Setters
    public String getIdAdministrateur() {
        return idAdministrateur;
    }

    public void setIdAdministrateur(String idAdministrateur) {
        this.idAdministrateur = idAdministrateur;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    // Méthodes métier
    public void gererAttributs() {
        System.out.println("Gestion des attributs du système");
    }

    public void gererSysteme() {
        System.out.println("Gestion globale du système");
    }

    public void raccourcirDelais() {
        System.out.println("Raccourcissement des délais système");
    }
}
