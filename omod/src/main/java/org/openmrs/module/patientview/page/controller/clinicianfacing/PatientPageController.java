package org.openmrs.module.patientview.page.controller.clinicianfacing;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewPrivileges;
import org.openmrs.module.patientview.api.PatientviewService;
import org.openmrs.ui.framework.page.PageModel;

public class PatientPageController {

    public void controller(PageModel model, @org.springframework.web.bind.annotation.RequestParam("patientId") String patientUuid) {
        Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
        model.addAttribute("patient", patient);

        // Explicit check rather than relying solely on @Authorized: the Reference Application
        // auto-grants plain API-level privileges to all roles, so this "App:" privilege is the
        // real access boundary here (see PatientviewPrivileges for the full explanation).
        if (!Context.hasPrivilege(PatientviewPrivileges.APP_VIEW_DASHBOARD)) {
            model.addAttribute("accessDenied", true);
            return;
        }
        model.addAttribute("accessDenied", false);
        model.addAttribute("canManage", Context.hasPrivilege(PatientviewPrivileges.APP_MANAGE_DASHBOARD));

        PatientviewService service = Context.getService(PatientviewService.class);
        model.addAttribute("latestGCS", service.getLatestGCS(patient));
        model.addAttribute("diagnosis", service.getPrimaryNeurologicalDiagnosis(patient));
        model.addAttribute("alerts", service.getActiveAlerts(patient));
    }
}
