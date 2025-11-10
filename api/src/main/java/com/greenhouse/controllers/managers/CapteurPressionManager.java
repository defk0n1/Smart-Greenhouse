package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurPressionRepository;
import com.greenhouse.entities.CapteurPression;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurPressionManager {

    @Inject
    private CapteurPressionRepository capteurPressionRepository;

    public CapteurPression enregistrerCapteur(CapteurPression capteur) {
        return capteurPressionRepository.save(capteur);
    }

    public Optional<CapteurPression> trouverParId(String id) {
        return capteurPressionRepository.findById(id);
    }

    public List<CapteurPression> trouverParPlagePression(double min, double max) {
        return capteurPressionRepository.findByPressionBetween(min, max);
    }

    public void mettreAJourPression(String id, double pression) {
        capteurPressionRepository.findById(id).ifPresent(capteur -> {
            capteur.setPression(pression);
            capteur.setValeurConcrete(pression);
            capteurPressionRepository.save(capteur);
        });
    }
}

