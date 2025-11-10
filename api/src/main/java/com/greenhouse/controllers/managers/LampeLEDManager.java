package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.LampeLEDRepository;
import com.greenhouse.entities.LampeLED;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LampeLEDManager {

    @Inject
    private LampeLEDRepository lampeLEDRepository;

    public LampeLED enregistrerLampe(LampeLED lampe) {
        return lampeLEDRepository.save(lampe);
    }

    public Optional<LampeLED> trouverParId(String id) {
        return lampeLEDRepository.findById(id);
    }

    public List<LampeLED> trouverParCouleur(String couleur) {
        return lampeLEDRepository.findByCouleur(couleur);
    }

    public List<LampeLED> trouverParPlageIntensite(int min, int max) {
        return lampeLEDRepository.findByIntensiteBetween(min, max);
    }

    public void augmenterIntensite(String id, int intensite) {
        lampeLEDRepository.findById(id).ifPresent(lampe -> {
            lampe.augmenterIntensite(intensite);
            lampeLEDRepository.save(lampe);
        });
    }

    public void reduireIntensite(String id, int intensite) {
        lampeLEDRepository.findById(id).ifPresent(lampe -> {
            lampe.reduireIntensite(intensite);
            lampeLEDRepository.save(lampe);
        });
    }

    public void activerLampe(String id) {
        lampeLEDRepository.findById(id).ifPresent(lampe -> {
            lampe.activer();
            lampeLEDRepository.save(lampe);
        });
    }

    public void desactiverLampe(String id) {
        lampeLEDRepository.findById(id).ifPresent(lampe -> {
            lampe.desactiver();
            lampeLEDRepository.save(lampe);
        });
    }
}

