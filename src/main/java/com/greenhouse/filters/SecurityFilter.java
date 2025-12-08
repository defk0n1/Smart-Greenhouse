package com.greenhouse.filters;

import com.greenhouse.security.JwtVerifier;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {

    @Inject
    private JwtVerifier jwtVerifier;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Allow public endpoints (e.g., health checks, swagger)
        if (path.startsWith("/openapi") || path.equals("/health")) {
            return;
        }

        // Allow OPTIONS method for CORS preflight
        if (requestContext.getMethod().equals("OPTIONS")) {
            requestContext.abortWith(Response.ok().build());
            return;
        }

        Cookie tokenCookie = requestContext.getCookies().get("access_token");
        if (tokenCookie == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        Map<String, Object> claims = jwtVerifier.verify(tokenCookie.getValue());
        if (claims == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // Optionally set security context here
    }
}
