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
public class MedicalTreatmentRestController {

    @RequestMapping(value = "/module/patientview/medicalTreatment.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getMedicalTreatments(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getMedicalTreatments(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/medicalTreatment.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveMedicalTreatment(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "corticosteroids", required = false) Boolean corticosteroids,
                                     @RequestParam(value = "antiepileptics", required = false) Boolean antiepileptics,
                                     @RequestParam(value = "antibiotics", required = false) Boolean antibiotics,
                                     @RequestParam(value = "anticoagulants", required = false) Boolean anticoagulants,
                                     @RequestParam(value = "analgesics", required = false) Boolean analgesics,
                                     @RequestParam(value = "otherTreatments", required = false) String otherTreatments,
                                     @RequestParam(value = "notes", required = false) String notes) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("corticosteroids", Boolean.TRUE.equals(corticosteroids));
        data.put("antiepileptics", Boolean.TRUE.equals(antiepileptics));
        data.put("antibiotics", Boolean.TRUE.equals(antibiotics));
        data.put("anticoagulants", Boolean.TRUE.equals(anticoagulants));
        data.put("analgesics", Boolean.TRUE.equals(analgesics));
        data.put("otherTreatments", otherTreatments);
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveMedicalTreatment(patient, data);
        response.put("success", true);
        return response;
    }
}
