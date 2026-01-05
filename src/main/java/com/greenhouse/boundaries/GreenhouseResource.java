package com.greenhouse.boundaries;

import com.greenhouse.controllers.repositories.GreenhouseRepository;
import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.controllers.managers.GreenhouseManager;
import com.greenhouse.entities.Greenhouse;
import com.greenhouse.entities.Sensor;
import com.greenhouse.entities.Actuator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/greenhouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GreenhouseResource {

    @Inject
    private GreenhouseRepository greenhouseRepository;

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    private GreenhouseManager greenhouseManager;

    @Context
    private SecurityContext securityContext;

    // -------------------------------------------------------------------------
    // OPTIONS (CORS preflight)
    // -------------------------------------------------------------------------
    @OPTIONS
    public Response optionsGreenhouses() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/{id}")
    public Response optionsGreenhouseById() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path("/{id}/sensors")
    public Response optionsGreenhouseSensors() {
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/sensors")
    public Response getGreenhouseSensors(@PathParam("id") String id) {
        try {
            Optional<Greenhouse> gh = greenhouseRepository.findById(id);
            if (!gh.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Greenhouse not found")
                        .build();
            }

            List<String> sensorIds = gh.get().getSensors();
            if (sensorIds == null || sensorIds.isEmpty()) {
                return Response.ok(java.util.Collections.emptyList()).build();
            }

            List<Sensor> sensors = sensorIds.stream()
                    .map(sensorId -> sensorRepository.findAll()
                            .filter(s -> s.getSensorId() != null && s.getSensorId().equals(sensorId))
                            .max((s1, s2) -> s1.getMeasurementTime().compareTo(s2.getMeasurementTime()))
                            .orElse(null))
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

            return Response.ok(sensors).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error fetching sensors: " + e.getMessage())
                    .build();
        }
    }

    @OPTIONS
    @Path("/{id}/actuators")
    public Response optionsGreenhouseActuators() {
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/actuators")
    public Response getGreenhouseActuators(@PathParam("id") String id) {
        try {
            Optional<Greenhouse> gh = greenhouseRepository.findById(id);
            if (!gh.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Greenhouse not found")
                        .build();
            }

            List<String> actuatorIds = gh.get().getActuators();
            if (actuatorIds == null || actuatorIds.isEmpty()) {
                return Response.ok(java.util.Collections.emptyList()).build();
            }

            List<Actuator> actuators = actuatorIds.stream()
                    .map(actuatorId -> actuatorRepository.findAll()
                            .filter(a -> a.getActuatorId() != null && a.getActuatorId().equals(actuatorId))
                            .max((a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()))
                            .orElse(null))
                    .filter(a -> a != null)
                    .collect(Collectors.toList());

            return Response.ok(actuators).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error fetching actuators: " + e.getMessage())
                    .build();
        }
    }

    @OPTIONS
    @Path("/{id}/users/{userId}")
    public Response optionsAssignUser() {
        return Response.ok().build();
    }

    // Helper to get User ID from Principal
    // Assuming Principal.getName() returns the User ID (or Username)
    // Adjust based on your JWT Principal implementation
    private String getUserId(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        return principal != null ? principal.getName() : null;
    }

    // Helper to check if user is Admin / Root
    private boolean isAdmin() {
        if (securityContext == null)
            return false;

        // TEMPORARY FIX: Force 'mohamed' to be treated as non-admin to verify isolation
        // The user 'mohamed' has the ROOT role in DB, preventing isolation testing.
        String userId = getUserId(securityContext);
        if (userId != null && userId.trim().equalsIgnoreCase("mohamed")) {
            return false;
        }

        // Check for root role (case insensitive strategy)
        return securityContext.isUserInRole("ROOT") || securityContext.isUserInRole("root");
    }

    // Helper to check if user can manage the greenhouse (Owner OR Admin)
    private boolean canManage(Greenhouse g, String userId) {
        return (g.getOwnerId() != null && g.getOwnerId().equals(userId)) || isAdmin();
    }

    // -------------------------------------------------------------------------
    // GET ALL ACCESSIBLE GREENHOUSES (Admins see ALL)
    // -------------------------------------------------------------------------
    @GET
    public Response getAccessibleGreenhouses(@Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Authentication required")
                        .build();
            }

            List<Greenhouse> greenhouses;
            if (isAdmin()) {
                greenhouses = greenhouseRepository.findAll().collect(Collectors.toList());
            } else {
                greenhouses = greenhouseRepository.findByUserId(userId).collect(Collectors.toList());
            }
            return Response.ok(greenhouses).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error fetching greenhouses: " + e.getMessage())
                    .build();
        }
    }

    // Helper: enrich greenhouse with sensor and actuator data (latest values only)
    private com.greenhouse.dto.GreenhouseWithDevices enrichGreenhouse(Greenhouse gh) {
        List<Sensor> sensors = gh.getSensors() != null
                ? gh.getSensors().stream()
                        .map(sensorId -> sensorRepository.findAll()
                                .filter(s -> s.getSensorId() != null && s.getSensorId().equals(sensorId))
                                .max((s1, s2) -> s1.getMeasurementTime().compareTo(s2.getMeasurementTime()))
                                .orElse(null))
                        .filter(s -> s != null)
                        .collect(Collectors.toList())
                : java.util.Collections.emptyList();

        List<Actuator> actuators = gh.getActuators() != null
                ? gh.getActuators().stream()
                        .map(actuatorId -> actuatorRepository.findAll()
                                .filter(a -> a.getActuatorId() != null && a.getActuatorId().equals(actuatorId))
                                .max((a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()))
                                .orElse(null))
                        .filter(a -> a != null)
                        .collect(Collectors.toList())
                : java.util.Collections.emptyList();

        return new com.greenhouse.dto.GreenhouseWithDevices(gh, sensors, actuators);
    }

    // -------------------------------------------------------------------------
    // GET GREENHOUSE BY ID
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    public Response getGreenhouseById(@PathParam("id") String id,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> greenhouseOpt = greenhouseRepository.findById(id);

            if (greenhouseOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Greenhouse not found")
                        .build();
            }

            Greenhouse g = greenhouseOpt.get();
            // Check Access
            if (!canManage(g, userId) && !g.getAuthorizedUsers().contains(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Access denied to this greenhouse")
                        .build();
            }

            // Return enriched data with sensors and actuators
            return Response.ok(enrichGreenhouse(g)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // CREATE GREENHOUSE (Authenticated)
    // -------------------------------------------------------------------------
    @POST
    public Response createGreenhouse(Greenhouse greenhouse,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            // Validate input
            if (greenhouse.getName() == null || greenhouse.getName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Greenhouse name is required")
                        .build();
            }

            greenhouse.setOwnerId(userId);
            Greenhouse created = greenhouseManager.createGreenhouse(greenhouse);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating greenhouse: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE GREENHOUSE
    // -------------------------------------------------------------------------
    @PUT
    @Path("/{id}")
    public Response updateGreenhouse(@PathParam("id") String id,
            Greenhouse greenhouse,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> existingOpt = greenhouseRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Greenhouse existing = existingOpt.get();

            // Check Permissions
            if (!canManage(existing, userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only the owner or admin can update this greenhouse")
                        .build();
            }

            // Update fields (preserve owner and id)
            existing.setName(greenhouse.getName());
            existing.setDescription(greenhouse.getDescription());
            existing.setLocation(greenhouse.getLocation());
            existing.setMqttTopic(greenhouse.getMqttTopic());
            existing.setStatus(greenhouse.getStatus());

            if (greenhouse.getAuthorizedUsers() != null) {
                existing.setAuthorizedUsers(greenhouse.getAuthorizedUsers());
            }

            Greenhouse updated = greenhouseManager.updateGreenhouse(existing);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating greenhouse: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE GREENHOUSE
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{id}")
    public Response deleteGreenhouse(@PathParam("id") String id,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> existingOpt = greenhouseRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return Response.ok("Greenhouse deleted successfully").build();
            }
            Greenhouse existing = existingOpt.get();

            if (!canManage(existing, userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only the owner or admin can delete this greenhouse")
                        .build();
            }

            greenhouseManager.deleteGreenhouse(id);
            return Response.ok("Greenhouse deleted successfully").build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting greenhouse: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // ASSIGN USER TO GREENHOUSE
    // -------------------------------------------------------------------------
    @POST
    @Path("/{greenhouseId}/users/{userId}")
    public Response assignUser(@PathParam("greenhouseId") String greenhouseId,
            @PathParam("userId") String targetUserId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> existingOpt = greenhouseRepository.findById(greenhouseId);
            if (existingOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Greenhouse existing = existingOpt.get();

            // Check Permissions
            if (!canManage(existing, userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only the owner or admin can assign users to this greenhouse")
                        .build();
            }

            List<String> users = existing.getAuthorizedUsers();
            if (!users.contains(targetUserId)) {
                users.add(targetUserId);
                existing.setAuthorizedUsers(users);
                greenhouseRepository.update(existing);
                return Response.ok("User assigned successfully").build();
            } else {
                return Response.ok("User already assigned").build();
            }

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error assigning user: " + e.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // REMOVE USER FROM GREENHOUSE
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{greenhouseId}/users/{userId}")
    public Response removeUser(@PathParam("greenhouseId") String greenhouseId,
            @PathParam("userId") String targetUserId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> existingOpt = greenhouseRepository.findById(greenhouseId);
            if (existingOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Greenhouse existing = existingOpt.get();

            // Check Permissions
            if (!canManage(existing, userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only the owner or admin can remove users from this greenhouse")
                        .build();
            }

            List<String> users = existing.getAuthorizedUsers();
            if (users.remove(targetUserId)) {
                existing.setAuthorizedUsers(users);
                greenhouseRepository.update(existing);
                return Response.ok("User removed successfully").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("User not in list")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error removing user: " + e.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // DEVICE ATTACHMENT ENDPOINTS
    // =========================================================================

    /**
     * Attach a sensor to a greenhouse
     * POST /api/greenhouses/{id}/sensors/{sensorId}
     */
    @POST
    @Path("/{id}/sensors/{sensorId}")
    public Response attachSensor(
            @PathParam("id") String greenhouseId,
            @PathParam("sensorId") String sensorId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Authentication required")
                        .build();
            }

            Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
            if (ghOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Greenhouse not found")
                        .build();
            }

            if (!canManage(ghOpt.get(), userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("You don't have permission to modify this greenhouse")
                        .build();
            }

            greenhouseManager.attachSensor(greenhouseId, sensorId);

            return Response.ok()
                    .entity("Sensor attached successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error attaching sensor: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Detach a sensor from a greenhouse
     * DELETE /api/greenhouses/{id}/sensors/{sensorId}
     */
    @DELETE
    @Path("/{id}/sensors/{sensorId}")
    public Response detachSensor(
            @PathParam("id") String greenhouseId,
            @PathParam("sensorId") String sensorId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
            if (ghOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (!canManage(ghOpt.get(), userId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            greenhouseManager.detachSensor(greenhouseId, sensorId);

            return Response.ok()
                    .entity("Sensor detached successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error detaching sensor: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Attach an actuator to a greenhouse
     * POST /api/greenhouses/{id}/actuators/{actuatorId}
     */
    @POST
    @Path("/{id}/actuators/{actuatorId}")
    public Response attachActuator(
            @PathParam("id") String greenhouseId,
            @PathParam("actuatorId") String actuatorId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
            if (ghOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (!canManage(ghOpt.get(), userId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            greenhouseManager.attachActuator(greenhouseId, actuatorId);

            return Response.ok()
                    .entity("Actuator attached successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error attaching actuator: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Detach an actuator from a greenhouse
     * DELETE /api/greenhouses/{id}/actuators/{actuatorId}
     */
    @DELETE
    @Path("/{id}/actuators/{actuatorId}")
    public Response detachActuator(
            @PathParam("id") String greenhouseId,
            @PathParam("actuatorId") String actuatorId,
            @Context SecurityContext securityContext) {
        try {
            String userId = getUserId(securityContext);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Optional<Greenhouse> ghOpt = greenhouseRepository.findById(greenhouseId);
            if (ghOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (!canManage(ghOpt.get(), userId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            greenhouseManager.detachActuator(greenhouseId, actuatorId);

            return Response.ok()
                    .entity("Actuator detached successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error detaching actuator: " + e.getMessage())
                    .build();
        }
    }

    /**
     * DELETE (endpoint removed - duplicated above)
     */
}
