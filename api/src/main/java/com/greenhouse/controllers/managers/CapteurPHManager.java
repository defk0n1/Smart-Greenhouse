package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurPHRepository;
import com.greenhouse.entities.CapteurPH;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurPHManager {

    @Inject
    private CapteurPHRepository capteurPHRepository;

    public CapteurPH enregistrerCapteur(CapteurPH capteur) {
        return capteurPHRepository.save(capteur);
    }

    public Optional<CapteurPH> trouverParId(String id) {
        return capteurPHRepository.findById(id);
    }

    public List<CapteurPH> trouverParPlagePH(double min, double max) {
        return capteurPHRepository.findByPhBetween(min, max);
    }

    public void mettreAJourPH(String id, double ph) {
        capteurPHRepository.findById(id).ifPresent(capteur -> {
            capteur.setPh(ph);
            capteur.setValeurConcrete(ph);
            capteurPHRepository.save(capteur);
        });
    }
}

