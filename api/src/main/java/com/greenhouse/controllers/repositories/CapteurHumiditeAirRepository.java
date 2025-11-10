package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurHumiditeAir;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurHumiditeAirRepository extends CrudRepository<CapteurHumiditeAir, String> {

    List<CapteurHumiditeAir> findByLocalisation(String localisation);
    List<CapteurHumiditeAir> findByHumiditeAirBetween(double min, double max);
}

