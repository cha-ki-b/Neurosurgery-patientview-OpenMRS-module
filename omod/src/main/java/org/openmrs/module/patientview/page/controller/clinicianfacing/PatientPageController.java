package org.openmrs.module.patientview.page.controller.clinicianfacing;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewService;
import org.openmrs.ui.framework.page.PageModel;

public class PatientPageController {

    public void controller(PageModel model, @org.springframework.web.bind.annotation.RequestParam("patientId") String patientUuid) {
        Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
        model.addAttribute("patient", patient);

        PatientviewService service = Context.getService(PatientviewService.class);
        model.addAttribute("latestGCS", service.getLatestGCS(patient));
        model.addAttribute("diagnosis", service.getPrimaryNeurologicalDiagnosis(patient));
        model.addAttribute("medications", service.getCurrentNeurologicalMedications(patient));
        model.addAttribute("imaging", service.getRecentImaging(patient, 30));
    }
}
