package com.greenhouse.boundaries;

import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.entities.Sensor;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

        @Inject
        private SensorRepository sensorRepository;

        // -------------------------------------------------------------------------
        // GET ALL
        // -------------------------------------------------------------------------
        @GET
        public List<Sensor> getAllSensors() {
                return sensorRepository.findAll()
                                .collect(Collectors.toList());
        }

        // -------------------------------------------------------------------------
        // GET BY ID
        // -------------------------------------------------------------------------
        @GET
        @Path("/{id}")
        public Response getSensorById(@PathParam("id") String id) {
                Optional<Sensor> sensor = sensorRepository.findById(id);
                return sensor
                                .map(value -> Response.ok(value).build())
                                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                                                .entity("Sensor not found with id: " + id)
                                                .build());
        }

        // -------------------------------------------------------------------------
        // GET BY TYPE
        // -------------------------------------------------------------------------
        @GET
        @Path("/type/{type}")
        public List<Sensor> getSensorsByType(@PathParam("type") String type) {
                return sensorRepository.findByType(type)
                                .collect(Collectors.toList());
        }

        // -------------------------------------------------------------------------
        // GET LATEST FOR EACH TYPE
        // -------------------------------------------------------------------------
        @GET
        @Path("/latest")
        public List<Sensor> getLatestSensors() {
                return sensorRepository.findAll()
                                .collect(Collectors.groupingBy(Sensor::getSensorId))
                                .values().stream()
                                .map(sensors -> sensors.stream()
                                                .max((s1, s2) -> s1.getMeasurementTime()
                                                                .compareTo(s2.getMeasurementTime()))
                                                .orElse(null))
                                .filter(s -> s != null)
                                .collect(Collectors.toList());
        }
}
