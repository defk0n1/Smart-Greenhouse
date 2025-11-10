package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.SystemeSerre;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface SystemeSerreRepository extends CrudRepository<SystemeSerre, String> {

    Optional<SystemeSerre> findByNom(String nom);
    Optional<SystemeSerre> findByLogin(String login);
    boolean existsByNom(String nom);
}

