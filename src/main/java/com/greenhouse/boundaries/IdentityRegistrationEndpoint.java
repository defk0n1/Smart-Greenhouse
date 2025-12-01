package com.greenhouse.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import com.greenhouse.controllers.repositories.TenantRepository;
import com.greenhouse.controllers.managers.IdentityManager;

import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
public class IdentityRegistrationEndpoint {

    private static final Logger LOGGER = Logger.getLogger(IdentityRegistrationEndpoint.class.getName());

    @Inject
    IdentityManager identityManager;

    @Inject
    TenantRepository tenantRepository;

    @GET
    @Path("/register/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorizeRegistration(@Context UriInfo uriInfo) {
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

            // Validate client_id
            String clientId = params.getFirst("client_id");
            if (isNullOrEmpty(clientId)) {
                return informUserAboutError("Invalid client_id: " + clientId);
            }

            var tenantOpt = tenantRepository.findByName(clientId);
            if (tenantOpt.isEmpty()) {
                return informUserAboutError("Invalid client_id: " + clientId);
            }
            var tenant = tenantOpt.get();

            // Validate redirectUri
            String redirectUri = params.getFirst("redirect_uri");
            if (!isRedirectUriValid(tenant.getRedirectUri(), redirectUri)) {
                return informUserAboutError("Invalid or mismatched redirect_uri");
            }

            // Stream the registration page
            StreamingOutput stream = createHtmlResponse("/Register.html");
            return Response.ok(stream)
                    .location(uriInfo.getBaseUri().resolve("/register"))
                    .build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during authorization: ", e);
            return informUserAboutError("An unexpected error occurred.");
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response register(@FormParam("username") String username,
            @FormParam("email") String email,
            @FormParam("password") String password,
            @Context UriInfo uriInfo) {
        try {
            String activationBaseUrl = uriInfo.getBaseUriBuilder()
                    .path(IdentityRegistrationEndpoint.class)
                    .path(IdentityRegistrationEndpoint.class, "activate")
                    .build()
                    .toString();

            identityManager.registerIdentity(username, password, email, activationBaseUrl);

            // Stream the confirmation page
            StreamingOutput stream = createHtmlResponse("/Activate.html");
            return Response.ok(stream).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration: ", e);
            return informUserAboutError(e.getMessage());
        }
    }

    @POST
    @Path("/register/activate")
    public Response activate(@FormParam("code") String code) {
        try {
            identityManager.activateIdentity(code);
            return Response.ok().build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during activation: ", e);

            return informUserAboutError(e.getMessage());
        }
    }

    @GET
    @Path("/register/activate")
    public Response activateAccount(@QueryParam("code") String code, @Context UriInfo uriInfo) {
        try {
            identityManager.activateIdentity(code);
            // Redirect to login page after successful activation
            return Response
                    .seeOther(uriInfo.getBaseUri()
                            .resolve("/authorize?client_id=smartgreenhouse&redirect_uri=http://localhost:8000"))
                    .build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during activation: ", e);
            return informUserAboutError(e.getMessage());
        }
    }

    /**
     * Helper method to validate redirectUri.
     */
    private boolean isRedirectUriValid(String tenantRedirectUri, String providedRedirectUri) {
        if (!isNullOrEmpty(tenantRedirectUri)) {
            return providedRedirectUri != null && providedRedirectUri.equals(tenantRedirectUri);
        }
        return !isNullOrEmpty(providedRedirectUri);
    }

    /**
     * Helper method to create an HTML response from a file.
     */
    private StreamingOutput createHtmlResponse(String filePath) {
        return output -> {
            try (InputStream is = Objects.requireNonNull(getClass().getResource(filePath)).openStream()) {
                output.write(is.readAllBytes());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error streaming HTML file: " + filePath, e);
                throw new WebApplicationException("Failed to load HTML content.", e);
            }
        };
    }

    /**
     * Helper method to check if a string is null or empty.
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Helper method to inform the user about an error using HTML.
     */
    private Response informUserAboutError(String error) {
        String errorMessage = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\"/>" +
                        "<title>Error</title>" +
                        "</head>" +
                        "<body>" +
                        "<aside class=\"container\">" +
                        "<h1>Error Occurred</h1>" +
                        "<p>%s</p>" +
                        "</aside>" +
                        "</body>" +
                        "</html>",
                error);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage)
                .type(MediaType.TEXT_HTML)
                .build();
    }
}
