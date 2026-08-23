package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Biologie (Fiche de Neurochirurgie, section 7).
 * <p>
 * Append-only: one row per blood draw.
 * <p>
 * Every result is free text rather than a number, deliberately. Most of these lines are
 * panels, not single values - an NFS or an ionogramme is a set of results a clinician
 * writes out - and the ones that are single values carry their own units on the lab report.
 * Storing what the report says keeps the record faithful to the paper form; imposing a
 * numeric column per analyte would mean guessing units and losing everything else on the line.
 */
public class LabResult {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Date sampleDate;
    private String completeBloodCount;
    private String crp;
    private String ionogram;
    private String glycemia;
    private String creatinine;
    private String coagulation;
    private String bloodGroup;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Date getSampleDate() { return sampleDate; }
    public void setSampleDate(Date sampleDate) { this.sampleDate = sampleDate; }

    public String getCompleteBloodCount() { return completeBloodCount; }
    public void setCompleteBloodCount(String completeBloodCount) { this.completeBloodCount = completeBloodCount; }

    public String getCrp() { return crp; }
    public void setCrp(String crp) { this.crp = crp; }

    public String getIonogram() { return ionogram; }
    public void setIonogram(String ionogram) { this.ionogram = ionogram; }

    public String getGlycemia() { return glycemia; }
    public void setGlycemia(String glycemia) { this.glycemia = glycemia; }

    public String getCreatinine() { return creatinine; }
    public void setCreatinine(String creatinine) { this.creatinine = creatinine; }

    public String getCoagulation() { return coagulation; }
    public void setCoagulation(String coagulation) { this.coagulation = coagulation; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
