
package org.openmrs.module.patientview.api;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class QuickDeploymentTest extends BaseModuleContextSensitiveTest {
    
    @Test
    public void testBasicServiceFunctionality() {
        // Get the service
        PatientviewService service = Context.getService(PatientviewService.class);
        assertNotNull("Service should be available", service);
        
        // Test static methods (don't require database)
        Map<String, String> gcsOptions = service.getGlasgowComaScaleOptions();
        assertTrue("Should have GCS options", gcsOptions.size() > 0);
        
        // Test with null patient (should not cause errors)
        List<Map<String, Object>> assessments = service.getRecentNeuroAssessments(null, 5);
        assertNotNull("Should handle null patient", assessments);
        assertEquals("Should return empty list", 0, assessments.size());
    }

}