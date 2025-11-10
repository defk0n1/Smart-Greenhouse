package com.greenhouse.test;

import com.greenhouse.entities.LampeLED;
import com.greenhouse.controllers.managers.LampeLEDManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LampeLEDTest {

    @Mock
    private LampeLEDManager lampeLEDManager;

    private LampeLED lampe1;

    @BeforeEach
    public void setUp() {
        lampe1 = new LampeLED(1, 50, "Blanc");
    }

    @Test
    public void testCreerLampe() {
        when(lampeLEDManager.enregistrerLampe(lampe1)).thenReturn(lampe1);

        LampeLED saved = lampeLEDManager.enregistrerLampe(lampe1);

        assertNotNull(saved);
        assertEquals(50, saved.getIntensite());
        assertEquals("Blanc", saved.getCouleur());

        verify(lampeLEDManager, times(1)).enregistrerLampe(lampe1);
    }

    @Test
    public void testAugmenterReduireIntensite() {
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            int intensite = invocation.getArgument(1);
            if (id.equals(lampe1.getIdEquipement())) {
                lampe1.augmenterIntensite(intensite);
            }
            return null;
        }).when(lampeLEDManager).augmenterIntensite(anyString(), anyInt());

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            int intensite = invocation.getArgument(1);
            if (id.equals(lampe1.getIdEquipement())) {
                lampe1.reduireIntensite(intensite);
            }
            return null;
        }).when(lampeLEDManager).reduireIntensite(anyString(), anyInt());

        when(lampeLEDManager.trouverParId(lampe1.getIdEquipement()))
                .thenReturn(Optional.of(lampe1));

        lampeLEDManager.augmenterIntensite(lampe1.getIdEquipement(), 20);
        var result1 = lampeLEDManager.trouverParId(lampe1.getIdEquipement());
        assertEquals(70, result1.get().getIntensite());

        lampeLEDManager.reduireIntensite(lampe1.getIdEquipement(), 30);
        var result2 = lampeLEDManager.trouverParId(lampe1.getIdEquipement());
        assertEquals(40, result2.get().getIntensite());

        verify(lampeLEDManager, times(1)).augmenterIntensite(lampe1.getIdEquipement(), 20);
        verify(lampeLEDManager, times(1)).reduireIntensite(lampe1.getIdEquipement(), 30);
        verify(lampeLEDManager, times(2)).trouverParId(lampe1.getIdEquipement());
    }

    @Test
    public void testActiverDesactiverLampe() {
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(lampe1.getIdEquipement())) lampe1.activer();
            return null;
        }).when(lampeLEDManager).activerLampe(anyString());

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.equals(lampe1.getIdEquipement())) lampe1.desactiver();
            return null;
        }).when(lampeLEDManager).desactiverLampe(anyString());

        when(lampeLEDManager.trouverParId(lampe1.getIdEquipement()))
                .thenReturn(Optional.of(lampe1));

        lampeLEDManager.activerLampe(lampe1.getIdEquipement());
        assertEquals("ACTIF", lampeLEDManager.trouverParId(lampe1.getIdEquipement()).get().getStatut());

        lampeLEDManager.desactiverLampe(lampe1.getIdEquipement());
        assertEquals("INACTIF", lampeLEDManager.trouverParId(lampe1.getIdEquipement()).get().getStatut());
    }
}
