package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

@Entity("capteurs_humidite_sol")
public class CapteurHumiditeSol extends Capteur {

    @Id
    @Column
    private String idEquipement;  // clé primaire MongoDB

    @Column
    private String localisation;

    @Column
    private double humiditeSol;

    @Column
    private boolean arroser;

    // Constructeur
    public CapteurHumiditeSol() {}

    public CapteurHumiditeSol(String idEquipement, String localisation, double humiditeSol) {
        this.idEquipement = idEquipement;
        setType("HUMIDITE_SOL");
        this.localisation = localisation;
        this.humiditeSol = humiditeSol;
        setUnite("%");
        this.arroser = humiditeSol < 30.0;
    }

    // Getters et Setters
    public String getIdEquipement() {
        return idEquipement;
    }

    public void setIdEquipement(String idEquipement) {
        this.idEquipement = idEquipement;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public double getHumiditeSol() {
        return humiditeSol;
    }

    public void setHumiditeSol(double humiditeSol) {
        this.humiditeSol = humiditeSol;
        this.arroser = humiditeSol < 30.0;
    }

    public boolean isArroser() {
        return arroser;
    }

    public void setArroser(boolean arroser) {
        this.arroser = arroser;
    }
}
