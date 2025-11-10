package com.greenhouse.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordUtil {

    private static final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        // Paramètres : iterations, memory, parallelism
        return argon2.hash(2, 65536, 1, password.toCharArray());
    }

    public boolean verify(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return argon2.verify(hash, password.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }
}

