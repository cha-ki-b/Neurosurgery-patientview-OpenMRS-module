package org.openmrs.module.patientview.api.dao;

import org.openmrs.Patient;
import org.openmrs.module.patientview.api.model.FhirProjection;
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
     * Every recorded version of the patient's medical history, newest first. The table is
     * append-only, so this is the full history rather than only the current state - which is what
     * the core-model projection needs, since it exports each version as its own dated encounter.
     * @param patient the patient
     * @return every version, newest first; empty when none is recorded
     */
    List<Map<String, Object>> getMedicalHistoryVersions(Patient patient);

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
    /**
     * Source-row uuid to the mapping fingerprint it was last projected with, for this patient and
     * set. A uuid absent from the map has never been projected; one whose fingerprint differs from
     * the current mapping is stale and must be projected again.
     * @param patient the patient
     * @param sourceSet manifest set id, e.g. "patientview.vitalSigns"
     * @return uuid to fingerprint; the fingerprint is null for rows written before 1.4.5
     */
    Map<String, String> getProjectionFingerprints(Patient patient, String sourceSet);

    /**
     * The ledger entry for one source row, or null if it has never been projected.
     * @param patient the patient
     * @param sourceSet manifest set id
     * @param sourceUuid uuid of the patientview row
     * @return the ledger entry, or null
     */
    FhirProjection getFhirProjection(Patient patient, String sourceSet, String sourceUuid);

    /**
     * Record that one source row has been projected, or update its entry when the row is
     * re-projected after a mapping change. The ledger is infrastructure rather than clinical
     * data, so unlike every other table here it is updated in place - the superseded encounter
     * itself is voided with a reason, which is where that audit trail lives.
     * @param projection the ledger entry
     */
    void saveFhirProjection(FhirProjection projection);

    /**
     * Every patient holding at least one projectable patientview record, for the server-wide
     * backfill of rows created before the projection existed.
     * @return the patients, without duplicates
     */
    List<Patient> getPatientsWithNeurosurgeryRecords();
}
