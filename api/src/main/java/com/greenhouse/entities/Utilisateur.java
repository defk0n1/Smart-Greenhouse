package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;

@Entity
public class Utilisateur extends Personne {

    @Column
    private String idUtilisateur;

    @Column
    private String login;

    @Column
    private String mdp;

    // Constructeur
    public Utilisateur() {}

    public Utilisateur(String id, String nom, String prenom, String email, String role,
                       String idUtilisateur, String login, String mdp) {
        super(id, nom, prenom, email, role);
        this.idUtilisateur = idUtilisateur;
        this.login = login;
        this.mdp = mdp;
    }

    // Getters et Setters
    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getMdp() { return mdp; }
    public void setMdp(String mdp) { this.mdp = mdp; }

    // Méthode métier
    public void envoyerNotifications() {
        // Implémentation pour envoyer des notifications
        System.out.println("Envoi de notifications à l'utilisateur: " + obtenirEmail());
    }
}