package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

@Entity
public abstract class Equipement {

    @Id
    @Column
    private String idEquipement;

    @Column
    private String type;

    @Column
    private String statut;

    @Column
    private String depositionDate;

    @Column
    private String depositFrom;

    @Column
    private String depositTo;

    @Column
    private String clientType;

    // Constructeurs
    public Equipement() {}

    public Equipement(String idEquipement, String type, String statut, String depositionDate,
                      String depositFrom, String depositTo, String clientType) {
        this.idEquipement = idEquipement;
        this.type = type;
        this.statut = statut;
        this.depositionDate = depositionDate;
        this.depositFrom = depositFrom;
        this.depositTo = depositTo;
        this.clientType = clientType;
    }

    // Getters et Setters
    public String getIdEquipement() {
        return idEquipement;
    }

    public void setIdEquipement(String idEquipement) {
        this.idEquipement = idEquipement;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDepositionDate() {
        return depositionDate;
    }

    public void setDepositionDate(String depositionDate) {
        this.depositionDate = depositionDate;
    }

    public String getDepositFrom() {
        return depositFrom;
    }

    public void setDepositFrom(String depositFrom) {
        this.depositFrom = depositFrom;
    }

    public String getDepositTo() {
        return depositTo;
    }

    public void setDepositTo(String depositTo) {
        this.depositTo = depositTo;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
