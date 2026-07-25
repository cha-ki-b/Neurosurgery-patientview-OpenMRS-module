package org.openmrs.module.patientview.web.controller;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientview.api.PatientviewService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class NeuroAssessmentRestController {

    @RequestMapping(value = "/module/patientview/neuroAssessment.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getLatestAssessment(@RequestParam("patientId") Integer patientId) {
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        Map<String, Object> gcs = Context.getService(PatientviewService.class).getLatestGCS(patient);
        response.put("success", true);
        response.put("data", Collections.singletonMap("latestGCS", gcs));
        return response;
    }

    @RequestMapping(value = "/module/patientview/checkAlerts.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> checkAlerts(@RequestParam("patientId") Integer patientId) {
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        response.put("newAlerts", patient == null
                ? Collections.emptyList()
                : Context.getService(PatientviewService.class).getActiveAlerts(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/addNeuroAssessment.form", method = RequestMethod.GET)
    @ResponseBody
    public String showForm(@RequestParam("patientId") Integer patientId) {
        return "<form id=\"neuroAssessmentForm\">"
                + "<input type=\"hidden\" name=\"patientId\" value=\"" + patientId + "\"/>"
                + "<label>Eye Response <select id=\"eyeResponse\" name=\"eyeResponse\">"
                + "<option value=\"1\">1</option><option value=\"2\">2</option>"
                + "<option value=\"3\">3</option><option value=\"4\">4</option></select></label><br/>"
                + "<label>Verbal Response <select id=\"verbalResponse\" name=\"verbalResponse\">"
                + "<option value=\"1\">1</option><option value=\"2\">2</option><option value=\"3\">3</option>"
                + "<option value=\"4\">4</option><option value=\"5\">5</option></select></label><br/>"
                + "<label>Motor Response <select id=\"motorResponse\" name=\"motorResponse\">"
                + "<option value=\"1\">1</option><option value=\"2\">2</option><option value=\"3\">3</option>"
                + "<option value=\"4\">4</option><option value=\"5\">5</option><option value=\"6\">6</option>"
                + "</select></label><br/>"
                + "<span id=\"gcsTotal\">0</span>"
                + "<textarea id=\"notes\" name=\"notes\"></textarea>"
                + "<button type=\"submit\">Save</button>"
                + "</form>";
    }

    @RequestMapping(value = "/module/patientview/addNeuroAssessment.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveAssessment(@RequestParam("patientId") Integer patientId,
                                               @RequestParam("eyeResponse") Integer eyeResponse,
                                               @RequestParam("verbalResponse") Integer verbalResponse,
                                               @RequestParam("motorResponse") Integer motorResponse,
                                               @RequestParam(value = "notes", required = false) String notes) {
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("eyeResponse", eyeResponse);
        data.put("verbalResponse", verbalResponse);
        data.put("motorResponse", motorResponse);
        data.put("notes", notes);
        Context.getService(PatientviewService.class).saveNeuroAssessment(patient, data);
        response.put("success", true);
        return response;
    }
}
