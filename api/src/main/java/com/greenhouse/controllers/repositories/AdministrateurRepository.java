package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Administrateur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface AdministrateurRepository extends CrudRepository<Administrateur, String> {

    Optional<Administrateur> findByEmail(String email);
    Optional<Administrateur> findByIdAdministrateur(String idAdministrateur);
    boolean existsByEmail(String email);
}

