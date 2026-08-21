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
        { label: "Sortie &amp; suivi" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "sortie"]) }

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

        <!-- Sortie (Fiche section 13) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Sortie</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('dischargeForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="dischargeForm" class="neuro-form" style="display:none;" onsubmit="return submitDischarge(event)">
                    <div class="neuro-form-grid">
                        <label>Date de sortie
                            <input type="date" name="dischargeDate"/>
                        </label>
                        <label>Mode de sortie
                            <select name="dischargeMode">
                                <option value="">--</option>
                                <option value="Gu&eacute;rison">Gu&eacute;rison</option>
                                <option value="Am&eacute;lioration">Am&eacute;lioration</option>
                                <option value="Stable">Stable</option>
                                <option value="Aggravation">Aggravation</option>
                                <option value="D&eacute;c&egrave;s">D&eacute;c&egrave;s</option>
                            </select>
                        </label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!discharges) { %>
                        <div class="neuro-no-data">Aucune sortie enregistr&eacute;e</div>
                    <% } else { discharges.each { d -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ d.dischargeDate ? ui.format(d.dischargeDate) : (d.dateCreated ? ui.format(d.dateCreated) : "") }</div>
                            <div><strong>Mode de sortie :</strong> ${ d.dischargeMode ? ui.format(d.dischargeMode) : "\u2014" }</div>
                            <% if (d.notes) { %><div>${ ui.format(d.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <!-- Suivi (Fiche section 14) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Suivi</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('followUpForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="followUpForm" class="neuro-form" style="display:none;" onsubmit="return submitFollowUp(event)">
                    <div class="neuro-form-grid">
                        <label>Date de consultation
                            <input type="date" name="consultationDate"/>
                        </label>
                        <label>Prochaine consultation
                            <input type="date" name="nextConsultationDate"/>
                        </label>
                        <label>Traitement en cours
                            <input type="text" name="currentTreatment"/>
                        </label>
                        <label class="neuro-full-width">Examen clinique
                            <textarea name="clinicalExam"></textarea>
                        </label>
                        <label class="neuro-full-width">Examen neurologique
                            <textarea name="neurologicalExam"></textarea>
                        </label>
                        <label class="neuro-full-width">IRM / scanner de contr&ocirc;le
                            <textarea name="controlImaging"></textarea>
                        </label>
                    </div>
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="recurrence"/> R&eacute;cidive</label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!followUps) { %>
                        <div class="neuro-no-data">Aucune consultation de suivi enregistr&eacute;e</div>
                    <% } else { followUps.each { v -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ v.consultationDate ? ui.format(v.consultationDate) : (v.dateCreated ? ui.format(v.dateCreated) : "") }</div>
                            <div><strong>Prochaine consultation :</strong> ${ v.nextConsultationDate ? ui.format(v.nextConsultationDate) : "\u2014" }</div>
                            <div><strong>Traitement en cours :</strong> ${ v.currentTreatment ? ui.format(v.currentTreatment) : "\u2014" }</div>
                            <div><strong>Examen clinique :</strong> ${ v.clinicalExam ? ui.format(v.clinicalExam) : "\u2014" }</div>
                            <div><strong>Examen neurologique :</strong> ${ v.neurologicalExam ? ui.format(v.neurologicalExam) : "\u2014" }</div>
                            <div><strong>IRM / scanner de contr&ocirc;le :</strong> ${ v.controlImaging ? ui.format(v.controlImaging) : "\u2014" }</div>
                            <div class="neuro-flags">
                                <% if (v.recurrence) { %><span class="neuro-flag-tag">R&eacute;cidive</span><% } %>
                            </div>
                            <% if (v.notes) { %><div>${ ui.format(v.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <% } %>
    </div>
</div>
