package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.SensorReading;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;

import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class SensorReadingRepository {

    private static final Logger LOGGER = Logger.getLogger(SensorReadingRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    public SensorReading save(SensorReading reading) {
        template.insert(reading);
        return reading;
    }

    public Stream<SensorReading> findBySensorId(String sensorId) {
        try {
            return template.select(SensorReading.class)
                    .stream()
                    .map(obj -> (SensorReading) obj)
                    .filter(r -> r.getSensorId() != null && r.getSensorId().equals(sensorId));
        } catch (Exception e) {
            LOGGER.warning("Error finding readings for sensor: " + sensorId);
            return Stream.empty();
        }
    }

    public Stream<SensorReading> findByGreenhouseId(String greenhouseId) {
        try {
            return template.select(SensorReading.class)
                    .stream()
                    .map(obj -> (SensorReading) obj)
                    .filter(r -> r.getGreenhouseId() != null && r.getGreenhouseId().equals(greenhouseId));
        } catch (Exception e) {
            LOGGER.warning("Error finding readings for greenhouse: " + greenhouseId);
            return Stream.empty();
        }
    }
}
