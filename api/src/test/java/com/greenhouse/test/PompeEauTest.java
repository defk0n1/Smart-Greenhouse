package com.greenhouse.test;

import com.greenhouse.entities.PompeEau;
import com.greenhouse.controllers.managers.PompeEauManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PompeEauTest {

    @Mock
    private PompeEauManager pompeEauManager;

    private PompeEau pompe1;
    private PompeEau pompe2;
    private PompeEau pompe3;

    @BeforeEach
    public void setUp() {
        pompe1 = new PompeEau(1, 50.0, 10);
        pompe2 = new PompeEau(2, 75.0, 15);
        pompe3 = new PompeEau(3, 30.0, 5);
    }

    @Test
    public void testCreerPompeEau() {
        when(pompeEauManager.enregistrerPompe(pompe1)).thenReturn(pompe1);

        PompeEau saved = pompeEauManager.enregistrerPompe(pompe1);

        assertNotNull(saved);
        assertEquals(50.0, saved.getQuantite(), 0.01);
        assertEquals(10, saved.getTempsFonctionnement());
        assertEquals("POMPE_EAU", saved.getType());

        verify(pompeEauManager, times(1)).enregistrerPompe(pompe1);
    }

    @Test
    public void testActiverDesactiverPompe() {
        // Activation
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(pompe2.getIdEquipement())) {
                pompe2.setStatut("ACTIF");
            }
            return null;
        }).when(pompeEauManager).activerPompe(anyString());

        // Désactivation
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(pompe2.getIdEquipement())) {
                pompe2.setStatut("INACTIF");
            }
            return null;
        }).when(pompeEauManager).desactiverPompe(anyString());

        when(pompeEauManager.trouverParId(pompe2.getIdEquipement()))
                .thenReturn(Optional.of(pompe2));

        // Activer
        pompeEauManager.activerPompe(pompe2.getIdEquipement());
        var result1 = pompeEauManager.trouverParId(pompe2.getIdEquipement());
        assertTrue(result1.isPresent());
        assertEquals("ACTIF", result1.get().getStatut());

        // Désactiver
        pompeEauManager.desactiverPompe(pompe2.getIdEquipement());
        var result2 = pompeEauManager.trouverParId(pompe2.getIdEquipement());
        assertTrue(result2.isPresent());
        assertEquals("INACTIF", result2.get().getStatut());

        verify(pompeEauManager, times(1)).activerPompe(pompe2.getIdEquipement());
        verify(pompeEauManager, times(1)).desactiverPompe(pompe2.getIdEquipement());
        verify(pompeEauManager, times(2)).trouverParId(pompe2.getIdEquipement());
    }

    @Test
    public void testConfigurerQuantite() {
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double quantite = invocation.getArgument(1);
            if (id.equals(pompe3.getIdEquipement())) {
                pompe3.setQuantite(quantite);
            }
            return null;
        }).when(pompeEauManager).configurerQuantite(anyString(), anyDouble());

        when(pompeEauManager.trouverParId(pompe3.getIdEquipement()))
                .thenReturn(Optional.of(pompe3));

        pompeEauManager.configurerQuantite(pompe3.getIdEquipement(), 60.0);
        var result = pompeEauManager.trouverParId(pompe3.getIdEquipement());
        assertTrue(result.isPresent());
        assertEquals(60.0, result.get().getQuantite(), 0.01);

        verify(pompeEauManager, times(1)).configurerQuantite(pompe3.getIdEquipement(), 60.0);
        verify(pompeEauManager, times(1)).trouverParId(pompe3.getIdEquipement());
    }
}
