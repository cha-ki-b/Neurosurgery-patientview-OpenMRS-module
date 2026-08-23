package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * S\u00e9quelles (Fiche de Neurochirurgie, section 12).
 * <p>
 * Kept separate from {@link PostopEvolution} even though the two share a tab: sequelae are
 * assessed at follow-up, weeks or months after the post-operative course is recorded. With
 * append-only rows, merging them would force a clinician to re-enter the whole
 * post-operative section just to add a deficit noticed later.
 * <p>
 * Append-only: one row per assessment.
 */
public class Sequelae {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String motorDeficit;
    private String sensoryDeficit;
    private boolean aphasia;
    private boolean cognitiveDisorders;
    private boolean secondaryEpilepsy;
    private String residualDisability;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getMotorDeficit() { return motorDeficit; }
    public void setMotorDeficit(String motorDeficit) { this.motorDeficit = motorDeficit; }

    public String getSensoryDeficit() { return sensoryDeficit; }
    public void setSensoryDeficit(String sensoryDeficit) { this.sensoryDeficit = sensoryDeficit; }

    public boolean isAphasia() { return aphasia; }
    public void setAphasia(boolean aphasia) { this.aphasia = aphasia; }

    public boolean isCognitiveDisorders() { return cognitiveDisorders; }
    public void setCognitiveDisorders(boolean cognitiveDisorders) { this.cognitiveDisorders = cognitiveDisorders; }

    public boolean isSecondaryEpilepsy() { return secondaryEpilepsy; }
    public void setSecondaryEpilepsy(boolean secondaryEpilepsy) { this.secondaryEpilepsy = secondaryEpilepsy; }

    public String getResidualDisability() { return residualDisability; }
    public void setResidualDisability(String residualDisability) { this.residualDisability = residualDisability; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
