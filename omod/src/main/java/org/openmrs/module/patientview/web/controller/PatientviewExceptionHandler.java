package org.openmrs.module.patientview.web.controller;

import org.openmrs.api.APIAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Every read/write method on PatientviewService is annotated with @Authorized, which OpenMRS's
 * AuthorizationAdvice enforces before the method body ever runs, throwing an
 * APIAuthenticationException if the current user lacks the required privilege
 * ("View Neurosurgery Data" / "Manage Neurosurgery Data"). Without this handler, that exception
 * would otherwise surface to the browser as a generic 500 error page; this turns it into a clean
 * 403 JSON response that the module's JS can show as a normal error message.
 */
@ControllerAdvice(basePackages = "org.openmrs.module.patientview.web.controller")
public class PatientviewExceptionHandler {

    @ExceptionHandler(APIAuthenticationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public Map<String, Object> handleAuthorizationFailure(APIAuthenticationException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Vous n'avez pas les autorisations n\u00e9cessaires pour cette action.");
        return response;
    }
}
