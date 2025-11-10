package com.greenhouse.test;

import com.greenhouse.entities.CapteurCO2;
import com.greenhouse.controllers.managers.CapteurCO2Manager;
import com.greenhouse.controllers.repositories.CapteurCO2Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapteurCO2Test {

    @Mock
    private CapteurCO2Repository capteurCO2Repository;

    @InjectMocks
    private CapteurCO2Manager capteurCO2Manager;

    private CapteurCO2 capteurCO2;

    @BeforeEach
    public void setUp() {
        capteurCO2 = new CapteurCO2("CO2_001", 400.0);
    }

    @Test
    public void testCreerCapteurCO2() {
        // Given
        when(capteurCO2Repository.save(any(CapteurCO2.class))).thenReturn(capteurCO2);

        // When
        CapteurCO2 saved = capteurCO2Manager.enregistrerCapteur(capteurCO2);

        // Then
        assertNotNull(saved);
        assertEquals("CO2_001", saved.getIdEquipement());
        assertEquals(400.0, saved.getCo2(), 0.01);
        assertEquals("ppm", saved.getUnite());
        verify(capteurCO2Repository, times(1)).save(capteurCO2);
    }

    @Test
    public void testMettreAJourCO2() {
        // Given
        when(capteurCO2Repository.findById("CO2_001")).thenReturn(Optional.of(capteurCO2));

        // When
        capteurCO2Manager.mettreAJourCO2("CO2_001", 450.0);

        // Then
        assertEquals(450.0, capteurCO2.getCo2(), 0.01);
        verify(capteurCO2Repository, times(1)).findById("CO2_001");
        verify(capteurCO2Repository, times(1)).save(capteurCO2);
    }

    @Test
    public void testTrouverParId() {
        // Given
        when(capteurCO2Repository.findById("CO2_001")).thenReturn(Optional.of(capteurCO2));

        // When
        Optional<CapteurCO2> found = capteurCO2Manager.trouverParId("CO2_001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("CO2_001", found.get().getIdEquipement());
        assertEquals(400.0, found.get().getCo2(), 0.01);
        verify(capteurCO2Repository, times(1)).findById("CO2_001");
    }

    @Test
    public void testTrouverParPlageCO2() {
        // Given
        List<CapteurCO2> capteurs = Arrays.asList(capteurCO2);
        when(capteurCO2Repository.findByCo2Between(300.0, 500.0)).thenReturn(capteurs);

        // When
        List<CapteurCO2> found = capteurCO2Manager.trouverParPlageCO2(300.0, 500.0);

        // Then
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(400.0, found.get(0).getCo2(), 0.01);
        verify(capteurCO2Repository, times(1)).findByCo2Between(300.0, 500.0);
    }

    @Test
    public void testSupprimerCapteurCO2() {
        // Given
        doNothing().when(capteurCO2Repository).deleteById("CO2_001");

        // When
        capteurCO2Manager.supprimerCapteur("CO2_001");

        // Then
        verify(capteurCO2Repository, times(1)).deleteById("CO2_001");
    }

    @Test
    public void testGettersAndSetters() {
        // Given
        CapteurCO2 capteur = new CapteurCO2("CO2_002", 350.0);

        // When
        capteur.setCo2(380.0);
        capteur.setUnite("ppb");
        capteur.setEtat("ACTIF");

        // Then
        assertEquals(380.0, capteur.getCo2(), 0.01);
        assertEquals("ppb", capteur.getUnite());
        assertEquals("ACTIF", capteur.getEtat());
    }
}