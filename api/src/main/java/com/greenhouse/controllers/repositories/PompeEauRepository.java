package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.PompeEau;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface PompeEauRepository extends CrudRepository<PompeEau, String> {

    List<PompeEau> findByQuantiteBetween(double min, double max);
    List<PompeEau> findByTempsFonctionnement(int temps);
}

