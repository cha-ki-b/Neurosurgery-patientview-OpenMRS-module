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
public class SurgicalTreatmentRestController {

    @RequestMapping(value = "/module/patientview/surgicalTreatment.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getSurgicalTreatments(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getSurgicalTreatments(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/surgicalTreatment.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveSurgicalTreatment(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "procedurePerformed", required = false) String procedurePerformed,
                                     @RequestParam(value = "resectionType", required = false) String resectionType,
                                     @RequestParam(value = "surgeryDate", required = false) String surgeryDate,
                                     @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,
                                     @RequestParam(value = "surgeon", required = false) String surgeon,
                                     @RequestParam(value = "intraoperativeComplications", required = false) String intraoperativeComplications,
                                     @RequestParam(value = "drainPlaced", required = false) Boolean drainPlaced,
                                     @RequestParam(value = "catheterPlaced", required = false) Boolean catheterPlaced,
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
        data.put("procedurePerformed", procedurePerformed);
        data.put("resectionType", resectionType);
        data.put("surgeryDate", parseDate(surgeryDate));
        data.put("durationMinutes", durationMinutes);
        data.put("surgeon", surgeon);
        data.put("intraoperativeComplications", intraoperativeComplications);
        data.put("drainPlaced", Boolean.TRUE.equals(drainPlaced));
        data.put("catheterPlaced", Boolean.TRUE.equals(catheterPlaced));
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveSurgicalTreatment(patient, data);
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
