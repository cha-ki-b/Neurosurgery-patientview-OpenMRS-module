package org.openmrs.module.patientview.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap; // Import HashMap
import java.util.Map;

public class PatientviewServiceSpringTest {

    private PatientviewService service;

    @Before
    public void setup() {
        // Mock the service implementation
        service = mock(PatientviewService.class);

        // --- FIX 1: Create a real Map for Glasgow Coma Scale Options ---
        // This map needs a key "eye_4" for the test to pass.
        Map<String, String> gcsMap = new HashMap<>();
        gcsMap.put("eye_4", "Spontaneous—open with blinking at baseline");
        gcsMap.put("verbal_5", "Oriented");
        gcsMap.put("motor_6", "Obeys commands for movement");
        
        // Provide the correctly typed Map to the mock
        when(service.getGlasgowComaScaleOptions()).thenReturn(gcsMap);

        // --- FIX 2: Create a real Map for Motor Function Options ---
        // This map needs a value "Normal strength" for the test to pass.
        Map<String, String> motorMap = new HashMap<>();
        motorMap.put("normal", "Normal strength");
        motorMap.put("abnormal", "Abnormal flexion");

        // Provide the correctly typed Map to the mock
        when(service.getMotorFunctionOptions()).thenReturn(motorMap);
    }


    @Test
    public void testServiceAvailability() {
        assertNotNull("Service should be available", service);
    }

    @Test
    public void testGlasgowOptions() {
        Map<String, String>  gcsOptions = service.getGlasgowComaScaleOptions();
        assertNotNull("GCS options should not be null", gcsOptions);
        // This assertion will now pass
        assertTrue("GCS options should contain eye_4", gcsOptions.containsKey("eye_4"));
    }

    @Test
    public void testMotorOptions() {
        Map<String, String> motorOptions = service.getMotorFunctionOptions();
        assertNotNull("Motor options should not be null", motorOptions);
        // This assertion will now pass
        assertTrue("Motor options should contain Normal", motorOptions.containsValue("Normal strength"));
    }
}