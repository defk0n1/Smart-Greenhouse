package com.greenhouse.filters;

import com.greenhouse.security.JwtVerifier;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {

    @Inject
    private JwtVerifier jwtVerifier;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        // DEBUG: Log all requests
        System.out.println("[SecurityFilter] " + method + " " + path);

        System.out.println("   Headers:");
        requestContext.getHeaders().forEach((k, v) -> System.out.println("   - " + k + ": " + v));

        // ============================================================================
        // IMPORTANT: DEVELOPMENT MODE - Allow all unauthenticated access
        // THIS SHOULD BE REMOVED IN PRODUCTION!
        // ============================================================================
        /*
         * System.out.
         * println("[SecurityFilter] ⚠️  DEVELOPMENT MODE - Bypassing authentication for: "
         * + path);
         * 
         * // Disable HSTS (max-age=0) just in case
         * // requestContext.setProperty("hsts-disabled", true);
         * 
         * // Set a DUMMY SecurityContext so the API thinks a user is logged in
         * final String devUser = "hatem"; // Using a known username
         * SecurityContext originalContext = requestContext.getSecurityContext();
         * 
         * requestContext.setSecurityContext(new SecurityContext() {
         * 
         * @Override
         * public Principal getUserPrincipal() {
         * return new Principal() {
         * 
         * @Override
         * public String getName() {
         * return devUser;
         * }
         * };
         * }
         * 
         * @Override
         * public boolean isUserInRole(String role) {
         * return true; // Pretend to be Admin/Root for dev mode
         * }
         * 
         * @Override
         * public boolean isSecure() {
         * return originalContext.isSecure();
         * }
         * 
         * @Override
         * public String getAuthenticationScheme() {
         * return "DEV-BYPASS";
         * }
         * });
         * 
         * return; // Allow request to proceed with dummy context
         */
        // Allow public endpoints (e.g., health checks, swagger, openapi)
        if (path.startsWith("/openapi") || path.equals("/health")) {
            return;
        }

        // Allow OPTIONS method for CORS preflight - Just in case CORSFilter didn't
        // catch it (it should)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        // Check Authorization Header
        String authHeader = requestContext.getHeaderString("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println(
                    "   > Found Token in Header: " + (token.length() > 10 ? token.substring(0, 10) + "..." : token));
        } else {
            // Check Cookie
            Map<String, Cookie> cookies = requestContext.getCookies();
            if (cookies != null && cookies.containsKey("access_token")) {
                token = cookies.get("access_token").getValue();
                System.out.println("   > Found Token in Cookie");
            }
        }

        if (token == null) {
            System.out.println("❌ SecurityFilter: No access_token found in Header or Cookie");

            // Return 401. CORSFilter response filter will add headers.
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
            responseBuilder.header("WWW-Authenticate", "Bearer realm=\"SmartGreenhouse\"");
            requestContext.abortWith(responseBuilder.build());
            return;
        }

        System.out.println("✅ SecurityFilter: token found");

        Map<String, Object> claims = jwtVerifier.verify(token);
        if (claims == null) {
            System.out.println("❌ SecurityFilter: JWT verification FAILED (claims is null)");

            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);

            responseBuilder.header("WWW-Authenticate", "Bearer realm=\"SmartGreenhouse\"");

            requestContext.abortWith(responseBuilder.build());
            return;
        }

        System.out.println("✅ SecurityFilter: JWT verification SUCCESS");

        // Create a simple security context with the authenticated user
        final String username = (String) claims.get("sub");
        SecurityContext originalContext = requestContext.getSecurityContext();

        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> username;
            }

            @Override
            public boolean isUserInRole(String role) {
                if (role == null)
                    return false;
                Object groups = claims.get("groups");
                if (groups instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) groups;
                    return list.contains(role);
                }
                return false;
            }

            @Override
            public boolean isSecure() {
                return originalContext.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return "JWT";
            }
        });
    }
}
