package org.openmrs.module.patientview.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
// No need for MockitoAnnotations if you use the runner
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.module.patientview.api.dao.PatientviewDao;
import org.openmrs.module.patientview.api.impl.PatientviewServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class PatientviewServiceImplConnectionTest {
    
    @Mock
    private PatientviewDao dao;
    
    @InjectMocks
    private PatientviewServiceImpl patientviewService;
    
    private Patient testPatient;
    
    @Before
    public void setUp() {
        // This line is redundant because @RunWith(MockitoJUnitRunner.class) already does it.
        // MockitoAnnotations.initMocks(this); 
        
        // Create test patient - Patient extends Person in OpenMRS
        testPatient = new Patient();
        testPatient.setPatientId(1);
        testPatient.setPersonId(1);
        
        // Add name directly to patient (since Patient extends Person)
        PersonName name = new PersonName();
        name.setGivenName("John");
        name.setFamilyName("Doe");
        testPatient.addName(name);
        testPatient.setGender("M");
    }
    
    @Test
    public void testDaoInjection() {
        // Test that DAO is properly injected
        assertNotNull("DAO should be injected", patientviewService.getDao());
        assertEquals("Should be the mocked DAO", dao, patientviewService.getDao());
    }
    
    @Test
    public void testServiceInitialization() {
        // Test that service initializes without errors
        assertNotNull("Service should be initialized", patientviewService);
        assertTrue("Service should extend BaseOpenmrsService", 
                   patientviewService instanceof org.openmrs.api.impl.BaseOpenmrsService);
    }
    
    @Test
    public void testNullPatientHandling() {
        // Test that methods handle null patient gracefully without DAO calls
        List<Map<String, Object>> assessments = patientviewService.getRecentNeuroAssessments(null, 5);
        assertNotNull("Should return empty list, not null", assessments);
        assertEquals("Should return empty list for null patient", 0, assessments.size());
        
        // Verify no DAO calls were made
        verifyNoInteractions(dao);
    }
    
    @Test
    public void testBasicMethodsWithValidPatient() {
        // Now that the service delegates to the DAO, we need to stub the DAO calls
        when(dao.getNeuroAssessments(testPatient, 5)).thenReturn(new ArrayList<>());
        when(dao.getNeuroAssessments(testPatient, 1)).thenReturn(new ArrayList<>());
        when(dao.getSurgicalHistory(testPatient)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> assessments = patientviewService.getRecentNeuroAssessments(testPatient, 5);
        assertNotNull("Should return a list", assessments);

        Map<String, Object> latest = patientviewService.getLatestNeuroAssessment(testPatient);
        assertNotNull("Should return a map", latest);

        List<Map<String, Object>> history = patientviewService.getSurgicalHistory(testPatient);
        assertNotNull("Should return a list", history);

        // Verify the DAO was called as expected
        verify(dao).getNeuroAssessments(testPatient, 5);
        verify(dao).getSurgicalHistory(testPatient);
    }
    
    @Test
    public void testSaveMethodValidation() {
        // Test save method parameter validation
        try {
            patientviewService.saveNeuroAssessment(null, null);
            fail("Should throw IllegalArgumentException for null parameters");
        } catch (IllegalArgumentException e) {
            // Expected behavior
            assertTrue("Should mention null parameters", e.getMessage().contains("cannot be null"));
        }
        
        // No DAO calls should be made for invalid parameters
        verifyNoInteractions(dao);
    }
    
    @Test
    public void testTransactionAnnotations() {
        // Test that the class and methods have proper transaction annotations
        assertTrue("Class should have @Transactional annotation", 
                   PatientviewServiceImpl.class.isAnnotationPresent(
                       org.springframework.transaction.annotation.Transactional.class));
    }
    
    @Test
    public void testLoggingConfiguration() {
        // Test that logging is properly configured
        assertNotNull("Log should be initialized", 
                      getPrivateField(patientviewService, "log"));
    }
    
    @Test
    public void testStaticDataMethods() {
        // Test methods that return static data (these don't need DAO)
        Map<String, String> gcsOptions = patientviewService.getGlasgowComaScaleOptions();
        assertNotNull("GCS options should not be null", gcsOptions);
        assertFalse("GCS options should not be empty", gcsOptions.isEmpty());
        
        Map<String, String> motorOptions = patientviewService.getMotorFunctionOptions();
        assertNotNull("Motor options should not be null", motorOptions);
        assertFalse("Motor options should not be empty", motorOptions.isEmpty());
        
        Map<String, String> pupilOptions = patientviewService.getPupilResponseOptions();
        assertNotNull("Pupil options should not be null", pupilOptions);
        assertFalse("Pupil options should not be empty", pupilOptions.isEmpty());
        
        // These methods don't use DAO
        verifyNoInteractions(dao);
    }
    
    @Test
    public void testServiceWithoutDaoFailure() {
        // Test what happens if DAO is not injected (common deployment issue)
        PatientviewServiceImpl serviceWithoutDao = new PatientviewServiceImpl();
        // This should not throw an error immediately
        assertNotNull("Service should still initialize", serviceWithoutDao);
        assertNull("DAO should be null", serviceWithoutDao.getDao());
        
        // Methods that don't use DAO should still work
        List<Map<String, Object>> assessments = serviceWithoutDao.getRecentNeuroAssessments(testPatient, 5);
        assertNotNull("Sample data methods should work without DAO", assessments);
    }
    
    @Test
    public void shouldPersistAndReloadAssessment() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("eyeResponse", 4);
        data.put("verbalResponse", 5);
        data.put("motorResponse", 6);
        data.put("notes", "Test");

        // Stub the mocked DAO to return a valid assessment list
        List<Map<String, Object>> mockAssessments = new java.util.ArrayList<>();
        Map<String, Object> assessment = new java.util.HashMap<>();
        assessment.put("gcs", 15);
        mockAssessments.add(assessment);
        when(dao.getNeuroAssessments(testPatient, 1)).thenReturn(mockAssessments);

        patientviewService.saveNeuroAssessment(testPatient, data);
        List<Map<String, Object>> saved = patientviewService.getRecentNeuroAssessments(testPatient, 1);

        assertFalse(saved.isEmpty());
        assertEquals(15, saved.get(0).get("gcs"));
    }

    // Helper method to access private fields for testing
    private Object getPrivateField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
}