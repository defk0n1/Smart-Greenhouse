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

@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActuatorResource {

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    private ActuatorManager actuatorManager;

    // -------------------------------------------------------------------------
    // GET ALL
    // -------------------------------------------------------------------------
    @GET
    public List<Actuator> getAllActuators() {
        return actuatorRepository.findAll()
                .collect(Collectors.groupingBy(Actuator::getActuatorId))
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
    public List<Actuator> getActuatorsByType(@PathParam("type") String type) {
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
            actuatorManager.controlActuator(id, command, value);
            return Response.ok("Command sent to actuator " + id).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }
}
