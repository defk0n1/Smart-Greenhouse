package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

@Entity("capteurs_humidite_air")
public class CapteurHumiditeAir extends Capteur {

    @Id
    @Column
    private String idEquipement;  // clé primaire MongoDB

    @Column
    private String localisation;

    @Column
    private double humiditeAir;

    // Constructeur
    public CapteurHumiditeAir() {}

    public CapteurHumiditeAir(String idEquipement, String localisation, double humiditeAir) {
        this.idEquipement = idEquipement;
        setType("HUMIDITE_AIR");
        this.localisation = localisation;
        this.humiditeAir = humiditeAir;
        setUnite("%");
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

    public double getHumiditeAir() {
        return humiditeAir;
    }

    public void setHumiditeAir(double humiditeAir) {
        this.humiditeAir = humiditeAir;
    }
}
