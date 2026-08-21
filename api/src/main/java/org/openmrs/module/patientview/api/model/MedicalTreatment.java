package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Traitement m\u00e9dical (Fiche de Neurochirurgie, section 9 - Prise en charge).
 * <p>
 * Append-only, like every other entity here: a change of regimen is a new row, so the
 * sequence of rows is the treatment history. The current regimen is the most recent row.
 * <p>
 * The five named drug classes are the checklist the paper form uses, modelled as booleans
 * the same way {@link MedicalHistory}'s comorbidity checklist is; anything outside them
 * goes in otherTreatments.
 */
public class MedicalTreatment {

    private Integer id;
    private String uuid;
    private Patient patient;
    private boolean corticosteroids;
    private boolean antiepileptics;
    private boolean antibiotics;
    private boolean anticoagulants;
    private boolean analgesics;
    private String otherTreatments;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public boolean isCorticosteroids() { return corticosteroids; }
    public void setCorticosteroids(boolean corticosteroids) { this.corticosteroids = corticosteroids; }

    public boolean isAntiepileptics() { return antiepileptics; }
    public void setAntiepileptics(boolean antiepileptics) { this.antiepileptics = antiepileptics; }

    public boolean isAntibiotics() { return antibiotics; }
    public void setAntibiotics(boolean antibiotics) { this.antibiotics = antibiotics; }

    public boolean isAnticoagulants() { return anticoagulants; }
    public void setAnticoagulants(boolean anticoagulants) { this.anticoagulants = anticoagulants; }

    public boolean isAnalgesics() { return analgesics; }
    public void setAnalgesics(boolean analgesics) { this.analgesics = analgesics; }

    public String getOtherTreatments() { return otherTreatments; }
    public void setOtherTreatments(String otherTreatments) { this.otherTreatments = otherTreatments; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
