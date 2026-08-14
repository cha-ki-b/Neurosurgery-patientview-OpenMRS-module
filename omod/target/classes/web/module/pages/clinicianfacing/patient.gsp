<%
    ui.decorateWith("appui", "standardEmrPage")
%>

${ ui.includeCss("patientview", "patientview.css") }
${ ui.includeJavascript("patientview", "patientview.js") }

<script type="text/javascript">
    var patientId = ${ patient.patientId };
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ patient.familyName ? ui.format(patient.familyName) : '' }, ${ patient.givenName ? ui.format(patient.givenName) : '' }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"}
    ];

    jq(function(){
        initializePatientDashboard();
    });
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "resume"]) }

    <div class="neuro-content">
        ${ ui.includeFragment("patientview", "patientHeader", [patient: patient]) }

        <% if (alerts) { %>
<<<<<<< HEAD
            <div class="neuro-alerts-banner">
                <% alerts.each { alertMessage -> %>
                    <div class="neuro-alert-item">&#9888; ${ ui.format(alertMessage) }</div>
=======
            <div class="patient-alerts-banner">
                <% alerts.each { alertMessage -> %>
                    <div class="alert-item">&#9888; ${ ui.format(alertMessage) }</div>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                <% } %>
            </div>
        <% } %>

<<<<<<< HEAD
        <div class="neuro-dashboard-grid">
            <div class="neuro-panel neuro-status">
                <div class="neuro-panel-header">
                    <h2>Statut neurologique</h2>
                </div>
                <div class="neuro-panel-content">
                    <div class="neuro-gcs-display">
                        <div class="neuro-gcs-score">
                            <span class="neuro-score-value">${ latestGCS.totalScore ? ui.format(latestGCS.totalScore) : "\u2014" }</span>
                            <span class="neuro-score-label">GCS / 15</span>
                        </div>
                        <div class="neuro-gcs-breakdown">
=======
        <div class="dashboard-grid">
            <div class="panel neuro-status">
                <div class="panel-header">
                    <h2>Statut neurologique</h2>
                </div>
                <div class="panel-content">
                    <div class="gcs-display">
                        <div class="gcs-score">
                            <span class="score-value">${ latestGCS.totalScore ? ui.format(latestGCS.totalScore) : "\u2014" }</span>
                            <span class="score-label">GCS / 15</span>
                        </div>
                        <div class="gcs-breakdown">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                            <div>E: ${ latestGCS.eyeResponse ?: "\u2014" }</div>
                            <div>V: ${ latestGCS.verbalResponse ?: "\u2014" }</div>
                            <div>M: ${ latestGCS.motorResponse ?: "\u2014" }</div>
                        </div>
                    </div>
<<<<<<< HEAD
                    <div class="neuro-karnofsky-score">
                        Karnofsky : <strong>${ latestGCS.karnofskyScore ? ui.format(latestGCS.karnofskyScore) : "\u2014" }</strong>
                    </div>
                    <div class="neuro-assessment-date">
=======
                    <div class="karnofsky-score">
                        Karnofsky : <strong>${ latestGCS.karnofskyScore ? ui.format(latestGCS.karnofskyScore) : "\u2014" }</strong>
                    </div>
                    <div class="assessment-date">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                        Derni&egrave;re &eacute;valuation : ${ latestGCS.dateRecorded ? ui.format(latestGCS.dateRecorded) : "Aucune &eacute;valuation" }
                    </div>
                </div>
            </div>

<<<<<<< HEAD
            <div class="neuro-panel">
                <div class="neuro-panel-header">
                    <h2>Diagnostic principal</h2>
                </div>
                <div class="neuro-panel-content">
                    <div class="neuro-procedure-name">${ ui.format(diagnosis) }</div>
=======
            <div class="panel">
                <div class="panel-header">
                    <h2>Diagnostic principal</h2>
                </div>
                <div class="panel-content">
                    <div class="procedure-name">${ ui.format(diagnosis) }</div>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                </div>
            </div>
        </div>

<<<<<<< HEAD
        <div class="neuro-quick-actions">
            <button class="neuro-action-btn" onclick="addNeuroAssessment(patientId)">Nouvelle &eacute;valuation Glasgow</button>
            <a class="neuro-action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patient.uuid]) }">Ant&eacute;c&eacute;dents</a>
            <a class="neuro-action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patient.uuid]) }">Examen clinique</a>
=======
        <div class="quick-actions">
            <button class="action-btn" onclick="addNeuroAssessment(patientId)">Nouvelle &eacute;valuation Glasgow</button>
            <a class="action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patient.uuid]) }">Ant&eacute;c&eacute;dents</a>
            <a class="action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patient.uuid]) }">Examen clinique</a>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
        </div>
    </div>
</div>

<!-- Neuro assessment modal (Glasgow Coma Scale + Karnofsky quick entry), reused from the Examen clinique tab -->
<<<<<<< HEAD
<div id="neuroAssessmentModal" class="neuro-modal" style="display:none;">
    <div class="neuro-modal-content">
        <span class="neuro-modal-close" onclick="closeNeuroAssessmentModal()">&times;</span>
=======
<div id="neuroAssessmentModal" class="modal" style="display:none;">
    <div class="modal-content">
        <span class="close" onclick="closeNeuroAssessmentModal()">&times;</span>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
        <h2>Nouvelle &eacute;valuation neurologique</h2>
        <div id="neuroAssessmentFormContent"></div>
    </div>
</div>
