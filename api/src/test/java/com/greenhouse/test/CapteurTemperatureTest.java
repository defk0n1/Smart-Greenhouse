package com.greenhouse.test;

import com.greenhouse.entities.CapteurTemperature;
import com.greenhouse.controllers.managers.CapteurTemperatureManager;
import com.greenhouse.controllers.repositories.CapteurTemperatureRepository;
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
public class CapteurTemperatureTest {

    @Mock
    private CapteurTemperatureRepository capteurTemperatureRepository;

    @InjectMocks
    private CapteurTemperatureManager capteurTemperatureManager;

    private CapteurTemperature capteurTemperature;

    @BeforeEach
    public void setUp() {
        capteurTemperature = new CapteurTemperature("CAPT_001", "Zone A", 25.5);
    }

    @Test
    public void testCreationCapteurTemperature() {
        // Given
        String id = "CAPT_001";
        String localisation = "Zone A";
        double temperature = 25.5;

        // When
        CapteurTemperature capteur = new CapteurTemperature(id, localisation, temperature);

        // Then
        assertNotNull(capteur);
        assertEquals(id, capteur.getIdEquipement());
        assertEquals(localisation, capteur.getLocalisation());
        assertEquals(temperature, capteur.getTemperature(), 0.01);
    }

    @Test
    public void testGettersAndSetters() {
        // Given
        CapteurTemperature capteur = new CapteurTemperature("CAPT_001", "Zone A", 25.5);

        // When
        capteur.setLocalisation("Zone B");
        capteur.setTemperature(28.0);

        // Then
        assertEquals("Zone B", capteur.getLocalisation());
        assertEquals(28.0, capteur.getTemperature(), 0.01);
    }

    @Test
    public void testHeritage() {
        // Given
        CapteurTemperature capteur = new CapteurTemperature("CAPT_001", "Zone A", 25.5);

        // Then
        assertNotNull(capteur.getUnite());
        assertNotNull(capteur.getValeurConcrete());
        assertNotNull(capteur.getEtat());
    }

    @Test
    public void testEnregistrerCapteur() {
        // Given
        when(capteurTemperatureRepository.save(any(CapteurTemperature.class))).thenReturn(capteurTemperature);

        // When
        CapteurTemperature saved = capteurTemperatureManager.enregistrerCapteur(capteurTemperature);

        // Then
        assertNotNull(saved);
        assertEquals("CAPT_001", saved.getIdEquipement());
        assertEquals("Zone A", saved.getLocalisation());
        assertEquals(25.5, saved.getTemperature(), 0.01);
        verify(capteurTemperatureRepository, times(1)).save(capteurTemperature);
    }

    @Test
    public void testTrouverParId() {
        // Given
        when(capteurTemperatureRepository.findById("CAPT_001")).thenReturn(Optional.of(capteurTemperature));

        // When
        Optional<CapteurTemperature> found = capteurTemperatureManager.trouverParId("CAPT_001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("CAPT_001", found.get().getIdEquipement());
        assertEquals("Zone A", found.get().getLocalisation());
        verify(capteurTemperatureRepository, times(1)).findById("CAPT_001");
    }

    @Test
    public void testTrouverParLocalisation() {
        // Given
        List<CapteurTemperature> capteurs = Arrays.asList(capteurTemperature);
        when(capteurTemperatureRepository.findByLocalisation("Zone A")).thenReturn(capteurs);

        // When
        List<CapteurTemperature> found = capteurTemperatureManager.trouverParLocalisation("Zone A");

        // Then
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals("Zone A", found.get(0).getLocalisation());
        verify(capteurTemperatureRepository, times(1)).findByLocalisation("Zone A");
    }

    @Test
    public void testMettreAJourTemperature() {
        // Given
        when(capteurTemperatureRepository.findById("CAPT_001")).thenReturn(Optional.of(capteurTemperature));

        // When
        capteurTemperatureManager.mettreAJourTemperature("CAPT_001", 30.0);

        // Then
        assertEquals(30.0, capteurTemperature.getTemperature(), 0.01);
        verify(capteurTemperatureRepository, times(1)).findById("CAPT_001");
        verify(capteurTemperatureRepository, times(1)).save(capteurTemperature);
    }

    @Test
    public void testSupprimerCapteur() {
        // Given
        doNothing().when(capteurTemperatureRepository).deleteById("CAPT_001");

        // When
        capteurTemperatureManager.supprimerCapteur("CAPT_001");

        // Then
        verify(capteurTemperatureRepository, times(1)).deleteById("CAPT_001");
    }
}