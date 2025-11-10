package com.greenhouse.test;

import com.greenhouse.entities.Administrateur;
import com.greenhouse.controllers.managers.AdministrateurManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdministrateurTest {

    @Mock
    private AdministrateurManager administrateurManager;

    private Administrateur admin1;
    private Administrateur admin2;
    private Administrateur admin3;

    @BeforeEach
    public void setUp() {
        // Préparer les objets Administrateur
        admin1 = new Administrateur(
                "1", "Admin", "Super", "admin@email.com",
                "ADMIN", "ADMIN_001", Arrays.asList("GESTION_SYSTEME", "GESTION_UTILISATEURS")
        );

        admin2 = new Administrateur(
                "2", "Admin2", "Test", "admin2@email.com",
                "ADMIN", "ADMIN_002", Arrays.asList("GESTION_SYSTEME")
        );

        admin3 = new Administrateur(
                "3", "Admin3", "System", "admin3@email.com",
                "ADMIN", "ADMIN_003", Arrays.asList("GESTION_SYSTEME")
        );
    }

    @Test
    public void testCreerAdministrateur() {
        // Simuler le comportement du manager
        when(administrateurManager.creerAdministrateur(admin1)).thenReturn(admin1);

        // Appel de la méthode
        Administrateur saved = administrateurManager.creerAdministrateur(admin1);

        // Vérifications
        assertNotNull(saved);
        assertEquals("admin@email.com", saved.getEmail());
        assertEquals(2, saved.getPermissions().size());

        // Vérifier que la méthode a été appelée exactement une fois
        verify(administrateurManager, times(1)).creerAdministrateur(admin1);
    }

    @Test
    public void testTrouverAdministrateurParId() {
        // Simuler le comportement du manager
        when(administrateurManager.trouverParId("2")).thenReturn(Optional.of(admin2));

        // Appel de la méthode
        var result = administrateurManager.trouverParId("2");

        // Vérifications
        assertTrue(result.isPresent());
        assertEquals("Admin2", result.get().getNom());

        verify(administrateurManager, times(1)).trouverParId("2");
    }

    @Test
    public void testGererSysteme() {
        // Comme ces méthodes ne retournent rien, on peut juste vérifier qu'elles sont appelées
        doNothing().when(administrateurManager).creerAdministrateur(any(Administrateur.class));

        // Vérifier que les méthodes de l'objet admin3 ne jettent pas d'exception
        assertDoesNotThrow(() -> admin3.gererSysteme());
        assertDoesNotThrow(() -> admin3.gererAttributs());
        assertDoesNotThrow(() -> admin3.raccourcirDelais());
    }
}
