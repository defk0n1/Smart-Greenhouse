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

}
