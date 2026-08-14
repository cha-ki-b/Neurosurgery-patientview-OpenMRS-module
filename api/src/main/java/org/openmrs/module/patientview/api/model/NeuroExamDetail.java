package org.openmrs.module.patientview.api.model;

import org.openmrs.Patient;
import org.openmrs.User;

import java.util.Date;

/**
 * Examen neurologique detaille (Fiche de Neurochirurgie, section 5, beyond
 * the Glasgow Coma Scale which is already tracked by NeuroAssessment).
 * One record per exam; the full history is kept.
 */
public class NeuroExamDetail {

    private Integer id;
    private String uuid;
    private Patient patient;
    private Date examDate;
    private String orientation;
    private String language;
    private String memory;
    private String cranialNerves;
    private String motorDeficit;
    private String sensoryDeficit;
    private String reflexes;
    private String plantarReflex;
    private String coordination;
    private String gait;
    private String balance;
    private boolean cerebellarSigns;
    private boolean pyramidalSyndrome;
    private boolean meningealSyndrome;
    private boolean seizures;
    private boolean headache;
    private boolean vomiting;
    private boolean visualDisturbances;
    private boolean sphincterDisturbances;
    private String notes;
    private User creator;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }

    public String getCranialNerves() { return cranialNerves; }
    public void setCranialNerves(String cranialNerves) { this.cranialNerves = cranialNerves; }

    public String getMotorDeficit() { return motorDeficit; }
    public void setMotorDeficit(String motorDeficit) { this.motorDeficit = motorDeficit; }

    public String getSensoryDeficit() { return sensoryDeficit; }
    public void setSensoryDeficit(String sensoryDeficit) { this.sensoryDeficit = sensoryDeficit; }

    public String getReflexes() { return reflexes; }
    public void setReflexes(String reflexes) { this.reflexes = reflexes; }

    public String getPlantarReflex() { return plantarReflex; }
    public void setPlantarReflex(String plantarReflex) { this.plantarReflex = plantarReflex; }

    public String getCoordination() { return coordination; }
    public void setCoordination(String coordination) { this.coordination = coordination; }

    public String getGait() { return gait; }
    public void setGait(String gait) { this.gait = gait; }

    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }

    public boolean isCerebellarSigns() { return cerebellarSigns; }
    public void setCerebellarSigns(boolean cerebellarSigns) { this.cerebellarSigns = cerebellarSigns; }

    public boolean isPyramidalSyndrome() { return pyramidalSyndrome; }
    public void setPyramidalSyndrome(boolean pyramidalSyndrome) { this.pyramidalSyndrome = pyramidalSyndrome; }

    public boolean isMeningealSyndrome() { return meningealSyndrome; }
    public void setMeningealSyndrome(boolean meningealSyndrome) { this.meningealSyndrome = meningealSyndrome; }

    public boolean isSeizures() { return seizures; }
    public void setSeizures(boolean seizures) { this.seizures = seizures; }

    public boolean isHeadache() { return headache; }
    public void setHeadache(boolean headache) { this.headache = headache; }

    public boolean isVomiting() { return vomiting; }
    public void setVomiting(boolean vomiting) { this.vomiting = vomiting; }

    public boolean isVisualDisturbances() { return visualDisturbances; }
    public void setVisualDisturbances(boolean visualDisturbances) { this.visualDisturbances = visualDisturbances; }

    public boolean isSphincterDisturbances() { return sphincterDisturbances; }
    public void setSphincterDisturbances(boolean sphincterDisturbances) { this.sphincterDisturbances = sphincterDisturbances; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
