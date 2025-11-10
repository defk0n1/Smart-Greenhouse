package com.greenhouse.test;

import com.greenhouse.entities.Ventilateur;
import com.greenhouse.controllers.managers.VentilateurManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VentilateurTest {

    @Mock
    private VentilateurManager ventilateurManager;

    private Ventilateur vent1;
    private Ventilateur vent2;

    @BeforeEach
    public void setUp() {
        vent1 = new Ventilateur(1, true, 50);
        vent2 = new Ventilateur(2, false, 30);
    }

    @Test
    public void testCreerVentilateur() {
        when(ventilateurManager.enregistrerVentilateur(vent1)).thenReturn(vent1);

        Ventilateur saved = ventilateurManager.enregistrerVentilateur(vent1);

        assertNotNull(saved);
        assertTrue(saved.isRefroidir());
        assertEquals(50, saved.getVitesse());

        verify(ventilateurManager, times(1)).enregistrerVentilateur(vent1);
    }

    @Test
    public void testAugmenterRalentirVitesse() {
        // Simuler augmentation
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            int v = invocation.getArgument(1);
            if (id.equals(vent2.getIdEquipement())) {
                vent2.augmenterVitesse(v);
            }
            return null;
        }).when(ventilateurManager).augmenterVitesse(anyString(), anyInt());

        // Simuler ralentissement
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            int v = invocation.getArgument(1);
            if (id.equals(vent2.getIdEquipement())) {
                vent2.ralentirVitesse(v);
            }
            return null;
        }).when(ventilateurManager).ralentirVitesse(anyString(), anyInt());

        // Simuler findById
        when(ventilateurManager.trouverParId(vent2.getIdEquipement()))
                .thenReturn(Optional.of(vent2));

        // Augmenter vitesse
        ventilateurManager.augmenterVitesse(vent2.getIdEquipement(), 20);
        var result1 = ventilateurManager.trouverParId(vent2.getIdEquipement());
        assertTrue(result1.isPresent());
        assertEquals(50, result1.get().getVitesse());

        // Ralentir vitesse
        ventilateurManager.ralentirVitesse(vent2.getIdEquipement(), 10);
        var result2 = ventilateurManager.trouverParId(vent2.getIdEquipement());
        assertTrue(result2.isPresent());
        assertEquals(40, result2.get().getVitesse());

        verify(ventilateurManager, times(1)).augmenterVitesse(vent2.getIdEquipement(), 20);
        verify(ventilateurManager, times(1)).ralentirVitesse(vent2.getIdEquipement(), 10);
        verify(ventilateurManager, times(2)).trouverParId(vent2.getIdEquipement());
    }

    @Test
    public void testActiverDesactiverVentilateur() {
        // Simuler activation
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(vent1.getIdEquipement())) {
                vent1.activer();
            }
            return null;
        }).when(ventilateurManager).activerVentilateur(anyString());

        // Simuler désactivation
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(vent1.getIdEquipement())) {
                vent1.desactiver();
            }
            return null;
        }).when(ventilateurManager).desactiverVentilateur(anyString());

        when(ventilateurManager.trouverParId(vent1.getIdEquipement()))
                .thenReturn(Optional.of(vent1));

        // Activer
        ventilateurManager.activerVentilateur(vent1.getIdEquipement());
        var result1 = ventilateurManager.trouverParId(vent1.getIdEquipement());
        assertTrue(result1.isPresent());
        assertEquals("ACTIF", result1.get().getStatut());

        // Désactiver
        ventilateurManager.desactiverVentilateur(vent1.getIdEquipement());
        var result2 = ventilateurManager.trouverParId(vent1.getIdEquipement());
        assertTrue(result2.isPresent());
        assertEquals("INACTIF", result2.get().getStatut());

        verify(ventilateurManager, times(1)).activerVentilateur(vent1.getIdEquipement());
        verify(ventilateurManager, times(1)).desactiverVentilateur(vent1.getIdEquipement());
        verify(ventilateurManager, times(2)).trouverParId(vent1.getIdEquipement());
    }
}
