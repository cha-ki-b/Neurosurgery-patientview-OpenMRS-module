package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;

import java.util.Date;

/**
 * Ledger of which patientview rows have already been projected into the core clinical model
 * (see {@code ObsProjector}). One row per projected source row.
 * <p>
 * This exists so projection is idempotent. Every patientview entity is append-only, so the
 * projector can simply ask "which of this patient's rows for this set are not in here yet?"
 * and project those - which makes the live path after a save and the one-off backfill of
 * historical rows the exact same operation, and makes re-running either one safe.
 * <p>
 * It is also the audit trail: it records which encounter carries each Fiche row's observations.
 * Append-only like the rest of the module.
 */
public class FhirProjection {

    private Integer id;
    private String uuid;
    private Patient patient;
    /** Manifest set id, e.g. "patientview.vitalSigns". */
    private String sourceSet;
    /** uuid of the patientview row this projection came from. */
    private String sourceUuid;
    /** uuid of the Encounter the projected observations were attached to. */
    private String encounterUuid;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getSourceSet() { return sourceSet; }
    public void setSourceSet(String sourceSet) { this.sourceSet = sourceSet; }

    public String getSourceUuid() { return sourceUuid; }
    public void setSourceUuid(String sourceUuid) { this.sourceUuid = sourceUuid; }

    public String getEncounterUuid() { return encounterUuid; }
    public void setEncounterUuid(String encounterUuid) { this.encounterUuid = encounterUuid; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
