package org.openmrs.module.patientview.api.dao;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.model.AdmissionContext;
import org.openmrs.module.patientview.api.model.MedicalHistory;
import org.openmrs.module.patientview.api.model.NeuroAssessment;
import org.openmrs.module.patientview.api.model.NeuroExamDetail;
import org.openmrs.module.patientview.api.model.NeurosurgicalDiagnosis;
import org.openmrs.module.patientview.api.model.Pathology;
import org.openmrs.module.patientview.api.model.SurgicalHistory;
import org.openmrs.module.patientview.api.model.VitalSigns;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PatientviewDAOImpl implements PatientviewDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Map<String, Object>> getNeuroAssessments(Patient patient, int limit) {
        List<NeuroAssessment> assessments = sessionFactory.getCurrentSession()
                .createQuery("from NeuroAssessment n where n.patient = :patient order by n.assessmentDate desc",
                        NeuroAssessment.class)
                .setParameter("patient", patient)
                .setMaxResults(limit)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (NeuroAssessment a : assessments) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", a.getAssessmentDate());
            map.put("gcs", a.getGcsTotal());
            map.put("eyeResponse", a.getEyeResponse());
            map.put("verbalResponse", a.getVerbalResponse());
            map.put("motorResponse", a.getMotorResponse());
            map.put("karnofskyScore", a.getKarnofskyScore());
            map.put("notes", a.getNotes());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveNeuroAssessment(Patient patient, Map<String, Object> assessmentData) {
        NeuroAssessment assessment = new NeuroAssessment();
        assessment.setUuid(UUID.randomUUID().toString());
        assessment.setPatient(patient);
        assessment.setAssessmentDate(new java.util.Date());

        Integer eye = (Integer) assessmentData.get("eyeResponse");
        Integer verbal = (Integer) assessmentData.get("verbalResponse");
        Integer motor = (Integer) assessmentData.get("motorResponse");
        assessment.setEyeResponse(eye);
        assessment.setVerbalResponse(verbal);
        assessment.setMotorResponse(motor);
        assessment.setGcsTotal((eye == null ? 0 : eye) + (verbal == null ? 0 : verbal) + (motor == null ? 0 : motor));
        assessment.setKarnofskyScore((Integer) assessmentData.get("karnofskyScore"));
        assessment.setNotes((String) assessmentData.get("notes"));
        assessment.setCreator(Context.getAuthenticatedUser());
        assessment.setDateCreated(new java.util.Date());

        sessionFactory.getCurrentSession().save(assessment);
    }

    @Override
    public List<Map<String, Object>> getSurgicalHistory(Patient patient) {
        List<SurgicalHistory> history = sessionFactory.getCurrentSession()
                .createQuery("from SurgicalHistory s where s.patient = :patient order by s.datePerformed desc",
                        SurgicalHistory.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SurgicalHistory s : history) {
            Map<String, Object> map = new HashMap<>();
            map.put("procedure", s.getProcedureName());
            map.put("date", s.getDatePerformed());
            map.put("surgeon", s.getSurgeon());
            map.put("outcome", s.getOutcome());
            map.put("location", s.getLocation());
            map.put("notes", s.getNotes());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveSurgicalHistory(Patient patient, Map<String, Object> data) {
        SurgicalHistory history = new SurgicalHistory();
        history.setUuid(UUID.randomUUID().toString());
        history.setPatient(patient);
        history.setProcedureName((String) data.get("procedureName"));
        history.setDatePerformed((Date) data.get("datePerformed"));
        history.setSurgeon((String) data.get("surgeon"));
        history.setLocation((String) data.get("location"));
        history.setOutcome((String) data.get("outcome"));
        history.setNotes((String) data.get("notes"));
        history.setCreator(Context.getAuthenticatedUser());
        history.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(history);
    }

    @Override
    public Map<String, Object> getMedicalHistory(Patient patient) {
        List<MedicalHistory> results = sessionFactory.getCurrentSession()
                .createQuery("from MedicalHistory m where m.patient = :patient order by m.dateCreated desc",
                        MedicalHistory.class)
                .setParameter("patient", patient)
                .setMaxResults(1)
                .list();

        Map<String, Object> map = new HashMap<>();
        if (results.isEmpty()) {
            return map;
        }
        MedicalHistory history = results.get(0);
        map.put("diabetes", history.isDiabetes());
        map.put("hypertension", history.isHypertension());
        map.put("epilepsy", history.isEpilepsy());
        map.put("stroke", history.isStroke());
        map.put("heartDisease", history.isHeartDisease());
        map.put("renalFailure", history.isRenalFailure());
        map.put("allergies", history.getAllergies());
        map.put("chronicTreatment", history.getChronicTreatment());
        map.put("otherHistory", history.getOtherHistory());
        map.put("dateChanged", history.getDateCreated());
        return map;
    }

    @Override
    public void saveMedicalHistory(Patient patient, Map<String, Object> data) {
        // Always insert a new row - never update an existing one. This is an append-only
        // history (like every other table in this module): correcting or updating a patient's
        // medical history creates a new, separately attributed and dated entry rather than
        // silently overwriting the previous values, so there is always a full audit trail of
        // who recorded what and when (non-repudiation).
        MedicalHistory history = new MedicalHistory();
        history.setUuid(UUID.randomUUID().toString());
        history.setPatient(patient);
        history.setCreator(Context.getAuthenticatedUser());
        history.setDateCreated(new Date());

        history.setDiabetes(Boolean.TRUE.equals(data.get("diabetes")));
        history.setHypertension(Boolean.TRUE.equals(data.get("hypertension")));
        history.setEpilepsy(Boolean.TRUE.equals(data.get("epilepsy")));
        history.setStroke(Boolean.TRUE.equals(data.get("stroke")));
        history.setHeartDisease(Boolean.TRUE.equals(data.get("heartDisease")));
        history.setRenalFailure(Boolean.TRUE.equals(data.get("renalFailure")));
        history.setAllergies((String) data.get("allergies"));
        history.setChronicTreatment((String) data.get("chronicTreatment"));
        history.setOtherHistory((String) data.get("otherHistory"));

        sessionFactory.getCurrentSession().save(history);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAdmissionContexts(Patient patient) {
        List<AdmissionContext> contexts = sessionFactory.getCurrentSession()
                .createQuery("from AdmissionContext a where a.patient = :patient order by a.dateCreated desc",
                        AdmissionContext.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionContext a : contexts) {
            Map<String, Object> map = new HashMap<>();
            map.put("admissionReason", a.getAdmissionReason());
            map.put("symptomOnsetDate", a.getSymptomOnsetDate());
            map.put("diagnosisDelayDays", a.getDiagnosisDelayDays());
            map.put("admissionDiagnosis", a.getAdmissionDiagnosis());
            map.put("primaryDiagnosis", a.getPrimaryDiagnosis());
            map.put("secondaryDiagnoses", a.getSecondaryDiagnoses());
            map.put("profession", a.getProfession());
            map.put("referringPhysician", a.getReferringPhysician());
            map.put("responsibleNeurosurgeon", a.getResponsibleNeurosurgeon());
            map.put("dateCreated", a.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveAdmissionContext(Patient patient, Map<String, Object> data) {
        AdmissionContext context = new AdmissionContext();
        context.setUuid(UUID.randomUUID().toString());
        context.setPatient(patient);
        context.setAdmissionReason((String) data.get("admissionReason"));
        context.setSymptomOnsetDate((Date) data.get("symptomOnsetDate"));
        context.setDiagnosisDelayDays((Integer) data.get("diagnosisDelayDays"));
        context.setAdmissionDiagnosis((String) data.get("admissionDiagnosis"));
        context.setPrimaryDiagnosis((String) data.get("primaryDiagnosis"));
        context.setSecondaryDiagnoses((String) data.get("secondaryDiagnoses"));
        context.setProfession((String) data.get("profession"));
        context.setReferringPhysician((String) data.get("referringPhysician"));
        context.setResponsibleNeurosurgeon((String) data.get("responsibleNeurosurgeon"));
        context.setCreator(Context.getAuthenticatedUser());
        context.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVitalSigns(Patient patient, int limit) {
        List<VitalSigns> vitals = sessionFactory.getCurrentSession()
                .createQuery("from VitalSigns v where v.patient = :patient order by v.examDate desc", VitalSigns.class)
                .setParameter("patient", patient)
                .setMaxResults(limit)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (VitalSigns v : vitals) {
            Map<String, Object> map = new HashMap<>();
            map.put("examDate", v.getExamDate());
            map.put("temperature", v.getTemperature());
            map.put("bpSystolic", v.getBpSystolic());
            map.put("bpDiastolic", v.getBpDiastolic());
            map.put("heartRate", v.getHeartRate());
            map.put("respiratoryRate", v.getRespiratoryRate());
            map.put("spo2", v.getSpo2());
            map.put("weightKg", v.getWeightKg());
            map.put("heightCm", v.getHeightCm());
            map.put("bmi", v.getBmi());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveVitalSigns(Patient patient, Map<String, Object> data) {
        VitalSigns vitals = new VitalSigns();
        vitals.setUuid(UUID.randomUUID().toString());
        vitals.setPatient(patient);
        vitals.setExamDate(new Date());
        vitals.setTemperature(toBigDecimal(data.get("temperature")));
        vitals.setBpSystolic((Integer) data.get("bpSystolic"));
        vitals.setBpDiastolic((Integer) data.get("bpDiastolic"));
        vitals.setHeartRate((Integer) data.get("heartRate"));
        vitals.setRespiratoryRate((Integer) data.get("respiratoryRate"));
        vitals.setSpo2((Integer) data.get("spo2"));
        BigDecimal weight = toBigDecimal(data.get("weightKg"));
        BigDecimal height = toBigDecimal(data.get("heightCm"));
        vitals.setWeightKg(weight);
        vitals.setHeightCm(height);
        vitals.setBmi(computeBmi(weight, height));
        vitals.setCreator(Context.getAuthenticatedUser());
        vitals.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(vitals);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private static BigDecimal computeBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.signum() == 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightM.multiply(heightM);
        if (heightSquared.signum() == 0) {
            return null;
        }
        return weightKg.divide(heightSquared, 1, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNeuroExamDetails(Patient patient, int limit) {
        List<NeuroExamDetail> exams = sessionFactory.getCurrentSession()
                .createQuery("from NeuroExamDetail n where n.patient = :patient order by n.examDate desc",
                        NeuroExamDetail.class)
                .setParameter("patient", patient)
                .setMaxResults(limit)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (NeuroExamDetail n : exams) {
            Map<String, Object> map = new HashMap<>();
            map.put("examDate", n.getExamDate());
            map.put("orientation", n.getOrientation());
            map.put("language", n.getLanguage());
            map.put("memory", n.getMemory());
            map.put("cranialNerves", n.getCranialNerves());
            map.put("motorDeficit", n.getMotorDeficit());
            map.put("sensoryDeficit", n.getSensoryDeficit());
            map.put("reflexes", n.getReflexes());
            map.put("plantarReflex", n.getPlantarReflex());
            map.put("coordination", n.getCoordination());
            map.put("gait", n.getGait());
            map.put("balance", n.getBalance());
            map.put("cerebellarSigns", n.isCerebellarSigns());
            map.put("pyramidalSyndrome", n.isPyramidalSyndrome());
            map.put("meningealSyndrome", n.isMeningealSyndrome());
            map.put("seizures", n.isSeizures());
            map.put("headache", n.isHeadache());
            map.put("vomiting", n.isVomiting());
            map.put("visualDisturbances", n.isVisualDisturbances());
            map.put("sphincterDisturbances", n.isSphincterDisturbances());
            map.put("notes", n.getNotes());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveNeuroExamDetail(Patient patient, Map<String, Object> data) {
        NeuroExamDetail exam = new NeuroExamDetail();
        exam.setUuid(UUID.randomUUID().toString());
        exam.setPatient(patient);
        exam.setExamDate(new Date());
        exam.setOrientation((String) data.get("orientation"));
        exam.setLanguage((String) data.get("language"));
        exam.setMemory((String) data.get("memory"));
        exam.setCranialNerves((String) data.get("cranialNerves"));
        exam.setMotorDeficit((String) data.get("motorDeficit"));
        exam.setSensoryDeficit((String) data.get("sensoryDeficit"));
        exam.setReflexes((String) data.get("reflexes"));
        exam.setPlantarReflex((String) data.get("plantarReflex"));
        exam.setCoordination((String) data.get("coordination"));
        exam.setGait((String) data.get("gait"));
        exam.setBalance((String) data.get("balance"));
        exam.setCerebellarSigns(Boolean.TRUE.equals(data.get("cerebellarSigns")));
        exam.setPyramidalSyndrome(Boolean.TRUE.equals(data.get("pyramidalSyndrome")));
        exam.setMeningealSyndrome(Boolean.TRUE.equals(data.get("meningealSyndrome")));
        exam.setSeizures(Boolean.TRUE.equals(data.get("seizures")));
        exam.setHeadache(Boolean.TRUE.equals(data.get("headache")));
        exam.setVomiting(Boolean.TRUE.equals(data.get("vomiting")));
        exam.setVisualDisturbances(Boolean.TRUE.equals(data.get("visualDisturbances")));
        exam.setSphincterDisturbances(Boolean.TRUE.equals(data.get("sphincterDisturbances")));
        exam.setNotes((String) data.get("notes"));
        exam.setCreator(Context.getAuthenticatedUser());
        exam.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(exam);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNeurosurgicalDiagnoses(Patient patient) {
        List<NeurosurgicalDiagnosis> diagnoses = sessionFactory.getCurrentSession()
                .createQuery("from NeurosurgicalDiagnosis d where d.patient = :patient order by d.dateCreated desc",
                        NeurosurgicalDiagnosis.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (NeurosurgicalDiagnosis d : diagnoses) {
            Map<String, Object> map = new HashMap<>();
            map.put("dateCreated", d.getDateCreated());
            map.put("diagnosis", d.getDiagnosis());
            map.put("lesionLocation", d.getLesionLocation());
            map.put("laterality", d.getLaterality());
            map.put("size", d.getSize());
            map.put("lesionCount", d.getLesionCount());
            map.put("whoDiagnosis", d.getWhoDiagnosis());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveNeurosurgicalDiagnosis(Patient patient, Map<String, Object> data) {
        NeurosurgicalDiagnosis diagnosis = new NeurosurgicalDiagnosis();
        diagnosis.setUuid(UUID.randomUUID().toString());
        diagnosis.setPatient(patient);
        diagnosis.setDiagnosis((String) data.get("diagnosis"));
        diagnosis.setLesionLocation((String) data.get("lesionLocation"));
        diagnosis.setLaterality((String) data.get("laterality"));
        diagnosis.setSize((String) data.get("size"));
        diagnosis.setLesionCount((Integer) data.get("lesionCount"));
        diagnosis.setWhoDiagnosis((String) data.get("whoDiagnosis"));
        diagnosis.setCreator(Context.getAuthenticatedUser());
        diagnosis.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(diagnosis);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPathologyReports(Patient patient) {
        List<Pathology> reports = sessionFactory.getCurrentSession()
                .createQuery("from Pathology p where p.patient = :patient order by p.dateCreated desc", Pathology.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Pathology p : reports) {
            Map<String, Object> map = new HashMap<>();
            map.put("dateCreated", p.getDateCreated());
            map.put("studyFindings", p.getStudyFindings());
            map.put("whoGrade", p.getWhoGrade());
            map.put("immunohistochemistry", p.getImmunohistochemistry());
            map.put("molecularMarkers", p.getMolecularMarkers());
            map.put("finalDiagnosis", p.getFinalDiagnosis());
            result.add(map);
        }
        return result;
    }

    @Override
    public void savePathologyReport(Patient patient, Map<String, Object> data) {
        Pathology pathology = new Pathology();
        pathology.setUuid(UUID.randomUUID().toString());
        pathology.setPatient(patient);
        pathology.setStudyFindings((String) data.get("studyFindings"));
        pathology.setWhoGrade((String) data.get("whoGrade"));
        pathology.setImmunohistochemistry((String) data.get("immunohistochemistry"));
        pathology.setMolecularMarkers((String) data.get("molecularMarkers"));
        pathology.setFinalDiagnosis((String) data.get("finalDiagnosis"));
        pathology.setCreator(Context.getAuthenticatedUser());
        pathology.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(pathology);
    }
}
