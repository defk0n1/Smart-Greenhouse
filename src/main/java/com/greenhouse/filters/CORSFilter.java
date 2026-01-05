package com.greenhouse.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        System.out.println("🔍 CORSFilter Request: " + method + " " + requestContext.getUriInfo().getPath());
        // Log all headers for debugging
        requestContext.getHeaders().forEach((k, v) -> System.out.println("   > " + k + ": " + v));

        if ("OPTIONS".equalsIgnoreCase(method)) {
            // Just return OK, let response filter add headers
            requestContext.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext cres) throws IOException {

        System.out.println("🛡️ CORSFilter Response: Adding headers to " + requestContext.getMethod());

        String origin = requestContext.getHeaderString("Origin");
        System.out.println("🛡️ CORSFilter: Origin received = " + origin);

        if (origin != null) {
            cres.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            cres.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            System.out.println("   => Set ACAO: " + origin + ", ACAC: true");
        } else {
            cres.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
            System.out.println("   => Set ACAO: *");
        }

        String requestHeaders = requestContext.getHeaderString("Access-Control-Request-Headers");
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            cres.getHeaders().putSingle("Access-Control-Allow-Headers", requestHeaders);
            System.out.println("   => Set ACAH (Echoed): " + requestHeaders);
        } else {
            cres.getHeaders().putSingle("Access-Control-Allow-Headers",
                    "origin, content-type, accept, authorization, pre-authorization, cache-control, x-requested-with");
            System.out.println("   => Set ACAH (Default)");
        }

        cres.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        cres.getHeaders().putSingle("Access-Control-Max-Age", "0"); // Disable caching for debugging
        cres.getHeaders().putSingle("Vary", "Origin"); // Critical for browsers to cache CORS responses correctly per
                                                       // origin
    }
}
