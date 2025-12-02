package tn.supcom.cot.iam.controllers.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.supcom.cot.iam.entities.Grant;

import java.util.Optional;

@Repository
public interface GrantRepository extends CrudRepository<Grant, String> {

    // Auto-implemented by Jakarta NoSQL
    Optional<Grant> findByTenantIdAndIdentityId(String tenantId, String identityId);

    Optional<Grant> findByTenantIdAndIdentityIdAndActiveTrue(String tenantId, String identityId);

    long countByActiveTrue();

    long countByTenantId(String tenantId);
}