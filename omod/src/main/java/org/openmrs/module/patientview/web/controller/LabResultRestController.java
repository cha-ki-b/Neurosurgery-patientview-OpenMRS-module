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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LabResultRestController {

    @RequestMapping(value = "/module/patientview/labResult.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getLabResults(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getLabResults(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/labResult.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveLabResult(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "sampleDate", required = false) String sampleDate,
                                     @RequestParam(value = "completeBloodCount", required = false) String completeBloodCount,
                                     @RequestParam(value = "crp", required = false) String crp,
                                     @RequestParam(value = "ionogram", required = false) String ionogram,
                                     @RequestParam(value = "glycemia", required = false) String glycemia,
                                     @RequestParam(value = "creatinine", required = false) String creatinine,
                                     @RequestParam(value = "coagulation", required = false) String coagulation,
                                     @RequestParam(value = "bloodGroup", required = false) String bloodGroup,
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
        data.put("sampleDate", parseDate(sampleDate));
        data.put("completeBloodCount", completeBloodCount);
        data.put("crp", crp);
        data.put("ionogram", ionogram);
        data.put("glycemia", glycemia);
        data.put("creatinine", creatinine);
        data.put("coagulation", coagulation);
        data.put("bloodGroup", bloodGroup);
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveLabResult(patient, data);
        response.put("success", true);
        return response;
    }

    private static Date parseDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(isoDate);
        } catch (ParseException e) {
            return null;
        }
    }
}
