package com.greenhouse.providers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @jakarta.ws.rs.core.Context
    private jakarta.servlet.http.HttpServletRequest request;

    @Override
    public Response toResponse(Throwable exception) {
        exception.printStackTrace(); // Log the error on the server side

        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Unknown error";

        // Use a generic response but try to be CORS compliant
        Response.ResponseBuilder builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + errorMessage + "\"}")
                .header("Content-Type", "application/json");

        String origin = request.getHeader("Origin");
        if (origin != null) {
            builder.header("Access-Control-Allow-Origin", origin);
            builder.header("Access-Control-Allow-Credentials", "true");
        } else {
            builder.header("Access-Control-Allow-Origin", "*");
        }

        return builder
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Allow-Headers",
                        "origin, content-type, accept, authorization, pre-authorization")
                .build();
    }
}
