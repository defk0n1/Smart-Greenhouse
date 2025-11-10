package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurPH;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurPHRepository extends CrudRepository<CapteurPH, String> {

    List<CapteurPH> findByPhBetween(double min, double max);
}

