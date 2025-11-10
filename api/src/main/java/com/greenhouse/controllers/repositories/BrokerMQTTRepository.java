package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.BrokerMQTT;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface BrokerMQTTRepository extends CrudRepository<BrokerMQTT, String> {

    List<BrokerMQTT> findByAdresse(String adresse);
    List<BrokerMQTT> findByPort(int port);
}

