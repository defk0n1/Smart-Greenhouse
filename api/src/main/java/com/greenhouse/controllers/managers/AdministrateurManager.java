package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.AdministrateurRepository;
import com.greenhouse.entities.Administrateur;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AdministrateurManager {

    @Inject
    private AdministrateurRepository administrateurRepository;

    public Administrateur creerAdministrateur(Administrateur administrateur) {
        if (administrateurRepository.existsByEmail(administrateur.getEmail())) {
            throw new IllegalArgumentException("Un administrateur avec cet email existe déjà");
        }
        return administrateurRepository.save(administrateur);
    }

    public Optional<Administrateur> trouverParId(String id) {
        return administrateurRepository.findById(id);
    }

    public Optional<Administrateur> trouverParEmail(String email) {
        return administrateurRepository.findByEmail(email);
    }

    public Optional<Administrateur> trouverParIdAdministrateur(String idAdministrateur) {
        return administrateurRepository.findByIdAdministrateur(idAdministrateur);
    }

    public void supprimerAdministrateur(String id) {
        administrateurRepository.deleteById(id);
    }

    public List<Administrateur> obtenirTousLesAdministrateurs() {
        return administrateurRepository.findAll().collect(java.util.stream.Collectors.toList());
    }
}

