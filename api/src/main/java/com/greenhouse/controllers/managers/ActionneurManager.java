package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActionneurRepository;
import com.greenhouse.entities.Actionneur;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ActionneurManager {

    @Inject
    private ActionneurRepository actionneurRepository;

    // Méthode pour injecter le mock dans les tests
    public void setRepository(ActionneurRepository repository) {
        this.actionneurRepository = repository;
    }

    public Actionneur enregistrerActionneur(Actionneur actionneur) {
        return actionneurRepository.save(actionneur);
    }

    public Optional<Actionneur> trouverActionneurParId(String id) {
        return actionneurRepository.findById(id);
    }

    public List<Actionneur> trouverActionneursParType(String type) {
        return actionneurRepository.findByType(type);
    }

    public List<Actionneur> obtenirActionneursActifs() {
        return actionneurRepository.findActionneursActifs();
    }

    public void activerActionneur(String id) {
        actionneurRepository.findById(id).ifPresent(actionneur -> {
            actionneur.activer();
            actionneurRepository.save(actionneur);
        });
    }

    public void desactiverActionneur(String id) {
        actionneurRepository.findById(id).ifPresent(actionneur -> {
            actionneur.desactiver();
            actionneurRepository.save(actionneur);
        });
    }
}
