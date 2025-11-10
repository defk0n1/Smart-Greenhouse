package tn.supcom.cot.iam.controllers.repositories;

import tn.supcom.cot.iam.entities.Tenant;

import java.util.Optional;

public interface TenantRepository {
    Optional<Tenant> findById(String id);
    Optional<Tenant> findByName(String name);
    Tenant save(Tenant entity);
}