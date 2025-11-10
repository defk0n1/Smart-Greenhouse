package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurLuminositeRepository;
import com.greenhouse.entities.CapteurLuminosite;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurLuminositeManager {

    @Inject
    private CapteurLuminositeRepository capteurLuminositeRepository;

    public CapteurLuminosite enregistrerCapteur(CapteurLuminosite capteur) {
        return capteurLuminositeRepository.save(capteur);
    }

    public Optional<CapteurLuminosite> trouverParId(String id) {
        return capteurLuminositeRepository.findById(id);
    }

    public List<CapteurLuminosite> trouverParLocalisation(String localisation) {
        return capteurLuminositeRepository.findByLocalisation(localisation);
    }

    public List<CapteurLuminosite> trouverParPlageLuminosite(double min, double max) {
        return capteurLuminositeRepository.findByLuminositeBetween(min, max);
    }

    public void mettreAJourLuminosite(String id, double luminosite) {
        capteurLuminositeRepository.findById(id).ifPresent(capteur -> {
            capteur.setLuminosite(luminosite);
            capteur.setValeurConcrete(luminosite);
            capteurLuminositeRepository.save(capteur);
        });
    }
}

