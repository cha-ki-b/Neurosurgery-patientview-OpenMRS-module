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
public class NeurosurgicalDiagnosisRestController {

    @RequestMapping(value = "/module/patientview/neurosurgicalDiagnosis.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getDiagnoses(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getNeurosurgicalDiagnoses(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/neurosurgicalDiagnosis.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveDiagnosis(@RequestParam("patientId") Integer patientId,
                                              @RequestParam(value = "diagnosis", required = false) String diagnosis,
                                              @RequestParam(value = "lesionLocation", required = false) String lesionLocation,
                                              @RequestParam(value = "laterality", required = false) String laterality,
                                              @RequestParam(value = "size", required = false) String size,
                                              @RequestParam(value = "lesionCount", required = false) Integer lesionCount,
                                              @RequestParam(value = "whoDiagnosis", required = false) String whoDiagnosis) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("diagnosis", diagnosis);
        data.put("lesionLocation", lesionLocation);
        data.put("laterality", laterality);
        data.put("size", size);
        data.put("lesionCount", lesionCount);
        data.put("whoDiagnosis", whoDiagnosis);

        Context.getService(PatientviewService.class).saveNeurosurgicalDiagnosis(patient, data);
        response.put("success", true);
        return response;
    }
}
