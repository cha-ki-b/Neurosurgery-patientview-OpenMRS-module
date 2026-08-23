package org.openmrs.module.patientview.page.controller.clinicianfacing;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewDisplayName;
import org.openmrs.module.patientview.api.PatientviewPrivileges;
import org.openmrs.module.patientview.api.PatientviewService;
import org.openmrs.ui.framework.page.PageModel;

public class ImageriePageController {

    // imagingStudies/imagingModuleAvailable come from the companion imaging module rather
    // than from a patientview table; the service returns an empty list and false when that
    // module is not installed, so this page degrades to its own comptes rendus alone.
    public void controller(PageModel model, @org.springframework.web.bind.annotation.RequestParam("patientId") String patientUuid) {
        Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
        model.addAttribute("patient", patient);
        model.addAttribute("patientDisplayName", PatientviewDisplayName.of(patient));

        if (!Context.hasPrivilege(PatientviewPrivileges.APP_VIEW_DASHBOARD)) {
            model.addAttribute("accessDenied", true);
            return;
        }
        model.addAttribute("accessDenied", false);
        model.addAttribute("canManage", Context.hasPrivilege(PatientviewPrivileges.APP_MANAGE_DASHBOARD));

        PatientviewService service = Context.getService(PatientviewService.class);
        model.addAttribute("imagingNotes", service.getImagingNotes(patient));
        model.addAttribute("imagingStudies", service.getImagingStudies(patient));
        model.addAttribute("imagingModuleAvailable", service.isImagingModuleAvailable());
    }
}
