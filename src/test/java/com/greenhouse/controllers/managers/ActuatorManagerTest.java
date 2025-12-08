package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.entities.Actuator;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuatorManagerTest {

    @Mock
    private ActuatorRepository actuatorRepository;

    @Mock
    private MqttClient mqttClient;

    @InjectMocks
    private ActuatorManager actuatorManager;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked MQTT client via reflection to bypass @PostConstruct
        java.lang.reflect.Field mqttClientField = ActuatorManager.class.getDeclaredField("mqttClient");
        mqttClientField.setAccessible(true);
        mqttClientField.set(actuatorManager, mqttClient);

        // Inject the actuator topic to avoid null topic
        java.lang.reflect.Field topicField = ActuatorManager.class.getDeclaredField("actuatorTopic");
        topicField.setAccessible(true);
        topicField.set(actuatorManager, "iot/control");
    }

    @Test
    void controlActuator_TurnOnFan_Success() throws Exception {
        // Arrange
        String actuatorId = "fan1";
        String command = "ON";
        Double value = 100.0;

        Actuator existingActuator = new Actuator();
        existingActuator.setType("ventilation");

        when(actuatorRepository.findByActuatorId(actuatorId)).thenReturn(Optional.of(existingActuator));

        // Act
        actuatorManager.controlActuator(actuatorId, command, value);

        // Assert - Verify actuator was saved
        ArgumentCaptor<Actuator> actuatorCaptor = ArgumentCaptor.forClass(Actuator.class);
        verify(actuatorRepository).save(actuatorCaptor.capture());

        Actuator savedActuator = actuatorCaptor.getValue();
        assertEquals(actuatorId, savedActuator.getActuatorId());
        assertEquals("ON", savedActuator.getState());
        assertEquals("ON", savedActuator.getLastCommand());
        assertEquals(value, savedActuator.getValue());
        assertEquals("ventilation", savedActuator.getType());

        // Assert - Verify MQTT message was published with correct format
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("iot/control"), messageCaptor.capture());

        String publishedPayload = new String(messageCaptor.getValue().getPayload());
        assertEquals("{\"Fan1\":1}", publishedPayload);
    }

    @Test
    void controlActuator_TurnOffBulb_Success() throws Exception {
        // Arrange
        String actuatorId = "bulb1";
        String command = "OFF";

        when(actuatorRepository.findByActuatorId(actuatorId)).thenReturn(Optional.empty());

        // Act
        actuatorManager.controlActuator(actuatorId, command, null);

        // Assert - Verify actuator state
        ArgumentCaptor<Actuator> actuatorCaptor = ArgumentCaptor.forClass(Actuator.class);
        verify(actuatorRepository).save(actuatorCaptor.capture());

        Actuator savedActuator = actuatorCaptor.getValue();
        assertEquals("OFF", savedActuator.getState());
        assertEquals(0.0, savedActuator.getValue());
        assertEquals("unknown", savedActuator.getType()); // No previous record

        // Assert - Verify MQTT payload
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("iot/control"), messageCaptor.capture());

        String publishedPayload = new String(messageCaptor.getValue().getPayload());
        assertEquals("{\"Bulb1\":0}", publishedPayload);
    }

    @Test
    void controlActuator_MqttException_ThrowsRuntimeException() throws Exception {
        // Arrange
        String actuatorId = "pump";
        String command = "ON";

        when(actuatorRepository.findByActuatorId(actuatorId)).thenReturn(Optional.empty());
        doThrow(new org.eclipse.paho.client.mqttv3.MqttException(0))
                .when(mqttClient).publish(eq("iot/control"), any(MqttMessage.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> actuatorManager.controlActuator(actuatorId, command, null));

        // Verify the actuator was saved before the exception
        verify(actuatorRepository).save(any(Actuator.class));
    }
}
