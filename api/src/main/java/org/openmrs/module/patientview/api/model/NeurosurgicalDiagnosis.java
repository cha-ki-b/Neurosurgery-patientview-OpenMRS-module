package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Diagnostic neurochirurgical (Fiche de Neurochirurgie, section 8).
 * Append-only: one record per diagnosis update, most recent first.
 */
public class NeurosurgicalDiagnosis {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String diagnosis;
    private String lesionLocation;
    private String laterality;
    private String size;
    private Integer lesionCount;
    private String whoDiagnosis;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getLesionLocation() { return lesionLocation; }
    public void setLesionLocation(String lesionLocation) { this.lesionLocation = lesionLocation; }

    public String getLaterality() { return laterality; }
    public void setLaterality(String laterality) { this.laterality = laterality; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Integer getLesionCount() { return lesionCount; }
    public void setLesionCount(Integer lesionCount) { this.lesionCount = lesionCount; }

    public String getWhoDiagnosis() { return whoDiagnosis; }
    public void setWhoDiagnosis(String whoDiagnosis) { this.whoDiagnosis = whoDiagnosis; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
