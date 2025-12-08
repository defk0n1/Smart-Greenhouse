package com.greenhouse.controllers.managers;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttManagerTest {

    @Mock
    private MqttClient mqttClient;

    @InjectMocks
    private MqttManager mqttManager;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked MQTT client via reflection
        java.lang.reflect.Field mqttClientField = MqttManager.class.getDeclaredField("client");
        mqttClientField.setAccessible(true);
        mqttClientField.set(mqttManager, mqttClient);
    }

    @Test
    void sendMessage_ValidMessage_PublishesSuccessfully() throws Exception {
        // Arrange
        String topic = "test/topic";
        String message = "{\"sensor\":\"temp\",\"value\":25}";
        when(mqttClient.isConnected()).thenReturn(true);

        // Act
        mqttManager.sendMessage(topic, message);

        // Assert
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq(topic), messageCaptor.capture());

        String publishedMessage = new String(messageCaptor.getValue().getPayload());
        assertEquals(message, publishedMessage);
    }

    @Test
    void sendMessage_ClientNotConnected_DoesNotThrow() throws Exception {
        // Arrange
        when(mqttClient.isConnected()).thenReturn(false);

        // Act & Assert (sendMessage returns void and doesn't throw, just logs error)
        mqttManager.sendMessage("test/topic", "message");
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    @Test
    void sendMessage_MqttException_PropagatesException() throws Exception {
        // Arrange
        when(mqttClient.isConnected()).thenReturn(true);
        doThrow(new MqttException(0)).when(mqttClient).publish(anyString(), any(MqttMessage.class));

        // Act & Assert
        assertThrows(Exception.class, () -> mqttManager.sendMessage("test/topic", "message"));
    }
}
