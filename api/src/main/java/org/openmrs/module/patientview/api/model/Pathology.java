package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Anatomopathologie (Fiche de Neurochirurgie, section 10).
 * Append-only: one record per report, most recent first.
 */
public class Pathology {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String studyFindings;
    private String whoGrade;
    private String immunohistochemistry;
    private String molecularMarkers;
    private String finalDiagnosis;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getStudyFindings() { return studyFindings; }
    public void setStudyFindings(String studyFindings) { this.studyFindings = studyFindings; }

    public String getWhoGrade() { return whoGrade; }
    public void setWhoGrade(String whoGrade) { this.whoGrade = whoGrade; }

    public String getImmunohistochemistry() { return immunohistochemistry; }
    public void setImmunohistochemistry(String immunohistochemistry) { this.immunohistochemistry = immunohistochemistry; }

    public String getMolecularMarkers() { return molecularMarkers; }
    public void setMolecularMarkers(String molecularMarkers) { this.molecularMarkers = molecularMarkers; }

    public String getFinalDiagnosis() { return finalDiagnosis; }
    public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
