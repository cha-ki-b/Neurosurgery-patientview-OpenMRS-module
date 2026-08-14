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
            <div class="neuro-alerts-banner">
                <% alerts.each { alertMessage -> %>
                    <div class="neuro-alert-item">&#9888; ${ ui.format(alertMessage) }</div>
                <% } %>
            </div>
        <% } %>

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
                            <div>E: ${ latestGCS.eyeResponse ?: "\u2014" }</div>
                            <div>V: ${ latestGCS.verbalResponse ?: "\u2014" }</div>
                            <div>M: ${ latestGCS.motorResponse ?: "\u2014" }</div>
                        </div>
                    </div>
                    <div class="neuro-karnofsky-score">
                        Karnofsky : <strong>${ latestGCS.karnofskyScore ? ui.format(latestGCS.karnofskyScore) : "\u2014" }</strong>
                    </div>
                    <div class="neuro-assessment-date">
                        Derni&egrave;re &eacute;valuation : ${ latestGCS.dateRecorded ? ui.format(latestGCS.dateRecorded) : "Aucune &eacute;valuation" }
                    </div>
                </div>
            </div>

            <div class="neuro-panel">
                <div class="neuro-panel-header">
                    <h2>Diagnostic principal</h2>
                </div>
                <div class="neuro-panel-content">
                    <div class="neuro-procedure-name">${ ui.format(diagnosis) }</div>
                </div>
            </div>
        </div>

        <div class="neuro-quick-actions">
            <button class="neuro-action-btn" onclick="addNeuroAssessment(patientId)">Nouvelle &eacute;valuation Glasgow</button>
            <a class="neuro-action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patient.uuid]) }">Ant&eacute;c&eacute;dents</a>
            <a class="neuro-action-btn" href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patient.uuid]) }">Examen clinique</a>
        </div>
    </div>
</div>

<!-- Neuro assessment modal (Glasgow Coma Scale + Karnofsky quick entry), reused from the Examen clinique tab -->
<div id="neuroAssessmentModal" class="neuro-modal" style="display:none;">
    <div class="neuro-modal-content">
        <span class="neuro-modal-close" onclick="closeNeuroAssessmentModal()">&times;</span>
        <h2>Nouvelle &eacute;valuation neurologique</h2>
        <div id="neuroAssessmentFormContent"></div>
    </div>
</div>
