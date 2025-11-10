package com.greenhouse.test;

import com.greenhouse.entities.Capteur;
import com.greenhouse.entities.Actionneur;
import com.greenhouse.entities.SystemeSerre;
import com.greenhouse.controllers.managers.SystemeSerreManager;
import com.greenhouse.controllers.repositories.SystemeSerreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemeSerreTest {

    private SystemeSerreManager manager;

    @Mock
    private SystemeSerreRepository repository;

    private SystemeSerre systeme;

    @BeforeEach
    public void setUp() {
        manager = new SystemeSerreManager();
        manager.setRepository(repository); // Injection du mock

        systeme = new SystemeSerre("SYS_001", "Serre Principale", "serre1", "pass");
    }

    @Test
    public void testCreerSystemeSerre_Success() {
        when(repository.existsByNom("Serre Principale")).thenReturn(false);
        when(repository.save(systeme)).thenReturn(systeme);

        SystemeSerre saved = manager.creerSystemeSerre(systeme);

        assertNotNull(saved);
        assertEquals("Serre Principale", saved.getNom());
        verify(repository, times(1)).save(systeme);
    }

    @Test
    public void testCreerSystemeSerre_Duplicate() {
        when(repository.existsByNom("Serre Principale")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.creerSystemeSerre(systeme);
        });

        assertEquals("Un système de serre avec ce nom existe déjà", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    public void testTrouverParId() {
        when(repository.findById("SYS_001")).thenReturn(Optional.of(systeme));

        Optional<SystemeSerre> result = manager.trouverParId("SYS_001");

        assertTrue(result.isPresent());
        assertEquals("Serre Principale", result.get().getNom());
        verify(repository, times(1)).findById("SYS_001");
    }

    @Test
    public void testTrouverParNom() {
        when(repository.findByNom("Serre Principale")).thenReturn(Optional.of(systeme));

        Optional<SystemeSerre> result = manager.trouverParNom("Serre Principale");

        assertTrue(result.isPresent());
        assertEquals("SYS_001", result.get().getId());
        verify(repository, times(1)).findByNom("Serre Principale");
    }

    @Test
    public void testEnregistrerCapteur() {
        Capteur c = mock(Capteur.class);
        when(c.getIdEquipement()).thenReturn("CAP_01");
        when(repository.findById("SYS_001")).thenReturn(Optional.of(systeme));

        manager.enregistrerCapteur("SYS_001", c);

        assertTrue(systeme.getCaptures().containsKey("CAP_01"));
        verify(repository, times(1)).save(systeme);
    }

    @Test
    public void testEnregistrerActionneur() {
        Actionneur a = mock(Actionneur.class);
        when(a.getIdEquipement()).thenReturn("ACT_01");
        when(repository.findById("SYS_001")).thenReturn(Optional.of(systeme));

        manager.enregistrerActionneur("SYS_001", a);

        assertTrue(systeme.getActionneurs().containsKey("ACT_01"));
        verify(repository, times(1)).save(systeme);
    }

    @Test
    public void testRegulerConditions() {
        when(repository.findById("SYS_001")).thenReturn(Optional.of(systeme));

        manager.regulerConditions("SYS_001");

        // Pas d'assertions, juste vérifier que save n'a pas été appelé
        verify(repository, never()).save(any());
    }

    @Test
    public void testSupprimerSystemeSerre() {
        manager.supprimerSystemeSerre("SYS_001");
        verify(repository, times(1)).deleteById("SYS_001");
    }
}
