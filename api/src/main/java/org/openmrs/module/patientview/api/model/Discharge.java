package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Sortie (Fiche de Neurochirurgie, section 13).
 * <p>
 * Append-only: one row per discharge episode, so a readmission and its later discharge sit
 * alongside the first rather than overwriting it.
 */
public class Discharge {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Date dischargeDate;
    private String dischargeMode;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Date getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(Date dischargeDate) { this.dischargeDate = dischargeDate; }

    public String getDischargeMode() { return dischargeMode; }
    public void setDischargeMode(String dischargeMode) { this.dischargeMode = dischargeMode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
