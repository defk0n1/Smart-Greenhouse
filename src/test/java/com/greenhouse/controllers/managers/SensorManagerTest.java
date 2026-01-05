package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.entities.Sensor;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorManagerTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private GreenhouseManager greenhouseManager;

    @Mock
    private ActuatorManager actuatorManager;

    @Mock
    private Event<SensorManager.SensorAlert> sensorAlertEvent;

    @InjectMocks
    private SensorManager sensorManager;

    @Test
    void testProcessSensorData_Normal() throws Exception {
        String sensorId = "sensor-1";
        String type = "temp";
        double value = 25.0;
        String ghId = "gh-1";

        sensorManager.processSensorData(sensorId, type, value, ghId);

        verify(sensorRepository, times(1)).save(any(Sensor.class));
        verify(greenhouseManager, times(1)).attachSensor(ghId, sensorId);
        verify(sensorAlertEvent, never()).fire(any());
    }

    @Test
    void testProcessSensorData_GasAlert() {
        String sensorId = "gas-1";
        String type = "gas";
        double value = 300.0; // > 200 trigger alert
        String ghId = "gh-1";

        sensorManager.processSensorData(sensorId, type, value, ghId);

        verify(sensorRepository, times(1)).save(any(Sensor.class));
        verify(sensorAlertEvent, times(1)).fire(any(SensorManager.SensorAlert.class));
    }

    @Test
    void testProcessSensorData_SoilMoistureLow() {
        String sensorId = "soil-1";
        String type = "soil";
        double value = 20.0; // < 30 trigger pump
        String ghId = "gh-1";

        sensorManager.processSensorData(sensorId, type, value, ghId);

        verify(actuatorManager, times(1)).controlActuator("pump", "ON", 80.0, ghId);
        verify(sensorAlertEvent, times(1)).fire(any(SensorManager.SensorAlert.class));
    }

    @Test
    void testProcessSensorData_AutoAttachFail() throws Exception {
        String sensorId = "sensor-1";
        String type = "temp";
        double value = 25.0;
        String ghId = "gh-1";

        doThrow(new RuntimeException("Fail")).when(greenhouseManager).attachSensor(ghId, sensorId);

        sensorManager.processSensorData(sensorId, type, value, ghId);

        // Should not throw exception
        verify(sensorRepository, times(1)).save(any(Sensor.class));
    }
}
