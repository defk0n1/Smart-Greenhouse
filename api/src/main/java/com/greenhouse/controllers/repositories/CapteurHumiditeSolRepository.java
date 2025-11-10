package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.CapteurHumiditeSol;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface CapteurHumiditeSolRepository extends CrudRepository<CapteurHumiditeSol, String> {

    List<CapteurHumiditeSol> findByLocalisation(String localisation);
    List<CapteurHumiditeSol> findByArroser(boolean arroser);
}

