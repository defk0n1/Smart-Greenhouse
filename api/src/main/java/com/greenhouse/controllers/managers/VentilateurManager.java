package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.VentilateurRepository;
import com.greenhouse.entities.Ventilateur;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VentilateurManager {

    @Inject
    private VentilateurRepository ventilateurRepository;

    public Ventilateur enregistrerVentilateur(Ventilateur ventilateur) {
        return ventilateurRepository.save(ventilateur);
    }

    public Optional<Ventilateur> trouverParId(String id) {
        return ventilateurRepository.findById(id);
    }

    public List<Ventilateur> trouverParRefroidir(boolean refroidir) {
        return ventilateurRepository.findByRefroidir(refroidir);
    }

    public List<Ventilateur> trouverParPlageVitesse(int min, int max) {
        return ventilateurRepository.findByVitesseBetween(min, max);
    }

    public void augmenterVitesse(String id, int vitesse) {
        ventilateurRepository.findById(id).ifPresent(ventilateur -> {
            ventilateur.augmenterVitesse(vitesse);
            ventilateurRepository.save(ventilateur);
        });
    }

    public void ralentirVitesse(String id, int vitesse) {
        ventilateurRepository.findById(id).ifPresent(ventilateur -> {
            ventilateur.ralentirVitesse(vitesse);
            ventilateurRepository.save(ventilateur);
        });
    }

    public void activerVentilateur(String id) {
        ventilateurRepository.findById(id).ifPresent(ventilateur -> {
            ventilateur.activer();
            ventilateurRepository.save(ventilateur);
        });
    }

    public void desactiverVentilateur(String id) {
        ventilateurRepository.findById(id).ifPresent(ventilateur -> {
            ventilateur.desactiver();
            ventilateurRepository.save(ventilateur);
        });
    }

    public void supprimerVentilateur(String id) {
        ventilateurRepository.deleteById(id);
    }
}

