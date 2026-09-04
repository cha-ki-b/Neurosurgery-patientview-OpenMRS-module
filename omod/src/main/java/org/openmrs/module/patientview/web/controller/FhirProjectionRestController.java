package org.openmrs.module.patientview.web.controller;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewPrivileges;
import org.openmrs.module.patientview.api.PatientviewService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Operates the export of this module's records into the core clinical model, where the FHIR2
 * module can serve them (1.4.0).
 * <p>
 * Records are exported automatically as they are saved, so this endpoint exists for the two
 * cases that automation does not cover: backfilling rows created before 1.4.0, and re-running
 * the export after new concepts have been curated into the mapping manifest. Both are safe to
 * repeat - the projection ledger skips whatever is already exported.
 */
@Controller
public class FhirProjectionRestController {

    /** Coverage report: how much of the mapping manifest is usable on this server. */
    @RequestMapping(value = "/module/patientview/fhirProjection.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getCoverage() {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getFhirCoverageReport());
        return response;
    }

    /**
     * Exports one patient's records, or every patient's when {@code allPatients} is true.
     * @param patientId the patient to export; ignored when allPatients is set
     * @param allPatients true to run the server-wide backfill
     */
    @RequestMapping(value = "/module/patientview/fhirProjection.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> project(
            @RequestParam(value = "patientId", required = false) Integer patientId,
            @RequestParam(value = "allPatients", required = false) Boolean allPatients) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        PatientviewService service = Context.getService(PatientviewService.class);

        if (Boolean.TRUE.equals(allPatients)) {
            response.put("success", true);
            response.put("data", service.projectAllPatients());
            return response;
        }

        if (patientId == null) {
            response.put("success", false);
            response.put("message", "patientId ou allPatients est requis");
            return response;
        }
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }
        response.put("success", true);
        response.put("data", service.projectToClinicalModel(patient));
        return response;
    }
}
