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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class VitalSignsRestController {

    @RequestMapping(value = "/module/patientview/vitalSigns.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getVitalSigns(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getVitalSigns(patient, 20));
        return response;
    }

    @RequestMapping(value = "/module/patientview/vitalSigns.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveVitalSigns(@RequestParam("patientId") Integer patientId,
                                               @RequestParam(value = "temperature", required = false) BigDecimal temperature,
                                               @RequestParam(value = "bpSystolic", required = false) Integer bpSystolic,
                                               @RequestParam(value = "bpDiastolic", required = false) Integer bpDiastolic,
                                               @RequestParam(value = "heartRate", required = false) Integer heartRate,
                                               @RequestParam(value = "respiratoryRate", required = false) Integer respiratoryRate,
                                               @RequestParam(value = "spo2", required = false) Integer spo2,
                                               @RequestParam(value = "weightKg", required = false) BigDecimal weightKg,
                                               @RequestParam(value = "heightCm", required = false) BigDecimal heightCm) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", temperature);
        data.put("bpSystolic", bpSystolic);
        data.put("bpDiastolic", bpDiastolic);
        data.put("heartRate", heartRate);
        data.put("respiratoryRate", respiratoryRate);
        data.put("spo2", spo2);
        data.put("weightKg", weightKg);
        data.put("heightCm", heightCm);

        Context.getService(PatientviewService.class).saveVitalSigns(patient, data);
        response.put("success", true);
        return response;
    }
}
