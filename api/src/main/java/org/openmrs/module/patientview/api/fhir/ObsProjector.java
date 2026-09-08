package org.openmrs.module.patientview.api.fhir;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.dao.PatientviewDao;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.ConceptRef;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.FieldSpec;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.SetSpec;
import org.openmrs.module.patientview.api.model.FhirProjection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Writes patientview's clinical rows into the core OpenMRS model - {@code Encounter},
 * {@code Obs}, {@code Condition} - so the FHIR2 module can serve them.
 * <p>
 * <b>This class contains no reference to FHIR2, deliberately.</b> It calls only ConceptService,
 * EncounterService, ObsService (via encounter cascade) and ConditionService, which are core
 * platform APIs stable across the whole 2.x line. FHIR2 reads the core model by itself. The one
 * thing that has broken downstream modules across fhir2 major versions is its DAO layer, and
 * nothing here touches it - which is what makes this survive an upgrade to fhir2 4.x untouched.
 * <p>
 * <b>Idempotent by construction.</b> Every patientview entity is append-only, so "project this
 * patient's rows that are not in the ledger yet" is a complete description of the work. That
 * makes the live path (after a save) and the backfill path (historical rows) the same operation,
 * makes re-running either safe, and makes it self-healing: a row whose projection failed is
 * retried the next time anything for that patient is saved. It is also why ordering does not
 * matter - a back-dated record is picked up just as reliably as the newest one.
 * <p>
 * <b>A dictionary gap can never fail a clinical save.</b> Every reason a field might not be
 * exportable - no concept curated yet, a declared concept that does not resolve here, a concept
 * whose datatype disagrees with the stored value, a boolean concept with no true-concept
 * configured - is checked <i>before</i> any obs is built, and reported rather than thrown. So the
 * export cannot poison the transaction that is saving the clinical record.
 * <p>
 * The two entry points deliberately differ in propagation, and the difference matters:
 * <ul>
 *   <li>{@link #projectSet} runs in the caller's transaction ({@code REQUIRED}) because it is
 *       called straight after a save and <i>must</i> see the row that save has just written.
 *       Suspending that transaction would leave the new row invisible and silently push every
 *       projection one save behind.</li>
 *   <li>{@link #projectPatient} takes {@link Propagation#REQUIRES_NEW}, because it only ever
 *       reads already-committed historical rows, and the server-wide backfill needs one
 *       patient's failure not to abandon the rest of the sweep.</li>
 * </ul>
 * A genuine platform failure during the export is left to propagate: rolling the whole unit back
 * is the safe direction, and it surfaces the real cause instead of an opaque
 * {@code UnexpectedRollbackException} at commit time.
 */
public class ObsProjector {

    protected final Log log = LogFactory.getLog(this.getClass());

    /** The limited getters take a max; projection always wants the whole history. */
    private static final int ALL_ROWS = Integer.MAX_VALUE;

    private static final String SUPERSEDED =
            "superseded by patientview re-projection after a mapping change";

    private PatientviewDao dao;

    public void setDao(PatientviewDao dao) {
        this.dao = dao;
    }

    // ------------------------------------------------------------------ entry points

    /**
     * Projects every not-yet-projected row of one set for one patient. Called after each save,
     * and by the backfill.
     */
    @Transactional
    public Map<String, Object> projectSet(Patient patient, String setId) {
        Map<String, Object> summary = newSummary();
        if (patient == null || dao == null) {
            return summary;
        }
        SetSpec spec = FhirMappingManifest.getInstance().getSet(setId);
        if (spec == null) {
            log.warn("No FHIR mapping declared for set " + setId);
            return summary;
        }
        EncounterType encounterType = resolveEncounterType();
        if (encounterType == null) {
            summary.put("encounterTypeMissing", true);
            return summary;
        }
        Map<String, String> projected = dao.getProjectionFingerprints(patient, setId);
        String fingerprint = fingerprintOf(spec);
        Map<String, Concept> conceptCache = new HashMap<String, Concept>();
        for (Map<String, Object> row : fetchRows(patient, setId)) {
            Object sourceUuid = row.get("uuid");
            if (sourceUuid == null) {
                continue;
            }
            String uuid = sourceUuid.toString();
            if (projected.containsKey(uuid)) {
                if (fingerprint.equals(projected.get(uuid))) {
                    continue;
                }
                // The mapping changed since this row was projected - a concept curated, a field
                // retyped - so its previous output is out of date. Void it and project again,
                // rather than leaving the row stuck with whatever resolved at the time.
                supersede(patient, setId, uuid, summary);
            }
            projectRow(patient, spec, row, uuid, encounterType, conceptCache, summary,
                    fingerprint);
        }
        return summary;
    }

    /** Projects every set for one patient. The per-patient backfill. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> projectPatient(Patient patient) {
        Map<String, Object> total = newSummary();
        for (SetSpec spec : FhirMappingManifest.getInstance().getSets()) {
            merge(total, projectSet(patient, spec.getId()));
        }
        return total;
    }

    // ------------------------------------------------------------------ one row

    private void projectRow(Patient patient, SetSpec spec, Map<String, Object> row,
                            String sourceUuid, EncounterType encounterType,
                            Map<String, Concept> conceptCache, Map<String, Object> summary,
                            String fingerprint) {
        Date when = asDate(row.get(spec.getDateKey()));
        if (when == null) {
            when = asDate(row.get("dateCreated"));
        }
        if (when == null) {
            when = new Date();
        }

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setEncounterType(encounterType);
        encounter.setEncounterDatetime(when);

        Obs group = null;
        if (!spec.getGroupConcept().isEmpty()) {
            Concept groupConcept = resolve(spec.getGroupConcept(), conceptCache);
            if (groupConcept != null) {
                group = new Obs();
                group.setPerson(patient);
                group.setConcept(groupConcept);
                group.setObsDatetime(when);
            }
        }

        int written = 0;
        List<Condition> conditions = new ArrayList<Condition>();
        for (FieldSpec field : spec.getFields()) {
            Object value = row.get(field.getId());
            if (isBlank(value)) {
                continue;
            }
            if ("condition".equals(field.getType())) {
                conditions.add(buildCondition(patient, spec, row, value, when));
                continue;
            }
            if ("conditionFlag".equals(field.getType())) {
                // Only a true flag is exported. The form cannot distinguish "no" from "not
                // assessed", so asserting the negative would invent a clinical finding.
                if (!Boolean.TRUE.equals(value)) {
                    continue;
                }
                if (!field.isCurated()) {
                    addTo(summary, "uncurated", spec.getId() + "." + field.getId());
                    continue;
                }
                Concept flagConcept = resolve(field.getConcept(), conceptCache);
                if (flagConcept == null) {
                    addTo(summary, "unresolved", spec.getId() + "." + field.getId()
                            + " (" + field.getConcept() + ")");
                    continue;
                }
                conditions.add(buildCodedCondition(patient, flagConcept, when));
                continue;
            }
            if (!field.isCurated()) {
                addTo(summary, "uncurated", spec.getId() + "." + field.getId());
                continue;
            }
            Concept concept = resolve(field.getConcept(), conceptCache);
            if (concept == null) {
                addTo(summary, "unresolved", spec.getId() + "." + field.getId()
                        + " (" + field.getConcept() + ")");
                continue;
            }
            Obs obs = buildObs(patient, concept, when, field, value, summary, spec);
            if (obs == null) {
                continue;
            }
            if (group != null) {
                group.addGroupMember(obs);
            } else {
                encounter.addObs(obs);
            }
            written++;
        }

        if (written == 0 && conditions.isEmpty()) {
            // Nothing resolved for this row - write no empty encounter, and leave it out of the
            // ledger so it is retried once its concepts are curated.
            addTo(summary, "skippedRows", spec.getId());
            return;
        }
        if (group != null && written > 0) {
            encounter.addObs(group);
        }

        Encounter saved = Context.getEncounterService().saveEncounter(encounter);
        for (Condition condition : conditions) {
            condition.setEncounter(saved);
            Context.getConditionService().saveCondition(condition);
            increment(summary, "conditions");
        }

        // Reuse the existing entry when re-projecting: one ledger row per source row is what the
        // unique index guarantees, and the superseded encounter carries its own void reason.
        FhirProjection ledger = dao.getFhirProjection(patient, spec.getId(), sourceUuid);
        if (ledger == null) {
            ledger = new FhirProjection();
            ledger.setUuid(UUID.randomUUID().toString());
            ledger.setPatient(patient);
            ledger.setSourceSet(spec.getId());
            ledger.setSourceUuid(sourceUuid);
        }
        ledger.setEncounterUuid(saved.getUuid());
        ledger.setManifestFingerprint(fingerprint);
        ledger.setDateCreated(new Date());
        dao.saveFhirProjection(ledger);

        increment(summary, "encounters");
        add(summary, "observations", written);
    }

    /**
     * OpenMRS Condition accepts free text via {@link CodedOrFreeText#setNonCoded}, and FHIR2
     * renders that as {@code Condition.code.text} - so section 8 exports correctly with no
     * dictionary curation at all, unlike every observation-backed set.
     */
    private Condition buildCondition(Patient patient, SetSpec spec, Map<String, Object> row,
                                     Object value, Date when) {
        CodedOrFreeText coded = new CodedOrFreeText();
        coded.setNonCoded(value.toString().trim());

        Condition condition = new Condition();
        condition.setPatient(patient);
        condition.setCondition(coded);
        condition.setClinicalStatus(ConditionClinicalStatus.ACTIVE);
        condition.setOnsetDate(when);

        String detail = describeLesion(spec, row);
        if (!detail.isEmpty()) {
            condition.setAdditionalDetail(detail);
        }
        return condition;
    }

    /**
     * A Condition carrying a coded diagnosis, for a boolean flag such as a comorbidity or a
     * post-operative complication.
     * <p>
     * These cannot be observations. CIEL models them as Diagnosis-class concepts with datatype
     * N/A, and an Obs must hold a value of the concept's datatype - so the projector would refuse
     * every one of them. Representing "this patient has hydrocephalus" as a Condition is both the
     * only thing that works and the correct FHIR modelling; FHIR2 serves it as a Condition
     * resource with a coded {@code Condition.code}.
     */
    private Condition buildCodedCondition(Patient patient, Concept concept, Date when) {
        CodedOrFreeText coded = new CodedOrFreeText();
        coded.setCoded(concept);

        Condition condition = new Condition();
        condition.setPatient(patient);
        condition.setCondition(coded);
        condition.setClinicalStatus(ConditionClinicalStatus.ACTIVE);
        condition.setOnsetDate(when);
        return condition;
    }

    /** Folds the lesion descriptors into Condition.additionalDetail, since they have no code. */
    private String describeLesion(SetSpec spec, Map<String, Object> row) {
        StringBuilder detail = new StringBuilder();
        for (FieldSpec field : spec.getFields()) {
            if ("condition".equals(field.getType()) || "conditionFlag".equals(field.getType())) {
                continue;
            }
            Object value = row.get(field.getId());
            if (isBlank(value)) {
                continue;
            }
            if (detail.length() > 0) {
                detail.append("; ");
            }
            detail.append(field.getName()).append(": ").append(value.toString().trim());
        }
        return detail.toString();
    }

    private Obs buildObs(Patient patient, Concept concept, Date when, FieldSpec field,
                         Object value, Map<String, Object> summary, SetSpec spec) {
        ConceptDatatype datatype = concept.getDatatype();
        if (datatype == null) {
            return null;
        }
        Obs obs = new Obs();
        obs.setPerson(patient);
        obs.setConcept(concept);
        obs.setObsDatetime(when);

        String type = field.getType();
        if ("numeric".equals(type) && datatype.isNumeric() && value instanceof Number) {
            obs.setValueNumeric(((Number) value).doubleValue());
        } else if ("text".equals(type) && datatype.isText()) {
            obs.setValueText(value.toString().trim());
        } else if ("date".equals(type) && (datatype.isDate() || datatype.isDateTime())
                && value instanceof Date) {
            obs.setValueDatetime((Date) value);
        } else if ("boolean".equals(type) && Boolean.TRUE.equals(value)) {
            if (datatype.isBoolean()) {
                obs.setValueBoolean(Boolean.TRUE);
            } else if (datatype.isCoded()) {
                Concept trueConcept = Context.getConceptService().getTrueConcept();
                if (trueConcept == null) {
                    // The concept.true global property is not configured on this server. An obs
                    // with no value would make saveEncounter throw and take the clinical save
                    // down with it, so report and skip instead.
                    addTo(summary, "datatypeMismatch", spec.getId() + "." + field.getId()
                            + " (coded boolean, but no concept.true is configured)");
                    return null;
                }
                obs.setValueCoded(trueConcept);
            } else {
                return mismatch(summary, spec, field, datatype);
            }
        } else {
            // Reported, never coerced: silently forcing a text lab result into a numeric concept
            // would corrupt the record. The coverage report surfaces these for curation.
            return mismatch(summary, spec, field, datatype);
        }
        return obs;
    }

    private Obs mismatch(Map<String, Object> summary, SetSpec spec, FieldSpec field,
                         ConceptDatatype datatype) {
        addTo(summary, "datatypeMismatch", spec.getId() + "." + field.getId()
                + " (field is " + field.getType() + ", concept is " + datatype.getName() + ")");
        return null;
    }

    /**
     * Voids the output of a previous projection of one row, so re-projecting supersedes it
     * instead of duplicating it. Voiding rather than deleting keeps the audit trail: the void
     * reason records why the encounter was replaced, which is where the history of a superseded
     * projection lives now that the ledger row itself is reused.
     */
    private void supersede(Patient patient, String setId, String sourceUuid,
                           Map<String, Object> summary) {
        FhirProjection existing = dao.getFhirProjection(patient, setId, sourceUuid);
        if (existing == null || existing.getEncounterUuid() == null) {
            return;
        }
        Encounter previous = Context.getEncounterService()
                .getEncounterByUuid(existing.getEncounterUuid());
        if (previous == null || Boolean.TRUE.equals(previous.getVoided())) {
            return;
        }
        // Conditions are linked to the encounter but are not voided by voidEncounter, which
        // cascades only to observations - so they have to be voided explicitly or they would
        // survive as duplicates of the ones the re-projection is about to create.
        for (Condition condition : Context.getConditionService().getConditionsByEncounter(previous)) {
            Context.getConditionService().voidCondition(condition, SUPERSEDED);
        }
        Context.getEncounterService().voidEncounter(previous, SUPERSEDED);
        increment(summary, "superseded");
    }

    /**
     * A stable digest of everything about a set's mapping that changes what a projection would
     * produce: each field's id, type and ordered concept references, plus the group concept and
     * the date key. Curating a concept changes it; editing a label or a comment does not, so
     * cosmetic manifest edits do not trigger a pointless re-projection of every record.
     */
    static String fingerprintOf(SetSpec spec) {
        StringBuilder material = new StringBuilder();
        material.append(spec.getId()).append('|').append(spec.getDateKey()).append('|');
        for (ConceptRef ref : spec.getGroupConcept()) {
            material.append(ref).append(',');
        }
        material.append('|');
        for (FieldSpec field : spec.getFields()) {
            material.append(field.getId()).append(':').append(field.getType()).append('=');
            for (ConceptRef ref : field.getConcept()) {
                material.append(ref).append(',');
            }
            material.append(';');
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.toString().getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            // No digest available is not a reason to stop exporting; fall back to the raw
            // material, which is longer but just as correct a change detector.
            return material.toString();
        }
    }

    // ------------------------------------------------------------------ coverage report

    /**
     * Read-only audit of how much of the manifest is usable on this server: which fields still
     * need a concept, and which declared concepts do not resolve against the loaded dictionary.
     * Writes nothing.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCoverageReport() {
        Map<String, Concept> cache = new HashMap<String, Concept>();
        List<Map<String, Object>> sets = new ArrayList<Map<String, Object>>();
        int fields = 0;
        int curated = 0;
        int resolvable = 0;

        for (SetSpec spec : FhirMappingManifest.getInstance().getSets()) {
            List<String> needsConcept = new ArrayList<String>();
            List<String> notResolvable = new ArrayList<String>();
            int setResolvable = 0;
            for (FieldSpec field : spec.getFields()) {
                if ("condition".equals(field.getType())) {
                    continue;
                }
                if ("conditionFlag".equals(field.getType())) {
                    // No datatype to satisfy - the concept becomes Condition.code, not an obs value.
                    fields++;
                    if (field.isCurated()) {
                        curated++;
                        if (resolve(field.getConcept(), cache) != null) {
                            setResolvable++;
                            resolvable++;
                        } else {
                            notResolvable.add(field.getId() + " (" + field.getConcept() + ")");
                        }
                    } else {
                        needsConcept.add(field.getId() + " \u2014 " + field.getName());
                    }
                    continue;
                }
                fields++;
                if (!field.isCurated()) {
                    needsConcept.add(field.getId() + " — " + field.getName());
                    continue;
                }
                curated++;
                if (resolve(field.getConcept(), cache) == null) {
                    notResolvable.add(field.getId() + " (" + field.getConcept() + ")");
                } else {
                    setResolvable++;
                    resolvable++;
                }
            }
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("set", spec.getId());
            entry.put("ficheSection", spec.getFicheSection());
            entry.put("target", spec.getTarget());
            entry.put("resolvableFields", setResolvable);
            entry.put("needsConcept", needsConcept);
            entry.put("declaredButNotResolvable", notResolvable);
            entry.put("exportsToday", setResolvable > 0 || hasConditionField(spec));
            sets.add(entry);
        }

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("encounterTypePresent", resolveEncounterType() != null);
        report.put("totalFields", fields);
        report.put("fieldsWithAConcept", curated);
        report.put("fieldsResolvableHere", resolvable);
        report.put("fieldsAwaitingCuration", fields - curated);
        report.put("sets", sets);
        return report;
    }

    private boolean hasConditionField(SetSpec spec) {
        for (FieldSpec field : spec.getFields()) {
            if ("condition".equals(field.getType())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private EncounterType resolveEncounterType() {
        FhirMappingManifest manifest = FhirMappingManifest.getInstance();
        EncounterType type = Context.getEncounterService()
                .getEncounterTypeByUuid(manifest.getEncounterTypeUuid());
        if (type == null) {
            type = Context.getEncounterService().getEncounterType(manifest.getEncounterTypeName());
        }
        return type;
    }

    private Concept resolve(List<ConceptRef> refs, Map<String, Concept> cache) {
        for (ConceptRef ref : refs) {
            String key = ref.toString();
            if (cache.containsKey(key)) {
                Concept cached = cache.get(key);
                if (cached != null) {
                    return cached;
                }
                continue;
            }
            Concept found = null;
            try {
                found = Context.getConceptService()
                        .getConceptByMapping(ref.getCode(), ref.getSource());
            } catch (Exception e) {
                log.debug("Could not resolve concept " + key, e);
            }
            cache.put(key, found);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private List<Map<String, Object>> fetchRows(Patient patient, String setId) {
        switch (setId) {
            case "patientview.admissionContext":
                return dao.getAdmissionContexts(patient);
            case "patientview.medicalHistory":
                return dao.getMedicalHistoryVersions(patient);
            case "patientview.surgicalHistory":
                return dao.getSurgicalHistory(patient);
            case "patientview.vitalSigns":
                return dao.getVitalSigns(patient, ALL_ROWS);
            case "patientview.neuroAssessment":
                return dao.getNeuroAssessments(patient, ALL_ROWS);
            case "patientview.neuroExam":
                return dao.getNeuroExamDetails(patient, ALL_ROWS);
            case "patientview.imagingNote":
                return dao.getImagingNotes(patient);
            case "patientview.labResult":
                return dao.getLabResults(patient);
            case "patientview.diagnosis":
                return dao.getNeurosurgicalDiagnoses(patient);
            case "patientview.medicalTreatment":
                return dao.getMedicalTreatments(patient);
            case "patientview.surgicalTreatment":
                return dao.getSurgicalTreatments(patient);
            case "patientview.pathology":
                return dao.getPathologyReports(patient);
            case "patientview.postopEvolution":
                return dao.getPostopEvolutions(patient);
            case "patientview.sequelae":
                return dao.getSequelae(patient);
            case "patientview.discharge":
                return dao.getDischarges(patient);
            case "patientview.followUp":
                return dao.getFollowUps(patient);
            default:
                log.warn("No source getter wired for manifest set " + setId);
                return Collections.emptyList();
        }
    }

    private static Date asDate(Object value) {
        return value instanceof Date ? (Date) value : null;
    }

    private static boolean isBlank(Object value) {
        return value == null || (value instanceof String && ((String) value).trim().isEmpty());
    }

    private static Map<String, Object> newSummary() {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("encounters", 0);
        summary.put("observations", 0);
        summary.put("conditions", 0);
        return summary;
    }

    private static void increment(Map<String, Object> summary, String key) {
        add(summary, key, 1);
    }

    private static void add(Map<String, Object> summary, String key, int amount) {
        Object current = summary.get(key);
        summary.put(key, (current instanceof Integer ? (Integer) current : 0) + amount);
    }

    @SuppressWarnings("unchecked")
    private static void addTo(Map<String, Object> summary, String key, String value) {
        Object current = summary.get(key);
        Set<String> values = current instanceof Set
                ? (Set<String>) current : new java.util.LinkedHashSet<String>();
        values.add(value);
        summary.put(key, values);
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> total, Map<String, Object> part) {
        for (Map.Entry<String, Object> entry : part.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Integer) {
                add(total, entry.getKey(), (Integer) value);
            } else if (value instanceof Set) {
                for (String item : (Set<String>) value) {
                    addTo(total, entry.getKey(), item);
                }
            } else {
                total.put(entry.getKey(), value);
            }
        }
    }
}
