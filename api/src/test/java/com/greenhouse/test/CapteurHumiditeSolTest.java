package com.greenhouse.test;

import com.greenhouse.entities.CapteurHumiditeSol;
import com.greenhouse.controllers.managers.CapteurHumiditeSolManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurHumiditeSolTest {

    @Mock
    private CapteurHumiditeSolManager capteurHumiditeSolManager;

    private CapteurHumiditeSol capteur1;
    private CapteurHumiditeSol capteur2;
    private CapteurHumiditeSol capteur3;

    @BeforeEach
    public void setUp() {
        capteur1 = new CapteurHumiditeSol("HUM_SOL_001", "Zone A", 45.0);
        capteur2 = new CapteurHumiditeSol("HUM_SOL_002", "Zone B", 25.0);
        capteur3 = new CapteurHumiditeSol("HUM_SOL_003", "Zone C", 50.0);
    }

    @Test
    public void testCreerCapteurHumiditeSol() {
        // Simuler l'enregistrement
        when(capteurHumiditeSolManager.enregistrerCapteur(capteur1)).thenReturn(capteur1);

        CapteurHumiditeSol saved = capteurHumiditeSolManager.enregistrerCapteur(capteur1);

        assertNotNull(saved);
        assertEquals("Zone A", saved.getLocalisation());
        assertEquals(45.0, saved.getHumiditeSol(), 0.01);
        assertFalse(saved.isArroser());

        verify(capteurHumiditeSolManager, times(1)).enregistrerCapteur(capteur1);
    }

    @Test
    public void testCapteurNecessitantArrosage() {
        // Simuler le retour d'une liste de capteurs nécessitant arrosage
        when(capteurHumiditeSolManager.trouverCapteursNecessitantArrosage())
                .thenReturn(Arrays.asList(capteur2));

        List<CapteurHumiditeSol> result = capteurHumiditeSolManager.trouverCapteursNecessitantArrosage();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> c.getIdEquipement().equals("HUM_SOL_002")));

        verify(capteurHumiditeSolManager, times(1)).trouverCapteursNecessitantArrosage();
    }

    @Test
    public void testMettreAJourHumidite() {
        // Simuler la mise à jour de l'humidité
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            double humidite = invocation.getArgument(1);
            if (id.equals(capteur3.getIdEquipement())) {
                capteur3.setHumiditeSol(humidite);
                capteur3.setArroser(humidite < 30.0);
            }
            return null;
        }).when(capteurHumiditeSolManager).mettreAJourHumidite(anyString(), anyDouble());

        // Simuler trouver par ID
        when(capteurHumiditeSolManager.trouverParId("HUM_SOL_003")).thenReturn(Optional.of(capteur3));

        capteurHumiditeSolManager.mettreAJourHumidite("HUM_SOL_003", 20.0);

        Optional<CapteurHumiditeSol> result = capteurHumiditeSolManager.trouverParId("HUM_SOL_003");
        assertTrue(result.isPresent());
        assertEquals(20.0, result.get().getHumiditeSol(), 0.01);
        assertTrue(result.get().isArroser());

        verify(capteurHumiditeSolManager, times(1)).mettreAJourHumidite("HUM_SOL_003", 20.0);
        verify(capteurHumiditeSolManager, times(1)).trouverParId("HUM_SOL_003");
    }
}
