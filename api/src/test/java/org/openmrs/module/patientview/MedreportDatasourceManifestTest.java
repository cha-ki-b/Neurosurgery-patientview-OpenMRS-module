package org.openmrs.module.patientview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.module.patientview.api.PatientviewService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Guards {@code medreport-datasource.json}, the file that announces this module's data to the
 * medreport reporting module.
 *
 * <p>The integration is deliberately loose: no shared Java type, no dependency in either
 * direction, just this manifest naming service methods and map keys that medreport calls
 * reflectively. That looseness is what keeps the two modules independent, but it also means
 * nothing at compile time notices when the manifest and this module's code drift apart -
 * renaming a DAO map key or changing a service signature would leave a report silently
 * missing a column, with no error anywhere.
 *
 * <p>These tests close that gap from this side. They need no OpenMRS context: they read the
 * manifest, reflect over {@link PatientviewService}, and scan the DAO for the keys it
 * actually produces.
 *
 * <p>This test is the <em>only</em> place this module mentions medreport, and it does so
 * without importing anything from it.
 */
public class MedreportDatasourceManifestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final File BASE = new File("").getAbsoluteFile();

    private static final File MANIFEST =
            new File(BASE, "src/main/resources/medreport-datasource.json");

    private static final File DAO = new File(BASE,
            "src/main/java/org/openmrs/module/patientview/api/dao/PatientviewDAOImpl.java");

    private JsonNode manifest() throws IOException {
        assertTrue("medreport-datasource.json is missing", MANIFEST.exists());
        return MAPPER.readTree(MANIFEST);
    }

    private String read(File file) throws IOException {
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

    @Test
    public void theManifestIsValidJson() throws Exception {
        JsonNode manifest = manifest();
        assertTrue("patientview".equals(manifest.path("id").asText()));
        assertTrue("the manifest must declare at least one set",
                manifest.path("sections").size() > 0);
    }

    /**
     * The privilege the manifest advertises must be one this module actually enforces,
     * otherwise medreport would gate reporting on a privilege nobody is ever granted.
     */
    @Test
    public void theDeclaredPrivilegeIsOneThisModuleActuallyUses() throws Exception {
        String declared = manifest().path("requiredPrivilege").asText();
        assertTrue("the manifest must declare a required privilege",
                declared != null && !declared.isEmpty());
        assertTrue("the manifest advertises a privilege this module does not define: " + declared,
                declared.equals(org.openmrs.module.patientview.api.PatientviewPrivileges.APP_VIEW_DASHBOARD)
                        || declared.equals(org.openmrs.module.patientview.api.PatientviewPrivileges.APP_MANAGE_DASHBOARD));
    }

    /**
     * The core of the contract: medreport resolves each declared method reflectively, so a
     * renamed or re-signed service method turns into a runtime lookup failure that only
     * shows up as a missing section in a generated report.
     */
    @Test
    public void everyDeclaredServiceMethodExistsWithTheExpectedSignature() throws Exception {
        JsonNode sections = manifest().path("sections");
        assertTrue(sections.size() > 0);

        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText();
            JsonNode source = section.path("source");
            String serviceClass = source.path("serviceClass").asText();
            String methodName = source.path("method").asText();

            assertTrue(sectionId + " must name a service class",
                    serviceClass != null && !serviceClass.isEmpty());

            Class<?> type;
            try {
                type = Class.forName(serviceClass);
            } catch (ClassNotFoundException e) {
                fail(sectionId + " names a service class that does not exist: " + serviceClass);
                return;
            }

            // Build the parameter list medreport will use: always the patient first, plus an
            // int for each "limit:n" token.
            int extraInts = 0;
            for (JsonNode arg : source.path("args")) {
                if (arg.asText().toLowerCase().startsWith("limit:")) {
                    extraInts++;
                }
            }
            Class<?>[] parameters = new Class<?>[1 + extraInts];
            parameters[0] = Patient.class;
            for (int i = 1; i < parameters.length; i++) {
                parameters[i] = int.class;
            }

            Method method;
            try {
                method = type.getMethod(methodName, parameters);
            } catch (NoSuchMethodException e) {
                fail(sectionId + " declares " + serviceClass + "." + methodName
                        + " with " + parameters.length + " parameter(s), which does not exist");
                return;
            }

            // medreport can only consume a Map or a List of Maps - anything else is silently
            // dropped, which would show up as an empty section rather than an error.
            Class<?> returnType = method.getReturnType();
            assertTrue(sectionId + " declares " + methodName + ", which returns "
                            + returnType.getName() + "; medreport can only read Map or List",
                    java.util.Map.class.isAssignableFrom(returnType)
                            || java.util.List.class.isAssignableFrom(returnType));

            // A repeating set needs a List; a flat one needs a Map. Getting this backwards
            // renders one record where there should be several, or vice versa.
            boolean repeating = section.path("repeating").asBoolean(false);
            if (repeating) {
                assertTrue(sectionId + " is declared repeating, so " + methodName
                                + " must return a List",
                        java.util.List.class.isAssignableFrom(returnType));
            } else {
                assertTrue(sectionId + " is not repeating, so " + methodName
                                + " must return a Map",
                        java.util.Map.class.isAssignableFrom(returnType));
            }
        }
    }

    /**
     * Every field the manifest advertises must be a key the DAO actually puts in its map.
     * This is the check that catches the quiet failure mode: a renamed key leaves the field
     * selectable in the report window but permanently blank in the document.
     */
    @Test
    public void everyDeclaredFieldIsAKeyTheDaoActuallyProduces() throws Exception {
        assertTrue("PatientviewDAOImpl.java not found at " + DAO, DAO.exists());

        Set<String> produced = new HashSet<String>();
        Matcher matcher = Pattern.compile("map\\.put\\(\\s*\"([^\"]+)\"").matcher(read(DAO));
        while (matcher.find()) {
            produced.add(matcher.group(1));
        }
        assertFalse("no map keys were found in the DAO - has it been restructured?",
                produced.isEmpty());

        Set<String> missing = new LinkedHashSet<String>();
        for (JsonNode section : manifest().path("sections")) {
            String sectionId = section.path("id").asText();
            for (JsonNode field : section.path("fields")) {
                String key = field.has("sourceKey")
                        ? field.path("sourceKey").asText() : field.path("id").asText();
                if (!produced.contains(key)) {
                    missing.add(sectionId + "." + key);
                }
            }
        }
        assertTrue("the manifest advertises fields the DAO never produces, so they would render"
                + " permanently blank: " + missing, missing.isEmpty());
    }

    /** A record title key is read from the same map, so it has the same requirement. */
    @Test
    public void everyRecordTitleKeyIsAlsoProducedByTheDao() throws Exception {
        Set<String> produced = new HashSet<String>();
        Matcher matcher = Pattern.compile("map\\.put\\(\\s*\"([^\"]+)\"").matcher(read(DAO));
        while (matcher.find()) {
            produced.add(matcher.group(1));
        }

        for (JsonNode section : manifest().path("sections")) {
            if (!section.path("repeating").asBoolean(false)) {
                continue;
            }
            String key = section.path("recordTitleKey").asText();
            assertNotNull(key);
            assertTrue("recordTitleKey '" + key + "' of " + section.path("id").asText()
                    + " is not produced by the DAO", produced.contains(key));
        }
    }

    /**
     * The manifest exists to make reporting possible without coupling. If this module ever
     * gains a real dependency on medreport, the whole design point is lost - so assert the
     * absence directly.
     */
    @Test
    public void thisModuleDoesNotDependOnMedreport() throws Exception {
        String pom = read(new File(BASE, "pom.xml"));
        assertFalse("patientview must not declare a dependency on medreport",
                pom.contains("medreport"));

        File sources = new File(BASE, "src/main/java");
        Set<String> offenders = new LinkedHashSet<String>();
        collectMedreportImports(sources, offenders);
        assertTrue("patientview main sources must not import anything from medreport: "
                + offenders, offenders.isEmpty());
    }

    private void collectMedreportImports(File directory, Set<String> offenders) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectMedreportImports(child, offenders);
            } else if (child.getName().endsWith(".java")
                    && read(child).contains("import org.openmrs.module.medreport")) {
                offenders.add(child.getName());
            }
        }
    }
}
