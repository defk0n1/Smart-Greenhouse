package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurTemperatureRepository;
import com.greenhouse.entities.CapteurTemperature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurTemperatureManager {

    @Inject
    private CapteurTemperatureRepository capteurTemperatureRepository;

    // Ajouter ce setter pour les tests Mockito
    public void setRepository(CapteurTemperatureRepository repository) {
        this.capteurTemperatureRepository = repository;
    }

    public CapteurTemperature enregistrerCapteur(CapteurTemperature capteur) {
        return capteurTemperatureRepository.save(capteur);
    }

    public Optional<CapteurTemperature> trouverParId(String id) {
        return capteurTemperatureRepository.findById(id);
    }

    public List<CapteurTemperature> trouverParLocalisation(String localisation) {
        return capteurTemperatureRepository.findByLocalisation(localisation);
    }

    public List<CapteurTemperature> trouverParPlageTemperature(double min, double max) {
        return capteurTemperatureRepository.findByTemperatureBetween(min, max);
    }

    public void mettreAJourTemperature(String id, double temperature) {
        capteurTemperatureRepository.findById(id).ifPresent(capteur -> {
            capteur.setTemperature(temperature);
            capteur.setValeurConcrete(temperature);
            capteurTemperatureRepository.save(capteur);
        });
    }

    public void supprimerCapteur(String id) {
        capteurTemperatureRepository.deleteById(id);
    }
}
