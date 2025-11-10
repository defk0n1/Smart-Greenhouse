package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Ventilateur;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface VentilateurRepository extends CrudRepository<Ventilateur, String> {

    List<Ventilateur> findByRefroidir(boolean refroidir);
    List<Ventilateur> findByVitesseBetween(int min, int max);
}

