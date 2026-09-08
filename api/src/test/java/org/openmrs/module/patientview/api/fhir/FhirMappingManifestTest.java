package org.openmrs.module.patientview.api.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openmrs.module.patientview.api.dao.PatientviewDao;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.ConceptRef;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.FieldSpec;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest.SetSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Guards {@code patientview-fhir-mapping.json}, which drives {@link ObsProjector}.
 *
 * <p>The manifest names field ids and concept codes as strings, so nothing at compile time
 * notices when it and the DAO drift apart. A renamed DAO map key would leave a field declared,
 * mapped to a real concept, and permanently never exported - with no error anywhere. These
 * checks close that gap, and need no OpenMRS context: they read the manifest, scan the DAO for
 * the keys it actually produces, and scan the projector for the sets it actually wires.
 */
public class FhirMappingManifestTest {

    private static final File BASE = new File("").getAbsoluteFile();

    private static final File DAO_SOURCE = new File(BASE,
            "src/main/java/org/openmrs/module/patientview/api/dao/PatientviewDAOImpl.java");

    private static final File PROJECTOR_SOURCE = new File(BASE,
            "src/main/java/org/openmrs/module/patientview/api/fhir/ObsProjector.java");

    private static final Set<String> VALID_TYPES =
            new HashSet<String>(Arrays.asList("numeric", "text", "boolean", "date", "condition",
                    "conditionFlag"));

