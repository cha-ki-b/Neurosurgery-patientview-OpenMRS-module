package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

public class NeuroAssessment {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Date assessmentDate;
    private Integer eyeResponse;
    private Integer verbalResponse;
    private Integer motorResponse;
    private Integer gcsTotal;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Date getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(Date assessmentDate) { this.assessmentDate = assessmentDate; }

    public Integer getEyeResponse() { return eyeResponse; }
    public void setEyeResponse(Integer eyeResponse) { this.eyeResponse = eyeResponse; }

    public Integer getVerbalResponse() { return verbalResponse; }
    public void setVerbalResponse(Integer verbalResponse) { this.verbalResponse = verbalResponse; }

    public Integer getMotorResponse() { return motorResponse; }
    public void setMotorResponse(Integer motorResponse) { this.motorResponse = motorResponse; }

    public Integer getGcsTotal() { return gcsTotal; }
    public void setGcsTotal(Integer gcsTotal) { this.gcsTotal = gcsTotal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
