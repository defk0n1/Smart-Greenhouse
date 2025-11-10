package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurCO2;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurCO2Repository extends CrudRepository<CapteurCO2, String> {

    List<CapteurCO2> findByCo2Between(double min, double max);
}

