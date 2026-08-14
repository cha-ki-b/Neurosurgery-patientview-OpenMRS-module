package org.openmrs.module.patientview.page.controller.clinicianfacing;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewPrivileges;
import org.openmrs.module.patientview.api.PatientviewService;
import org.openmrs.ui.framework.page.PageModel;

public class DiagnosticPageController {

    public void controller(PageModel model, @org.springframework.web.bind.annotation.RequestParam("patientId") String patientUuid) {
        Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
        model.addAttribute("patient", patient);

        if (!Context.hasPrivilege(PatientviewPrivileges.APP_VIEW_DASHBOARD)) {
            model.addAttribute("accessDenied", true);
            return;
        }
        model.addAttribute("accessDenied", false);
        model.addAttribute("canManage", Context.hasPrivilege(PatientviewPrivileges.APP_MANAGE_DASHBOARD));

        PatientviewService service = Context.getService(PatientviewService.class);
        model.addAttribute("diagnoses", service.getNeurosurgicalDiagnoses(patient));
    }
}
