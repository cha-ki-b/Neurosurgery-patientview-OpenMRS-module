/**
 * Generic CRUD helpers for the neurosurgery dashboard's Phase 1 tabs
 * (Antecedents, Examen clinique). Each record type is a simple
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
        error: function() {
            showErrorMessage('Erreur lors de l\'enregistrement');
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
