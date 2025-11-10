package com.greenhouse.test;

import com.greenhouse.entities.CapteurNiveauEau;
import com.greenhouse.controllers.managers.CapteurNiveauEauManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurNiveauEauTest {

    @Mock
    private CapteurNiveauEauManager capteurNiveauEauManager;

    private CapteurNiveauEau capteur1;
    private CapteurNiveauEau capteur2;

    @BeforeEach
    public void setUp() {
        capteur1 = new CapteurNiveauEau("EAU_001", 1000.0, 750.0);
        capteur2 = new CapteurNiveauEau("EAU_002", 500.0, 400.0);
    }

    @Test
    public void testCreerCapteurNiveauEau() {
        when(capteurNiveauEauManager.enregistrerCapteur(capteur1)).thenReturn(capteur1);

        CapteurNiveauEau saved = capteurNiveauEauManager.enregistrerCapteur(capteur1);

        assertNotNull(saved);
        assertEquals(1000.0, saved.getCapaciteMax(), 0.01);
        assertEquals(750.0, saved.getNiveauEau(), 0.01);
        assertEquals("L", saved.getUnite());

        verify(capteurNiveauEauManager, times(1)).enregistrerCapteur(capteur1);
    }

    @Test
    public void testMettreAJourNiveauEau() {
        // Simuler la mise à jour du niveau d’eau
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double niveau = invocation.getArgument(1);
            if (id.equals(capteur2.getIdEquipement())) {
                capteur2.setNiveauEau(niveau);
            }
            return null;
        }).when(capteurNiveauEauManager).mettreAJourNiveauEau(anyString(), anyDouble());

        // Simuler trouver par ID
        when(capteurNiveauEauManager.trouverParId("EAU_002")).thenReturn(Optional.of(capteur2));

        capteurNiveauEauManager.mettreAJourNiveauEau("EAU_002", 300.0);

        Optional<CapteurNiveauEau> result = capteurNiveauEauManager.trouverParId("EAU_002");
        assertTrue(result.isPresent());
        assertEquals(300.0, result.get().getNiveauEau(), 0.01);

        verify(capteurNiveauEauManager, times(1)).mettreAJourNiveauEau("EAU_002", 300.0);
        verify(capteurNiveauEauManager, times(1)).trouverParId("EAU_002");
    }
}
