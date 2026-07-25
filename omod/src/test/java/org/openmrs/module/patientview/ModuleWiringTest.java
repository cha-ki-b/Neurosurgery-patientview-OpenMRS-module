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

        assertTrue("config.xml <mappingFiles> should list NeuroAssessment.hbm.xml",
                mappingFiles.contains("org/openmrs/module/patientview/api/model/NeuroAssessment.hbm.xml"));
        assertTrue("config.xml <mappingFiles> should list SurgicalHistory.hbm.xml",
                mappingFiles.contains("org/openmrs/module/patientview/api/model/SurgicalHistory.hbm.xml"));
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
        // PatientPageController puts the raw org.openmrs.Patient object into the model under
        // "patient" - there is no "config" variable on page templates (that's a fragment-only
        // concept), and "patient" is not wrapped in a PatientDomainWrapper (no nested ".patient",
        // no ".primaryIdentifiers"). Both mistakes throw MissingPropertyException at render time.
        String gsp = readProjectFile("src/main/webapp/pages/clinicianfacing/patient.gsp");
        assertFalse("patient.gsp must not reference \"config\" - page templates have no such variable",
                gsp.contains("config."));
        assertFalse("patient.gsp must not reference \"patient.patient.\" - \"patient\" is the raw "
                + "org.openmrs.Patient object, not a PatientDomainWrapper",
                gsp.contains("patient.patient."));
        assertFalse("patient.gsp must not reference \"patient.primaryIdentifiers\" - that getter exists "
                + "only on PatientDomainWrapper; use patient.activeIdentifiers instead",
                gsp.contains("patient.primaryIdentifiers"));
    }
}
