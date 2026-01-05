package com.greenhouse.boundaries;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.greenhouse.security.JwtManager;

@Path("/jwk")
public class JwkEndpoint {

    @EJB
    private JwtManager jwtManager;

    @GET
    @Path("/{kid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicKey(@PathParam("kid") String kid) {
        try {
            return Response.ok(jwtManager.getPublicKeyAsJWK(kid)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/keys")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKeys() {
        return Response.ok(jwtManager.getAllPublicKeysAsJWKS()).build();
    }
}
