package com.greenhouse.controllers.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;
import com.greenhouse.entities.Identity;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Logger;

@ApplicationScoped
public class IdentityRepository {
    private static final Logger LOGGER = Logger.getLogger(IdentityRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    public void save(Identity identity) {
        // Check if identity already exists
        Optional<Identity> existing = findById(identity.getId());

        if (existing.isPresent()) {
            // Update existing identity
            template.update(identity);
            LOGGER.info("Updated identity: " + identity.getId());
        } else {
            // Insert new identity
            template.insert(identity);
            LOGGER.info("Inserted new identity: " + identity.getId());
        }
    }

    public Optional<Identity> findById(String id) {
        try {
            return template.find(Identity.class, id);
        } catch (Exception e) {
            LOGGER.warning("Error finding identity by ID: " + id + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Identity> findByEmail(String email) {
        try {
            return template.select(Identity.class)
                    .stream()
                    .map(obj -> (Identity) obj)
                    .filter(identity -> identity.getEmail() != null && identity.getEmail().equals(email))
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warning("Error finding identity by email: " + email + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Identity> findByUsername(String username) {
        try {
            return template.select(Identity.class)
                    .stream()
                    .map(obj -> (Identity) obj)
                    .filter(identity -> identity.getUsername() != null && identity.getUsername().equals(username))
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warning("Error finding identity by username: " + username + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Stream<Identity> findAll() {
        try {
            return template.select(Identity.class).stream().map(obj -> (Identity) obj);
        } catch (Exception e) {
            LOGGER.warning("Error finding all identities: " + e.getMessage());
            return Stream.empty();
        }
    }

    public void delete(Identity identity) {
        template.delete(Identity.class, identity.getId());
        LOGGER.info("Deleted identity: " + identity.getId());
    }
}