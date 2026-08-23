package org.openmrs.module.patientview.api;

import org.openmrs.Patient;

/**
 * The single place this module turns a patient into a name for display.
 * <p>
 * This exists because rendering the name inline in each {@code .gsp} has now regressed three
 * times, each time differently, and each time visibly at the very top of the dashboard:
 * <ol>
 *   <li>v1.0.1 interpolated {@code ${ui.format(patient.familyName)}} directly, so a true null
 *       rendered as Groovy's literal text {@code "null"};</li>
 *   <li>v1.2.0 added a truthiness guard ({@code x ? ... : ""}), which does not catch a stored
 *       value that is the four-character <em>string</em> {@code "null"} - a real state in this
 *       deployment's data, produced by whatever registered/imported those patients;</li>
 *   <li>v1.2.1/1.2.2 added the string check too, but only in the template, and only for the
 *       two name parts - so a patient with no usable name renders as a bare {@code ", "}: a
 *       header with a stray comma and no name at all.</li>
 * </ol>
 * A template is the wrong place for this logic: it cannot be unit-tested, and the fix has to be
 * copy-pasted into every page (five of them before this version, ten after) plus the breadcrumb
 * of each - which is exactly how the previous rounds ended up inconsistent. Assembling the name
 * here instead means one implementation, covered by {@code PatientviewDisplayNameTest}, and
 * {@code ModuleWiringTest.gspsMustNotFormatPatientNamesThemselves()} fails the build if a
 * {@code .gsp} ever reaches for {@code patient.familyName}/{@code patient.givenName} again.
 */
public final class PatientviewDisplayName {

    /**
     * Shown when a patient has no usable name at all. Deliberately explicit rather than blank:
     * a header that silently renders nothing looks like a broken page, whereas this reads as
     * what it is - a record whose name needs fixing. The patient's identifiers are rendered
     * immediately below it in {@code patientHeader.gsp}, so the patient is still identifiable.
     */
    public static final String NO_NAME_RECORDED = "Patient sans nom enregistr\u00e9";

    /**
     * Values that mean "no name" despite being a non-empty string. These are stringified nulls,
     * not names - the artefacts of a data-entry or import path that wrote the placeholder out
     * literally.
     */
    private static final String[] PLACEHOLDER_VALUES = { "null", "nil", "none", "undefined", "n/a" };

    private PatientviewDisplayName() {
    }

    /**
     * @param patient the patient, may be null (a page controller reached with an unknown uuid
     *                puts a null patient in its model rather than failing outright)
     * @return "Familyname, Givenname", or whichever single part is usable, or
     *         {@link #NO_NAME_RECORDED} - never null, never blank, never a stray separator
     */
    public static String of(Patient patient) {
        if (patient == null) {
            return NO_NAME_RECORDED;
        }
        // Person.getFamilyName()/getGivenName() read through getPersonName(), which is null for a
        // patient with no name at all - both getters then return null rather than throwing.
        String family = sanitize(patient.getFamilyName());
        String given = sanitize(patient.getGivenName());

        if (family != null && given != null) {
            return family + ", " + given;
        }
        if (family != null) {
            return family;
        }
        if (given != null) {
            return given;
        }
        return NO_NAME_RECORDED;
    }

    /**
     * @param value a raw name part
     * @return the trimmed value, or null if it is missing, blank, or one of the stringified-null
     *         placeholders in {@link #PLACEHOLDER_VALUES}
     *         <p>
     *         The comparison is against the whole trimmed value, never against individual words
     *         inside it: "Null" is a real surname, and stripping the token wherever it appeared
     *         would quietly corrupt a legitimate name to fix a data-entry artefact.
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (String placeholder : PLACEHOLDER_VALUES) {
            if (placeholder.equalsIgnoreCase(trimmed)) {
                return null;
            }
        }
        return trimmed;
    }
}
