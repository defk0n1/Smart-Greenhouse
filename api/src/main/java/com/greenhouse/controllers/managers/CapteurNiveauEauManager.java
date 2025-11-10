package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurNiveauEauRepository;
import com.greenhouse.entities.CapteurNiveauEau;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurNiveauEauManager {

    @Inject
    private CapteurNiveauEauRepository capteurNiveauEauRepository;

    public CapteurNiveauEau enregistrerCapteur(CapteurNiveauEau capteur) {
        return capteurNiveauEauRepository.save(capteur);
    }

    public Optional<CapteurNiveauEau> trouverParId(String id) {
        return capteurNiveauEauRepository.findById(id);
    }

    public List<CapteurNiveauEau> trouverParPlageNiveau(double min, double max) {
        return capteurNiveauEauRepository.findByNiveauEauBetween(min, max);
    }

    public List<CapteurNiveauEau> trouverParCapacite(double capaciteMax) {
        return capteurNiveauEauRepository.findByCapaciteMax(capaciteMax);
    }

    public void mettreAJourNiveauEau(String id, double niveauEau) {
        capteurNiveauEauRepository.findById(id).ifPresent(capteur -> {
            capteur.setNiveauEau(niveauEau);
            capteur.setValeurConcrete(niveauEau);
            capteurNiveauEauRepository.save(capteur);
        });
    }
}

