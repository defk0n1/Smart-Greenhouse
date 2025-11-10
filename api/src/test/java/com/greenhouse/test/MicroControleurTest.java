package com.greenhouse.test;

import com.greenhouse.entities.*;
import com.greenhouse.controllers.managers.MicroControleurManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MicroControleurTest {

    @Mock
    private MicroControleurManager microControleurManager;

    private MicroControleur micro1;
    private MicroControleur micro2;
    private MicroControleur micro3;
    private MicroControleur micro4;

    private CapteurTemperature capteur1;
    private Ventilateur ventilateur1;

    @BeforeEach
    public void setUp() {
        micro1 = new MicroControleur("MICRO_001", "192.168.1.100");
        micro2 = new MicroControleur("MICRO_002", "192.168.1.101");
        micro3 = new MicroControleur("MICRO_003", "192.168.1.102");
        micro4 = new MicroControleur("MICRO_004", "192.168.1.103");

        capteur1 = new CapteurTemperature("CAPT_003", "Zone C", 28.5);
        ventilateur1 = new Ventilateur(1, true, 75);

        micro3.getListeCapteur().add(capteur1);
        micro4.getListeActionneur().add(ventilateur1);
    }

    @Test
    public void testCreerMicroControleur() {
        when(microControleurManager.creerMicroControleur(micro1)).thenReturn(micro1);

        MicroControleur saved = microControleurManager.creerMicroControleur(micro1);

        assertNotNull(saved);
        assertEquals("192.168.1.100", saved.getAdresseIP());
        assertNotNull(saved.getListeCapteur());
        assertNotNull(saved.getListeActionneur());

        verify(microControleurManager, times(1)).creerMicroControleur(micro1);
    }

    @Test
    public void testAjouterCapteur() {
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            CapteurTemperature capteur = invocation.getArgument(1);
            if (id.equals(micro2.getId())) {
                micro2.getListeCapteur().add(capteur);
            }
            return null;
        }).when(microControleurManager).ajouterCapteur(anyString(), any(CapteurTemperature.class));

        when(microControleurManager.trouverParId(micro2.getId())).thenReturn(Optional.of(micro2));

        CapteurTemperature nouveauCapteur = new CapteurTemperature("CAPT_002", "Zone B", 22.0);
        microControleurManager.ajouterCapteur(micro2.getId(), nouveauCapteur);

        var result = microControleurManager.trouverParId(micro2.getId());
        assertTrue(result.isPresent());
        assertFalse(result.get().getListeCapteur().isEmpty());

        verify(microControleurManager, times(1)).ajouterCapteur(micro2.getId(), nouveauCapteur);
        verify(microControleurManager, times(1)).trouverParId(micro2.getId());
    }

    @Test
    public void testLireDonneesCapteur() {
        when(microControleurManager.lireDonneesCapteur(micro3.getId(), capteur1))
                .thenReturn("Capteur CAPT_003 : 28.5°C");

        String donnees = microControleurManager.lireDonneesCapteur(micro3.getId(), capteur1);

        assertNotNull(donnees);
        assertTrue(donnees.contains("CAPT_003"));

        verify(microControleurManager, times(1)).lireDonneesCapteur(micro3.getId(), capteur1);
    }

    @Test
    public void testEnvoyerCommande() {
        when(microControleurManager.envoyerCommande(micro4.getId(), ventilateur1, "ACTIVER"))
                .thenReturn("Commande envoyée à Ventilateur 1 : ACTIVER");

        String resultat = microControleurManager.envoyerCommande(micro4.getId(), ventilateur1, "ACTIVER");

        assertNotNull(resultat);
        assertTrue(resultat.contains("Commande envoyée"));

        verify(microControleurManager, times(1)).envoyerCommande(micro4.getId(), ventilateur1, "ACTIVER");
    }
}
