package com.greenhouse.controllers.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.greenhouse.controllers.Role;

import java.util.HashSet;

@ApplicationScoped
public class IamRepository {

    @Inject
    IdentityRepository identityRepository;

    public String[] getRoles(String username) {
        Long roles = identityRepository.findByUsername(username).get().getRoles();
        HashSet<String> ret = new HashSet<>();

        // CRITICAL FIX: ROOT must be exact match, not bitwise
        // Bug: (1 & Long.MAX_VALUE) != 0 is TRUE, incorrectly granting ROOT to all
        // users
        if (roles.equals(Role.ROOT.getValue())) {
            ret.add(Role.ROOT.name().toLowerCase());
            return ret.toArray(new String[0]);
        }

        // For non-ROOT roles, use bitwise check
        for (Role role : Role.values()) {
            if (role == Role.ROOT)
                continue; // Already handled above

            if ((roles & role.getValue()) != 0L) {
                String value = Role.byValue(role.getValue());
                if (value != null) {
                    ret.add(value);
                }
            }
        }
        return ret.toArray(new String[0]);
    }
}