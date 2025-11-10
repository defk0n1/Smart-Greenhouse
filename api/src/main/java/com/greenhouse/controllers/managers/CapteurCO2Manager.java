package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurCO2Repository;
import com.greenhouse.entities.CapteurCO2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurCO2Manager {

    @Inject
    private CapteurCO2Repository capteurCO2Repository;

    public CapteurCO2 enregistrerCapteur(CapteurCO2 capteur) {
        return capteurCO2Repository.save(capteur);
    }

    public Optional<CapteurCO2> trouverParId(String id) {
        return capteurCO2Repository.findById(id);
    }

    public List<CapteurCO2> trouverParPlageCO2(double min, double max) {
        return capteurCO2Repository.findByCo2Between(min, max);
    }

    public void mettreAJourCO2(String id, double co2) {
        capteurCO2Repository.findById(id).ifPresent(capteur -> {
            capteur.setCo2(co2);
            capteur.setValeurConcrete(co2);
            capteurCO2Repository.save(capteur);
        });
    }

    // Add delete method to align with tests
    public void supprimerCapteur(String id) {
        capteurCO2Repository.deleteById(id);
    }
}

