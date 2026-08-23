package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * \u00c9volution postop\u00e9ratoire (Fiche de Neurochirurgie, section 11).
 * <p>
 * Append-only: one row per post-operative assessment, so the sequence of rows is the
 * patient's post-operative course. The Glasgow and Karnofsky scores here are recorded
 * separately from {@link NeuroAssessment}'s because the fiche tracks them as a distinct
 * post-operative observation, alongside the complications checklist of the same section.
 */
public class PostopEvolution {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Integer postopGcs;
    private Integer postopKarnofsky;
    private String neurologicalStatus;
    private boolean infection;
    private boolean hemorrhage;
    private boolean hydrocephalus;
    private boolean csfLeak;
    private boolean seizures;
    private boolean deceased;
    private Date deathDate;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Integer getPostopGcs() { return postopGcs; }
    public void setPostopGcs(Integer postopGcs) { this.postopGcs = postopGcs; }

    public Integer getPostopKarnofsky() { return postopKarnofsky; }
    public void setPostopKarnofsky(Integer postopKarnofsky) { this.postopKarnofsky = postopKarnofsky; }

    public String getNeurologicalStatus() { return neurologicalStatus; }
    public void setNeurologicalStatus(String neurologicalStatus) { this.neurologicalStatus = neurologicalStatus; }

    public boolean isInfection() { return infection; }
    public void setInfection(boolean infection) { this.infection = infection; }

    public boolean isHemorrhage() { return hemorrhage; }
    public void setHemorrhage(boolean hemorrhage) { this.hemorrhage = hemorrhage; }

    public boolean isHydrocephalus() { return hydrocephalus; }
    public void setHydrocephalus(boolean hydrocephalus) { this.hydrocephalus = hydrocephalus; }

    public boolean isCsfLeak() { return csfLeak; }
    public void setCsfLeak(boolean csfLeak) { this.csfLeak = csfLeak; }

    public boolean isSeizures() { return seizures; }
    public void setSeizures(boolean seizures) { this.seizures = seizures; }

    public boolean isDeceased() { return deceased; }
    public void setDeceased(boolean deceased) { this.deceased = deceased; }

    public Date getDeathDate() { return deathDate; }
    public void setDeathDate(Date deathDate) { this.deathDate = deathDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
