package org.openmrs.module.patientview.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientview.api.PatientviewService;
import org.openmrs.module.patientview.api.dao.PatientviewDao;
import org.openmrs.module.patientview.api.fhir.ObsProjector;
import org.openmrs.module.patientview.api.imaging.DicomStudyBridge;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of the PatientviewService interface
 * This is the service layer that handles business logic for neurosurgery patient data
 */
@Transactional
public class PatientviewServiceImpl extends BaseOpenmrsService implements PatientviewService {
    
    protected final Log log = LogFactory.getLog(this.getClass());
    
    private PatientviewDao dao;

    private ObsProjector projector;
    
    /**
     * Setter for the DAO - Spring will inject this
     */
    public void setDao(PatientviewDao dao) {
        this.dao = dao;
    }

    /**
     * Setter for the core-model projector - Spring will inject this. Left optional: when it is
     * absent (as in the plain Mockito unit tests) every save behaves exactly as before.
     */
    public void setProjector(ObsProjector projector) {
        this.projector = projector;
    }
    
    /**
     * @return the dao
     */
    public PatientviewDao getDao() {
        return dao;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentNeuroAssessments(Patient patient, int limit) {
        if (patient == null) {
            return new ArrayList<>();
        }
        if (dao == null) {
            return new ArrayList<>();
        }
        return dao.getNeuroAssessments(patient, limit);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLatestNeuroAssessment(Patient patient) {
        if (patient == null) {
            return new HashMap<>();
        }
        
        List<Map<String, Object>> recent = getRecentNeuroAssessments(patient, 1);
        return recent.isEmpty() ? new HashMap<>() : recent.get(0);
    }
    
    @Override
    @Transactional
    public void saveNeuroAssessment(Patient patient, Map<String, Object> assessmentData) {
        if (patient == null || assessmentData == null) {
            throw new IllegalArgumentException("Patient and assessment data cannot be null");
        }
        if (dao != null) {
            dao.saveNeuroAssessment(patient, assessmentData);
            projectQuietly(patient, "patientview.neuroAssessment");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSurgicalHistory(Patient patient) {
        if (patient == null) {
            return new ArrayList<>();
        }
        if (dao == null) {
            return new ArrayList<>();
        }
        return dao.getSurgicalHistory(patient);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDetailedSurgicalHistory(Patient patient) {
        // For now, return the same as basic history
        return getSurgicalHistory(patient);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLatestGCS(Patient patient) {
        Map<String, Object> latest = getLatestNeuroAssessment(patient);
        if (latest == null || latest.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> gcs = new HashMap<>();
        gcs.put("totalScore", latest.get("gcs"));
        gcs.put("eyeResponse", latest.get("eyeResponse"));
        gcs.put("verbalResponse", latest.get("verbalResponse"));
        gcs.put("motorResponse", latest.get("motorResponse"));
        gcs.put("karnofskyScore", latest.get("karnofskyScore"));
        gcs.put("dateRecorded", latest.get("date"));
        return gcs;
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getPrimaryNeurologicalDiagnosis(Patient patient) {
        if (patient == null || dao == null) {
            return "No diagnosis available";
        }

        List<Map<String, Object>> diagnoses = dao.getNeurosurgicalDiagnoses(patient);
        if (!diagnoses.isEmpty()) {
            String diagnosis = (String) diagnoses.get(0).get("diagnosis");
            if (diagnosis != null && !diagnosis.trim().isEmpty()) {
                return diagnosis;
            }
        }

        List<Map<String, Object>> contexts = dao.getAdmissionContexts(patient);
        if (contexts.isEmpty()) {
            return "No diagnosis available";
        }
        String admissionDiagnosis = (String) contexts.get(0).get("primaryDiagnosis");
        return (admissionDiagnosis == null || admissionDiagnosis.trim().isEmpty())
                ? "No diagnosis available" : admissionDiagnosis;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveAlerts(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }

        List<String> alerts = new ArrayList<>();
        Map<String, Object> latestGcs = getLatestGCS(patient);
        Object totalScore = latestGcs.get("totalScore");
        if (totalScore instanceof Integer) {
            int score = (Integer) totalScore;
            if (score <= 8) {
                alerts.add("Glasgow \u2264 8 : risque vital, surveillance neurologique rapproch\u00e9e requise");
            } else if (score <= 12) {
                alerts.add("Glasgow entre 9 et 12 : traumatisme mod\u00e9r\u00e9, surveillance renforc\u00e9e");
            }
        }
        return alerts;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getGlasgowComaScaleOptions() {
        Map<String, String> gcsOptions = new LinkedHashMap<>();
        
        // Eye Response
        gcsOptions.put("eye_1", "No eye opening");
        gcsOptions.put("eye_2", "Eye opening to pain");
        gcsOptions.put("eye_3", "Eye opening to verbal command");
        gcsOptions.put("eye_4", "Eyes open spontaneously");
        
        return gcsOptions;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getMotorFunctionOptions() {
        Map<String, String> motorOptions = new LinkedHashMap<>();
        
        motorOptions.put("0", "0 - No movement");
        motorOptions.put("1", "1 - Trace movement");
        motorOptions.put("2", "2 - Active movement, gravity eliminated");
        motorOptions.put("3", "3 - Active movement against gravity");
        motorOptions.put("4", "4 - Active movement against resistance");
        motorOptions.put("5", "5 - Normal strength");
        
        return motorOptions;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPupilResponseOptions() {
        Map<String, String> pupilOptions = new LinkedHashMap<>();
        
        pupilOptions.put("brisk", "Brisk");
        pupilOptions.put("sluggish", "Sluggish");
        pupilOptions.put("fixed", "Fixed");
        pupilOptions.put("not_assessed", "Not Assessed");
        
        return pupilOptions;
    }

    @Override
    @Transactional
    public void saveSurgicalHistory(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and surgical history data cannot be null");
        }
        if (dao != null) {
            dao.saveSurgicalHistory(patient, data);
            projectQuietly(patient, "patientview.surgicalHistory");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMedicalHistory(Patient patient) {
        if (patient == null || dao == null) {
            return new HashMap<>();
        }
        return dao.getMedicalHistory(patient);
    }

    @Override
    @Transactional
    public void saveMedicalHistory(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and medical history data cannot be null");
        }
        if (dao != null) {
            dao.saveMedicalHistory(patient, data);
            projectQuietly(patient, "patientview.medicalHistory");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdmissionContexts(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getAdmissionContexts(patient);
    }

    @Override
    @Transactional
    public void saveAdmissionContext(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and admission context data cannot be null");
        }
        if (dao != null) {
            dao.saveAdmissionContext(patient, data);
            projectQuietly(patient, "patientview.admissionContext");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVitalSigns(Patient patient, int limit) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getVitalSigns(patient, limit);
    }

    @Override
    @Transactional
    public void saveVitalSigns(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and vital signs data cannot be null");
        }
        if (dao != null) {
            dao.saveVitalSigns(patient, data);
            projectQuietly(patient, "patientview.vitalSigns");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNeuroExamDetails(Patient patient, int limit) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getNeuroExamDetails(patient, limit);
    }

    @Override
    @Transactional
    public void saveNeuroExamDetail(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and neuro exam data cannot be null");
        }
        if (dao != null) {
            dao.saveNeuroExamDetail(patient, data);
            projectQuietly(patient, "patientview.neuroExam");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNeurosurgicalDiagnoses(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getNeurosurgicalDiagnoses(patient);
    }

    @Override
    @Transactional
    public void saveNeurosurgicalDiagnosis(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and diagnosis data cannot be null");
        }
        if (dao != null) {
            dao.saveNeurosurgicalDiagnosis(patient, data);
            projectQuietly(patient, "patientview.diagnosis");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPathologyReports(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getPathologyReports(patient);
    }

    @Override
    @Transactional
    public void savePathologyReport(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and pathology data cannot be null");
        }
        if (dao != null) {
            dao.savePathologyReport(patient, data);
            projectQuietly(patient, "patientview.pathology");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMedicalTreatments(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getMedicalTreatments(patient);
    }

    @Override
    @Transactional
    public void saveMedicalTreatment(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and medical treatment entry data cannot be null");
        }
        if (dao != null) {
            dao.saveMedicalTreatment(patient, data);
            projectQuietly(patient, "patientview.medicalTreatment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSurgicalTreatments(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getSurgicalTreatments(patient);
    }

    @Override
    @Transactional
    public void saveSurgicalTreatment(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and surgical treatment entry data cannot be null");
        }
        if (dao != null) {
            dao.saveSurgicalTreatment(patient, data);
            projectQuietly(patient, "patientview.surgicalTreatment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostopEvolutions(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getPostopEvolutions(patient);
    }

    @Override
    @Transactional
    public void savePostopEvolution(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and post-operative evolution entry data cannot be null");
        }
        if (dao != null) {
            dao.savePostopEvolution(patient, data);
            projectQuietly(patient, "patientview.postopEvolution");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSequelae(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getSequelae(patient);
    }

    @Override
    @Transactional
    public void saveSequelae(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and sequelae assessment data cannot be null");
        }
        if (dao != null) {
            dao.saveSequelae(patient, data);
            projectQuietly(patient, "patientview.sequelae");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLabResults(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getLabResults(patient);
    }

    @Override
    @Transactional
    public void saveLabResult(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and laboratory result data cannot be null");
        }
        if (dao != null) {
            dao.saveLabResult(patient, data);
            projectQuietly(patient, "patientview.labResult");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDischarges(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getDischarges(patient);
    }

    @Override
    @Transactional
    public void saveDischarge(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and discharge episode data cannot be null");
        }
        if (dao != null) {
            dao.saveDischarge(patient, data);
            projectQuietly(patient, "patientview.discharge");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowUps(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getFollowUps(patient);
    }

    @Override
    @Transactional
    public void saveFollowUp(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and follow-up consultation data cannot be null");
        }
        if (dao != null) {
            dao.saveFollowUp(patient, data);
            projectQuietly(patient, "patientview.followUp");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getImagingNotes(Patient patient) {
        if (patient == null || dao == null) {
            return new ArrayList<>();
        }
        return dao.getImagingNotes(patient);
    }

    @Override
    @Transactional
    public void saveImagingNote(Patient patient, Map<String, Object> data) {
        if (patient == null || data == null) {
            throw new IllegalArgumentException("Patient and imaging note data cannot be null");
        }
        if (dao != null) {
            dao.saveImagingNote(patient, data);
            projectQuietly(patient, "patientview.imagingNote");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getImagingStudies(Patient patient) {
        if (patient == null) {
            return new ArrayList<>();
        }
        return DicomStudyBridge.getStudiesOfPatient(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isImagingModuleAvailable() {
        return DicomStudyBridge.isAvailable();
    }
    // ------------------------------------------------------------------ core-model projection

    @Override
    @Transactional
    public Map<String, Object> projectToClinicalModel(Patient patient) {
        if (patient == null || projector == null) {
            return new HashMap<>();
        }
        return projector.projectPatient(patient);
    }

    @Override
    @Transactional
    public Map<String, Object> projectAllPatients() {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (projector == null || dao == null) {
            return summary;
        }
        List<Patient> patients = dao.getPatientsWithNeurosurgeryRecords();
        summary.put("patientsProcessed", patients.size());
        for (Patient patient : patients) {
            // REQUIRES_NEW inside the projector keeps one patient's failure from abandoning
            // the rest of the backfill.
            try {
                projector.projectPatient(patient);
            } catch (Exception e) {
                log.warn("Backfill failed for patient " + patient.getPatientId(), e);
                summary.put("failures", asInt(summary.get("failures")) + 1);
            }
        }
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFhirCoverageReport() {
        if (projector == null) {
            return new HashMap<>();
        }
        return projector.getCoverageReport();
    }

    private static int asInt(Object value) {
        return value instanceof Integer ? (Integer) value : 0;
    }

    /**
     * Exports one set for one patient, swallowing any failure. Losing a clinical record because
     * its export failed would be far worse than not exporting it, so this can never propagate
     * (see ObsProjector's class doc; the projector also runs in its own transaction).
     */
    private void projectQuietly(Patient patient, String setId) {
        if (projector == null) {
            return;
        }
        try {
            projector.projectSet(patient, setId);
        } catch (Exception e) {
            log.warn("Projection of " + setId + " failed; the clinical record was still saved", e);
        }
    }
}
