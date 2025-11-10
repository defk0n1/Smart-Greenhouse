package com.greenhouse.test;

import com.greenhouse.entities.Utilisateur;
import com.greenhouse.controllers.managers.UtilisateurManager;
import com.greenhouse.controllers.repositories.UtilisateurRepository;
import com.greenhouse.security.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UtilisateurManagerTest {

    private UtilisateurRepository utilisateurRepository;
    private PasswordUtil passwordUtil;
    private UtilisateurManager utilisateurManager;

    @BeforeEach
    public void setup() {
        // Créer les mocks
        utilisateurRepository = mock(UtilisateurRepository.class);
        passwordUtil = mock(PasswordUtil.class);

        // Créer le manager et injecter les mocks
        utilisateurManager = new UtilisateurManager();
        utilisateurManager.setUtilisateurRepository(utilisateurRepository);
        utilisateurManager.setPasswordUtil(passwordUtil);
    }

    @Test
    public void testCreerUtilisateur_Succes() {
        // Données
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("test@email.com");
        utilisateur.setMdp("plainPassword");

        // Comportement des mocks
        when(utilisateurRepository.existsByEmail("test@email.com")).thenReturn(false);
        when(passwordUtil.hash("plainPassword")).thenReturn("hashedPassword");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Appel de la méthode
        Utilisateur saved = utilisateurManager.creerUtilisateur(utilisateur);

        // Vérifications
        assertNotNull(saved);
        assertEquals("hashedPassword", saved.getMdp());
        verify(utilisateurRepository, times(1)).save(saved);
    }

    @Test
    public void testCreerUtilisateur_EmailExiste() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("existing@email.com");

        when(utilisateurRepository.existsByEmail("existing@email.com")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                utilisateurManager.creerUtilisateur(utilisateur)
        );

        assertEquals("Un utilisateur avec cet email existe déjà", exception.getMessage());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    public void testAuthentifier_Succes() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("user1");
        utilisateur.setMdp("hashedPassword");

        when(utilisateurRepository.findByLogin("user1")).thenReturn(Optional.of(utilisateur));
        when(passwordUtil.verify("plainPassword", "hashedPassword")).thenReturn(true);

        Optional<Utilisateur> result = utilisateurManager.authentifier("user1", "plainPassword");

        assertTrue(result.isPresent());
        assertEquals(utilisateur, result.get());
    }

    @Test
    public void testAuthentifier_Echec() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("user1");
        utilisateur.setMdp("hashedPassword");

        when(utilisateurRepository.findByLogin("user1")).thenReturn(Optional.of(utilisateur));
        when(passwordUtil.verify("wrongPassword", "hashedPassword")).thenReturn(false);

        Optional<Utilisateur> result = utilisateurManager.authentifier("user1", "wrongPassword");

        assertTrue(result.isEmpty());
    }
}
