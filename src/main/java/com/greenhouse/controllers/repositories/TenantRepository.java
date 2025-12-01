package com.greenhouse.controllers.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;
import com.greenhouse.entities.Tenant;

import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class TenantRepository {
    private static final Logger LOGGER = Logger.getLogger(TenantRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    public void save(Tenant tenant) {
        template.insert(tenant);
        LOGGER.info("Saved tenant: " + tenant.getName());
    }

    public Optional<Tenant> findById(String id) {
        try {
            return template.find(Tenant.class, id);
        } catch (Exception e) {
            LOGGER.warning("Error finding tenant by ID: " + id + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Tenant> findByName(String name) {
        try {
            return template.select(Tenant.class)
                    .stream()
                    .map(obj -> (Tenant) obj)
                    .filter(tenant -> tenant.getName() != null && tenant.getName().equals(name))
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warning("Error finding tenant by name: " + name + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(Tenant tenant) {
        template.delete(Tenant.class, tenant.getName());
        LOGGER.info("Deleted tenant: " + tenant.getName());
    }
}
