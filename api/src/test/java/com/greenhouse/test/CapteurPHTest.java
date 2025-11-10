package com.greenhouse.test;

import com.greenhouse.entities.CapteurPH;
import com.greenhouse.controllers.managers.CapteurPHManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurPHTest {

    @Mock
    private CapteurPHManager capteurPHManager;

    private CapteurPH capteur1;
    private CapteurPH capteur2;

    @BeforeEach
    public void setUp() {
        capteur1 = new CapteurPH("PH_001", 6.5);
        capteur2 = new CapteurPH("PH_002", 7.0);
    }

    @Test
    public void testCreerCapteurPH() {
        when(capteurPHManager.enregistrerCapteur(capteur1)).thenReturn(capteur1);

        CapteurPH saved = capteurPHManager.enregistrerCapteur(capteur1);

        assertNotNull(saved);
        assertEquals(6.5, saved.getPh(), 0.01);
        assertEquals("pH", saved.getUnite());

        verify(capteurPHManager, times(1)).enregistrerCapteur(capteur1);
    }

    @Test
    public void testMettreAJourPH() {
        // Simuler la mise à jour du pH
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double ph = invocation.getArgument(1);
            if (id.equals(capteur2.getIdEquipement())) {
                capteur2.setPh(ph);
            }
            return null;
        }).when(capteurPHManager).mettreAJourPH(anyString(), anyDouble());

        // Simuler trouver par ID
        when(capteurPHManager.trouverParId("PH_002")).thenReturn(Optional.of(capteur2));

        capteurPHManager.mettreAJourPH("PH_002", 6.8);

        Optional<CapteurPH> result = capteurPHManager.trouverParId("PH_002");
        assertTrue(result.isPresent());
        assertEquals(6.8, result.get().getPh(), 0.01);

        verify(capteurPHManager, times(1)).mettreAJourPH("PH_002", 6.8);
        verify(capteurPHManager, times(1)).trouverParId("PH_002");
    }
}
