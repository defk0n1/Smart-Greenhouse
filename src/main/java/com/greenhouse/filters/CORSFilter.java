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
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext cres) throws IOException {

        String origin = requestContext.getHeaderString("Origin");
        if (origin != null) {
            cres.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
        } else {
            cres.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        }

        cres.getHeaders().putSingle("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, pre-authorization");
        cres.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        cres.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        cres.getHeaders().putSingle("Access-Control-Max-Age", "1209600");
    }
}