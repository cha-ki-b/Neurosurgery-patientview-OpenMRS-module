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
        { label: "${ ui.escapeJs(patientDisplayName) }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"},
        { label: "Diagnostic neurochirurgical" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "diagnostic"]) }

    <div class="neuro-content">
        ${ ui.includeFragment("patientview", "patientHeader", [patient: patient, displayName: patientDisplayName]) }

        <% if (accessDenied) { %>
            <div class="neuro-panel">
                <div class="neuro-panel-content">
                    <div class="neuro-no-data">
                        Vous n'avez pas l'autorisation de consulter le dossier de neurochirurgie de ce patient.
                        Contactez un administrateur si vous pensez que c'est une erreur.
                    </div>
                </div>
            </div>
        <% } else { %>

        <!-- Diagnostic neurochirurgical (Fiche section 8) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Diagnostic neurochirurgical</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('diagnosisForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="diagnosisForm" class="neuro-form" style="display:none;" onsubmit="return submitNeurosurgicalDiagnosis(event)">
                    <div class="neuro-form-grid">
                        <label class="neuro-full-width">Diagnostic retenu
                            <input type="text" name="diagnosis"/>
                        </label>
                        <label>Localisation de la l&eacute;sion
                            <input type="text" name="lesionLocation"/>
                        </label>
                        <label>Lat&eacute;ralit&eacute;
                            <select name="laterality">
                                <option value="">--</option>
                                <option value="Droite">Droite</option>
                                <option value="Gauche">Gauche</option>
                                <option value="Bilat\u00e9rale">Bilat&eacute;rale</option>
                                <option value="Non applicable">Non applicable</option>
                            </select>
                        </label>
                        <label>Taille
                            <input type="text" name="size" placeholder="ex : 3.2 cm"/>
                        </label>
                        <label>Nombre de l&eacute;sions
                            <input type="number" name="lesionCount" min="0"/>
                        </label>
                        <label>Diagnostic OMS (si tumeur)
                            <input type="text" name="whoDiagnosis"/>
                        </label>
                    </div>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!diagnoses) { %>
                        <div class="neuro-no-data">Aucun diagnostic neurochirurgical enregistr&eacute;</div>
                    <% } else { diagnoses.each { d -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ d.dateCreated ? ui.format(d.dateCreated) : "" }</div>
                            <div><strong>Diagnostic :</strong> ${ d.diagnosis ? ui.format(d.diagnosis) : "\u2014" }</div>
                            <div>
                                <strong>Localisation :</strong> ${ d.lesionLocation ? ui.format(d.lesionLocation) : "\u2014" }
                                &nbsp;|&nbsp; <strong>Lat&eacute;ralit&eacute; :</strong> ${ d.laterality ? ui.format(d.laterality) : "\u2014" }
                            </div>
                            <div>
                                <strong>Taille :</strong> ${ d.size ? ui.format(d.size) : "\u2014" }
                                &nbsp;|&nbsp; <strong>Nombre de l&eacute;sions :</strong> ${ d.lesionCount != null ? ui.format(d.lesionCount) : "\u2014" }
                            </div>
                            <% if (d.whoDiagnosis) { %>
                                <div><strong>Diagnostic OMS :</strong> ${ ui.format(d.whoDiagnosis) }</div>
                            <% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <% } %>
    </div>
</div>
