package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurRepository;
import com.greenhouse.entities.Capteur;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurManager {

    @Inject
    private CapteurRepository capteurRepository;

    public Capteur enregistrerCapteur(Capteur capteur) {
        return capteurRepository.save(capteur);
    }

    public Optional<Capteur> trouverCapteurParId(String id) {
        return capteurRepository.findById(id);
    }

    public List<Capteur> trouverCapteursParType(String type) {
        return capteurRepository.findByType(type);
    }

    public List<Capteur> obtenirCapteursActifs() {
        return capteurRepository.findCapteursActifs();
    }

    public void mettreAJourValeurCapteur(String capteurId, double nouvelleValeur) {
        capteurRepository.findById(capteurId).ifPresent(capteur -> {
            capteur.setValeurConcrete(nouvelleValeur);
            capteurRepository.save(capteur);
        });
    }
}

