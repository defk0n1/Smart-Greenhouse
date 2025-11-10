package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurNiveauEau;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurNiveauEauRepository extends CrudRepository<CapteurNiveauEau, String> {

    List<CapteurNiveauEau> findByNiveauEauBetween(double min, double max);
    List<CapteurNiveauEau> findByCapaciteMax(double capaciteMax);
}

