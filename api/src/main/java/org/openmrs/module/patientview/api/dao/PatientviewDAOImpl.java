package org.openmrs.module.patientview.api.dao;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.model.AdmissionContext;
import org.openmrs.module.patientview.api.model.Discharge;
import org.openmrs.module.patientview.api.model.FhirProjection;
import org.openmrs.module.patientview.api.model.FollowUp;
import org.openmrs.module.patientview.api.model.ImagingNote;
import org.openmrs.module.patientview.api.model.LabResult;
import org.openmrs.module.patientview.api.model.MedicalHistory;
import org.openmrs.module.patientview.api.model.MedicalTreatment;
import org.openmrs.module.patientview.api.model.NeuroAssessment;
import org.openmrs.module.patientview.api.model.NeuroExamDetail;
import org.openmrs.module.patientview.api.model.NeurosurgicalDiagnosis;
import org.openmrs.module.patientview.api.model.Pathology;
import org.openmrs.module.patientview.api.model.PostopEvolution;
import org.openmrs.module.patientview.api.model.Sequelae;
import org.openmrs.module.patientview.api.model.SurgicalHistory;
import org.openmrs.module.patientview.api.model.SurgicalTreatment;
import org.openmrs.module.patientview.api.model.VitalSigns;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            map.put("uuid", a.getUuid());
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
            map.put("uuid", s.getUuid());
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
        map.put("uuid", history.getUuid());
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
            map.put("uuid", a.getUuid());
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
            map.put("uuid", v.getUuid());
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
            map.put("uuid", n.getUuid());
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
            map.put("uuid", d.getUuid());
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
            map.put("uuid", p.getUuid());
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

    @Override
    public List<Map<String, Object>> getMedicalTreatments(Patient patient) {
        List<MedicalTreatment> records = sessionFactory.getCurrentSession()
                .createQuery("from MedicalTreatment m where m.patient = :patient order by m.dateCreated desc",
                        MedicalTreatment.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (MedicalTreatment treatment : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", treatment.getUuid());
            map.put("corticosteroids", treatment.isCorticosteroids());
            map.put("antiepileptics", treatment.isAntiepileptics());
            map.put("antibiotics", treatment.isAntibiotics());
            map.put("anticoagulants", treatment.isAnticoagulants());
            map.put("analgesics", treatment.isAnalgesics());
            map.put("otherTreatments", treatment.getOtherTreatments());
            map.put("notes", treatment.getNotes());
            map.put("dateCreated", treatment.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveMedicalTreatment(Patient patient, Map<String, Object> data) {
        MedicalTreatment treatment = new MedicalTreatment();
        treatment.setUuid(UUID.randomUUID().toString());
        treatment.setPatient(patient);
        treatment.setCorticosteroids(Boolean.TRUE.equals(data.get("corticosteroids")));
        treatment.setAntiepileptics(Boolean.TRUE.equals(data.get("antiepileptics")));
        treatment.setAntibiotics(Boolean.TRUE.equals(data.get("antibiotics")));
        treatment.setAnticoagulants(Boolean.TRUE.equals(data.get("anticoagulants")));
        treatment.setAnalgesics(Boolean.TRUE.equals(data.get("analgesics")));
        treatment.setOtherTreatments((String) data.get("otherTreatments"));
        treatment.setNotes((String) data.get("notes"));
        treatment.setCreator(Context.getAuthenticatedUser());
        treatment.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(treatment);
    }

    @Override
    public List<Map<String, Object>> getSurgicalTreatments(Patient patient) {
        List<SurgicalTreatment> records = sessionFactory.getCurrentSession()
                .createQuery("from SurgicalTreatment s where s.patient = :patient order by s.surgeryDate desc",
                        SurgicalTreatment.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SurgicalTreatment treatment : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", treatment.getUuid());
            map.put("procedurePerformed", treatment.getProcedurePerformed());
            map.put("resectionType", treatment.getResectionType());
            map.put("surgeryDate", treatment.getSurgeryDate());
            map.put("durationMinutes", treatment.getDurationMinutes());
            map.put("surgeon", treatment.getSurgeon());
            map.put("intraoperativeComplications", treatment.getIntraoperativeComplications());
            map.put("drainPlaced", treatment.isDrainPlaced());
            map.put("catheterPlaced", treatment.isCatheterPlaced());
            map.put("notes", treatment.getNotes());
            map.put("dateCreated", treatment.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveSurgicalTreatment(Patient patient, Map<String, Object> data) {
        SurgicalTreatment treatment = new SurgicalTreatment();
        treatment.setUuid(UUID.randomUUID().toString());
        treatment.setPatient(patient);
        treatment.setProcedurePerformed((String) data.get("procedurePerformed"));
        treatment.setResectionType((String) data.get("resectionType"));
        treatment.setSurgeryDate((Date) data.get("surgeryDate"));
        treatment.setDurationMinutes((Integer) data.get("durationMinutes"));
        treatment.setSurgeon((String) data.get("surgeon"));
        treatment.setIntraoperativeComplications((String) data.get("intraoperativeComplications"));
        treatment.setDrainPlaced(Boolean.TRUE.equals(data.get("drainPlaced")));
        treatment.setCatheterPlaced(Boolean.TRUE.equals(data.get("catheterPlaced")));
        treatment.setNotes((String) data.get("notes"));
        treatment.setCreator(Context.getAuthenticatedUser());
        treatment.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(treatment);
    }

    @Override
    public List<Map<String, Object>> getPostopEvolutions(Patient patient) {
        List<PostopEvolution> records = sessionFactory.getCurrentSession()
                .createQuery("from PostopEvolution e where e.patient = :patient order by e.dateCreated desc",
                        PostopEvolution.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (PostopEvolution evolution : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", evolution.getUuid());
            map.put("postopGcs", evolution.getPostopGcs());
            map.put("postopKarnofsky", evolution.getPostopKarnofsky());
            map.put("neurologicalStatus", evolution.getNeurologicalStatus());
            map.put("infection", evolution.isInfection());
            map.put("hemorrhage", evolution.isHemorrhage());
            map.put("hydrocephalus", evolution.isHydrocephalus());
            map.put("csfLeak", evolution.isCsfLeak());
            map.put("seizures", evolution.isSeizures());
            map.put("deceased", evolution.isDeceased());
            map.put("deathDate", evolution.getDeathDate());
            map.put("notes", evolution.getNotes());
            map.put("dateCreated", evolution.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void savePostopEvolution(Patient patient, Map<String, Object> data) {
        PostopEvolution evolution = new PostopEvolution();
        evolution.setUuid(UUID.randomUUID().toString());
        evolution.setPatient(patient);
        evolution.setPostopGcs((Integer) data.get("postopGcs"));
        evolution.setPostopKarnofsky((Integer) data.get("postopKarnofsky"));
        evolution.setNeurologicalStatus((String) data.get("neurologicalStatus"));
        evolution.setInfection(Boolean.TRUE.equals(data.get("infection")));
        evolution.setHemorrhage(Boolean.TRUE.equals(data.get("hemorrhage")));
        evolution.setHydrocephalus(Boolean.TRUE.equals(data.get("hydrocephalus")));
        evolution.setCsfLeak(Boolean.TRUE.equals(data.get("csfLeak")));
        evolution.setSeizures(Boolean.TRUE.equals(data.get("seizures")));
        evolution.setDeceased(Boolean.TRUE.equals(data.get("deceased")));
        evolution.setDeathDate((Date) data.get("deathDate"));
        evolution.setNotes((String) data.get("notes"));
        evolution.setCreator(Context.getAuthenticatedUser());
        evolution.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(evolution);
    }

    @Override
    public List<Map<String, Object>> getSequelae(Patient patient) {
        List<Sequelae> records = sessionFactory.getCurrentSession()
                .createQuery("from Sequelae s where s.patient = :patient order by s.dateCreated desc",
                        Sequelae.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Sequelae sequelae : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", sequelae.getUuid());
            map.put("motorDeficit", sequelae.getMotorDeficit());
            map.put("sensoryDeficit", sequelae.getSensoryDeficit());
            map.put("aphasia", sequelae.isAphasia());
            map.put("cognitiveDisorders", sequelae.isCognitiveDisorders());
            map.put("secondaryEpilepsy", sequelae.isSecondaryEpilepsy());
            map.put("residualDisability", sequelae.getResidualDisability());
            map.put("notes", sequelae.getNotes());
            map.put("dateCreated", sequelae.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveSequelae(Patient patient, Map<String, Object> data) {
        Sequelae sequelae = new Sequelae();
        sequelae.setUuid(UUID.randomUUID().toString());
        sequelae.setPatient(patient);
        sequelae.setMotorDeficit((String) data.get("motorDeficit"));
        sequelae.setSensoryDeficit((String) data.get("sensoryDeficit"));
        sequelae.setAphasia(Boolean.TRUE.equals(data.get("aphasia")));
        sequelae.setCognitiveDisorders(Boolean.TRUE.equals(data.get("cognitiveDisorders")));
        sequelae.setSecondaryEpilepsy(Boolean.TRUE.equals(data.get("secondaryEpilepsy")));
        sequelae.setResidualDisability((String) data.get("residualDisability"));
        sequelae.setNotes((String) data.get("notes"));
        sequelae.setCreator(Context.getAuthenticatedUser());
        sequelae.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(sequelae);
    }

    @Override
    public List<Map<String, Object>> getLabResults(Patient patient) {
        List<LabResult> records = sessionFactory.getCurrentSession()
                .createQuery("from LabResult l where l.patient = :patient order by l.sampleDate desc",
                        LabResult.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (LabResult labResult : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", labResult.getUuid());
            map.put("sampleDate", labResult.getSampleDate());
            map.put("completeBloodCount", labResult.getCompleteBloodCount());
            map.put("crp", labResult.getCrp());
            map.put("ionogram", labResult.getIonogram());
            map.put("glycemia", labResult.getGlycemia());
            map.put("creatinine", labResult.getCreatinine());
            map.put("coagulation", labResult.getCoagulation());
            map.put("bloodGroup", labResult.getBloodGroup());
            map.put("notes", labResult.getNotes());
            map.put("dateCreated", labResult.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveLabResult(Patient patient, Map<String, Object> data) {
        LabResult labResult = new LabResult();
        labResult.setUuid(UUID.randomUUID().toString());
        labResult.setPatient(patient);
        labResult.setSampleDate((Date) data.get("sampleDate"));
        labResult.setCompleteBloodCount((String) data.get("completeBloodCount"));
        labResult.setCrp((String) data.get("crp"));
        labResult.setIonogram((String) data.get("ionogram"));
        labResult.setGlycemia((String) data.get("glycemia"));
        labResult.setCreatinine((String) data.get("creatinine"));
        labResult.setCoagulation((String) data.get("coagulation"));
        labResult.setBloodGroup((String) data.get("bloodGroup"));
        labResult.setNotes((String) data.get("notes"));
        labResult.setCreator(Context.getAuthenticatedUser());
        labResult.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(labResult);
    }

    @Override
    public List<Map<String, Object>> getDischarges(Patient patient) {
        List<Discharge> records = sessionFactory.getCurrentSession()
                .createQuery("from Discharge d where d.patient = :patient order by d.dischargeDate desc",
                        Discharge.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Discharge discharge : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", discharge.getUuid());
            map.put("dischargeDate", discharge.getDischargeDate());
            map.put("dischargeMode", discharge.getDischargeMode());
            map.put("notes", discharge.getNotes());
            map.put("dateCreated", discharge.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveDischarge(Patient patient, Map<String, Object> data) {
        Discharge discharge = new Discharge();
        discharge.setUuid(UUID.randomUUID().toString());
        discharge.setPatient(patient);
        discharge.setDischargeDate((Date) data.get("dischargeDate"));
        discharge.setDischargeMode((String) data.get("dischargeMode"));
        discharge.setNotes((String) data.get("notes"));
        discharge.setCreator(Context.getAuthenticatedUser());
        discharge.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(discharge);
    }

    @Override
    public List<Map<String, Object>> getFollowUps(Patient patient) {
        List<FollowUp> records = sessionFactory.getCurrentSession()
                .createQuery("from FollowUp f where f.patient = :patient order by f.consultationDate desc",
                        FollowUp.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (FollowUp followUp : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", followUp.getUuid());
            map.put("consultationDate", followUp.getConsultationDate());
            map.put("clinicalExam", followUp.getClinicalExam());
            map.put("neurologicalExam", followUp.getNeurologicalExam());
            map.put("controlImaging", followUp.getControlImaging());
            map.put("recurrence", followUp.isRecurrence());
            map.put("currentTreatment", followUp.getCurrentTreatment());
            map.put("nextConsultationDate", followUp.getNextConsultationDate());
            map.put("notes", followUp.getNotes());
            map.put("dateCreated", followUp.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveFollowUp(Patient patient, Map<String, Object> data) {
        FollowUp followUp = new FollowUp();
        followUp.setUuid(UUID.randomUUID().toString());
        followUp.setPatient(patient);
        followUp.setConsultationDate((Date) data.get("consultationDate"));
        followUp.setClinicalExam((String) data.get("clinicalExam"));
        followUp.setNeurologicalExam((String) data.get("neurologicalExam"));
        followUp.setControlImaging((String) data.get("controlImaging"));
        followUp.setRecurrence(Boolean.TRUE.equals(data.get("recurrence")));
        followUp.setCurrentTreatment((String) data.get("currentTreatment"));
        followUp.setNextConsultationDate((Date) data.get("nextConsultationDate"));
        followUp.setNotes((String) data.get("notes"));
        followUp.setCreator(Context.getAuthenticatedUser());
        followUp.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(followUp);
    }

    @Override
    public List<Map<String, Object>> getImagingNotes(Patient patient) {
        List<ImagingNote> records = sessionFactory.getCurrentSession()
                .createQuery("from ImagingNote i where i.patient = :patient order by i.examDate desc",
                        ImagingNote.class)
                .setParameter("patient", patient)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ImagingNote note : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", note.getUuid());
            map.put("examType", note.getExamType());
            map.put("examDate", note.getExamDate());
            map.put("findings", note.getFindings());
            map.put("notes", note.getNotes());
            map.put("dateCreated", note.getDateCreated());
            result.add(map);
        }
        return result;
    }

    @Override
    public void saveImagingNote(Patient patient, Map<String, Object> data) {
        ImagingNote note = new ImagingNote();
        note.setUuid(UUID.randomUUID().toString());
        note.setPatient(patient);
        note.setExamType((String) data.get("examType"));
        note.setExamDate((Date) data.get("examDate"));
        note.setFindings((String) data.get("findings"));
        note.setNotes((String) data.get("notes"));
        note.setCreator(Context.getAuthenticatedUser());
        note.setDateCreated(new Date());

        sessionFactory.getCurrentSession().save(note);
    }
    /**
     * Entities the projector can read. Used only to collect the distinct patients holding any
     * neurosurgery record - Hibernate's HQL has no UNION, and sixteen index-backed "select
     * distinct patient" queries are cheaper to read than one hand-written native union.
     */
    private static final String[] PROJECTABLE_ENTITIES = {
            "NeuroAssessment", "SurgicalHistory", "MedicalHistory", "AdmissionContext",
            "VitalSigns", "NeuroExamDetail", "NeurosurgicalDiagnosis", "Pathology",
            "MedicalTreatment", "SurgicalTreatment", "PostopEvolution", "Sequelae",
            "LabResult", "Discharge", "FollowUp", "ImagingNote"
    };

    @Override
    public List<String> getProjectedSourceUuids(Patient patient, String sourceSet) {
        return sessionFactory.getCurrentSession()
                .createQuery("select p.sourceUuid from FhirProjection p where p.patient = :patient"
                        + " and p.sourceSet = :sourceSet", String.class)
                .setParameter("patient", patient)
                .setParameter("sourceSet", sourceSet)
                .list();
    }

    @Override
    public void saveFhirProjection(FhirProjection projection) {
        sessionFactory.getCurrentSession().save(projection);
    }

    @Override
    public List<Patient> getPatientsWithNeurosurgeryRecords() {
        Set<Patient> patients = new LinkedHashSet<>();
        for (String entity : PROJECTABLE_ENTITIES) {
            patients.addAll(sessionFactory.getCurrentSession()
                    .createQuery("select distinct e.patient from " + entity + " e", Patient.class)
                    .list());
        }
        return new ArrayList<>(patients);
    }
}
