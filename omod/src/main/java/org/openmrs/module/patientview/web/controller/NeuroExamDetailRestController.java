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
public class NeuroExamDetailRestController {

    @RequestMapping(value = "/module/patientview/neuroExamDetail.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getExams(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getNeuroExamDetails(patient, 20));
        return response;
    }

    @RequestMapping(value = "/module/patientview/neuroExamDetail.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveExam(@RequestParam("patientId") Integer patientId,
                                         @RequestParam(value = "orientation", required = false) String orientation,
                                         @RequestParam(value = "language", required = false) String language,
                                         @RequestParam(value = "memory", required = false) String memory,
                                         @RequestParam(value = "cranialNerves", required = false) String cranialNerves,
                                         @RequestParam(value = "motorDeficit", required = false) String motorDeficit,
                                         @RequestParam(value = "sensoryDeficit", required = false) String sensoryDeficit,
                                         @RequestParam(value = "reflexes", required = false) String reflexes,
                                         @RequestParam(value = "plantarReflex", required = false) String plantarReflex,
                                         @RequestParam(value = "coordination", required = false) String coordination,
                                         @RequestParam(value = "gait", required = false) String gait,
                                         @RequestParam(value = "balance", required = false) String balance,
                                         @RequestParam(value = "cerebellarSigns", required = false) Boolean cerebellarSigns,
                                         @RequestParam(value = "pyramidalSyndrome", required = false) Boolean pyramidalSyndrome,
                                         @RequestParam(value = "meningealSyndrome", required = false) Boolean meningealSyndrome,
                                         @RequestParam(value = "seizures", required = false) Boolean seizures,
                                         @RequestParam(value = "headache", required = false) Boolean headache,
                                         @RequestParam(value = "vomiting", required = false) Boolean vomiting,
                                         @RequestParam(value = "visualDisturbances", required = false) Boolean visualDisturbances,
                                         @RequestParam(value = "sphincterDisturbances", required = false) Boolean sphincterDisturbances,
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
        data.put("orientation", orientation);
        data.put("language", language);
        data.put("memory", memory);
        data.put("cranialNerves", cranialNerves);
        data.put("motorDeficit", motorDeficit);
        data.put("sensoryDeficit", sensoryDeficit);
        data.put("reflexes", reflexes);
        data.put("plantarReflex", plantarReflex);
        data.put("coordination", coordination);
        data.put("gait", gait);
        data.put("balance", balance);
        data.put("cerebellarSigns", Boolean.TRUE.equals(cerebellarSigns));
        data.put("pyramidalSyndrome", Boolean.TRUE.equals(pyramidalSyndrome));
        data.put("meningealSyndrome", Boolean.TRUE.equals(meningealSyndrome));
        data.put("seizures", Boolean.TRUE.equals(seizures));
        data.put("headache", Boolean.TRUE.equals(headache));
        data.put("vomiting", Boolean.TRUE.equals(vomiting));
        data.put("visualDisturbances", Boolean.TRUE.equals(visualDisturbances));
        data.put("sphincterDisturbances", Boolean.TRUE.equals(sphincterDisturbances));
        data.put("notes", notes);

        Context.getService(PatientviewService.class).saveNeuroExamDetail(patient, data);
        response.put("success", true);
        return response;
    }
}
