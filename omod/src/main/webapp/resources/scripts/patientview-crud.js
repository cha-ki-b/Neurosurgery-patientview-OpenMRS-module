/**
 * Generic CRUD helpers for every tab of the neurosurgery dashboard.
 * Each record type is a simple
 * "fill a form, POST it, reload the page" flow: the server always
 * renders the current list, so there is no client-side list state
 * to keep in sync.
 */

function toggleForm(formId) {
    var form = document.getElementById(formId);
    if (!form) {
        return;
    }
    var hidden = (form.style.display === 'none' || !form.style.display);
    form.style.display = hidden ? 'block' : 'none';
}

function postPatientviewForm(url, formEvent) {
    formEvent.preventDefault();
    var form = formEvent.target;
    var params = new URLSearchParams(new FormData(form));
    params.set('patientId', patientId);

    jQuery.ajax({
        url: url,
        type: 'POST',
        data: params.toString(),
        contentType: 'application/x-www-form-urlencoded',
        success: function(response) {
            if (response && response.success) {
                window.location.reload();
            } else {
                showErrorMessage((response && response.message) || 'Erreur lors de l\'enregistrement');
            }
        },
        error: function(xhr) {
            var message = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                || 'Erreur lors de l\'enregistrement';
            showErrorMessage(message);
        }
    });
    return false;
}

function submitAdmissionContext(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/admissionContext.form', event);
}

function submitMedicalHistory(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/medicalHistory.form', event);
}

function submitSurgicalHistory(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/surgicalHistory.form', event);
}

function submitVitalSigns(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/vitalSigns.form', event);
}

function submitNeuroExamDetail(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/neuroExamDetail.form', event);
}

function submitNeurosurgicalDiagnosis(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/neurosurgicalDiagnosis.form', event);
}

function submitPathologyReport(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/pathology.form', event);
}

function submitMedicalTreatment(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/medicalTreatment.form', event);
}

function submitSurgicalTreatment(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/surgicalTreatment.form', event);
}

function submitPostopEvolution(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/postopEvolution.form', event);
}

function submitSequelae(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/sequelae.form', event);
}

function submitLabResult(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/labResult.form', event);
}

function submitDischarge(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/discharge.form', event);
}

function submitFollowUp(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/followUp.form', event);
}

function submitImagingNote(event) {
    return postPatientviewForm(openmrsContextPath + '/module/patientview/imagingNote.form', event);
}

/**
 * Exports this patient's records into the core clinical model, so the FHIR2 module can serve
 * them. Records save automatically; this is for rows created before 1.4.0, or a re-run after
 * new concepts have been curated. Safe to press twice - already-exported rows are skipped.
 */
function syncFhirProjection() {
    jQuery.ajax({
        url: openmrsContextPath + '/module/patientview/fhirProjection.form',
        type: 'POST',
        data: { patientId: patientId },
        success: function(response) {
            if (response && response.success) {
                var d = response.data || {};
                showSuccessMessage('Export FHIR : ' + (d.encounters || 0) + ' consultation(s), '
                    + (d.observations || 0) + ' observation(s), '
                    + (d.conditions || 0) + ' diagnostic(s).');
            } else {
                showErrorMessage((response && response.message) || "Erreur lors de l'export");
            }
        },
        error: function(xhr) {
            showErrorMessage((xhr && xhr.responseJSON && xhr.responseJSON.message)
                || "Erreur lors de l'export");
        }
    });
}
