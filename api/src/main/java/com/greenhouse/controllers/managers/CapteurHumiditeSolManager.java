package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.CapteurHumiditeSolRepository;
import com.greenhouse.entities.CapteurHumiditeSol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CapteurHumiditeSolManager {

    @Inject
    private CapteurHumiditeSolRepository capteurHumiditeSolRepository;

    public CapteurHumiditeSol enregistrerCapteur(CapteurHumiditeSol capteur) {
        return capteurHumiditeSolRepository.save(capteur);
    }

    public Optional<CapteurHumiditeSol> trouverParId(String id) {
        return capteurHumiditeSolRepository.findById(id);
    }

    public List<CapteurHumiditeSol> trouverParLocalisation(String localisation) {
        return capteurHumiditeSolRepository.findByLocalisation(localisation);
    }

    public List<CapteurHumiditeSol> trouverCapteursNecessitantArrosage() {
        return capteurHumiditeSolRepository.findByArroser(true);
    }

    public void mettreAJourHumidite(String id, double humidite) {
        capteurHumiditeSolRepository.findById(id).ifPresent(capteur -> {
            capteur.setHumiditeSol(humidite);
            capteur.setValeurConcrete(humidite);
            capteurHumiditeSolRepository.save(capteur);
        });
    }
}

