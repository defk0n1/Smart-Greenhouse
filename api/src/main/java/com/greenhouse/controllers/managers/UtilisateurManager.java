package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.UtilisateurRepository;
import com.greenhouse.entities.Utilisateur;
import com.greenhouse.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class UtilisateurManager {

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private PasswordUtil passwordUtil;

    // ======= Setters pour tests Mockito =======
    public void setUtilisateurRepository(UtilisateurRepository repository) {
        this.utilisateurRepository = repository;
    }

    public void setPasswordUtil(PasswordUtil passwordUtil) {
        this.passwordUtil = passwordUtil;
    }

    // ======= Méthodes métier =======
    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        String motDePasseHash = passwordUtil.hash(utilisateur.getMdp());
        utilisateur.setMdp(motDePasseHash);

        return utilisateurRepository.save(utilisateur);
    }

    public Optional<Utilisateur> authentifier(String login, String password) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByLogin(login);

        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            if (passwordUtil.verify(password, utilisateur.getMdp())) {
                return Optional.of(utilisateur);
            }
        }

        return Optional.empty();
    }

    public Optional<Utilisateur> trouverParId(String id) {
        return utilisateurRepository.findById(id);
    }

    public void supprimerUtilisateur(String id) {
        utilisateurRepository.deleteById(id);
    }
}
