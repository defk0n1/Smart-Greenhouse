package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Utilisateur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository extends CrudRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByLogin(String login);
    boolean existsByEmail(String email);
}

