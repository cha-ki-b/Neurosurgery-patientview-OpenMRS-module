package org.openmrs.module.patientview;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.patientview.api.fhir.FhirMappingManifest;

public class PatientViewActivator implements ModuleActivator {

    protected final Log log = LogFactory.getLog(this.getClass());

    public void willRefreshContext() {}
    public void contextRefreshed() {}
    public void willStart() {}

    /**
     * Creates the encounter type the core-model projector attaches its observations to, if it is
     * not there yet (1.4.0). Metadata, not clinical data, and idempotent - it is looked up by the
     * fixed uuid the mapping manifest declares, so restarting the module never creates a second
     * one and an administrator who renames it keeps their name.
     * <p>
     * Done here rather than left as a manual step because without it the projector has nothing to
     * hang an encounter on and silently exports nothing.
     */
    public void started() {
        try {
            FhirMappingManifest manifest = FhirMappingManifest.getInstance();
            EncounterType existing = Context.getEncounterService()
                    .getEncounterTypeByUuid(manifest.getEncounterTypeUuid());
            if (existing != null) {
                return;
            }
            EncounterType type = new EncounterType(manifest.getEncounterTypeName(),
                    manifest.getEncounterTypeDescription());
            type.setUuid(manifest.getEncounterTypeUuid());
            Context.getEncounterService().saveEncounterType(type);
            log.info("Created the '" + manifest.getEncounterTypeName() + "' encounter type");
        } catch (Exception e) {
            // Never block module startup over this. The coverage report says whether the
            // encounter type is present, and it can be created by hand from the admin UI.
            log.warn("Could not create the neurosurgery encounter type; the core-model export "
                    + "will stay inactive until it exists", e);
        }
    }

    public void willStop() {}
    public void stopped() {}
}
