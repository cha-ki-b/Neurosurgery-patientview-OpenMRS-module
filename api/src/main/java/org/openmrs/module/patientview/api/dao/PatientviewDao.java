package org.openmrs.module.patientview.api.dao;

import org.openmrs.Patient;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object interface for Patientview module
 * This interface defines database operations for neurosurgical patient data
 */
public interface PatientviewDao {
    
    /**
     * Get neurological assessments for a patient
     * @param patient the patient
     * @param limit maximum number to return
     * @return list of assessments
     */
    List<Map<String, Object>> getNeuroAssessments(Patient patient, int limit);
    
    /**
     * Save a neurological assessment
     * @param patient the patient
     * @param assessmentData the assessment data
     */
    void saveNeuroAssessment(Patient patient, Map<String, Object> assessmentData);
    
    /**
     * Get surgical history for a patient
     * @param patient the patient
     * @return list of surgical procedures
     */
    List<Map<String, Object>> getSurgicalHistory(Patient patient);
}