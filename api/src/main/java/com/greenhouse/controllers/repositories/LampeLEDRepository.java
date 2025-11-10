package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.LampeLED;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface LampeLEDRepository extends CrudRepository<LampeLED, String> {

    List<LampeLED> findByCouleur(String couleur);
    List<LampeLED> findByIntensiteBetween(int min, int max);
}

