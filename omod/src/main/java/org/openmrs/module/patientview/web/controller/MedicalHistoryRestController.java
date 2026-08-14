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
public class MedicalHistoryRestController {

    @RequestMapping(value = "/module/patientview/medicalHistory.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getHistory(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getMedicalHistory(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/medicalHistory.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveHistory(@RequestParam("patientId") Integer patientId,
                                            @RequestParam(value = "diabetes", required = false) Boolean diabetes,
                                            @RequestParam(value = "hypertension", required = false) Boolean hypertension,
                                            @RequestParam(value = "epilepsy", required = false) Boolean epilepsy,
                                            @RequestParam(value = "stroke", required = false) Boolean stroke,
                                            @RequestParam(value = "heartDisease", required = false) Boolean heartDisease,
                                            @RequestParam(value = "renalFailure", required = false) Boolean renalFailure,
                                            @RequestParam(value = "allergies", required = false) String allergies,
                                            @RequestParam(value = "chronicTreatment", required = false) String chronicTreatment,
                                            @RequestParam(value = "otherHistory", required = false) String otherHistory) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("diabetes", Boolean.TRUE.equals(diabetes));
        data.put("hypertension", Boolean.TRUE.equals(hypertension));
        data.put("epilepsy", Boolean.TRUE.equals(epilepsy));
        data.put("stroke", Boolean.TRUE.equals(stroke));
        data.put("heartDisease", Boolean.TRUE.equals(heartDisease));
        data.put("renalFailure", Boolean.TRUE.equals(renalFailure));
        data.put("allergies", allergies);
        data.put("chronicTreatment", chronicTreatment);
        data.put("otherHistory", otherHistory);

        Context.getService(PatientviewService.class).saveMedicalHistory(patient, data);
        response.put("success", true);
        return response;
    }
}
