package tn.supcom.cot.iam.controllers.repositories.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Tenant;

import java.util.Optional;

@ApplicationScoped
public class TenantRepositoryImpl implements TenantRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Tenant> findById(String id) {
        return Optional.ofNullable(em.find(Tenant.class, id));
    }

    @Override
    public Optional<Tenant> findByName(String name) {
        TypedQuery<Tenant> q = em.createQuery(
                "SELECT t FROM Tenant t WHERE t.name = :name", Tenant.class);
        q.setParameter("name", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    @Override
    public Tenant save(Tenant entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }
}