package tn.supcom.cot.iam.controllers.repositories.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.entities.Identity;

import java.util.Optional;

@ApplicationScoped
public class IdentityRepositoryImpl implements IdentityRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Identity> findById(String id) {
        return Optional.ofNullable(em.find(Identity.class, id));
    }

    @Override
    public Optional<Identity> findByUsername(String username) {
        TypedQuery<Identity> q = em.createQuery(
                "SELECT i FROM Identity i WHERE i.username = :username", Identity.class);
        q.setParameter("username", username);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    @Override
    public Identity save(Identity entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }
}