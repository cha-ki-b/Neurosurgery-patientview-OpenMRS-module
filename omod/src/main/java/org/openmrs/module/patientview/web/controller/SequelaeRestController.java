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
public class SequelaeRestController {

    @RequestMapping(value = "/module/patientview/sequelae.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getSequelae(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getSequelae(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/sequelae.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveSequelae(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "motorDeficit", required = false) String motorDeficit,
                                     @RequestParam(value = "sensoryDeficit", required = false) String sensoryDeficit,
                                     @RequestParam(value = "aphasia", required = false) Boolean aphasia,
                                     @RequestParam(value = "cognitiveDisorders", required = false) Boolean cognitiveDisorders,
                                     @RequestParam(value = "secondaryEpilepsy", required = false) Boolean secondaryEpilepsy,
                                     @RequestParam(value = "residualDisability", required = false) String residualDisability,
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
        data.put("motorDeficit", motorDeficit);
        data.put("sensoryDeficit", sensoryDeficit);
        data.put("aphasia", Boolean.TRUE.equals(aphasia));
        data.put("cognitiveDisorders", Boolean.TRUE.equals(cognitiveDisorders));
        data.put("secondaryEpilepsy", Boolean.TRUE.equals(secondaryEpilepsy));
        data.put("residualDisability", residualDisability);
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveSequelae(patient, data);
        response.put("success", true);
        return response;
    }
}
