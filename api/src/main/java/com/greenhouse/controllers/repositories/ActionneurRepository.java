package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Actionneur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface ActionneurRepository extends CrudRepository<Actionneur, String> {

    List<Actionneur> findByType(String type);
    Stream<Actionneur> findByStatut(String statut);

    default List<Actionneur> findActionneursActifs() {
        return findByStatut("ACTIF").toList();
    }
}

