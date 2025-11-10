package com.greenhouse.test;

import com.greenhouse.entities.CapteurLuminosite;
import com.greenhouse.controllers.managers.CapteurLuminositeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurLuminositeTest {

    @Mock
    private CapteurLuminositeManager capteurLuminositeManager;

    private CapteurLuminosite capteur1;
    private CapteurLuminosite capteur2;

    @BeforeEach
    public void setUp() {
        capteur1 = new CapteurLuminosite("LUM_001", "Zone Nord", 5000.0);
        capteur2 = new CapteurLuminosite("LUM_002", "Zone Sud", 3000.0);
    }

    @Test
    public void testCreerCapteurLuminosite() {
        // Simuler l'enregistrement
        when(capteurLuminositeManager.enregistrerCapteur(capteur1)).thenReturn(capteur1);

        CapteurLuminosite saved = capteurLuminositeManager.enregistrerCapteur(capteur1);

        assertNotNull(saved);
        assertEquals("Zone Nord", saved.getLocalisation());
        assertEquals(5000.0, saved.getLuminosite(), 0.01);
        assertEquals("lux", saved.getUnite());

        verify(capteurLuminositeManager, times(1)).enregistrerCapteur(capteur1);
    }

    @Test
    public void testMettreAJourLuminosite() {
        // Simuler la mise à jour de la luminosité
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double luminosite = invocation.getArgument(1);
            if (id.equals(capteur2.getIdEquipement())) {
                capteur2.setLuminosite(luminosite);
            }
            return null;
        }).when(capteurLuminositeManager).mettreAJourLuminosite(anyString(), anyDouble());

        // Simuler trouver par ID
        when(capteurLuminositeManager.trouverParId("LUM_002")).thenReturn(Optional.of(capteur2));

        capteurLuminositeManager.mettreAJourLuminosite("LUM_002", 6000.0);

        Optional<CapteurLuminosite> result = capteurLuminositeManager.trouverParId("LUM_002");
        assertTrue(result.isPresent());
        assertEquals(6000.0, result.get().getLuminosite(), 0.01);

        verify(capteurLuminositeManager, times(1)).mettreAJourLuminosite("LUM_002", 6000.0);
        verify(capteurLuminositeManager, times(1)).trouverParId("LUM_002");
    }
}
