package com.greenhouse.test;

import com.greenhouse.entities.Actionneur;
import com.greenhouse.entities.Ventilateur;
import com.greenhouse.entities.LampeLED;
import com.greenhouse.controllers.managers.ActionneurManager;
import com.greenhouse.controllers.repositories.ActionneurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActionneurTest {

    private ActionneurManager manager;

    @Mock
    private ActionneurRepository repository;

    @BeforeEach
    public void setUp() {
        manager = new ActionneurManager();
        manager.setRepository(repository); // Injection du mock
    }

    @Test
    public void testEnregistrerVentilateur() {
        Ventilateur ventilateur = new Ventilateur(1, true, 50);
        when(repository.save(ventilateur)).thenReturn(ventilateur);

        Actionneur saved = manager.enregistrerActionneur(ventilateur);

        assertNotNull(saved);
        assertEquals("VENTILATEUR", saved.getType());
        verify(repository, times(1)).save(ventilateur);
    }

    @Test
    public void testEnregistrerLampeLED() {
        LampeLED lampe = new LampeLED(1, 80, "BLANC");
        when(repository.save(lampe)).thenReturn(lampe);

        Actionneur saved = manager.enregistrerActionneur(lampe);

        assertNotNull(saved);
        assertEquals("LAMPE_LED", saved.getType());
        verify(repository, times(1)).save(lampe);
    }

    @Test
    public void testActiverActionneur() {
        Ventilateur ventilateur = new Ventilateur(2, false, 40);
        when(repository.findById(ventilateur.getIdEquipement())).thenReturn(Optional.of(ventilateur));
        when(repository.save(ventilateur)).thenReturn(ventilateur);

        manager.activerActionneur(ventilateur.getIdEquipement());

        assertEquals("ACTIF", ventilateur.getStatut());
        verify(repository, times(1)).save(ventilateur);
    }

    @Test
    public void testDesactiverActionneur() {
        LampeLED lampe = new LampeLED(2, 100, "ROUGE");
        lampe.activer(); // Simuler activation
        when(repository.findById(lampe.getIdEquipement())).thenReturn(Optional.of(lampe));
        when(repository.save(lampe)).thenReturn(lampe);

        manager.desactiverActionneur(lampe.getIdEquipement());

        assertEquals("INACTIF", lampe.getStatut());
        verify(repository, times(1)).save(lampe);
    }
}
