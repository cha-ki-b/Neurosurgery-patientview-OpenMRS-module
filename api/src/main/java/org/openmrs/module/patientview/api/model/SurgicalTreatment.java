package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Traitement chirurgical (Fiche de Neurochirurgie, section 9 - Prise en charge).
 * <p>
 * Deliberately a separate entity from {@link SurgicalHistory}, not a reuse of it: that one
 * is section 3, the patient's <em>ant\u00e9c\u00e9dents chirurgicaux</em> - operations from before
 * this episode, often elsewhere and by other teams. This one is the intervention performed
 * as part of the current neurosurgical management, which is why it carries operative detail
 * (ex\u00e9r\u00e8se type, duration, per-operative complications, drain/catheter) that a history
 * entry has no place for. Folding them together would have meant a table where half the
 * columns are always null depending on which section produced the row.
 * <p>
 * Append-only: one row per operation.
 */
public class SurgicalTreatment {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String procedurePerformed;
    private String resectionType;
    private Date surgeryDate;
    private Integer durationMinutes;
    private String surgeon;
    private String intraoperativeComplications;
    private boolean drainPlaced;
    private boolean catheterPlaced;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getProcedurePerformed() { return procedurePerformed; }
    public void setProcedurePerformed(String procedurePerformed) { this.procedurePerformed = procedurePerformed; }

    public String getResectionType() { return resectionType; }
    public void setResectionType(String resectionType) { this.resectionType = resectionType; }

    public Date getSurgeryDate() { return surgeryDate; }
    public void setSurgeryDate(Date surgeryDate) { this.surgeryDate = surgeryDate; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getSurgeon() { return surgeon; }
    public void setSurgeon(String surgeon) { this.surgeon = surgeon; }

    public String getIntraoperativeComplications() { return intraoperativeComplications; }
    public void setIntraoperativeComplications(String intraoperativeComplications) { this.intraoperativeComplications = intraoperativeComplications; }

    public boolean isDrainPlaced() { return drainPlaced; }
    public void setDrainPlaced(boolean drainPlaced) { this.drainPlaced = drainPlaced; }

    public boolean isCatheterPlaced() { return catheterPlaced; }
    public void setCatheterPlaced(boolean catheterPlaced) { this.catheterPlaced = catheterPlaced; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
