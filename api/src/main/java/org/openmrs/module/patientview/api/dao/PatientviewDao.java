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

    /**
     * Save a new surgical history entry (Antecedents chirurgicaux)
     * @param patient the patient
     * @param data procedureName, datePerformed, surgeon, location, outcome, notes
     */
    void saveSurgicalHistory(Patient patient, Map<String, Object> data);

    /**
     * Get the current medical history (Antecedents medicaux) for a patient
     * @param patient the patient
     * @return the medical history as a map, or an empty map if none recorded yet
     */
    Map<String, Object> getMedicalHistory(Patient patient);

    /**
     * Create or update the medical history for a patient (one record per patient)
     * @param patient the patient
     * @param data the medical history fields
     */
    void saveMedicalHistory(Patient patient, Map<String, Object> data);

    /**
     * Get admission context entries (Motif d'hospitalisation) for a patient, most recent first
     * @param patient the patient
     * @return list of admission context entries
     */
    List<Map<String, Object>> getAdmissionContexts(Patient patient);

    /**
     * Save a new admission context entry
     * @param patient the patient
     * @param data the admission context fields
     */
    void saveAdmissionContext(Patient patient, Map<String, Object> data);

    /**
     * Get vital signs history for a patient, most recent first
     * @param patient the patient
     * @param limit maximum number to return
     * @return list of vital signs records
     */
    List<Map<String, Object>> getVitalSigns(Patient patient, int limit);

    /**
     * Save a new vital signs entry
     * @param patient the patient
     * @param data the vital signs fields
     */
    void saveVitalSigns(Patient patient, Map<String, Object> data);

    /**
     * Get detailed neurological exam history for a patient, most recent first
     * @param patient the patient
     * @param limit maximum number to return
     * @return list of detailed neuro exam records
     */
    List<Map<String, Object>> getNeuroExamDetails(Patient patient, int limit);

    /**
     * Save a new detailed neurological exam entry
     * @param patient the patient
     * @param data the neuro exam fields
     */
    void saveNeuroExamDetail(Patient patient, Map<String, Object> data);

    /**
     * Get neurosurgical diagnosis entries for a patient, most recent first
     * @param patient the patient
     * @return list of diagnosis records
     */
    List<Map<String, Object>> getNeurosurgicalDiagnoses(Patient patient);

    /**
     * Save a new neurosurgical diagnosis entry
     * @param patient the patient
     * @param data the diagnosis fields
     */
    void saveNeurosurgicalDiagnosis(Patient patient, Map<String, Object> data);

    /**
     * Get anatomopathology reports for a patient, most recent first
     * @param patient the patient
     * @return list of pathology records
     */
    List<Map<String, Object>> getPathologyReports(Patient patient);

    /**
     * Save a new anatomopathology report
     * @param patient the patient
     * @param data the pathology fields
     */
    void savePathologyReport(Patient patient, Map<String, Object> data);

    /**
     * Get medical treatment entries (Fiche section 9 - traitement medical) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getMedicalTreatments(Patient patient);

    /**
     * Save a new medical treatment entry
     * @param patient the patient
     * @param data the record's fields
     */
    void saveMedicalTreatment(Patient patient, Map<String, Object> data);

    /**
     * Get surgical treatment entries (Fiche section 9 - traitement chirurgical) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getSurgicalTreatments(Patient patient);

    /**
     * Save a new surgical treatment entry
     * @param patient the patient
     * @param data the record's fields
     */
    void saveSurgicalTreatment(Patient patient, Map<String, Object> data);

    /**
     * Get post-operative evolution entries (Fiche section 11) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getPostopEvolutions(Patient patient);

    /**
     * Save a new post-operative evolution entry
     * @param patient the patient
     * @param data the record's fields
     */
    void savePostopEvolution(Patient patient, Map<String, Object> data);

    /**
     * Get sequelae assessments (Fiche section 12) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getSequelae(Patient patient);

    /**
     * Save a new sequelae assessment
     * @param patient the patient
     * @param data the record's fields
     */
    void saveSequelae(Patient patient, Map<String, Object> data);

    /**
     * Get laboratory results (Fiche section 7) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getLabResults(Patient patient);

    /**
     * Save a new laboratory result
     * @param patient the patient
     * @param data the record's fields
     */
    void saveLabResult(Patient patient, Map<String, Object> data);

    /**
     * Get discharge episodes (Fiche section 13) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getDischarges(Patient patient);

    /**
     * Save a new discharge episode
     * @param patient the patient
     * @param data the record's fields
     */
    void saveDischarge(Patient patient, Map<String, Object> data);

    /**
     * Get follow-up consultations (Fiche section 14) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getFollowUps(Patient patient);

    /**
     * Save a new follow-up consultation
     * @param patient the patient
     * @param data the record's fields
     */
    void saveFollowUp(Patient patient, Map<String, Object> data);

    /**
     * Get imaging notes / comptes rendus (Fiche section 6) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    List<Map<String, Object>> getImagingNotes(Patient patient);

    /**
     * Save a new imaging note
     * @param patient the patient
     * @param data the record's fields
     */
    void saveImagingNote(Patient patient, Map<String, Object> data);
}