package org.openmrs.module.patientview.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonName;

/**
 * Covers the exact inputs that produced the "null null" header this deployment reported, plus
 * the blank-header state the previous template-only fix left behind. No OpenMRS context needed:
 * Patient/PersonName are plain beans until they touch a session.
 */
public class PatientviewDisplayNameTest {

    private Patient patientNamed(String given, String family) {
        Patient patient = new Patient();
        PersonName name = new PersonName();
        name.setGivenName(given);
        name.setFamilyName(family);
        patient.addName(name);
        return patient;
    }

    @Test
    public void shouldRenderFamilyThenGivenForANormalPatient() {
        assertEquals("Benali, Amina", PatientviewDisplayName.of(patientNamed("Amina", "Benali")));
    }

    @Test
    public void shouldTreatTheLiteralStringNullAsAMissingName() {
        // The reported bug: person_name rows holding the four-character string "null" rather
        // than SQL NULL. A truthiness check in a template passes these straight through.
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED,
                PatientviewDisplayName.of(patientNamed("null", "null")));
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED,
                PatientviewDisplayName.of(patientNamed("NULL", "Null")));
    }

    @Test
    public void shouldTreatTrueNullAndBlanksAsMissingNames() {
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED,
                PatientviewDisplayName.of(patientNamed(null, null)));
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED,
                PatientviewDisplayName.of(patientNamed("   ", "")));
    }

    @Test
    public void shouldNeverRenderAStraySeparatorWhenOnlyOneHalfOfTheNameIsUsable() {
        // What v1.2.2 rendered as ", Amina" and "Benali, " - a header with a dangling comma.
        assertEquals("Amina", PatientviewDisplayName.of(patientNamed("Amina", "null")));
        assertEquals("Benali", PatientviewDisplayName.of(patientNamed(null, "Benali")));
    }

    @Test
    public void shouldHandleAPatientWithNoNameRecordAtAll() {
        // getPersonName() is null here, so Person's getFamilyName()/getGivenName() return null.
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED,
                PatientviewDisplayName.of(new Patient()));
    }

    @Test
    public void shouldHandleANullPatient() {
        // A page controller reached with an unknown patient uuid puts null in its model.
        assertEquals(PatientviewDisplayName.NO_NAME_RECORDED, PatientviewDisplayName.of(null));
    }

    @Test
    public void shouldNotStripPlaceholderWordsFromInsideARealName() {
        // "Null" is a real surname. Matching per-word rather than on the whole trimmed value
        // would silently corrupt it while chasing a data-entry artefact.
        assertEquals("Van Null, Jan", PatientviewDisplayName.of(patientNamed("Jan", "Van Null")));
    }

    @Test
    public void shouldTrimSurroundingWhitespace() {
        assertEquals("Benali, Amina", PatientviewDisplayName.of(patientNamed(" Amina ", " Benali ")));
    }
}
