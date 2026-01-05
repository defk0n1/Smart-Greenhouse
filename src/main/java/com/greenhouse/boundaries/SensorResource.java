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
        // OPTIONS (CORS preflight)
        // -------------------------------------------------------------------------
        @OPTIONS
        public Response optionsSensors() {
                return Response.ok().build();
        }

        @OPTIONS
        @Path("/{id}")
        public Response optionsSensorById() {
                return Response.ok().build();
        }

        @OPTIONS
        @Path("/type/{type}")
        public Response optionsSensorByType() {
                return Response.ok().build();
        }

        @OPTIONS
        @Path("/latest")
        public Response optionsLatestSensors() {
                return Response.ok().build();
        }

        // -------------------------------------------------------------------------
        // GET ALL (with optional greenhouse filtering)
        // -------------------------------------------------------------------------
        @GET
        public List<Sensor> getAllSensors(@QueryParam("greenhouseId") String greenhouseId) {
                if (greenhouseId != null && !greenhouseId.isEmpty()) {
                        return sensorRepository.findByGreenhouseId(greenhouseId)
                                        .collect(Collectors.toList());
                }
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
        // GET BY TYPE (with optional greenhouse filtering)
        // -------------------------------------------------------------------------
        @GET
        @Path("/type/{type}")
        public List<Sensor> getSensorsByType(@PathParam("type") String type,
                        @QueryParam("greenhouseId") String greenhouseId) {
                if (greenhouseId != null && !greenhouseId.isEmpty()) {
                        return sensorRepository.findByGreenhouseIdAndType(greenhouseId, type)
                                        .collect(Collectors.toList());
                }
                return sensorRepository.findByType(type)
                                .collect(Collectors.toList());
        }

        // -------------------------------------------------------------------------
        // GET LATEST FOR EACH TYPE (with optional greenhouse filtering)
        // -------------------------------------------------------------------------
        @GET
        @Path("/latest")
        public List<Sensor> getLatestSensors(@QueryParam("greenhouseId") String greenhouseId) {
                if (greenhouseId != null && !greenhouseId.isEmpty()) {
                        return sensorRepository.findByGreenhouseId(greenhouseId)
                                        .collect(Collectors.groupingBy(Sensor::getSensorId))
                                        .values().stream()
                                        .map(sensors -> sensors.stream()
                                                        .max((s1, s2) -> s1.getMeasurementTime()
                                                                        .compareTo(s2.getMeasurementTime()))
                                                        .orElse(null))
                                        .filter(s -> s != null)
                                        .collect(Collectors.toList());
                }
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

        // -------------------------------------------------------------------------
        // CREATE SENSOR
        // -------------------------------------------------------------------------
        @POST
        public Response createSensor(Sensor sensor) {
                try {
                        if (sensor == null) {
                                return Response.status(Response.Status.BAD_REQUEST)
                                                .entity("Sensor data is required")
                                                .build();
                        }

                        // Validate required fields
                        if (sensor.getSensorId() == null || sensor.getSensorId().isEmpty()) {
                                return Response.status(Response.Status.BAD_REQUEST)
                                                .entity("Sensor ID is required")
                                                .build();
                        }

                        Sensor created = sensorRepository.save(sensor);
                        return Response.status(Response.Status.CREATED)
                                        .entity(created)
                                        .build();
                } catch (Exception e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("Error creating sensor: " + e.getMessage())
                                        .build();
                }
        }

        // -------------------------------------------------------------------------
        // UPDATE SENSOR
        // -------------------------------------------------------------------------
        @PUT
        @Path("/{id}")
        public Response updateSensor(@PathParam("id") String id, Sensor sensor) {
                try {
                        Optional<Sensor> existing = sensorRepository.findById(id);
                        if (existing.isEmpty()) {
                                return Response.status(Response.Status.NOT_FOUND)
                                                .entity("Sensor not found with id: " + id)
                                                .build();
                        }

                        // Update fields
                        sensor.setId(id); // Ensure ID doesn't change
                        Sensor updated = sensorRepository.update(sensor);
                        return Response.ok(updated).build();
                } catch (Exception e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("Error updating sensor: " + e.getMessage())
                                        .build();
                }
        }

        // -------------------------------------------------------------------------
        // DELETE SENSOR
        // -------------------------------------------------------------------------
        @DELETE
        @Path("/{id}")
        public Response deleteSensor(@PathParam("id") String id) {
                try {
                        Optional<Sensor> existing = sensorRepository.findById(id);
                        if (existing.isEmpty()) {
                                return Response.status(Response.Status.NOT_FOUND)
                                                .entity("Sensor not found with id: " + id)
                                                .build();
                        }

                        sensorRepository.delete(id);
                        return Response.ok()
                                        .entity("Sensor deleted successfully")
                                        .build();
                } catch (Exception e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("Error deleting sensor: " + e.getMessage())
                                        .build();
                }
        }
}
