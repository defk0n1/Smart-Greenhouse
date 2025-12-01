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
        for (Role role : Role.values()) {
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