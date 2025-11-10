package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurPression;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurPressionRepository extends CrudRepository<CapteurPression, String> {

    List<CapteurPression> findByPressionBetween(double min, double max);
}

