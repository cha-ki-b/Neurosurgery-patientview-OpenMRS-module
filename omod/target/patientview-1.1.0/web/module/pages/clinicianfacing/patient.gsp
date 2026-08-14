<%
    ui.decorateWith("appui", "standardEmrPage")
%>

${ ui.includeCss("patientview", "patientview.css") }
${ ui.includeJavascript("patientview", "patientview.js") }

<script type="text/javascript">
    var patientId = ${ patient.patientId };
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"}
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
            <div class="patient-alerts-banner">
                <% alerts.each { alertMessage -> %>
                    <div class="alert-item">&#9888; ${ ui.format(alertMessage) }</div>
                <% } %>
            </div>
        <% } %>

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
                            <div>E: ${ latestGCS.eyeResponse ?: "\u2014" }</div>
                            <div>V: ${ latestGCS.verbalResponse ?: "\u2014" }</div>
                            <div>M: ${ latestGCS.motorResponse ?: "\u2014" }</div>
                        </div>
                    </div>
                    <div class="karnofsky-score">
                        Karnofsky : <strong>${ latestGCS.karnofskyScore ? ui.format(latestGCS.karnofskyScore) : "\u2014" }</strong>
                    </div>
                    <div class="assessment-date">
                        Derni&egrave;re &eacute;valuation : ${ latestGCS.dateRecorded ? ui.format(latestGCS.dateRecorded) : "Aucune &eacute;valuation" }
                    </div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-header">
                    <h2>Diagnostic principal</h2>
                </div>
                <div class="panel-content">
                    <div class="procedure-name">${ ui.format(diagnosis) }</div>
                </div>
            </div>
        </div>

        <div class="quick-actions">
            <button class="action-btn" onclick="addNeuroAssessment(patientId)">Nouvelle &eacute;valuation Glasgow</button>
            <a class="action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patient.uuid]) }">Ant&eacute;c&eacute;dents</a>
            <a class="action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patient.uuid]) }">Examen clinique</a>
        </div>
    </div>
</div>

<!-- Neuro assessment modal (Glasgow Coma Scale + Karnofsky quick entry), reused from the Examen clinique tab -->
<div id="neuroAssessmentModal" class="modal" style="display:none;">
    <div class="modal-content">
        <span class="close" onclick="closeNeuroAssessmentModal()">&times;</span>
        <h2>Nouvelle &eacute;valuation neurologique</h2>
        <div id="neuroAssessmentFormContent"></div>
    </div>
</div>
