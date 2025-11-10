package tn.supcom.cot.iam.controllers.repositories.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;

import java.util.Optional;

@ApplicationScoped
public class GrantRepositoryImpl implements GrantRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Grant> findById(GrantPK pk) {
        return Optional.ofNullable(em.find(Grant.class, pk));
    }

    @Override
    public Grant save(Grant entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }
}