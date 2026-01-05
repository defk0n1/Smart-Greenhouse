package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.ActuatorRepository;
import com.greenhouse.controllers.repositories.GreenhouseRepository;
import com.greenhouse.controllers.repositories.SensorRepository;
import com.greenhouse.entities.Actuator;
import com.greenhouse.entities.Greenhouse;
import com.greenhouse.entities.Sensor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreenhouseManagerTest {

    @Mock
    private GreenhouseRepository greenhouseRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private ActuatorRepository actuatorRepository;

    @Mock
    private MqttManager mqttManager;

    @InjectMocks
    private GreenhouseManager greenhouseManager;

    @Test
    void testFindOrCreateByName_Existing() {
        String name = "GH1";
        Greenhouse existing = new Greenhouse();
        existing.setName(name);
        existing.setId("existing-id");

        when(greenhouseRepository.findByName(name)).thenReturn(Optional.of(existing));

        Greenhouse result = greenhouseManager.findOrCreateByName(name);

        assertEquals(existing, result);
        verify(greenhouseRepository, times(1)).findByName(name);
        verify(greenhouseRepository, never()).save(any());
    }

    @Test
    void testFindOrCreateByName_New() {
        String name = "GH2";
        when(greenhouseRepository.findByName(name)).thenReturn(Optional.empty());
        when(greenhouseRepository.save(any(Greenhouse.class))).thenAnswer(invocation -> {
            Greenhouse g = invocation.getArgument(0);
            g.setId("new-id");
            return g;
        });

        Greenhouse result = greenhouseManager.findOrCreateByName(name);

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("marwen", result.getOwnerId());
        verify(greenhouseRepository, times(1)).save(any(Greenhouse.class));
    }

    @Test
    void testAttachSensor_Success() throws Exception {
        String ghId = "gh-1";
        String sensorId = "sensor-1";
        Greenhouse gh = new Greenhouse();
        gh.setId(ghId);
        Sensor sensor = new Sensor();
        sensor.setSensorId(sensorId);
        sensor.setType("temp");
        sensor.setStatus("ACTIVE");

        when(greenhouseRepository.findById(ghId)).thenReturn(Optional.of(gh));
        when(sensorRepository.findAll()).thenReturn(Stream.of(sensor));
        
        greenhouseManager.attachSensor(ghId, sensorId);

        verify(greenhouseRepository, times(1)).update(gh);
        verify(sensorRepository, times(1)).update(sensor);
        assertTrue(gh.getSensors().contains(sensorId));
        assertEquals(ghId, sensor.getGreenhouseId());
    }

    @Test
    void testAttachSensor_AlreadyAttached() throws Exception {
        String ghId = "gh-1";
        String sensorId = "sensor-1";
        Greenhouse gh = new Greenhouse();
        gh.setId(ghId);
        gh.attachSensor(sensorId, "temp", true);

        when(greenhouseRepository.findById(ghId)).thenReturn(Optional.of(gh));

        greenhouseManager.attachSensor(ghId, sensorId);

        verify(sensorRepository, never()).findAll();
        verify(greenhouseRepository, never()).update(gh);
    }
}
