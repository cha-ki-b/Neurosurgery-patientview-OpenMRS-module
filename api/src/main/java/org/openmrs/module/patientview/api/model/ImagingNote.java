package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Imagerie (Fiche de Neurochirurgie, section 6) - the clinician's reading of an imaging
 * study, not the study itself.
 * <p>
 * The images and their DICOM metadata stay in Orthanc, surfaced through the separate
 * {@code imaging} module (see {@code DicomStudyBridge}); Orthanc remains the single source
 * of truth for them and nothing about them is duplicated here. What this table holds is the
 * compte rendu the fiche asks for - the interpretation a neurosurgeon writes down, which
 * exists whether or not the study was ever pushed to the PACS.
 * <p>
 * Append-only: one row per note.
 */
public class ImagingNote {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String examType;
    private Date examDate;
    private String findings;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
