package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurHumiditeAirRepository;
import com.greenhouse.entities.CapteurHumiditeAir;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurHumiditeAirManager {

    @Inject
    private CapteurHumiditeAirRepository capteurHumiditeAirRepository;

    public CapteurHumiditeAir enregistrerCapteur(CapteurHumiditeAir capteur) {
        return capteurHumiditeAirRepository.save(capteur);
    }

    public Optional<CapteurHumiditeAir> trouverParId(String id) {
        return capteurHumiditeAirRepository.findById(id);
    }

    public List<CapteurHumiditeAir> trouverParLocalisation(String localisation) {
        return capteurHumiditeAirRepository.findByLocalisation(localisation);
    }

    public List<CapteurHumiditeAir> trouverParPlageHumidite(double min, double max) {
        return capteurHumiditeAirRepository.findByHumiditeAirBetween(min, max);
    }

    public void mettreAJourHumidite(String id, double humidite) {
        capteurHumiditeAirRepository.findById(id).ifPresent(capteur -> {
            capteur.setHumiditeAir(humidite);
            capteur.setValeurConcrete(humidite);
            capteurHumiditeAirRepository.save(capteur);
        });
    }

    public void supprimerCapteur(String id) {
        capteurHumiditeAirRepository.deleteById(id);
    }
}

