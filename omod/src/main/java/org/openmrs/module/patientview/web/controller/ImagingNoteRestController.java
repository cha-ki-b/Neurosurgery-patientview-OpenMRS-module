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
public class ImagingNoteRestController {

    @RequestMapping(value = "/module/patientview/imagingNote.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getImagingNotes(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getImagingNotes(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/imagingNote.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveImagingNote(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "examType", required = false) String examType,
                                     @RequestParam(value = "examDate", required = false) String examDate,
                                     @RequestParam(value = "findings", required = false) String findings,
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
        data.put("examType", examType);
        data.put("examDate", parseDate(examDate));
        data.put("findings", findings);
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveImagingNote(patient, data);
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
