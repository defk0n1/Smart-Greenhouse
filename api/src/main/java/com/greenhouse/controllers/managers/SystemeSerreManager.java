package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.SystemeSerreRepository;
import com.greenhouse.entities.Actionneur;
import com.greenhouse.entities.Capteur;
import com.greenhouse.entities.SystemeSerre;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class SystemeSerreManager {

    @Inject
    private SystemeSerreRepository systemeSerreRepository;

    // --- setter pour les tests ---
    public void setRepository(SystemeSerreRepository repository) {
        this.systemeSerreRepository = repository;
    }

    public SystemeSerre creerSystemeSerre(SystemeSerre systemeSerre) {
        if (systemeSerreRepository.existsByNom(systemeSerre.getNom())) {
            throw new IllegalArgumentException("Un système de serre avec ce nom existe déjà");
        }
        return systemeSerreRepository.save(systemeSerre);
    }

    public Optional<SystemeSerre> trouverParId(String id) {
        return systemeSerreRepository.findById(id);
    }

    public Optional<SystemeSerre> trouverParNom(String nom) {
        return systemeSerreRepository.findByNom(nom);
    }

    public void enregistrerCapteur(String systemeId, Capteur capteur) {
        systemeSerreRepository.findById(systemeId).ifPresent(systeme -> {
            systeme.enregistrerCapture(capteur);
            systemeSerreRepository.save(systeme);
        });
    }

    public void enregistrerActionneur(String systemeId, Actionneur actionneur) {
        systemeSerreRepository.findById(systemeId).ifPresent(systeme -> {
            systeme.enregistrerActionneur(actionneur);
            systemeSerreRepository.save(systeme);
        });
    }

    public void regulerConditions(String systemeId) {
        systemeSerreRepository.findById(systemeId).ifPresent(SystemeSerre::regulerConditions);
    }

    public void supprimerSystemeSerre(String id) {
        systemeSerreRepository.deleteById(id);
    }
}

