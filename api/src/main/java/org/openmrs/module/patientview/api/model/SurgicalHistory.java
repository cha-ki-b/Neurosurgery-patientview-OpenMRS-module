package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

public class SurgicalHistory {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String procedureName;
    private Date datePerformed;
    private String surgeon;
    private String outcome;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

    public Date getDatePerformed() { return datePerformed; }
    public void setDatePerformed(Date datePerformed) { this.datePerformed = datePerformed; }

    public String getSurgeon() { return surgeon; }
    public void setSurgeon(String surgeon) { this.surgeon = surgeon; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
