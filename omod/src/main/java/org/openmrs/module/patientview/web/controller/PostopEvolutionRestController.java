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
public class PostopEvolutionRestController {

    @RequestMapping(value = "/module/patientview/postopEvolution.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getPostopEvolutions(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getPostopEvolutions(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/postopEvolution.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> savePostopEvolution(@RequestParam("patientId") Integer patientId,
                                     @RequestParam(value = "postopGcs", required = false) Integer postopGcs,
                                     @RequestParam(value = "postopKarnofsky", required = false) Integer postopKarnofsky,
                                     @RequestParam(value = "neurologicalStatus", required = false) String neurologicalStatus,
                                     @RequestParam(value = "infection", required = false) Boolean infection,
                                     @RequestParam(value = "hemorrhage", required = false) Boolean hemorrhage,
                                     @RequestParam(value = "hydrocephalus", required = false) Boolean hydrocephalus,
                                     @RequestParam(value = "csfLeak", required = false) Boolean csfLeak,
                                     @RequestParam(value = "seizures", required = false) Boolean seizures,
                                     @RequestParam(value = "deceased", required = false) Boolean deceased,
                                     @RequestParam(value = "deathDate", required = false) String deathDate,
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
        data.put("postopGcs", postopGcs);
        data.put("postopKarnofsky", postopKarnofsky);
        data.put("neurologicalStatus", neurologicalStatus);
        data.put("infection", Boolean.TRUE.equals(infection));
        data.put("hemorrhage", Boolean.TRUE.equals(hemorrhage));
        data.put("hydrocephalus", Boolean.TRUE.equals(hydrocephalus));
        data.put("csfLeak", Boolean.TRUE.equals(csfLeak));
        data.put("seizures", Boolean.TRUE.equals(seizures));
        data.put("deceased", Boolean.TRUE.equals(deceased));
        data.put("deathDate", parseDate(deathDate));
        data.put("notes", notes);

        Context.getService(PatientviewService.class).savePostopEvolution(patient, data);
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
