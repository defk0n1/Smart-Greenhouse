package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Actuator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Logger;

@ApplicationScoped
public class ActuatorRepository {
    private static final Logger LOGGER = Logger.getLogger(ActuatorRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    public void save(Actuator actuator) {
        template.insert(actuator);
        LOGGER.info("Saved actuator: " + actuator.getId());
    }

    public Optional<Actuator> findByActuatorId(String actuatorId) {
        try {
            return template.select(Actuator.class)
                    .where("actuator_id").eq(actuatorId)
                    .stream()
                    .map(obj -> (Actuator) obj)
                    .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        } catch (Exception e) {
            LOGGER.warning("Error finding actuator by actuatorId: " + actuatorId + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Actuator> findById(String id) {
        try {
            return template.find(Actuator.class, id);
        } catch (Exception e) {
            LOGGER.warning("Error finding actuator by ID: " + id + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Stream<Actuator> findAll() {
        try {
            return template.select(Actuator.class).stream().map(obj -> (Actuator) obj);
        } catch (Exception e) {
            LOGGER.warning("Error finding all actuators: " + e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<Actuator> findByType(String type) {
        try {
            return template.select(Actuator.class)
                    .stream()
                    .map(obj -> (Actuator) obj)
                    .filter(actuator -> actuator.getType() != null && actuator.getType().equals(type));
        } catch (Exception e) {
            LOGGER.warning("Error finding actuators by type: " + type + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    public void delete(Actuator actuator) {
        template.delete(Actuator.class, actuator.getId());
        LOGGER.info("Deleted actuator: " + actuator.getId());
    }
}
