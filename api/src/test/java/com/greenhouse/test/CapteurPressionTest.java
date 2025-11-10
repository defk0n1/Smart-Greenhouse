package com.greenhouse.test;

import com.greenhouse.entities.CapteurPression;
import com.greenhouse.controllers.managers.CapteurPressionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurPressionTest {

    @Mock
    private CapteurPressionManager capteurPressionManager;

    private CapteurPression capteur1;
    private CapteurPression capteur2;

    @BeforeEach
    public void setUp() {
        capteur1 = new CapteurPression("PRESS_001", 1013.25);
        capteur2 = new CapteurPression("PRESS_002", 1010.0);
    }

    @Test
    public void testCreerCapteurPression() {
        when(capteurPressionManager.enregistrerCapteur(capteur1)).thenReturn(capteur1);

        CapteurPression saved = capteurPressionManager.enregistrerCapteur(capteur1);

        assertNotNull(saved);
        assertEquals(1013.25, saved.getPression(), 0.01);
        assertEquals("hPa", saved.getUnite());

        verify(capteurPressionManager, times(1)).enregistrerCapteur(capteur1);
    }

    @Test
    public void testMettreAJourPression() {
        // Simuler la mise à jour de la pression
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double pression = invocation.getArgument(1);
            if (id.equals(capteur2.getIdEquipement())) {
                capteur2.setPression(pression);
            }
            return null;
        }).when(capteurPressionManager).mettreAJourPression(anyString(), anyDouble());

        // Simuler trouver par ID
        when(capteurPressionManager.trouverParId("PRESS_002")).thenReturn(Optional.of(capteur2));

        capteurPressionManager.mettreAJourPression("PRESS_002", 1015.0);

        Optional<CapteurPression> result = capteurPressionManager.trouverParId("PRESS_002");
        assertTrue(result.isPresent());
        assertEquals(1015.0, result.get().getPression(), 0.01);

        verify(capteurPressionManager, times(1)).mettreAJourPression("PRESS_002", 1015.0);
        verify(capteurPressionManager, times(1)).trouverParId("PRESS_002");
    }
}
