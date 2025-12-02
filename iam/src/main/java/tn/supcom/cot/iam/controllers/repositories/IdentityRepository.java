package tn.supcom.cot.iam.controllers.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.supcom.cot.iam.entities.Identity;

import java.util.Optional;

@Repository
public interface IdentityRepository extends CrudRepository<Identity, String> {

    // Auto-implemented by Jakarta NoSQL
    Optional<Identity> findByUsername(String username);

    Optional<Identity> findByEmail(String email);

    Optional<Identity> findByUsernameAndActiveTrue(String username);

    long countByActiveTrue();
}