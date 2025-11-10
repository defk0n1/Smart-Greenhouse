package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurTemperature;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurTemperatureRepository extends CrudRepository<CapteurTemperature, String> {

    List<CapteurTemperature> findByLocalisation(String localisation);
    List<CapteurTemperature> findByTemperatureBetween(double min, double max);
}

