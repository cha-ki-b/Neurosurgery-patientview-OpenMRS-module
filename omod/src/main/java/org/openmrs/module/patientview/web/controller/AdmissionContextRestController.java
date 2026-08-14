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
public class AdmissionContextRestController {

    @RequestMapping(value = "/module/patientview/admissionContext.form", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getContexts(@RequestParam("patientId") Integer patientId) {
        PatientviewPrivileges.requireView();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            return response;
        }
        response.put("success", true);
        response.put("data", Context.getService(PatientviewService.class).getAdmissionContexts(patient));
        return response;
    }

    @RequestMapping(value = "/module/patientview/admissionContext.form", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveContext(@RequestParam("patientId") Integer patientId,
                                            @RequestParam(value = "admissionReason", required = false) String admissionReason,
                                            @RequestParam(value = "symptomOnsetDate", required = false) String symptomOnsetDate,
                                            @RequestParam(value = "diagnosisDelayDays", required = false) Integer diagnosisDelayDays,
                                            @RequestParam(value = "admissionDiagnosis", required = false) String admissionDiagnosis,
                                            @RequestParam(value = "primaryDiagnosis", required = false) String primaryDiagnosis,
                                            @RequestParam(value = "secondaryDiagnoses", required = false) String secondaryDiagnoses,
                                            @RequestParam(value = "profession", required = false) String profession,
                                            @RequestParam(value = "referringPhysician", required = false) String referringPhysician,
                                            @RequestParam(value = "responsibleNeurosurgeon", required = false) String responsibleNeurosurgeon) {
        PatientviewPrivileges.requireManage();
        Map<String, Object> response = new HashMap<>();
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient introuvable");
            return response;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("admissionReason", admissionReason);
        data.put("symptomOnsetDate", parseDate(symptomOnsetDate));
        data.put("diagnosisDelayDays", diagnosisDelayDays);
        data.put("admissionDiagnosis", admissionDiagnosis);
        data.put("primaryDiagnosis", primaryDiagnosis);
        data.put("secondaryDiagnoses", secondaryDiagnoses);
        data.put("profession", profession);
        data.put("referringPhysician", referringPhysician);
        data.put("responsibleNeurosurgeon", responsibleNeurosurgeon);

        Context.getService(PatientviewService.class).saveAdmissionContext(patient, data);
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
