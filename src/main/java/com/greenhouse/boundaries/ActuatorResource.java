package com.greenhouse.boundaries;

import com.greenhouse.controllers.managers.ActuatorManager;
import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.entities.Actuator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActuatorResource {

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    private ActuatorManager actuatorManager;

    // -------------------------------------------------------------------------
    // OPTIONS (CORS preflight)
    // -------------------------------------------------------------------------
    @OPTIONS
    public Response optionsActuators() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/{id}")
    public Response optionsActuatorById() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/type/{type}")
    public Response optionsActuatorByType() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/{id}/toggle")
    public Response optionsToggle() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/{id}/set")
    public Response optionsSet() {
        return Response.ok().build();
    }

    // -------------------------------------------------------------------------
    // GET ALL (with optional greenhouse filtering)
    // -------------------------------------------------------------------------
    @GET
    public List<Actuator> getAllActuators(@QueryParam("greenhouseId") String greenhouseId) {
        Stream<Actuator> stream = (greenhouseId != null && !greenhouseId.isEmpty())
                ? actuatorRepository.findByGreenhouseId(greenhouseId)
                : actuatorRepository.findAll();

        return stream.collect(Collectors.groupingBy(Actuator::getActuatorId))
                .values().stream()
                .map(list -> list.stream()
                        .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                        .orElse(null))
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // GET BY ID
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    public Response getActuatorById(@PathParam("id") String id) {
        Optional<Actuator> actuator = actuatorRepository.findByActuatorId(id);

        if (actuator.isPresent()) {
            return Response.ok(actuator.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Actuator not found: " + id)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET BY TYPE
    // -------------------------------------------------------------------------
    @GET
    @Path("/type/{type}")
    public List<Actuator> getActuatorsByType(@PathParam("type") String type,
            @QueryParam("greenhouseId") String greenhouseId) {
        if (greenhouseId != null && !greenhouseId.isEmpty()) {
            return actuatorRepository.findByGreenhouseIdAndType(greenhouseId, type)
                    .collect(Collectors.toList());
        }
        return actuatorRepository.findByType(type)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // SEND COMMAND
    // -------------------------------------------------------------------------
    @POST
    @Path("/{id}/command")
    public Response sendCommand(
            @PathParam("id") String id,
            @QueryParam("command") String command,
            @QueryParam("value") Double value) {

        if (command == null || command.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing command parameter")
                    .build();
        }

        try {
            // Retrieve actuator to find its greenhouseId
            Optional<Actuator> actuatorOpt = actuatorRepository.findByActuatorId(id);
            String greenhouseId = actuatorOpt.map(Actuator::getGreenhouseId).orElse(null);

            actuatorManager.controlActuator(id, command, value, greenhouseId);
            return Response.ok("Command sent to actuator " + id).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // CREATE ACTUATOR
    // -------------------------------------------------------------------------
    @POST
    public Response createActuator(Actuator actuator) {
        try {
            if (actuator == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Actuator data is required")
                        .build();
            }

            // Validate required fields
            if (actuator.getActuatorId() == null || actuator.getActuatorId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Actuator ID is required")
                        .build();
            }

            Actuator created = actuatorRepository.save(actuator);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating actuator: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE ACTUATOR
    // -------------------------------------------------------------------------
    @PUT
    @Path("/{id}")
    public Response updateActuator(@PathParam("id") String id, Actuator actuator) {
        try {
            Optional<Actuator> existing = actuatorRepository.findByActuatorId(id);
            if (existing.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Actuator not found with id: " + id)
                        .build();
            }

            // Update fields
            actuator.setActuatorId(id); // Ensure ID doesn't change
            Actuator updated = actuatorRepository.update(actuator);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating actuator: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE ACTUATOR
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{id}")
    public Response deleteActuator(@PathParam("id") String id) {
        try {
            Optional<Actuator> existing = actuatorRepository.findByActuatorId(id);
            if (existing.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Actuator not found with id: " + id)
                        .build();
            }

            actuatorRepository.deleteByActuatorId(id);
            return Response.ok()
                    .entity("Actuator deleted successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting actuator: " + e.getMessage())
                    .build();
        }
    }
}
