package com.greenhouse.boundaries;

import com.greenhouse.entities.Tenant;
import com.greenhouse.controllers.repositories.TenantRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

/**
 * Endpoint for managing OAuth Tenants (Clients)
 */
@Path("/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenantManagementEndpoint {
    private static final Logger LOGGER = Logger.getLogger(TenantManagementEndpoint.class.getName());

    @Inject
    TenantRepository tenantRepository;

    /**
     * Initialize default tenant "smartgreenhouse"
     * This can be called once to create the default OAuth client
     */
    @POST
    @Path("/init")
    public Response initializeDefaultTenant() {
        try {
            // Check if tenant already exists
            var existingTenant = tenantRepository.findByName("smartgreenhouse");
            if (existingTenant.isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"message\": \"Tenant 'smartgreenhouse' already exists\"}")
                        .build();
            }

            // Create new tenant
            Tenant tenant = new Tenant();
            tenant.setId("smartgreenhouse");
            tenant.setName("smartgreenhouse");
            tenant.setSecret(""); // No secret required for PKCE
            tenant.setRedirectUri(""); // Allow any redirect URI
            tenant.setSupportedGrantTypes("authorization_code");
            tenant.setRequiredScopes("resource.read resource.write");
            tenant.setAllowedRoles(0L); // All roles allowed

            tenantRepository.save(tenant);

            LOGGER.info("Default tenant 'smartgreenhouse' created successfully");

            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Tenant 'smartgreenhouse' created successfully\"}")
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error creating default tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create tenant: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Create a new tenant
     */
    @POST
    public Response createTenant(Tenant tenant) {
        try {
            // Check if tenant already exists
            var existingTenant = tenantRepository.findByName(tenant.getName());
            if (existingTenant.isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"message\": \"Tenant already exists\"}")
                        .build();
            }

            tenantRepository.save(tenant);

            LOGGER.info("Tenant created: " + tenant.getName());

            return Response.status(Response.Status.CREATED)
                    .entity(tenant)
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error creating tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create tenant\"}")
                    .build();
        }
    }

    /**
     * Get tenant by name
     */
    @GET
    @Path("/{name}")
    public Response getTenant(@PathParam("name") String name) {
        try {
            var tenant = tenantRepository.findByName(name);
            if (tenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Tenant not found\"}")
                        .build();
            }

            return Response.ok(tenant.get()).build();

        } catch (Exception e) {
            LOGGER.severe("Error getting tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get tenant\"}")
                    .build();
        }
    }
}