    private String read(File file) throws IOException {
        assertTrue("expected source file not found: " + file, file.isFile());
        StringBuilder text = new StringBuilder();
        Reader reader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
        try {
            char[] chunk = new char[8192];
            int count;
            while ((count = reader.read(chunk)) != -1) {
                text.append(chunk, 0, count);
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }

    /**
     * Map keys the DAO really produces, per getter. Mirrors how MedreportDatasourceManifestTest
     * reads the DAO, but grouped by method so a key belonging to another set cannot satisfy a
     * declaration in this one.
     */
    private Map<String, Set<String>> daoKeysByGetter() throws IOException {
        Map<String, Set<String>> keys = new LinkedHashMap<String, Set<String>>();
        String source = read(DAO_SOURCE);
        String[] chunks = source.split("\\n    (?:@Override\\n    )?"
                + "(?:@SuppressWarnings\\([^)]*\\)\\n    )?public ");
        for (int i = 1; i < chunks.length; i++) {
            String chunk = chunks[i];
            Matcher signature = Pattern.compile("^.*?\\s(\\w+)\\(").matcher(
                    chunk.substring(0, Math.max(0, chunk.indexOf('{'))));
            if (!signature.find()) {
                continue;
            }
            String method = signature.group(1);
            if (!method.startsWith("get")) {
                continue;
            }
            Set<String> produced = new LinkedHashSet<String>();
            Matcher puts = Pattern.compile("map\\.put\\(\\s*\"([^\"]+)\"").matcher(chunk);
            while (puts.find()) {
                produced.add(puts.group(1));
            }
            if (!produced.isEmpty()) {
                keys.put(method, produced);
            }
        }
        assertFalse("no map-building getters found - has the DAO been restructured?",
                keys.isEmpty());
        return keys;
    }

    @Test
    public void theManifestParsesAndDeclaresEveryFicheSection() {
        FhirMappingManifest manifest = FhirMappingManifest.load();
        assertNotNull(manifest.getEncounterTypeUuid());
        assertFalse("the manifest must name an encounter type",
                manifest.getEncounterTypeName().isEmpty());
        assertEquals("one set per patientview clinical entity", 16, manifest.getSets().size());
    }

    /**
     * The core parity check, in both directions: a declared field must be a key its getter really
     * produces, and a key the getter produces must be either mapped or explicitly excluded. The
     * second direction is what stops a newly added clinical field from silently never being
     * exported.
     */
    @Test
    public void everySetAgreesExactlyWithItsDaoGetter() throws Exception {
        Map<String, Set<String>> daoKeys = daoKeysByGetter();
        Map<String, Set<String>> excluded = excludedIds();
        Set<String> problems = new LinkedHashSet<String>();

        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            Set<String> produced = daoKeys.get(spec.getSourceGetter());
            if (produced == null) {
                problems.add(spec.getId() + " names sourceGetter " + spec.getSourceGetter()
                        + ", which builds no map in the DAO");
                continue;
            }
            Set<String> declared = new LinkedHashSet<String>();
            for (FieldSpec field : spec.getFields()) {
                declared.add(field.getId());
                if (!produced.contains(field.getId())) {
                    problems.add(spec.getId() + " maps '" + field.getId() + "', which "
                            + spec.getSourceGetter() + " does not produce");
                }
            }
            Set<String> excludedHere = excluded.get(spec.getId());
            if (excludedHere != null) {
                declared.addAll(excludedHere);
            }
            for (String key : produced) {
                if ("uuid".equals(key)) {
                    continue;
                }
                if (!declared.contains(key)) {
                    problems.add(spec.getId() + ": " + spec.getSourceGetter() + " produces '"
                            + key + "' but the manifest neither maps nor excludes it");
                }
            }
            if (!produced.contains(spec.getDateKey())) {
                problems.add(spec.getId() + " uses dateKey '" + spec.getDateKey()
                        + "', which is not a DAO key");
            }
        }
        assertTrue("manifest and DAO have drifted apart: " + problems, problems.isEmpty());
    }

    /**
     * Ids the manifest explicitly excludes from projection, per set. "excluded" is
     * documentation-only in the parsed model, so it is read straight from the file - with a real
     * JSON parser, because locating it by string offsets is exactly the kind of thing that looks
     * correct and quietly returns nothing.
     */
    private Map<String, Set<String>> excludedIds() throws IOException {
        JsonNode root = new ObjectMapper().readTree(
                new File(BASE, "src/main/resources/" + FhirMappingManifest.RESOURCE));
        Map<String, Set<String>> excluded = new LinkedHashMap<String, Set<String>>();
        for (JsonNode set : root.path("sets")) {
            Set<String> ids = new LinkedHashSet<String>();
            for (JsonNode entry : set.path("excluded")) {
                ids.add(entry.path("id").asText());
                assertFalse(set.path("id").asText() + " excludes '" + entry.path("id").asText()
                                + "' without recording why",
                        entry.path("reason").asText("").isEmpty());
            }
            excluded.put(set.path("id").asText(), ids);
        }
        return excluded;
    }

    /** The ledger identifies a source row by its uuid, so every getter has to expose one. */
    @Test
    public void everyGetterExposesTheUuidTheProjectionLedgerNeeds() throws Exception {
        Set<String> missing = new LinkedHashSet<String>();
        for (Map.Entry<String, Set<String>> entry : daoKeysByGetter().entrySet()) {
            if (!entry.getValue().contains("uuid")) {
                missing.add(entry.getKey());
            }
        }
        assertTrue("these getters produce no 'uuid', so rows they return can never be recorded "
                + "in the projection ledger and would be exported again on every run: " + missing,
                missing.isEmpty());
    }

    /** A set with no case in the projector reads no rows, so it would export nothing, silently. */
    @Test
    public void everySetIsWiredToASourceGetterInTheProjector() throws Exception {
        String projector = read(PROJECTOR_SOURCE);
        Set<String> unwired = new LinkedHashSet<String>();
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            if (!projector.contains("case \"" + spec.getId() + "\"")) {
                unwired.add(spec.getId());
            }
        }
        assertTrue("ObsProjector.fetchRows has no case for: " + unwired, unwired.isEmpty());
    }

