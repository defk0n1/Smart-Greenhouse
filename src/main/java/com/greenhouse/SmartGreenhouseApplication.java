package com.greenhouse;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class SmartGreenhouseApplication extends Application {
    @Override
    public java.util.Set<Class<?>> getClasses() {
        java.util.Set<Class<?>> classes = new java.util.HashSet<>();
        // Resources
        classes.add(com.greenhouse.boundaries.GreenhouseResource.class);
        classes.add(com.greenhouse.boundaries.SensorResource.class);
        classes.add(com.greenhouse.boundaries.ActuatorResource.class);

        // Filters
        classes.add(com.greenhouse.filters.CORSFilter.class);
        classes.add(com.greenhouse.filters.SecurityFilter.class);

        classes.add(com.greenhouse.filters.HSTSFilter.class);

        // Providers
        classes.add(com.greenhouse.providers.GlobalExceptionMapper.class);

        return classes;
    }
}
