package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Sensor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Logger;

@ApplicationScoped
public class SensorRepository {
    private static final Logger LOGGER = Logger.getLogger(SensorRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    public void save(Sensor sensor) {
        template.insert(sensor);
        LOGGER.info("Saved sensor: " + sensor.getId());
    }

    public Optional<Sensor> findById(String id) {
        try {
            return template.find(Sensor.class, id);
        } catch (Exception e) {
            LOGGER.warning("Error finding sensor by ID: " + id + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Stream<Sensor> findAll() {
        try {
            return template.select(Sensor.class).stream().map(obj -> (Sensor) obj);
        } catch (Exception e) {
            LOGGER.warning("Error finding all sensors: " + e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<Sensor> findByType(String type) {
        try {
            return template.select(Sensor.class)
                    .stream()
                    .map(obj -> (Sensor) obj)
                    .filter(sensor -> sensor.getType() != null && sensor.getType().equals(type));
        } catch (Exception e) {
            LOGGER.warning("Error finding sensors by type: " + type + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    public void delete(Sensor sensor) {
        template.delete(Sensor.class, sensor.getId());
        LOGGER.info("Deleted sensor: " + sensor.getId());
    }
}
