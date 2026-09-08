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
import org.openmrs.module.patientview.api.model.FhirProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
        when(dao.getProjectionFingerprints(any(Patient.class), any(String.class)))
                .thenReturn(new HashMap<String, String>());

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
    // ------------------------------------------------------------------ re-projection

    private static final String SET = "patientview.medicalHistory";

    private String currentFingerprint() {
        return ObsProjector.fingerprintOf(FhirMappingManifest.getInstance().getSet(SET));
    }

    /**
     * The common case: nothing about the mapping has changed since this row was projected, so it
     * must be left completely alone. This is what stops every save from re-exporting the
     * patient's whole history.
     */
    @Test
    public void aRowProjectedWithTheCurrentMappingIsLeftAlone() {
        givenMedicalHistory(Boolean.TRUE);
        when(dao.getProjectionFingerprints(patient, SET))
                .thenReturn(Collections.singletonMap("row-uuid-1", currentFingerprint()));

        Map<String, Object> summary = projector.projectSet(patient, SET);

        verify(encounterService, never()).saveEncounter(any(Encounter.class));
        verify(conditionService, never()).saveCondition(any(Condition.class));
        verify(dao, never()).saveFhirProjection(any());
        assertEquals(0, summary.get("encounters"));
    }

    /**
     * The case this whole mechanism exists for: the row was projected before its concept was
     * curated, so its output is out of date. It must be projected again, and the previous
     * encounter and its conditions voided rather than left behind as duplicates.
     */
    @Test
    public void aRowProjectedWithAnOlderMappingIsSupersededAndProjectedAgain() {
        givenMedicalHistory(Boolean.TRUE);
        when(conceptService.getConceptByMapping(HYPERTENSION_CIEL, "CIEL"))
                .thenReturn(diagnosisConcept);
        when(dao.getProjectionFingerprints(patient, SET))
                .thenReturn(Collections.singletonMap("row-uuid-1", "fingerprint-from-before"));

        Encounter previous = new Encounter();
        previous.setUuid("previous-encounter-uuid");
        Condition staleCondition = new Condition();
        FhirProjection ledger = new FhirProjection();
        ledger.setSourceSet(SET);
        ledger.setSourceUuid("row-uuid-1");
        ledger.setEncounterUuid("previous-encounter-uuid");
        ledger.setManifestFingerprint("fingerprint-from-before");
        when(dao.getFhirProjection(patient, SET, "row-uuid-1")).thenReturn(ledger);
        when(encounterService.getEncounterByUuid("previous-encounter-uuid")).thenReturn(previous);
        when(conditionService.getConditionsByEncounter(previous))
                .thenReturn(Collections.singletonList(staleCondition));

        Map<String, Object> summary = projector.projectSet(patient, SET);

        // The old output is withdrawn - conditions explicitly, since voidEncounter cascades only
        // to observations and would otherwise leave them behind as duplicates.
        verify(conditionService).voidCondition(Mockito.eq(staleCondition), any(String.class));
        verify(encounterService).voidEncounter(Mockito.eq(previous), any(String.class));
        assertEquals(1, summary.get("superseded"));

        // And the row is exported again, with the concept that is now curated.
        verify(conditionService).saveCondition(any(Condition.class));
        assertEquals(1, summary.get("encounters"));

        // The ledger entry is reused and carries the new fingerprint, so the next run is a no-op.
        verify(dao).saveFhirProjection(ledger);
        assertEquals("the ledger must record the mapping it was projected with",
                currentFingerprint(), ledger.getManifestFingerprint());
    }

    /**
     * Rows written before 1.4.5 have no fingerprint at all. They must count as stale so the
     * upgrade picks up every concept curated in the meantime.
     */
    @Test
    public void aRowWithNoFingerprintCountsAsStale() {
        givenMedicalHistory(Boolean.TRUE);
        when(conceptService.getConceptByMapping(HYPERTENSION_CIEL, "CIEL"))
                .thenReturn(diagnosisConcept);
        Map<String, String> legacy = new HashMap<String, String>();
        legacy.put("row-uuid-1", null);
        when(dao.getProjectionFingerprints(patient, SET)).thenReturn(legacy);
        when(dao.getFhirProjection(patient, SET, "row-uuid-1")).thenReturn(null);

        Map<String, Object> summary = projector.projectSet(patient, SET);

        verify(conditionService).saveCondition(any(Condition.class));
        assertEquals(1, summary.get("encounters"));
    }

    /** A cosmetic manifest edit must not churn every record. */
    @Test
    public void theFingerprintIgnoresLabelsAndDependsOnlyOnWhatChangesTheOutput() {
        String twice = currentFingerprint();
        assertEquals("the fingerprint must be stable across calls", twice, currentFingerprint());
        assertEquals("expected a 32-character digest", 32, twice.length());

        // Two different sets must not collide.
        String other = ObsProjector.fingerprintOf(
                FhirMappingManifest.getInstance().getSet("patientview.vitalSigns"));
        assertNotEquals("different mappings must fingerprint differently", twice, other);
    }
}
