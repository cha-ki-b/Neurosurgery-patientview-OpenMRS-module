package org.openmrs.module.patientview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Static checks on the module's packaging files (config.xml, moduleApplicationContext.xml,
 * liquibase.xml). These do not require a running OpenMRS context, so they catch packaging
 * regressions (bad Spring wiring, missing mapping files, malformed XML) at unit-test speed,
 * before they ever reach a real OpenMRS startup.
 *
 * Each check below corresponds to a real bug that broke module startup:
 *  - a bogus Spring bean referencing a non-existent "mappingResources" parent bean
 *  - Hibernate mapping files not registered via config.xml
 *  - a malformed/misnamed liquibase changelog
 */
public class ModuleWiringTest {

    private String readClasspathResource(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull("Expected classpath resource not found: " + path, in);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private Document parseClasspathXml(String path) throws IOException, ParserConfigurationException, SAXException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull("Expected classpath resource not found: " + path, in);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        }
    }

    @Test
    public void moduleApplicationContextShouldNotReferenceMissingMappingResourcesBean() throws Exception {
        String contents = readClasspathResource("moduleApplicationContext.xml");
        assertFalse(
                "moduleApplicationContext.xml must not declare a bean with parent=\"mappingResources\" - "
                        + "that bean does not exist in OpenMRS core and breaks module startup. "
                        + "Register Hibernate mappings via config.xml's <mappingFiles> element instead.",
                contents.contains("mappingResources"));
    }

    @Test
    public void patientviewServiceMustBeRegisteredWithOpenmrsServiceContext() throws Exception {
        String contents = readClasspathResource("moduleApplicationContext.xml");
        assertTrue(
                "moduleApplicationContext.xml must register PatientviewService via "
                        + "<bean parent=\"serviceContext\">...</bean>, otherwise "
                        + "Context.getService(PatientviewService.class) throws ServiceNotFoundException "
                        + "even though the Spring bean itself loads fine.",
                contents.contains("parent=\"serviceContext\"")
                        && contents.contains("org.openmrs.module.patientview.api.PatientviewService"));
    }

    @Test
    public void configXmlShouldDeclareMappingFilesForPersistentClasses() throws Exception {
        Document config = parseClasspathXml("config.xml");
        String mappingFiles = config.getElementsByTagName("mappingFiles").item(0).getTextContent();

        // Checked against the mapping files that actually exist, rather than a hand-written list:
        // an entity whose .hbm.xml is never registered here compiles, packages and deploys fine,
        // then fails at the first HQL query against it with "not mapped" - long after the build
        // went green. There are seventeen mapped classes now, and a list maintained by hand is the
        // wrong tool: this way a mapping added later is covered without anyone remembering to
        // come back and extend the test.
        File modelDir = new File("../api/src/main/resources/org/openmrs/module/patientview/api/model");
        assertTrue("mapping directory not found (test must run with the omod module directory as "
                + "CWD): " + modelDir.getAbsolutePath(), modelDir.isDirectory());
        File[] mappings = modelDir.listFiles(new java.io.FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".hbm.xml");
            }
        });
        assertNotNull("mapping directory should be readable", mappings);
        assertTrue("expected to find the module's .hbm.xml files, found " + mappings.length,
                mappings.length >= 17);
        for (File mapping : mappings) {
            String path = "org/openmrs/module/patientview/api/model/" + mapping.getName();
            assertTrue("config.xml <mappingFiles> does not list " + mapping.getName()
                    + " - Hibernate will not know that class exists", mappingFiles.contains(path));
        }
    }

    @Test
    public void configXmlShouldRegisterThePrivilegesUsedByAuthorizedAnnotations() throws Exception {
        // PatientviewService methods are gated with @Authorized({PatientviewPrivileges.VIEW_NEURO_DATA})
        // and @Authorized({PatientviewPrivileges.MANAGE_NEURO_DATA}). Those checks work even if the
        // privilege was never registered (hasPrivilege is just a string match against the user's
        // roles), but without a <privilege> entry in config.xml an administrator has no way to
        // actually grant them to a role from the admin UI - the module would be permanently unusable.
        //
        // The two "App:" privileges are the ones that actually matter on a Reference Application
        // install (see PatientviewPrivileges' class doc): the Reference Application auto-grants
        // every plain API-level privilege to all roles, so VIEW_NEURO_DATA/MANAGE_NEURO_DATA end up
        // effectively meaningless there - APP_VIEW_DASHBOARD/APP_MANAGE_DASHBOARD are the real
        // access boundary, checked explicitly in every controller rather than relying on @Authorized.
        Document config = parseClasspathXml("config.xml");
        NodeList privilegeNodes = config.getElementsByTagName("privilege");
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int i = 0; i < privilegeNodes.getLength(); i++) {
            Element privilege = (Element) privilegeNodes.item(i);
            names.add(privilege.getElementsByTagName("name").item(0).getTextContent());
        }
        assertTrue("config.xml should register the \"View Neurosurgery Data\" privilege",
                names.contains("View Neurosurgery Data"));
        assertTrue("config.xml should register the \"Manage Neurosurgery Data\" privilege",
                names.contains("Manage Neurosurgery Data"));
        assertTrue("config.xml should register the \"App: patientview.neurosurgeryDashboard\" privilege",
                names.contains("App: patientview.neurosurgeryDashboard"));
        assertTrue("config.xml should register the \"App: patientview.neurosurgeryDashboard.manage\" privilege",
                names.contains("App: patientview.neurosurgeryDashboard.manage"));
    }

    @Test
    public void extensionLinkMustRequireTheAppViewPrivilege() throws Exception {
        // Without this, the "Neurosurgery Dashboard" link would be visible to every user
        // regardless of whether they've been granted access, since the Reference Application does
        // not gate extension links by default - it relies on each module setting requiredPrivilege.
        String extensionJson = readProjectFile("src/main/resources/apps/patientview_extension.json");
        assertTrue("patientview_extension.json's dashboard link must set requiredPrivilege",
                extensionJson.contains("\"requiredPrivilege\": \"App: patientview.neurosurgeryDashboard\""));
    }

    @Test
    public void everyRestControllerMustEnforceAPrivilegeCheck() throws Exception {
        // @Authorized on the service layer alone is not a reliable boundary on a Reference
        // Application install (see PatientviewPrivileges' class doc), so every REST controller
        // must call PatientviewPrivileges.requireView()/requireManage() explicitly rather than
        // depending on that alone. This scans source rather than hardcoding one file at a time so
        // a newly added controller can't accidentally be forgotten.
        String controllerDir = "src/main/java/org/openmrs/module/patientview/web/controller";
        java.io.File dir = new java.io.File(controllerDir);
        assertTrue("controller directory not found (test must run with the omod module directory as CWD): "
                + dir.getAbsolutePath(), dir.isDirectory());
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith("RestController.java"));
        assertNotNull("controller directory should exist and be readable", files);
        assertTrue("expected to find at least one REST controller", files.length > 0);
        for (java.io.File file : files) {
            String source = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            assertTrue(file.getName() + " must call PatientviewPrivileges.requireView() or requireManage()",
                    source.contains("PatientviewPrivileges.requireView()")
                            || source.contains("PatientviewPrivileges.requireManage()"));
        }
    }

    @Test
    public void mappedHbmFilesReferencedInConfigXmlMustActuallyExistOnClasspath() throws Exception {
        Document config = parseClasspathXml("config.xml");
        String mappingFiles = config.getElementsByTagName("mappingFiles").item(0).getTextContent();

        for (String line : mappingFiles.split("\\r?\\n")) {
            String path = line.trim();
            if (path.isEmpty()) {
                continue;
            }
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull("mappingFiles entry does not exist on classpath: " + path, in);
            }
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        File f = new File(relativePath);
        assertTrue("Expected file not found (test must run with the omod module directory as CWD): "
                + f.getAbsolutePath(), f.isFile());
        try (InputStream in = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void extensionFileMustBeNamedToMatchTheExtensionGlobNotTheAppGlob() throws Exception {
        // AppConfigurationLoaderFactory routes purely by filename glob, independent of content:
        //   classpath*:/apps/*app.json       -> parsed as List<AppDescriptor> (a whole standalone app)
        //   classpath*:/apps/*extension.json -> parsed as List<Extension>     (a pluggable extension)
        // A file with extensionPointId/type/url content but named "..._app.json" is parsed as an
        // AppDescriptor (which has no extensionPointId field) - it loads without error, but is never
        // registered as an Extension, so it never appears anywhere.
        assertNotNull("apps/patientview_extension.json must exist on the classpath",
                getClass().getClassLoader().getResourceAsStream("apps/patientview_extension.json"));
    }

    @Test
    public void patientDashboardExtensionMustUseTheRealCoreappsExtensionPointId() throws Exception {
        String contents = readClasspathResource("apps/patientview_extension.json");
        assertTrue(
                "patientview_extension.json's top-level JSON must be an array ([...]) - every real OpenMRS "
                        + "app/extension file uses this shape, even for a single entry. A bare object is "
                        + "silently rejected at load time (no startup error, the extension just never appears).",
                contents.trim().startsWith("["));
        assertTrue(
                "patientview_extension.json's extensionPointId must be the bare \"patientDashboard.overallActions\" "
                        + "(no \"coreapps.\" prefix) - confirmed against coreapps' PatientPageController source, "
                        + "where the \"dashboard\" variable defaults to the literal string \"patientDashboard\". "
                        + "A wrong id fails silently: no error, the link just never renders.",
                contents.contains("\"patientDashboard.overallActions\""));
    }

    @Test
    public void liquibaseChangeLogMustBeNamedExactlyLiquibaseXmlAndBeWellFormed() throws Exception {
        // OpenMRS looks up this exact classpath resource name to run module DB migrations;
        // a typo'd filename (e.g. "liquidebase.xml") means migrations are silently never run.
        Document changelog = parseClasspathXml("liquibase.xml");
        assertNotNull("liquibase.xml should parse as well-formed XML", changelog);
        assertTrue("liquibase.xml root element should be databaseChangeLog",
                changelog.getDocumentElement().getTagName().equals("databaseChangeLog"));
    }

    @Test
    public void patientGspMustOnlyUsePropertiesThePageControllerActuallyProvides() throws Exception {
        // Each *PageController puts the raw org.openmrs.Patient object into the model under
        // "patient" - there is no "config" variable on page templates (that's a fragment-only
        // concept), and "patient" is not wrapped in a PatientDomainWrapper (no nested ".patient",
        // no ".primaryIdentifiers"). Both mistakes throw MissingPropertyException at render time.
        // Enumerated from disk rather than hardcoded: the module has ten pages now, and a
        // hand-maintained list is a check that quietly stops covering whatever is added next.
        for (File file : listGspFiles(new File("src/main/webapp/pages"))) {
            String page = file.getPath();
            String gsp = readProjectFile(page);
            assertFalse(page + " must not reference \"config.\" - page templates have no such variable",
                    gsp.contains("config."));
            assertFalse(page + " must not reference \"patient.patient.\" - \"patient\" is the raw "
                    + "org.openmrs.Patient object, not a PatientDomainWrapper",
                    gsp.contains("patient.patient."));
            assertFalse(page + " must not reference \"patient.primaryIdentifiers\" - that getter exists "
                    + "only on PatientDomainWrapper; use patient.activeIdentifiers instead",
                    gsp.contains("patient.primaryIdentifiers"));
        }
    }

    /**
     * Every .gsp under a directory, recursively. Enumerating from disk rather than hardcoding a
     * list means a page added later is covered automatically - the same reason
     * everyRestControllerMustEnforceAPrivilegeCheck() scans its directory instead of naming files.
     */
    private java.util.List<File> listGspFiles(File directory) {
        assertTrue("directory not found (test must run with the omod module directory as CWD): "
                + directory.getAbsolutePath(), directory.isDirectory());
        java.util.List<File> found = new java.util.ArrayList<File>();
        File[] children = directory.listFiles();
        assertNotNull("directory should be readable: " + directory, children);
        for (File child : children) {
            if (child.isDirectory()) {
                found.addAll(listGspFiles(child));
            } else if (child.getName().endsWith(".gsp")) {
                found.add(child);
            }
        }
        return found;
    }

    @Test
    public void gspsMustNotFormatPatientNamesThemselves() throws Exception {
        // The "null null" header regressed three times running, because every page and every
        // breadcrumb formatted the name inline: each fix had to be copy-pasted into five places
        // and one of them was always missed or subtly different (PatientviewDisplayName's class
        // doc has the full history). The name is now assembled once in Java, unit-tested, and
        // handed to the templates as the "patientDisplayName" model attribute. A template
        // reaching for the raw name parts again IS the regression, so fail the build on it.
        java.util.List<File> templates = listGspFiles(new File("src/main/webapp/pages"));
        templates.addAll(listGspFiles(new File("src/main/webapp/fragments")));
        assertFalse("expected to find some .gsp templates to check", templates.isEmpty());
        for (File file : templates) {
            String gsp = readProjectFile(file.getPath());
            assertFalse(file.getPath() + " must not read patient.familyName - render the "
                            + "patientDisplayName model attribute instead "
                            + "(PatientviewDisplayName.of(patient))",
                    gsp.contains("patient.familyName"));
            assertFalse(file.getPath() + " must not read patient.givenName - render the "
                            + "patientDisplayName model attribute instead "
                            + "(PatientviewDisplayName.of(patient))",
                    gsp.contains("patient.givenName"));
        }
    }

    @Test
    public void everyPageControllerMustProvideThePatientDisplayName() throws Exception {
        // The other half of the guard above: forbidding the templates from formatting the name
        // only helps if every page controller actually supplies it. A page that renders
        // patientHeader without it shows an empty <h1> and throws on the breadcrumb.
        File dir = new File("src/main/java/org/openmrs/module/patientview/page/controller/clinicianfacing");
        assertTrue("page controller directory not found (test must run with the omod module "
                + "directory as CWD): " + dir.getAbsolutePath(), dir.isDirectory());
        File[] files = dir.listFiles(new java.io.FilenameFilter() {
            public boolean accept(File d, String name) {
                return name.endsWith("PageController.java");
            }
        });
        assertNotNull("page controller directory should be readable", files);
        assertTrue("expected to find at least one page controller", files.length > 0);
        for (File file : files) {
            String source = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            assertTrue(file.getName() + " must put PatientviewDisplayName.of(patient) into the "
                            + "model as \"patientDisplayName\"",
                    source.contains("model.addAttribute(\"patientDisplayName\", "
                            + "PatientviewDisplayName.of(patient))"));
        }
    }

    @Test
    public void fragmentsMayUseConfigButPagesMayNot() throws Exception {
        // Sanity check for the inverse of the rule above: sidebarNav and patientHeader are
        // fragments (included via ui.includeFragment), so "config" is exactly how they receive
        // patientUuid/active/patient from the including page. If these ever stop using "config",
        // that's a sign they were accidentally converted into pages, which would break inclusion.
        String sidebarNav = readProjectFile("src/main/webapp/fragments/sidebarNav.gsp");
        String patientHeader = readProjectFile("src/main/webapp/fragments/patientHeader.gsp");
        assertTrue("sidebarNav.gsp is a fragment and should read its parameters from \"config\"",
                sidebarNav.contains("config."));
        assertTrue("patientHeader.gsp is a fragment and should read its parameters from \"config\"",
                patientHeader.contains("config."));
    }
}
