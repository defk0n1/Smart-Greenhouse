package com.greenhouse.controllers.repositories;

import com.greenhouse.entities.Greenhouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Logger;

@ApplicationScoped
public class GreenhouseRepository {

    private static final Logger LOGGER = Logger.getLogger(GreenhouseRepository.class.getName());

    @Inject
    private DocumentTemplate template;

    // -------------------------------------------------------------------------
    // CREATE / UPDATE
    // -------------------------------------------------------------------------
    public Greenhouse save(Greenhouse greenhouse) {
        template.insert(greenhouse);
        LOGGER.info("Saved greenhouse: " + greenhouse.getId());
        return greenhouse;
    }

    public Greenhouse update(Greenhouse greenhouse) {
        template.update(greenhouse);
        LOGGER.info("Updated greenhouse: " + greenhouse.getId());
        return greenhouse;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------
    public Optional<Greenhouse> findById(String id) {
        try {
            return template.find(Greenhouse.class, id);
        } catch (Exception e) {
            LOGGER.warning("Error finding greenhouse by ID: " + id + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public Stream<Greenhouse> findAll() {
        try {
            return template.select(Greenhouse.class).stream().map(obj -> (Greenhouse) obj);
        } catch (Exception e) {
            LOGGER.warning("Error finding all greenhouses: " + e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<Greenhouse> findByOwnerId(String ownerId) {
        try {
            return template.select(Greenhouse.class)
                    .stream()
                    .map(obj -> (Greenhouse) obj)
                    .filter(greenhouse -> greenhouse.getOwnerId() != null && greenhouse.getOwnerId().equals(ownerId));
        } catch (Exception e) {
            LOGGER.warning("Error finding greenhouses by owner: " + ownerId + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Find all greenhouses where user is either owner OR in authorizedUsers list
     */
    public Stream<Greenhouse> findByUserId(String userId) {
        try {
            return template.select(Greenhouse.class)
                    .stream()
                    .map(obj -> (Greenhouse) obj)
                    .filter(g -> (g.getOwnerId() != null && g.getOwnerId().equals(userId)) ||
                            (g.getAuthorizedUsers() != null && g.getAuthorizedUsers().contains(userId)));
        } catch (Exception e) {
            LOGGER.warning("Error finding greenhouses for user: " + userId + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<Greenhouse> findByStatus(String status) {
        try {
            return template.select(Greenhouse.class)
                    .stream()
                    .map(obj -> (Greenhouse) obj)
                    .filter(greenhouse -> greenhouse.getStatus() != null && greenhouse.getStatus().equals(status));
        } catch (Exception e) {
            LOGGER.warning("Error finding greenhouses by status: " + status + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Find greenhouse by name (for MQTT auto-discovery)
     */
    public Optional<Greenhouse> findByName(String name) {
        try {
            return template.select(Greenhouse.class)
                    .stream()
                    .map(obj -> (Greenhouse) obj)
                    .filter(g -> g.getName() != null && g.getName().equals(name))
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warning("Error finding greenhouse by name: " + name + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------
    public void delete(String id) {
        template.delete(Greenhouse.class, id);
        LOGGER.info("Deleted greenhouse: " + id);
    }

    public void delete(Greenhouse greenhouse) {
        template.delete(Greenhouse.class, greenhouse.getId());
        LOGGER.info("Deleted greenhouse: " + greenhouse.getId());
    }
}
