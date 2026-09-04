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

    /**
     * Get medical treatment entries (Fiche section 9 - traitement medical) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getMedicalTreatments(Patient patient);

    /**
     * Save a new medical treatment entry
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveMedicalTreatment(Patient patient, Map<String, Object> data);

    /**
     * Get surgical treatment entries (Fiche section 9 - traitement chirurgical) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getSurgicalTreatments(Patient patient);

    /**
     * Save a new surgical treatment entry
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveSurgicalTreatment(Patient patient, Map<String, Object> data);

    /**
     * Get post-operative evolution entries (Fiche section 11) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getPostopEvolutions(Patient patient);

    /**
     * Save a new post-operative evolution entry
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void savePostopEvolution(Patient patient, Map<String, Object> data);

    /**
     * Get sequelae assessments (Fiche section 12) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getSequelae(Patient patient);

    /**
     * Save a new sequelae assessment
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveSequelae(Patient patient, Map<String, Object> data);

    /**
     * Get laboratory results (Fiche section 7) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getLabResults(Patient patient);

    /**
     * Save a new laboratory result
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveLabResult(Patient patient, Map<String, Object> data);

    /**
     * Get discharge episodes (Fiche section 13) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getDischarges(Patient patient);

    /**
     * Save a new discharge episode
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveDischarge(Patient patient, Map<String, Object> data);

    /**
     * Get follow-up consultations (Fiche section 14) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getFollowUps(Patient patient);

    /**
     * Save a new follow-up consultation
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveFollowUp(Patient patient, Map<String, Object> data);

    /**
     * Get imaging notes / comptes rendus (Fiche section 6) for a patient, most recent first
     * @param patient the patient
     * @return list of records
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getImagingNotes(Patient patient);

    /**
     * Save a new imaging note
     * @param patient the patient
     * @param data the record's fields
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    void saveImagingNote(Patient patient, Map<String, Object> data);

    /**
     * Get the patient's DICOM studies from the companion {@code imaging} module (Fiche section 6).
     * <p>
     * Unlike every other method here this reads no patientview table: the images and their
     * metadata live in Orthanc, reached through the {@code imaging} module, and are deliberately
     * not duplicated locally (see
     * {@link org.openmrs.module.patientview.api.imaging.DicomStudyBridge}). It is exposed on this
     * service anyway so that controllers keep having exactly one entry point, and so this
     * module's own view privilege is enforced before another module's data is surfaced inside a
     * patientview page.
     * @param patient the patient
     * @return one map per study, or empty if the imaging module is not installed or not started
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    List<Map<String, Object>> getImagingStudies(Patient patient);

    /**
     * @return whether the companion {@code imaging} module can be reached at all, so the Imagerie
     *         tab can tell "this patient has no studies" apart from "the PACS link is not
     *         configured here"
     */
    boolean isImagingModuleAvailable();
    /**
     * Projects this patient's neurosurgery records into the core OpenMRS clinical model
     * (Encounter / Obs / Condition), so the FHIR2 module can serve them as Observation,
     * DiagnosticReport and Condition resources.
     * <p>
     * Rows already projected are skipped, so this is safe to call repeatedly - it is the same
     * operation that runs automatically after every save, and the per-patient backfill of
     * records created before 1.4.0.
     * @param patient the patient
     * @return counts of what was written, plus any fields awaiting dictionary curation
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    Map<String, Object> projectToClinicalModel(Patient patient);

    /**
     * Server-wide backfill: projects every patient holding a neurosurgery record. Intended as a
     * one-off after upgrading, or after curating new concepts.
     * @return number of patients processed and any failures
     */
    @Authorized({ PatientviewPrivileges.MANAGE_NEURO_DATA })
    Map<String, Object> projectAllPatients();

    /**
     * Read-only audit of how much of the FHIR mapping manifest is usable on this server: which
     * fields still need a concept, and which declared concepts do not resolve against the
     * dictionary as loaded here. Writes nothing.
     * @return the coverage report
     */
    @Authorized({ PatientviewPrivileges.VIEW_NEURO_DATA })
    Map<String, Object> getFhirCoverageReport();
}
