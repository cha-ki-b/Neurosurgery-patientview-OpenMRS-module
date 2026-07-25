package org.openmrs.module.patientview.api.dao;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.model.NeuroAssessment;
import org.openmrs.module.patientview.api.model.SurgicalHistory;

import java.util.HashMap;
import java.util.ArrayList;
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
            result.add(map);
        }
        return result;
    }
}
