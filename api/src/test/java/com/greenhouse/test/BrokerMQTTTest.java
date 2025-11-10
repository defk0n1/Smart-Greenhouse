package com.greenhouse.test;

import com.greenhouse.entities.BrokerMQTT;
import com.greenhouse.controllers.managers.BrokerMQTTManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BrokerMQTTTest {

    @Mock
    private BrokerMQTTManager brokerMQTTManager;

    private BrokerMQTT broker1;
    private BrokerMQTT broker2;
    private BrokerMQTT broker3;

    @BeforeEach
    public void setUp() {
        // Préparer les brokers
        Map<String, String> topics1 = new HashMap<>();
        topics1.put("sensors/temperature", "25.5");
        broker1 = new BrokerMQTT("BROKER_001", "localhost", 1883, topics1);

        broker2 = new BrokerMQTT("BROKER_002", "mqtt.example.com", 1883, new HashMap<>());
        broker3 = new BrokerMQTT("BROKER_003", "mqtt.test.com", 1883, new HashMap<>());
    }

    @Test
    public void testCreerBrokerMQTT() {
        // Simuler le comportement du manager
        when(brokerMQTTManager.creerBrokerMQTT(broker1)).thenReturn(broker1);

        // Appel
        BrokerMQTT saved = brokerMQTTManager.creerBrokerMQTT(broker1);

        // Vérifications
        assertNotNull(saved);
        assertEquals("localhost", saved.getAdresse());
        assertEquals(1883, saved.getPort());
        assertFalse(saved.getTopics().isEmpty());

        verify(brokerMQTTManager, times(1)).creerBrokerMQTT(broker1);
    }

    @Test
    public void testPublierMessage() {
        // Simuler le comportement
        when(brokerMQTTManager.trouverParId("BROKER_002")).thenReturn(Optional.of(broker2));

        // Simuler l'ajout d'un topic après publication
        doAnswer(invocation -> {
            String topic = invocation.getArgument(1);
            String message = invocation.getArgument(2);
            broker2.getTopics().put(topic, message);
            return null;
        }).when(brokerMQTTManager).publierMessage(anyString(), anyString(), anyString());

        // Appel
        brokerMQTTManager.publierMessage("BROKER_002", "sensors/humidity", "65.0");

        // Vérifications
        Optional<BrokerMQTT> result = brokerMQTTManager.trouverParId("BROKER_002");
        assertTrue(result.isPresent());
        assertTrue(result.get().getTopics().containsKey("sensors/humidity"));

        verify(brokerMQTTManager, times(1))
                .publierMessage("BROKER_002", "sensors/humidity", "65.0");
    }

    @Test
    public void testSubscrireTopic() {
        // Simuler le comportement
        when(brokerMQTTManager.trouverParId("BROKER_003")).thenReturn(Optional.of(broker3));

        // Simuler l'ajout d'un topic après subscription
        doAnswer(invocation -> {
            String topic = invocation.getArgument(1);
            String value = invocation.getArgument(2);
            broker3.getTopics().put(topic, value);
            return null;
        }).when(brokerMQTTManager).subscrireTopic(anyString(), anyString(), anyString());

        // Appel
        brokerMQTTManager.subscrireTopic("BROKER_003", "sensors/co2", "400");

        // Vérifications
        Optional<BrokerMQTT> result = brokerMQTTManager.trouverParId("BROKER_003");
        assertTrue(result.isPresent());
        assertTrue(result.get().getTopics().containsKey("sensors/co2"));

        verify(brokerMQTTManager, times(1))
                .subscrireTopic("BROKER_003", "sensors/co2", "400");
    }
}