    @Test
    public void everyFieldDeclaresAKnownType() {
        Set<String> problems = new LinkedHashSet<String>();
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            for (FieldSpec field : spec.getFields()) {
                if (!VALID_TYPES.contains(field.getType())) {
                    problems.add(spec.getId() + "." + field.getId() + " = " + field.getType());
                }
                if (field.getName() == null || field.getName().isEmpty()) {
                    problems.add(spec.getId() + "." + field.getId() + " has no clinical label, so "
                            + "there is nothing to search the dictionary for when curating it");
                }
            }
        }
        assertTrue("unknown field types (valid: " + VALID_TYPES + "): " + problems,
                problems.isEmpty());
    }

    /**
     * Concepts must be referenced by mapping, never by a bare OpenMRS concept_id: those are
     * install-specific, so a numeric id that works here would silently point at a different
     * concept - or none - on another server.
     */
    @Test
    public void conceptsAreReferencedByMappingSourceNotByLocalId() {
        Set<String> problems = new LinkedHashSet<String>();
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            for (FieldSpec field : spec.getFields()) {
                for (ConceptRef ref : field.getConcept()) {
                    if (ref.getSource() == null || ref.getSource().trim().isEmpty()) {
                        problems.add(spec.getId() + "." + field.getId()
                                + " declares a code with no mapping source");
                    }
                    if (ref.getCode() == null || ref.getCode().trim().isEmpty()) {
                        problems.add(spec.getId() + "." + field.getId()
                                + " declares a mapping source with no code");
                    }
                }
            }
        }
        assertTrue(problems.toString(), problems.isEmpty());
    }

    /** Whatever the manifest declares, the service methods the projector needs must exist. */
    @Test
    public void theDaoExposesEveryGetterTheManifestNames() throws Exception {
        Set<String> problems = new LinkedHashSet<String>();
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            boolean found = false;
            for (Method method : PatientviewDao.class.getMethods()) {
                if (method.getName().equals(spec.getSourceGetter())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                problems.add(spec.getId() + " -> " + spec.getSourceGetter());
            }
        }
        assertTrue("PatientviewDao has no such method: " + problems, problems.isEmpty());
    }

    /**
     * Records the curation state this release actually ships with, so the number cannot drift
     * unnoticed and a reader of the test suite can see how much of the Fiche exports today.
     */
    @Test
    public void theShippedManifestReportsItsOwnCurationState() {
        int total = 0;
        int curated = 0;
        int setsThatExportToday = 0;
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            boolean exports = false;
            for (FieldSpec field : spec.getFields()) {
                if ("condition".equals(field.getType())) {
                    exports = true;
                    continue;
                }
                total++;
                if (field.isCurated()) {
                    curated++;
                    exports = true;
                }
            }
            if (exports) {
                setsThatExportToday++;
            }
        }
        // The field count is exact: it can only change with a schema change, and a silent change
        // means the manifest and the DAO have drifted (which the parity test would also catch).
        assertEquals("projectable observation fields", 120, total);

        // Curation counts are floors, not equalities. Curating concepts is expected, routine work
        // - tools/ciel_match.py exists to make it routine - so an exact assertion would fail the
        // build on every batch and train people to edit the number without reading it. A floor
        // still catches the regression that matters: concepts silently disappearing from the
        // manifest. Raise these as curation progresses.
        assertTrue("curation has regressed: only " + curated + " fields carry a concept, "
                        + "down from the 44 this release shipped with",
                curated >= 44);
        assertTrue("only " + setsThatExportToday + " sets export anything, down from 14",
                setsThatExportToday >= 14);
    }

    @Test
    public void groupedSetsDeclareTheirGroupConceptWithALabel() {
        for (SetSpec spec : FhirMappingManifest.load().getSets()) {
            List<ConceptRef> group = spec.getGroupConcept();
            if (group.isEmpty()) {
                continue;
            }
            assertNotNull(spec.getId() + " declares a group concept but no label",
                    spec.getGroupConceptName());
            assertFalse(spec.getId() + " declares a group concept but no label",
                    spec.getGroupConceptName().isEmpty());
        }
    }
}
