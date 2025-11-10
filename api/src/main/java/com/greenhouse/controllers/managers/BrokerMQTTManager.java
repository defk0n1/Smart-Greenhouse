package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.BrokerMQTTRepository;
import com.greenhouse.entities.BrokerMQTT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BrokerMQTTManager {

    @Inject
    private BrokerMQTTRepository brokerMQTTRepository;

    public BrokerMQTT creerBrokerMQTT(BrokerMQTT broker) {
        return brokerMQTTRepository.save(broker);
    }

    public Optional<BrokerMQTT> trouverParId(String id) {
        return brokerMQTTRepository.findById(id);
    }

    public List<BrokerMQTT> trouverParAdresse(String adresse) {
        return brokerMQTTRepository.findByAdresse(adresse);
    }

    public void publierMessage(String brokerId, String topic, String message) {
        brokerMQTTRepository.findById(brokerId).ifPresent(broker -> {
            broker.publierMessage(topic, message);
            brokerMQTTRepository.save(broker);
        });
    }

    public void subscrireTopic(String brokerId, String topic, String message) {
        brokerMQTTRepository.findById(brokerId).ifPresent(broker -> {
            broker.subscrireTopic(topic, message);
            brokerMQTTRepository.save(broker);
        });
    }

    public void supprimerBroker(String id) {
        brokerMQTTRepository.deleteById(id);
    }
}

