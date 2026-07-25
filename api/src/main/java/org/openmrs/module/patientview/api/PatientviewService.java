package org.openmrs.module.patientview.api;

import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service interface for neurosurgery patient view functionality
 * This service provides methods for retrieving and managing neurosurgical patient data
 */
@Transactional
public interface PatientviewService extends OpenmrsService {
    
    /**
     * Get the most recent neurological assessments for a patient
     * @param patient the patient
     * @param limit maximum number of assessments to return
     * @return list of recent assessments
     */
    List<Map<String, Object>> getRecentNeuroAssessments(Patient patient, int limit);
    
    /**
     * Get the latest neurological assessment for a patient
     * @param patient the patient
     * @return the latest assessment data
     */
    Map<String, Object> getLatestNeuroAssessment(Patient patient);
    
    /**
     * Save a new neurological assessment
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
    
    /**
     * Get detailed surgical history for a patient
     * @param patient the patient
     * @return detailed list of surgical procedures
     */
    List<Map<String, Object>> getDetailedSurgicalHistory(Patient patient);
    
    /**
     * Get current neurological medications for a patient
     * @param patient the patient
     * @return list of current neurological medications
     */
    List<Map<String, Object>> getCurrentNeurologicalMedications(Patient patient);
    
    /**
     * Get upcoming appointments for a patient
     * @param patient the patient
     * @param daysAhead number of days to look ahead
     * @return list of upcoming appointments
     */
    List<Map<String, Object>> getUpcomingAppointments(Patient patient, int daysAhead);
    
    /**
     * Get recent imaging studies for a patient
     * @param patient the patient
     * @param daysBack number of days to look back
     * @return list of recent imaging studies
     */
    List<Map<String, Object>> getRecentImaging(Patient patient, int daysBack);
    
    /**
     * Get the latest Glasgow Coma Scale assessment
     * @param patient the patient
     * @return latest GCS data
     */
    Map<String, Object> getLatestGCS(Patient patient);
    
    /**
     * Get primary neurological diagnosis for a patient
     * @param patient the patient
     * @return primary diagnosis string
     */
    String getPrimaryNeurologicalDiagnosis(Patient patient);
    
    /**
     * Get active alerts for a patient
     * @param patient the patient
     * @return list of active alerts
     */
    List<String> getActiveAlerts(Patient patient);
    
    /**
     * Get Glasgow Coma Scale options for forms
     * @return map of GCS options
     */
    Map<String, String> getGlasgowComaScaleOptions();
    
    /**
     * Get motor function assessment options
     * @return map of motor function options
     */
    Map<String, String> getMotorFunctionOptions();
    
    /**
     * Get pupil response options
     * @return map of pupil response options
     */
    Map<String, String> getPupilResponseOptions();
}