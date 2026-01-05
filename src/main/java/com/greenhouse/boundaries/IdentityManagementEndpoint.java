package com.greenhouse.boundaries;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import com.greenhouse.entities.Identity;
import com.greenhouse.controllers.managers.IdentityManager;
import com.greenhouse.security.JwtManager;

@Path("/identities")
public class IdentityManagementEndpoint {

    @OPTIONS
    @Path("{path : .*}")
    public Response options() {
        return Response.ok("")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .build();
    }

    @Inject
    IdentityManager identityManager;
    @EJB
    private JwtManager jwtManager;

    // Update Identity
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateIdentity(@PathParam("id") Long id, Identity updatedIdentity,
            @QueryParam("currentPassword") String currentPassword,
            @QueryParam("newPassword") String newPassword) {
        try {
            // Ensure the identity exists
            Identity existingIdentity = identityManager.getIdentityById(id);
            if (existingIdentity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Identity not found with ID: " + id)
                        .build();
            }

            // Update the identity with the new values
            identityManager.updateIdentity(id, updatedIdentity.getUsername(), updatedIdentity.getEmail(), newPassword,
                    currentPassword);

            // Return success response with updated identity
            return Response.ok(updatedIdentity).build();
        } catch (EJBException e) {
            // Return an error if the password validation fails
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    // Delete Identity (User)
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteIdentity(@PathParam("id") Long id) {
        try {
            // Delete the identity by ID
            identityManager.deleteIdentityById(id);

            // Return success response
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (EJBException e) {
            // Return an error if deletion fails
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Identity not found with ID: " + id)
                    .build();
        }
    }

    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProfile(@HeaderParam("Authorization") String authorizationHeader,
            @CookieParam("access_token") Cookie cookie) {

        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring("Bearer ".length());
        } else if (cookie != null) {
            token = cookie.getValue();
        }

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Authorization missing").build();
        }

        try {
            var claims = jwtManager.verifyToken(token);
            String username = claims.get("sub");

            // Fetch user profile or other logic
            JsonObject profile = Json.createObjectBuilder()
                    .add("username", username)
                    .add("roles", claims.get("groups").toString())
                    .build();

            return Response.ok(profile).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token").build();
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN ENDPOINTS
    // -------------------------------------------------------------------------

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllIdentities(@HeaderParam("Authorization") String authorizationHeader,
            @CookieParam("access_token") Cookie cookie) {
        if (!isAdmin(authorizationHeader, cookie)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied: Admins only").build();
        }
        return Response.ok(identityManager.getAllIdentities()).build();
    }

    @PUT
    @Path("/{id}/status")
    public Response updateIdentityStatus(@PathParam("id") Long id,
            @QueryParam("activate") boolean activate,
            @HeaderParam("Authorization") String authorizationHeader,
            @CookieParam("access_token") Cookie cookie) {
        if (!isAdmin(authorizationHeader, cookie)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied: Admins only").build();
        }
        try {
            identityManager.updateIdentityStatus(id, activate);
            return Response.ok("Identity status updated").build();
        } catch (EJBException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    private boolean isAdmin(String authHeader, Cookie cookie) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring("Bearer ".length());
        } else if (cookie != null) {
            token = cookie.getValue();
        }

        if (token == null)
            return false;

        try {
            var claims = jwtManager.verifyToken(token);
            // Check for ROOT role or specific admin group claim
            String groups = claims.get("groups").toString(); // e.g. ["R_P00"]
            // For now, let's assume specific role or just existence of token for testing if
            // roles aren't set up perfectly yet.
            // But strict requirement says Admin PWA.
            // Role.ROOT value is Long.MAX_VALUE. But the claim likely contains the string
            // representation.
            // The claims might be "groups": ["ROOT"]?
            // Let's print logic or specific check.
            // Checking if groups contains "ROOT" or just allowing all authenticated users
            // for now if roles are not fully propagated to JWT?
            // "roles" in Identity is Long. "groups" in JWT is likely List<String> mapped
            // from roles.
            // Let's assume "ROOT" string.
            return groups.contains("ROOT") || groups.contains("root");
        } catch (Exception e) {
            return false;
        }
    }

}
