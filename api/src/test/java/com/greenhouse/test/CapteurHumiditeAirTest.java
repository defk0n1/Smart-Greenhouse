package com.greenhouse.test;

import com.greenhouse.entities.CapteurHumiditeAir;
import com.greenhouse.controllers.managers.CapteurHumiditeAirManager;
import com.greenhouse.controllers.repositories.CapteurHumiditeAirRepository;
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
public class CapteurHumiditeAirTest {

    @Mock
    private CapteurHumiditeAirRepository capteurHumiditeAirRepository;

    @InjectMocks
    private CapteurHumiditeAirManager capteurHumiditeAirManager;

    private CapteurHumiditeAir capteurHumiditeAir;

    @BeforeEach
    public void setUp() {
        capteurHumiditeAir = new CapteurHumiditeAir("HUM_AIR_001", "Zone Nord", 65.5);
    }

    @Test
    public void testCreerCapteurHumiditeAir() {
        // Given
        when(capteurHumiditeAirRepository.save(any(CapteurHumiditeAir.class))).thenReturn(capteurHumiditeAir);

        // When
        CapteurHumiditeAir saved = capteurHumiditeAirManager.enregistrerCapteur(capteurHumiditeAir);

        // Then
        assertNotNull(saved);
        assertEquals("HUM_AIR_001", saved.getIdEquipement());
        assertEquals("Zone Nord", saved.getLocalisation());
        assertEquals(65.5, saved.getHumiditeAir(), 0.01);
        assertEquals("%", saved.getUnite());
        verify(capteurHumiditeAirRepository, times(1)).save(capteurHumiditeAir);
    }

    @Test
    public void testMettreAJourHumidite() {
        // Given
        when(capteurHumiditeAirRepository.findById("HUM_AIR_001")).thenReturn(Optional.of(capteurHumiditeAir));
        when(capteurHumiditeAirRepository.save(any(CapteurHumiditeAir.class))).thenReturn(capteurHumiditeAir);

        // When
        capteurHumiditeAirManager.mettreAJourHumidite("HUM_AIR_001", 70.5);

        // Then
        assertEquals(70.5, capteurHumiditeAir.getHumiditeAir(), 0.01);
        assertEquals(70.5, capteurHumiditeAir.getValeurConcrete(), 0.01);
        verify(capteurHumiditeAirRepository, times(1)).findById("HUM_AIR_001");
        verify(capteurHumiditeAirRepository, times(1)).save(capteurHumiditeAir);
    }

    @Test
    public void testTrouverParId() {
        // Given
        when(capteurHumiditeAirRepository.findById("HUM_AIR_001")).thenReturn(Optional.of(capteurHumiditeAir));

        // When
        Optional<CapteurHumiditeAir> found = capteurHumiditeAirManager.trouverParId("HUM_AIR_001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("HUM_AIR_001", found.get().getIdEquipement());
        assertEquals("Zone Nord", found.get().getLocalisation());
        assertEquals(65.5, found.get().getHumiditeAir(), 0.01);
        verify(capteurHumiditeAirRepository, times(1)).findById("HUM_AIR_001");
    }

    @Test
    public void testTrouverParLocalisation() {
        // Given
        CapteurHumiditeAir capteur2 = new CapteurHumiditeAir("HUM_AIR_002", "Zone Est", 58.0);
        List<CapteurHumiditeAir> capteurs = Arrays.asList(capteurHumiditeAir, capteur2);
        when(capteurHumiditeAirRepository.findByLocalisation("Zone Est")).thenReturn(capteurs);

        // When
        List<CapteurHumiditeAir> found = capteurHumiditeAirManager.trouverParLocalisation("Zone Est");

        // Then
        assertNotNull(found);
        assertEquals(2, found.size());
        assertEquals("Zone Est", found.get(0).getLocalisation());
        assertEquals("Zone Est", found.get(1).getLocalisation());
        verify(capteurHumiditeAirRepository, times(1)).findByLocalisation("Zone Est");
    }

    @Test
    public void testTrouverParPlageHumidite() {
        // Given
        List<CapteurHumiditeAir> capteurs = Arrays.asList(capteurHumiditeAir);
        when(capteurHumiditeAirRepository.findByHumiditeAirBetween(60.0, 70.0)).thenReturn(capteurs);

        // When
        List<CapteurHumiditeAir> found = capteurHumiditeAirManager.trouverParPlageHumidite(60.0, 70.0);

        // Then
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(65.5, found.get(0).getHumiditeAir(), 0.01);
        verify(capteurHumiditeAirRepository, times(1)).findByHumiditeAirBetween(60.0, 70.0);
    }

    @Test
    public void testSupprimerCapteurHumiditeAir() {
        // Given
        doNothing().when(capteurHumiditeAirRepository).deleteById("HUM_AIR_001");

        // When
        capteurHumiditeAirManager.supprimerCapteur("HUM_AIR_001");

        // Then
        verify(capteurHumiditeAirRepository, times(1)).deleteById("HUM_AIR_001");
    }

    @Test
    public void testGettersAndSetters() {
        // Given
        CapteurHumiditeAir capteur = new CapteurHumiditeAir("HUM_AIR_002", "Zone Sud", 60.0);

        // When
        capteur.setLocalisation("Zone Ouest");
        capteur.setHumiditeAir(75.0);
        capteur.setUnite("g/m3");
        capteur.setEtat("INACTIF");

        // Then
        assertEquals("Zone Ouest", capteur.getLocalisation());
        assertEquals(75.0, capteur.getHumiditeAir(), 0.01);
        assertEquals("g/m3", capteur.getUnite());
        assertEquals("INACTIF", capteur.getEtat());
        assertEquals(75.0, capteur.getValeurConcrete(), 0.01);
    }
}