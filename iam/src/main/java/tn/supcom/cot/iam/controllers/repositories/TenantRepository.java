package tn.supcom.cot.iam.controllers.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.supcom.cot.iam.entities.Tenant;

import java.util.Optional;

@Repository
public interface TenantRepository extends CrudRepository<Tenant, String> {

    // Auto-implemented by Jakarta NoSQL
    Optional<Tenant> findByName(String name);

    Optional<Tenant> findByNameAndActiveTrue(String name);

    long countByActiveTrue();
}