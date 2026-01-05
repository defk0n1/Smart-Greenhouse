package com.greenhouse.boundaries;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import com.greenhouse.controllers.repositories.IamRepository;
import com.greenhouse.security.AuthorizationCode;
import com.greenhouse.security.JwtManager;

import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/oauth/token")
public class OAuthTokenEndpoint {
    private final Set<String> supportedGrantTypes = Set.of("authorization_code", "refresh_token");

    @Inject
    private IamRepository iamRepository;

    @EJB
    private JwtManager jwtManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response token(@FormParam("grant_type") String grantType,
            @FormParam("code") String authCode,
            @FormParam("code_verifier") String codeVerifier) {

        if (grantType == null || grantType.isEmpty())
            return responseError("Invalid_request", "grant_type is required", Response.Status.BAD_REQUEST);

        if (!supportedGrantTypes.contains(grantType)) {
            return responseError("unsupported_grant_type", "grant_type should be one of :" + supportedGrantTypes,
                    Response.Status.BAD_REQUEST);
        }

        // Common cookie builder
        // IMPORTANT: Set domain to "localhost" to share cookie across ports (8080,
        // 8081, etc.) during development
        NewCookie.Builder cookieBuilder = new NewCookie.Builder("access_token")
                .path("/")
                .domain("localhost") // Enable cross-port cookie sharing
                .httpOnly(true)
                .secure(false) // Set to true in production
                .maxAge(ConfigProvider.getConfig().getValue("jwt.lifetime.duration", Integer.class))
                .sameSite(NewCookie.SameSite.LAX);

        if ("refresh_token".equals(grantType)) {
            var previousAccessToken = jwtManager.verifyToken(authCode);
            var previousRefreshToken = jwtManager.verifyToken(codeVerifier);

            if (!previousAccessToken.isEmpty() && !previousRefreshToken.isEmpty()) {
                try {
                    // Extract values from Map<String, String>
                    String tenantId = previousAccessToken.get("tenant_id");
                    String subject = previousRefreshToken.get("sub");
                    String scopes = previousAccessToken.get("scope");

                    // Handle roles extraction
                    String[] roles = Json.createReader(new StringReader(previousAccessToken.get("groups")))
                            .readArray()
                            .getValuesAs(JsonString.class)
                            .stream()
                            .map(JsonString::getString)
                            .collect(Collectors.toList())
                            .toArray(new String[0]);

                    String accessToken = jwtManager.generateToken(tenantId, subject, scopes, roles);
                    String refreshToken = jwtManager.generateToken(tenantId, subject, scopes,
                            new String[] { "refresh_role" });

                    String refreshSubject = previousRefreshToken.get("sub");
                    String refreshTenantId = previousRefreshToken.get("tenant_id");
                    String refreshScopes = previousRefreshToken.get("scope");

                    if (refreshScopes.equals(scopes) && refreshTenantId.equals(tenantId)
                            && refreshSubject.equals(subject)) {
                        return Response.ok(Json.createObjectBuilder()
                                .add("token_type", "Bearer")
                                .add("access_token", accessToken)
                                .add("expires_in",
                                        ConfigProvider.getConfig().getValue("jwt.lifetime.duration", Integer.class))
                                .add("scope", scopes)
                                .add("refresh_token", refreshToken)
                                .build())
                                .cookie(cookieBuilder.value(accessToken).build())
                                .header("Cache-Control", "no-store")
                                .header("Pragma", "no-cache")
                                .build();
                    } else {
                        return responseError("Invalid_request", "Can't get token", Response.Status.UNAUTHORIZED);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }
            }
            return Response.ok().build();
        }

        // Authorization Code Grant
        try {
            AuthorizationCode decoded = AuthorizationCode.decode(authCode, codeVerifier);
            assert decoded != null;
            String tenantName = decoded.tenantName();

            String accessToken = jwtManager.generateToken(tenantName, decoded.identityUsername(),
                    decoded.approvedScopes(), iamRepository.getRoles(decoded.identityUsername()));

            String refreshToken = jwtManager.generateToken(tenantName, decoded.identityUsername(),
                    decoded.approvedScopes(), new String[] { "refresh_role" });

            return Response.ok(Json.createObjectBuilder()
                    .add("token_type", "Bearer")
                    .add("access_token", accessToken)
                    .add("expires_in", ConfigProvider.getConfig().getValue("jwt.lifetime.duration", Integer.class))
                    .add("scope", decoded.approvedScopes())
                    .add("refresh_token", refreshToken)
                    .build())
                    .cookie(cookieBuilder.value(accessToken).build())
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .build();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (WebApplicationException e) {
            e.printStackTrace();
            return e.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DEBUG: OAuthTokenEndpoint failure: " + e.getMessage());
            return responseError("Invalid_request", "Can't get token: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response responseError(String error, String errorDescription, Response.Status status) {
        JsonObject errorResponse = Json.createObjectBuilder()
                .add("error", error)
                .add("error_description", errorDescription)
                .build();
        return Response.status(status)
                .entity(errorResponse).build();
    }
}