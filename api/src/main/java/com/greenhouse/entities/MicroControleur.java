package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.ArrayList;
import java.util.List;

@Entity
public class MicroControleur {

    @Id
    @Column
    private String id;

    @Column
    private String adresseIP;

    @Column
    private List<Capteur> listeCapteur;

    @Column
    private List<Actionneur> listeActionneur;

    // Constructeur
    public MicroControleur() {
        this.listeCapteur = new ArrayList<>();
        this.listeActionneur = new ArrayList<>();
    }

    public MicroControleur(String id, String adresseIP) {
        this.id = id;
        this.adresseIP = adresseIP;
        this.listeCapteur = new ArrayList<>();
        this.listeActionneur = new ArrayList<>();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdresseIP() {
        return adresseIP;
    }

    public void setAdresseIP(String adresseIP) {
        this.adresseIP = adresseIP;
    }

    public List<Capteur> getListeCapteur() {
        return listeCapteur;
    }

    public void setListeCapteur(List<Capteur> listeCapteur) {
        this.listeCapteur = listeCapteur;
    }

    public List<Actionneur> getListeActionneur() {
        return listeActionneur;
    }

    public void setListeActionneur(List<Actionneur> listeActionneur) {
        this.listeActionneur = listeActionneur;
    }

    // Méthodes métier
    public String lireDonneesCapteur(Capteur c) {
        if (c != null && listeCapteur.contains(c)) {
            return "Données du capteur " + c.getIdEquipement() + ": " + c.getValeurConcrete() + " " + c.getUnite();
        }
        return "Capteur non trouvé";
    }

    public String envoyerCaptures(List<Capteur> c) {
        if (c != null && !c.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Envoi de ").append(c.size()).append(" captures: ");
            for (Capteur capteur : c) {
                sb.append(capteur.getIdEquipement()).append("=").append(capteur.getValeurConcrete()).append(" ");
            }
            return sb.toString();
        }
        return "Aucune capture à envoyer";
    }

    public String envoyerCommande(Actionneur a, String donnee) {
        if (a != null && listeActionneur.contains(a)) {
            a.donnerOrdre(donnee);
            return "Commande envoyée à " + a.getIdEquipement() + ": " + donnee;
        }
        return "Actionneur non trouvé";
    }

    public void envoyerConnect() {
        System.out.println("Connexion du microcontrôleur " + id + " à l'adresse " + adresseIP);
    }
}

