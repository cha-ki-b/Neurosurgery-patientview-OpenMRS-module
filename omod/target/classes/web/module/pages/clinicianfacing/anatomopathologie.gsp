<%
    ui.decorateWith("appui", "standardEmrPage")
%>

${ ui.includeCss("patientview", "patientview.css") }
${ ui.includeJavascript("patientview", "patientview.js") }
${ ui.includeJavascript("patientview", "patientview-crud.js") }

<script type="text/javascript">
    var patientId = ${ patient.patientId };
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ patient.familyName ? ui.format(patient.familyName) : '' }, ${ patient.givenName ? ui.format(patient.givenName) : '' }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"},
        { label: "Anatomopathologie" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "anatomopathologie"]) }

    <div class="neuro-content">
        ${ ui.includeFragment("patientview", "patientHeader", [patient: patient]) }

        <!-- Anatomopathologie (Fiche section 10) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Anatomopathologie</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('pathologyForm')">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <form id="pathologyForm" class="neuro-form" style="display:none;" onsubmit="return submitPathologyReport(event)">
                    <div class="neuro-form-grid">
                        <label class="neuro-full-width">&Eacute;tude anatomopathologique
                            <textarea name="studyFindings"></textarea>
                        </label>
                        <label>Grade OMS
                            <select name="whoGrade">
                                <option value="">--</option>
                                <option value="I">I</option>
                                <option value="II">II</option>
                                <option value="III">III</option>
                                <option value="IV">IV</option>
                            </select>
                        </label>
                        <label class="neuro-full-width">Immunohistochimie
                            <textarea name="immunohistochemistry"></textarea>
                        </label>
                        <label class="neuro-full-width">Marqueurs mol&eacute;culaires
                            <textarea name="molecularMarkers"></textarea>
                        </label>
                        <label class="neuro-full-width">Diagnostic final
                            <input type="text" name="finalDiagnosis"/>
                        </label>
                    </div>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>

                <div class="neuro-record-list">
                    <% if (!pathologyReports) { %>
                        <div class="neuro-no-data">Aucun rapport anatomopathologique enregistr&eacute;</div>
                    <% } else { pathologyReports.each { p -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ p.dateCreated ? ui.format(p.dateCreated) : "" }</div>
                            <% if (p.finalDiagnosis) { %>
                                <div><strong>Diagnostic final :</strong> ${ ui.format(p.finalDiagnosis) }</div>
                            <% } %>
                            <% if (p.whoGrade) { %>
                                <div><strong>Grade OMS :</strong> ${ ui.format(p.whoGrade) }</div>
                            <% } %>
                            <% if (p.studyFindings) { %>
                                <div><strong>&Eacute;tude :</strong> ${ ui.format(p.studyFindings) }</div>
                            <% } %>
                            <% if (p.immunohistochemistry) { %>
                                <div><strong>Immunohistochimie :</strong> ${ ui.format(p.immunohistochemistry) }</div>
                            <% } %>
                            <% if (p.molecularMarkers) { %>
                                <div><strong>Marqueurs mol&eacute;culaires :</strong> ${ ui.format(p.molecularMarkers) }</div>
                            <% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>
    </div>
</div>
