package org.openmrs.module.patientview.api.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code patientview-fhir-mapping.json}, which declares how this module's fields map onto
 * concepts so {@link ObsProjector} can write them into the core clinical model.
 * <p>
 * Parsed once, lazily, and cached: the manifest is a packaged resource and cannot change while
 * the module is running. Jackson comes transitively from openmrs-api at compile scope, so this
 * adds no dependency to the module.
 * <p>
 * Nothing here knows anything about FHIR. The file is named for its purpose - making the data
 * reachable by the FHIR2 module - but its content is concepts and datatypes, which is why an
 * fhir2 upgrade cannot affect it.
 */
public final class FhirMappingManifest {

    public static final String RESOURCE = "patientview-fhir-mapping.json";

    /** One candidate way to find a concept, tried in declaration order. */
    public static final class ConceptRef {

        private final String source;
        private final String code;

        ConceptRef(String source, String code) {
            this.source = source;
            this.code = code;
        }

        public String getSource() { return source; }
        public String getCode() { return code; }

        @Override
        public String toString() { return source + ":" + code; }
    }

    public static final class FieldSpec {

        private final String id;
        private final String name;
        private final String type;
        private final List<ConceptRef> concept;

        FieldSpec(String id, String name, String type, List<ConceptRef> concept) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.concept = Collections.unmodifiableList(concept);
        }

        public String getId() { return id; }
        /** Clinical label - the term to search the dictionary for when curating this field. */
        public String getName() { return name; }
        /** numeric | text | boolean | date | condition */
        public String getType() { return type; }
        public List<ConceptRef> getConcept() { return concept; }
        /** False when this field still awaits dictionary curation. */
        public boolean isCurated() { return !concept.isEmpty(); }
    }

    public static final class SetSpec {

        private final String id;
        private final String ficheSection;
        private final String sourceGetter;
        private final String target;
        private final String dateKey;
        private final String groupConceptName;
        private final List<ConceptRef> groupConcept;
        private final List<FieldSpec> fields;

        SetSpec(String id, String ficheSection, String sourceGetter, String target, String dateKey,
                String groupConceptName, List<ConceptRef> groupConcept, List<FieldSpec> fields) {
            this.id = id;
            this.ficheSection = ficheSection;
            this.sourceGetter = sourceGetter;
            this.target = target;
            this.dateKey = dateKey;
            this.groupConceptName = groupConceptName;
            this.groupConcept = Collections.unmodifiableList(groupConcept);
            this.fields = Collections.unmodifiableList(fields);
        }

        public String getId() { return id; }
        public String getFicheSection() { return ficheSection; }
        /** Name of the DAO getter this set's rows come from. Used by the parity test. */
        public String getSourceGetter() { return sourceGetter; }
        /** The FHIR resource FHIR2 will serve this set as, for documentation only. */
        public String getTarget() { return target; }
        /** Field whose value becomes the encounter datetime. */
        public String getDateKey() { return dateKey; }
        public String getGroupConceptName() { return groupConceptName; }
        public List<ConceptRef> getGroupConcept() { return groupConcept; }
        public List<FieldSpec> getFields() { return fields; }

        public boolean hasAnyCuratedField() {
            for (FieldSpec field : fields) {
                if (field.isCurated() || "condition".equals(field.getType())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static volatile FhirMappingManifest instance;

    private final String encounterTypeUuid;
    private final String encounterTypeName;
    private final String encounterTypeDescription;
    private final Map<String, SetSpec> sets;

    private FhirMappingManifest(String encounterTypeUuid, String encounterTypeName,
                                String encounterTypeDescription, Map<String, SetSpec> sets) {
        this.encounterTypeUuid = encounterTypeUuid;
        this.encounterTypeName = encounterTypeName;
        this.encounterTypeDescription = encounterTypeDescription;
        this.sets = Collections.unmodifiableMap(sets);
    }

    public static FhirMappingManifest getInstance() {
        FhirMappingManifest loaded = instance;
        if (loaded == null) {
            synchronized (FhirMappingManifest.class) {
                loaded = instance;
                if (loaded == null) {
                    loaded = load();
                    instance = loaded;
                }
            }
        }
        return loaded;
    }

    /** Parses the manifest from the classpath. Package-visible so tests can parse it directly. */
    static FhirMappingManifest load() {
        InputStream in = FhirMappingManifest.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException(RESOURCE + " is not on the classpath");
        }
        try {
            try {
                return parse(new ObjectMapper().readTree(in));
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + RESOURCE, e);
        }
    }

    static FhirMappingManifest parse(JsonNode root) {
        JsonNode encounterType = root.path("encounterType");
        Map<String, SetSpec> parsed = new LinkedHashMap<String, SetSpec>();
        for (JsonNode set : root.path("sets")) {
            List<FieldSpec> fields = new ArrayList<FieldSpec>();
            for (JsonNode field : set.path("fields")) {
                fields.add(new FieldSpec(
                        field.path("id").asText(),
                        field.path("name").asText(),
                        field.path("type").asText(),
                        conceptRefs(field.path("concept"))));
            }
            SetSpec spec = new SetSpec(
                    set.path("id").asText(),
                    set.path("ficheSection").asText(),
                    set.path("sourceGetter").asText(),
                    set.path("target").asText(),
                    set.path("dateKey").asText(),
                    set.path("groupConcept").path("name").asText(null),
                    conceptRefs(set.path("groupConcept").path("concept")),
                    fields);
            parsed.put(spec.getId(), spec);
        }
        return new FhirMappingManifest(
                encounterType.path("uuid").asText(),
                encounterType.path("name").asText(),
                encounterType.path("description").asText(),
                parsed);
    }

    private static List<ConceptRef> conceptRefs(JsonNode array) {
        List<ConceptRef> refs = new ArrayList<ConceptRef>();
        for (JsonNode ref : array) {
            refs.add(new ConceptRef(ref.path("source").asText(), ref.path("code").asText()));
        }
        return refs;
    }

    public String getEncounterTypeUuid() { return encounterTypeUuid; }
    public String getEncounterTypeName() { return encounterTypeName; }
    public String getEncounterTypeDescription() { return encounterTypeDescription; }

    public SetSpec getSet(String id) { return sets.get(id); }
    public List<SetSpec> getSets() { return new ArrayList<SetSpec>(sets.values()); }
}
