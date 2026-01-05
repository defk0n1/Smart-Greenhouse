package com.greenhouse.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class HSTSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        // Enforce Strict-Transport-Security
        // Disable HSTS (max-age=0) to allow HTTP on localhost
        responseContext.getHeaders().putSingle("Strict-Transport-Security", "max-age=0");
    }
}
