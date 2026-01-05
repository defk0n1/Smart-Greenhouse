package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.controllers.repositories.GreenhouseRepository;
import com.greenhouse.entities.Actuator;
import com.greenhouse.entities.Greenhouse;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActuatorManagerTest {

    @Mock
    private ActuatorRepository actuatorRepository;

    @Mock
    private GreenhouseRepository greenhouseRepository;

    @Mock
    private GreenhouseManager greenhouseManager;

    @Mock
    private Instance<MqttManager> mqttManagerInstance;

    @Mock
    private MqttManager mqttManager;

    @InjectMocks
    private ActuatorManager actuatorManager;

    @BeforeEach
    void setUp() {
        when(mqttManagerInstance.get()).thenReturn(mqttManager);
    }

    @Test
    void testControlActuator_WithGreenhouse() throws Exception {
        String id = "fan1";
        String command = "ON";
        Double value = 1.0;
        String ghId = "gh-1";
        String topic = "GH1";

        Greenhouse gh = new Greenhouse();
        gh.setMqttTopic(topic);

        when(greenhouseRepository.findById(ghId)).thenReturn(Optional.of(gh));

        actuatorManager.controlActuator(id, command, value, ghId);

        verify(actuatorRepository, times(1)).save(any(Actuator.class));
        verify(mqttManager, times(1)).sendMessage(contains(topic), contains("Fan1"));
    }

    @Test
    void testControlActuator_NoGreenhouseId() throws Exception {
        String id = "fan1";
        String command = "ON";
        Double value = 1.0;

        actuatorManager.controlActuator(id, command, value, null);

        verify(actuatorRepository, times(1)).save(any(Actuator.class));
        verify(mqttManager, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void testProcessActuatorData() throws Exception {
        String actuatorId = "act-1";
        String type = "unknown";
        String state = "ON";
        double value = 1.0;
        String lastCommand = "ON";
        String ghId = "gh-1";

        actuatorManager.processActuatorData(actuatorId, type, state, value, lastCommand, ghId);

        verify(actuatorRepository, times(1)).save(any(Actuator.class));
        verify(greenhouseManager, times(1)).attachActuator(ghId, actuatorId);
    }
}
