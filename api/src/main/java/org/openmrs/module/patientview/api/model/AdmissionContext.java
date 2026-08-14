package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Motif d'hospitalisation (Fiche de Neurochirurgie, section 2), plus the parts
 * of section 1 (Informations administratives) that are not already covered by
 * OpenMRS's own demographics/visit data: profession, medecin referent,
 * neurochirurgien responsable. One record per admission episode.
 */
public class AdmissionContext {

    private Integer id;
    private String uuid;
    private Patient patient;
    private String admissionReason;
    private Date symptomOnsetDate;
    private Integer diagnosisDelayDays;
    private String admissionDiagnosis;
    private String primaryDiagnosis;
    private String secondaryDiagnoses;
    private String profession;
    private String referringPhysician;
    private String responsibleNeurosurgeon;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getAdmissionReason() { return admissionReason; }
    public void setAdmissionReason(String admissionReason) { this.admissionReason = admissionReason; }

    public Date getSymptomOnsetDate() { return symptomOnsetDate; }
    public void setSymptomOnsetDate(Date symptomOnsetDate) { this.symptomOnsetDate = symptomOnsetDate; }

    public Integer getDiagnosisDelayDays() { return diagnosisDelayDays; }
    public void setDiagnosisDelayDays(Integer diagnosisDelayDays) { this.diagnosisDelayDays = diagnosisDelayDays; }

    public String getAdmissionDiagnosis() { return admissionDiagnosis; }
    public void setAdmissionDiagnosis(String admissionDiagnosis) { this.admissionDiagnosis = admissionDiagnosis; }

    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public void setPrimaryDiagnosis(String primaryDiagnosis) { this.primaryDiagnosis = primaryDiagnosis; }

    public String getSecondaryDiagnoses() { return secondaryDiagnoses; }
    public void setSecondaryDiagnoses(String secondaryDiagnoses) { this.secondaryDiagnoses = secondaryDiagnoses; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getReferringPhysician() { return referringPhysician; }
    public void setReferringPhysician(String referringPhysician) { this.referringPhysician = referringPhysician; }

    public String getResponsibleNeurosurgeon() { return responsibleNeurosurgeon; }
    public void setResponsibleNeurosurgeon(String responsibleNeurosurgeon) { this.responsibleNeurosurgeon = responsibleNeurosurgeon; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
