package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Suivi (Fiche de Neurochirurgie, section 14).
 * <p>
 * Append-only: one row per follow-up consultation.
 */
public class FollowUp {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Date consultationDate;
    private String clinicalExam;
    private String neurologicalExam;
    private String controlImaging;
    private boolean recurrence;
    private String currentTreatment;
    private Date nextConsultationDate;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Date getConsultationDate() { return consultationDate; }
    public void setConsultationDate(Date consultationDate) { this.consultationDate = consultationDate; }

    public String getClinicalExam() { return clinicalExam; }
    public void setClinicalExam(String clinicalExam) { this.clinicalExam = clinicalExam; }

    public String getNeurologicalExam() { return neurologicalExam; }
    public void setNeurologicalExam(String neurologicalExam) { this.neurologicalExam = neurologicalExam; }

    public String getControlImaging() { return controlImaging; }
    public void setControlImaging(String controlImaging) { this.controlImaging = controlImaging; }

    public boolean isRecurrence() { return recurrence; }
    public void setRecurrence(boolean recurrence) { this.recurrence = recurrence; }

    public String getCurrentTreatment() { return currentTreatment; }
    public void setCurrentTreatment(String currentTreatment) { this.currentTreatment = currentTreatment; }

    public Date getNextConsultationDate() { return nextConsultationDate; }
    public void setNextConsultationDate(Date nextConsultationDate) { this.nextConsultationDate = nextConsultationDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
