package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurLuminosite;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurLuminositeRepository extends CrudRepository<CapteurLuminosite, String> {

    List<CapteurLuminosite> findByLocalisation(String localisation);
    List<CapteurLuminosite> findByLuminositeBetween(double min, double max);
}

