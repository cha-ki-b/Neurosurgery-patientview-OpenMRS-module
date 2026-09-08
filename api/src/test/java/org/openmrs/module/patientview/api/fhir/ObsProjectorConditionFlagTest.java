package org.openmrs.module.patientview.api.fhir;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Condition;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.dao.PatientviewDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the {@code conditionFlag} field type, which exports a true boolean as an OpenMRS
 * {@link Condition} rather than an {@link org.openmrs.Obs}.
 *
 * <p>It exists because CIEL carries comorbidities and complications as Diagnosis-class concepts
 * with datatype N/A. No observation can hold a value of that datatype, so before this type every
 * one of them was reported as a datatype mismatch and exported nothing - the concept was right
 * and the representation was wrong.
 *
 * <p>These are plain Mockito tests over a static-mocked {@code Context}: no OpenMRS runtime, so
 * they run at unit-test speed and can assert precisely what reaches ConditionService.
 */
public class ObsProjectorConditionFlagTest {

    private static final String HYPERTENSION_CIEL = "117399";

    private MockedStatic<Context> context;
    private PatientviewDao dao;
    private ConceptService conceptService;
    private ConditionService conditionService;
    private EncounterService encounterService;
    private ObsProjector projector;
    private Patient patient;
    private Concept diagnosisConcept;

    @Before
    public void setUp() {
        dao = mock(PatientviewDao.class);
        conceptService = mock(ConceptService.class);
        conditionService = mock(ConditionService.class);
        encounterService = mock(EncounterService.class);

        patient = new Patient();
        patient.setPatientId(1);

        // A Diagnosis-class concept: datatype N/A, exactly what CIEL gives for a comorbidity.
        ConceptDatatype notApplicable = new ConceptDatatype();
        notApplicable.setName("N/A");
        diagnosisConcept = new Concept();
        diagnosisConcept.setConceptId(117399);
        diagnosisConcept.setDatatype(notApplicable);

        EncounterType encounterType = new EncounterType();
        encounterType.setName("Neurosurgery Fiche");

        context = Mockito.mockStatic(Context.class);
        context.when(Context::getConceptService).thenReturn(conceptService);
        context.when(Context::getConditionService).thenReturn(conditionService);
        context.when(Context::getEncounterService).thenReturn(encounterService);

        when(encounterService.getEncounterTypeByUuid(any(String.class))).thenReturn(encounterType);
        when(encounterService.saveEncounter(any(Encounter.class)))
                .thenAnswer(invocation -> {
                    Encounter saved = invocation.getArgument(0);
                    saved.setUuid(UUID.randomUUID().toString());
                    return saved;
                });
        when(dao.getProjectedSourceUuids(any(Patient.class), any(String.class)))
                .thenReturn(new ArrayList<String>());

        projector = new ObsProjector();
        projector.setDao(dao);
    }

    @After
    public void tearDown() {
        context.close();
    }

    private void givenMedicalHistory(Object hypertensionValue) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("uuid", "row-uuid-1");
        row.put("dateChanged", new java.util.Date());
        row.put("hypertension", hypertensionValue);
        when(dao.getMedicalHistoryVersions(patient))
                .thenReturn(Collections.singletonList(row));
    }

    @Test
    public void aTrueFlagBecomesACodedCondition() {
        givenMedicalHistory(Boolean.TRUE);
        when(conceptService.getConceptByMapping(HYPERTENSION_CIEL, "CIEL"))
                .thenReturn(diagnosisConcept);

        Map<String, Object> summary = projector.projectSet(patient, "patientview.medicalHistory");

        ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
        verify(conditionService).saveCondition(captor.capture());
        Condition saved = captor.getValue();

        assertEquals("the condition must be attached to the patient", patient, saved.getPatient());
        assertNotNull("a coded condition must carry a CodedOrFreeText", saved.getCondition());
        assertEquals("Condition.code must be the CIEL diagnosis concept, not free text",
                diagnosisConcept, saved.getCondition().getCoded());
        assertNotNull("the condition needs an onset date", saved.getOnsetDate());
        assertEquals(1, summary.get("conditions"));
    }

    /**
     * The form cannot distinguish "no" from "not assessed", so exporting a false flag would
     * assert a clinical negative nobody recorded.
     */
    @Test
    public void aFalseFlagExportsNothingAtAll() {
        givenMedicalHistory(Boolean.FALSE);

        Map<String, Object> summary = projector.projectSet(patient, "patientview.medicalHistory");

        verify(conditionService, never()).saveCondition(any(Condition.class));
        verify(encounterService, never()).saveEncounter(any(Encounter.class));
        assertEquals("nothing resolved, so no encounter and no ledger entry", 0,
                summary.get("encounters"));
        verify(dao, never()).saveFhirProjection(any());
    }

    /**
     * A dictionary that lacks the concept must be reported, never thrown - a clinical save runs
     * in the same transaction and must not be lost to a missing concept.
     */
    @Test
    public void anUnresolvableConceptIsReportedNotThrown() {
        givenMedicalHistory(Boolean.TRUE);
        when(conceptService.getConceptByMapping(HYPERTENSION_CIEL, "CIEL")).thenReturn(null);

        Map<String, Object> summary = projector.projectSet(patient, "patientview.medicalHistory");

        verify(conditionService, never()).saveCondition(any(Condition.class));
        Object unresolved = summary.get("unresolved");
        assertNotNull("the unresolvable field must be reported", unresolved);
        assertTrue("the report must name the field",
                unresolved.toString().contains("hypertension"));
    }

    /** One row can raise several conditions - medical history carries three comorbidities. */
    @Test
    public void severalFlagsOnOneRowBecomeSeveralConditions() {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("uuid", "row-uuid-2");
        row.put("dateChanged", new java.util.Date());
        row.put("hypertension", Boolean.TRUE);
        row.put("stroke", Boolean.TRUE);
        row.put("renalFailure", Boolean.TRUE);
        when(dao.getMedicalHistoryVersions(patient)).thenReturn(Collections.singletonList(row));
        when(conceptService.getConceptByMapping(any(String.class), any(String.class)))
                .thenReturn(diagnosisConcept);

        Map<String, Object> summary = projector.projectSet(patient, "patientview.medicalHistory");

        List<Condition> saved = new ArrayList<Condition>();
        ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
        verify(conditionService, Mockito.atLeast(3)).saveCondition(captor.capture());
        saved.addAll(captor.getAllValues());
        assertTrue("all three comorbidities should have been exported", saved.size() >= 3);
        assertEquals("a single encounter carries them all", 1, summary.get("encounters"));
        verify(dao).saveFhirProjection(any());
    }
}
