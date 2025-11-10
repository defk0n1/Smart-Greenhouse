package com.greenhouse.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.HashMap;
import java.util.Map;

@Entity
public class BrokerMQTT {

    @Id
    @Column
    private String id;

    @Column
    private String adresse;

    @Column
    private int port;

    @Column
    private Map<String, String> topics;

    // Constructeur
    public BrokerMQTT() {
        this.topics = new HashMap<>();
    }

    public BrokerMQTT(String id, String adresse, int port, Map<String, String> topics) {
        this.id = id;
        this.adresse = adresse;
        this.port = port;
        this.topics = topics != null ? topics : new HashMap<>();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }

    // Méthodes métier
    public void subscrireTopic(String topic, String message) {
        if (topic != null && message != null) {
            this.topics.put(topic, message);
            System.out.println("Souscription au topic: " + topic);
        }
    }

    public void publierMessage(String topic, String message) {
        if (topic != null && message != null) {
            System.out.println("Publication sur le topic '" + topic + "': " + message);
            this.topics.put(topic, message);
        }
    }

    public void recevoirNouveauMessage(String topic, String message) {
        if (topic != null && message != null) {
            this.topics.put(topic, message);
            System.out.println("Nouveau message reçu sur '" + topic + "': " + message);
        }
    }
}

