package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.MicroControleur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface MicroControleurRepository extends CrudRepository<MicroControleur, String> {

    Optional<MicroControleur> findByAdresseIP(String adresseIP);
}

