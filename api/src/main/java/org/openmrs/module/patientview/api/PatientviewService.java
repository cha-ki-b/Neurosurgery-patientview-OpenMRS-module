package org.openmrs.module.patientview.api;

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service interface for neurosurgery patient view functionality
 * This service provides methods for retrieving and managing neurosurgical patient data
 * <p>
 * Every method that reads or writes patient data is annotated with {@code @Authorized},
 * which OpenMRS enforces automatically (via AuthorizationAdvice) for any call made through
 * {@code Context.getService(PatientviewService.class)} - callers without the required
 * privilege get an {@code APIAuthenticationException} before any DAO code runs.
 */
@Transactional
public interface PatientviewService extends OpenmrsService {

    /**
     * Get the most recent neurological assessments for a patient
     * @param patient the patient
     * @param limit maximum number of assessments to return
     * @return list of recent assessments
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getRecentNeuroAssessments(Patient patient, int limit);

    /**
     * Get the latest neurological assessment for a patient
     * @param patient the patient
     * @return the latest assessment data
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    Map<String, Object> getLatestNeuroAssessment(Patient patient);

    /**
     * Save a new neurological assessment
     * @param patient the patient
     * @param assessmentData the assessment data
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveNeuroAssessment(Patient patient, Map<String, Object> assessmentData);

    /**
     * Get surgical history for a patient
     * @param patient the patient
     * @return list of surgical procedures
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getSurgicalHistory(Patient patient);

    /**
     * Get detailed surgical history for a patient
     * @param patient the patient
     * @return detailed list of surgical procedures
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getDetailedSurgicalHistory(Patient patient);

    /**
     * Get the latest Glasgow Coma Scale assessment
     * @param patient the patient
     * @return latest GCS data
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    Map<String, Object> getLatestGCS(Patient patient);

    /**
     * Get primary neurological diagnosis for a patient
     * @param patient the patient
     * @return primary diagnosis string
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    String getPrimaryNeurologicalDiagnosis(Patient patient);

    /**
     * Get active alerts for a patient
     * @param patient the patient
     * @return list of active alerts
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<String> getActiveAlerts(Patient patient);

    /**
     * Get Glasgow Coma Scale options for forms (static reference data, not patient data)
     * @return map of GCS options
     */
    Map<String, String> getGlasgowComaScaleOptions();

    /**
     * Get motor function assessment options (static reference data, not patient data)
     * @return map of motor function options
     */
    Map<String, String> getMotorFunctionOptions();

    /**
     * Get pupil response options (static reference data, not patient data)
     * @return map of pupil response options
     */
    Map<String, String> getPupilResponseOptions();

    /**
     * Save a new surgical history entry (Antecedents chirurgicaux)
     * @param patient the patient
     * @param data procedureName, datePerformed, surgeon, location, outcome, notes
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveSurgicalHistory(Patient patient, Map<String, Object> data);

    /**
     * Get the most recent medical history (Antecedents medicaux) for a patient.
     * Medical history is append-only (see {@link #saveMedicalHistory}) so this
     * returns the latest recorded version, not a mutable single record.
     * @param patient the patient
     * @return the medical history as a map, or an empty map if none recorded yet
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    Map<String, Object> getMedicalHistory(Patient patient);

    /**
     * Record a new version of the patient's medical history. This always inserts
     * a new row rather than overwriting the previous one, so the full history of
     * who-changed-what-and-when is preserved (non-repudiation) - the DAO never
     * updates or deletes an existing medical history record.
     * @param patient the patient
     * @param data the medical history fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveMedicalHistory(Patient patient, Map<String, Object> data);

    /**
     * Get admission context entries (Motif d'hospitalisation) for a patient, most recent first
     * @param patient the patient
     * @return list of admission context entries
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getAdmissionContexts(Patient patient);

    /**
     * Save a new admission context entry
     * @param patient the patient
     * @param data the admission context fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveAdmissionContext(Patient patient, Map<String, Object> data);

    /**
     * Get vital signs history for a patient, most recent first
     * @param patient the patient
     * @param limit maximum number to return
     * @return list of vital signs records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getVitalSigns(Patient patient, int limit);

    /**
     * Save a new vital signs entry
     * @param patient the patient
     * @param data the vital signs fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveVitalSigns(Patient patient, Map<String, Object> data);

    /**
     * Get detailed neurological exam history for a patient, most recent first
     * @param patient the patient
     * @param limit maximum number to return
     * @return list of detailed neuro exam records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getNeuroExamDetails(Patient patient, int limit);

    /**
     * Save a new detailed neurological exam entry
     * @param patient the patient
     * @param data the neuro exam fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveNeuroExamDetail(Patient patient, Map<String, Object> data);

    /**
     * Get neurosurgical diagnosis entries for a patient, most recent first (Fiche section 8)
     * @param patient the patient
     * @return list of diagnosis records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getNeurosurgicalDiagnoses(Patient patient);

    /**
     * Save a new neurosurgical diagnosis entry
     * @param patient the patient
     * @param data the diagnosis fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveNeurosurgicalDiagnosis(Patient patient, Map<String, Object> data);

    /**
     * Get anatomopathology reports for a patient, most recent first (Fiche section 10)
     * @param patient the patient
     * @return list of pathology records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getPathologyReports(Patient patient);

    /**
     * Save a new anatomopathology report
     * @param patient the patient
     * @param data the pathology fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void savePathologyReport(Patient patient, Map<String, Object> data);
}
