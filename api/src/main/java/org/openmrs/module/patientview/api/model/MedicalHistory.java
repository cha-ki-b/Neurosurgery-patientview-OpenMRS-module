package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Antecedents medicaux (Fiche de Neurochirurgie, section 3).
 * One current record per patient; edited in place rather than versioned,
 * since the source document models this as a static checklist, not a
 * repeated observation.
 */
public class MedicalHistory {

    private Integer id;
    private String uuid;
    private Patient patient;
    private boolean diabetes;
    private boolean hypertension;
    private boolean epilepsy;
    private boolean stroke;
    private boolean heartDisease;
    private boolean renalFailure;
    private String allergies;
    private String chronicTreatment;
    private String otherHistory;
    private User creator;
    private Date dateCreated;
    private User changedBy;
    private Date dateChanged;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public boolean isDiabetes() { return diabetes; }
    public void setDiabetes(boolean diabetes) { this.diabetes = diabetes; }

    public boolean isHypertension() { return hypertension; }
    public void setHypertension(boolean hypertension) { this.hypertension = hypertension; }

    public boolean isEpilepsy() { return epilepsy; }
    public void setEpilepsy(boolean epilepsy) { this.epilepsy = epilepsy; }

    public boolean isStroke() { return stroke; }
    public void setStroke(boolean stroke) { this.stroke = stroke; }

    public boolean isHeartDisease() { return heartDisease; }
    public void setHeartDisease(boolean heartDisease) { this.heartDisease = heartDisease; }

    public boolean isRenalFailure() { return renalFailure; }
    public void setRenalFailure(boolean renalFailure) { this.renalFailure = renalFailure; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getChronicTreatment() { return chronicTreatment; }
    public void setChronicTreatment(String chronicTreatment) { this.chronicTreatment = chronicTreatment; }

    public String getOtherHistory() { return otherHistory; }
    public void setOtherHistory(String otherHistory) { this.otherHistory = otherHistory; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }

    public Date getDateChanged() { return dateChanged; }
    public void setDateChanged(Date dateChanged) { this.dateChanged = dateChanged; }
}
