package org.openmrs.module.patientview.api.imaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a patient's DICOM studies from the separate {@code imaging} module, reflectively.
 * <p>
 * <b>Why not just call Orthanc.</b> The Imagerie tab needs a list of the patient's imaging
 * studies. The obvious implementation - an HTTP client in this module hitting Orthanc's
 * {@code /patients} and {@code /studies}, with the base URL and credentials as OpenMRS global
 * properties - would mean a second, independent place to configure Orthanc, a second copy of the
 * DICOM-PatientID-to-OpenMRS-patient matching rule, and a second set of credentials to rotate.
 * The {@code imaging} module already does all of that: it stores the Orthanc URL, credentials and
 * proxy URL in its own {@code OrthancConfiguration}, syncs studies, resolves them to OpenMRS
 * patients, and serves its own series/instance/viewer pages. Duplicating it here would guarantee
 * the two drift apart. Orthanc stays the source of truth for images; {@code imaging} stays the
 * source of truth for how to reach it.
 * <p>
 * <b>Why reflection.</b> Same reasoning as this module's {@code medreport-datasource.json}
 * integration, in the other direction: no {@code pom.xml} dependency, no compile-time coupling,
 * no shared type. {@code imaging} is declared only under {@code <aware_of_modules>} in
 * {@code config.xml}, which is OpenMRS's way of saying "start it before me if it's there, and let
 * me see its classes - but load me fine if it isn't". Everything here degrades to an empty list
 * when it isn't: the Imagerie tab still works, showing this module's own comptes rendus with a
 * note explaining that the PACS link is unavailable.
 * <p>
 * The catch blocks are deliberately broad. Every failure mode - module absent, module stopped,
 * a version whose method signature moved, a classloader that cannot see it - means exactly one
 * thing to a clinician looking at this tab: there are no studies to show. None of them is worth
 * turning a patient record into a stack trace.
 */
public final class DicomStudyBridge {

    private static final Log log = LogFactory.getLog(DicomStudyBridge.class);

    private static final String SERVICE_CLASS = "org.openmrs.module.imaging.api.DicomStudyService";

    private DicomStudyBridge() {
    }

    /**
     * @return true if the {@code imaging} module is installed, started, and exposes the expected
     *         service - i.e. whether the Imagerie tab can show a PACS study list at all
     */
    public static boolean isAvailable() {
        return serviceClass() != null;
    }

    /**
     * @param patient the patient, may be null
     * @return one map per DICOM study held for this patient, keyed {@code studyId},
     *         {@code studyDate}, {@code studyTime}, {@code studyDescription} and
     *         {@code studyInstanceUID}; empty (never null) if the {@code imaging} module is not
     *         usable. {@code studyId} is imaging's own primary key, which is what its
     *         {@code series} page expects as a request parameter.
     */
    public static List<Map<String, Object>> getStudiesOfPatient(Patient patient) {
        List<Map<String, Object>> studies = new ArrayList<>();
        Class<?> type = serviceClass();
        if (type == null || patient == null) {
            return studies;
        }
        try {
            Object service = Context.getService(type);
            Object result = type.getMethod("getStudiesOfPatient", Patient.class)
                    .invoke(service, patient);
            if (!(result instanceof List)) {
                return studies;
            }
            for (Object study : (List<?>) result) {
                Map<String, Object> map = new HashMap<>();
                map.put("studyId", read(study, "getId"));
                map.put("studyDate", formatDicomDate(read(study, "getStudyDate")));
                map.put("studyTime", read(study, "getStudyTime"));
                map.put("studyDescription", read(study, "getStudyDescription"));
                map.put("studyInstanceUID", read(study, "getStudyInstanceUID"));
                studies.add(map);
            }
        } catch (Exception e) {
            log.debug("imaging module present but its study list could not be read", e);
            return new ArrayList<>();
        } catch (LinkageError e) {
            log.debug("imaging module present but incompatible", e);
            return new ArrayList<>();
        }
        return studies;
    }

    private static Class<?> serviceClass() {
        try {
            // Context.loadClass goes through OpenmrsClassLoader, which searches every started
            // module's class loader - a plain Class.forName from this module's own loader would
            // not find it.
            return Context.loadClass(SERVICE_CLASS);
        } catch (Exception e) {
            return null;
        } catch (LinkageError e) {
            return null;
        }
    }

    private static Object read(Object target, String getter) {
        try {
            return target.getClass().getMethod(getter).invoke(target);
        } catch (Exception e) {
            return null;
        } catch (LinkageError e) {
            return null;
        }
    }

    /**
     * DICOM stores dates as {@code YYYYMMDD} (tag 0008,0020) and imaging keeps them as that raw
     * string. Rendered verbatim a clinician reads "20260815"; this turns it into 15/08/2026.
     * Anything that is not eight digits is passed through untouched rather than guessed at.
     */
    private static Object formatDicomDate(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim();
        if (!raw.matches("\\d{8}")) {
            return raw;
        }
        return raw.substring(6, 8) + "/" + raw.substring(4, 6) + "/" + raw.substring(0, 4);
    }
}
