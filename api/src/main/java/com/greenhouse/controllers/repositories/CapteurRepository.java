package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Capteur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface CapteurRepository extends CrudRepository<Capteur, String> {

    List<Capteur> findByType(String type);
    Stream<Capteur> findByStatut(String statut);

    default List<Capteur> findCapteursActifs() {
        return findByStatut("ACTIF").toList();
    }
}

