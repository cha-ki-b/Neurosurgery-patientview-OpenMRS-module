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

@Controller
public class PathologyRestController {

    @RequestMapping(value = "/module/patientview/pathology.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getReports(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getPathologyReports(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/pathology.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveReport(@RequestParam("patientId") Integer patientId,
                                           @RequestParam(value = "studyFindings", required = false) String studyFindings,
                                           @RequestParam(value = "whoGrade", required = false) String whoGrade,
                                           @RequestParam(value = "immunohistochemistry", required = false) String immunohistochemistry,
                                           @RequestParam(value = "molecularMarkers", required = false) String molecularMarkers,
                                           @RequestParam(value = "finalDiagnosis", required = false) String finalDiagnosis) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studyFindings", studyFindings);
        data.put("whoGrade", whoGrade);
        data.put("immunohistochemistry", immunohistochemistry);
        data.put("molecularMarkers", molecularMarkers);
        data.put("finalDiagnosis", finalDiagnosis);

        Context.getService(PatientviewService.class).savePathologyReport(patient, data);
        response.put("success", true);
        return response;
    }
}
