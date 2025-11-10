package tn.supcom.cot.iam.controllers.repositories;

import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;

import java.util.Optional;

public interface GrantRepository {
    Optional<Grant> findById(GrantPK pk);
    Grant save(Grant entity);
}