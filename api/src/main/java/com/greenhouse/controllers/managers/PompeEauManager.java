package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.PompeEauRepository;
import com.greenhouse.entities.PompeEau;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PompeEauManager {

    @Inject
    private PompeEauRepository pompeEauRepository;

    public PompeEau enregistrerPompe(PompeEau pompe) {
        return pompeEauRepository.save(pompe);
    }

    public Optional<PompeEau> trouverParId(String id) {
        return pompeEauRepository.findById(id);
    }

    public List<PompeEau> trouverParPlageQuantite(double min, double max) {
        return pompeEauRepository.findByQuantiteBetween(min, max);
    }

    public List<PompeEau> trouverParTempsFonctionnement(int temps) {
        return pompeEauRepository.findByTempsFonctionnement(temps);
    }

    public void activerPompe(String id) {
        pompeEauRepository.findById(id).ifPresent(pompe -> {
            pompe.activer();
            pompeEauRepository.save(pompe);
        });
    }

    public void desactiverPompe(String id) {
        pompeEauRepository.findById(id).ifPresent(pompe -> {
            pompe.desactiver();
            pompeEauRepository.save(pompe);
        });
    }

    public void configurerQuantite(String id, double quantite) {
        pompeEauRepository.findById(id).ifPresent(pompe -> {
            pompe.setQuantite(quantite);
            pompeEauRepository.save(pompe);
        });
    }
}

