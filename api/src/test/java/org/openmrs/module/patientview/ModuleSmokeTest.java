package org.openmrs.module.patientview;

import org.junit.Test;

public class ModuleSmokeTest {

    @Test
    public void shouldLoadModuleClass() {
        // Simply instantiate your main activator or any important class
        PatientViewActivator activator = new PatientViewActivator();
        // If it compiles and doesn't throw on construction, you're good
        assert activator != null;
    }
}
