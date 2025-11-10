package tn.supcom.cot.iam.controllers.repositories;

import tn.supcom.cot.iam.entities.Identity;

import java.util.Optional;

public interface IdentityRepository {
    Optional<Identity> findById(String id);
    Optional<Identity> findByUsername(String username);
    Identity save(Identity entity);
}