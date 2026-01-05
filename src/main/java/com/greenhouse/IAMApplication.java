package com.greenhouse;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/rest-iam")
public class IAMApplication extends Application {
    @Override
    public java.util.Set<Class<?>> getClasses() {
        java.util.Set<Class<?>> classes = new java.util.HashSet<>();
        // Resources
        classes.add(com.greenhouse.boundaries.IdentityManagementEndpoint.class);
        classes.add(com.greenhouse.boundaries.IdentityRegistrationEndpoint.class);
        classes.add(com.greenhouse.boundaries.JwkEndpoint.class);
        classes.add(com.greenhouse.boundaries.OAuthAuthorizationEndpoint.class);
        classes.add(com.greenhouse.boundaries.OAuthTokenEndpoint.class);
        classes.add(com.greenhouse.boundaries.TenantManagementEndpoint.class);

        // Filters
        classes.add(com.greenhouse.filters.CORSFilter.class);
        classes.add(com.greenhouse.filters.HSTSFilter.class);

        return classes;
    }
}
