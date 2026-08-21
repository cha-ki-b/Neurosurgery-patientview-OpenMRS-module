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
        { label: "Prise en charge" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "priseEnCharge"]) }

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

        <!-- Traitement m&eacute;dical (Fiche section 9) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Traitement m&eacute;dical</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('medicalTreatmentForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="medicalTreatmentForm" class="neuro-form" style="display:none;" onsubmit="return submitMedicalTreatment(event)">
                    <div class="neuro-form-grid">
                        <label class="neuro-full-width">Autres traitements
                            <textarea name="otherTreatments"></textarea>
                        </label>
                    </div>
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="corticosteroids"/> Corticoth&eacute;rapie</label>
                        <label><input type="checkbox" name="antiepileptics"/> Anti&eacute;pileptiques</label>
                        <label><input type="checkbox" name="antibiotics"/> Antibioth&eacute;rapie</label>
                        <label><input type="checkbox" name="anticoagulants"/> Anticoagulants</label>
                        <label><input type="checkbox" name="analgesics"/> Antalgiques</label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!medicalTreatments) { %>
                        <div class="neuro-no-data">Aucun traitement m&eacute;dical enregistr&eacute;</div>
                    <% } else { medicalTreatments.each { t -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ t.dateCreated ? ui.format(t.dateCreated) : "" }</div>
                            <div><strong>Autres traitements :</strong> ${ t.otherTreatments ? ui.format(t.otherTreatments) : "\u2014" }</div>
                            <div class="neuro-flags">
                                <% if (t.corticosteroids) { %><span class="neuro-flag-tag">Corticoth&eacute;rapie</span><% } %>
                                <% if (t.antiepileptics) { %><span class="neuro-flag-tag">Anti&eacute;pileptiques</span><% } %>
                                <% if (t.antibiotics) { %><span class="neuro-flag-tag">Antibioth&eacute;rapie</span><% } %>
                                <% if (t.anticoagulants) { %><span class="neuro-flag-tag">Anticoagulants</span><% } %>
                                <% if (t.analgesics) { %><span class="neuro-flag-tag">Antalgiques</span><% } %>
                            </div>
                            <% if (t.notes) { %><div>${ ui.format(t.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <!-- Traitement chirurgical (Fiche section 9) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Traitement chirurgical</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('surgicalTreatmentForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="surgicalTreatmentForm" class="neuro-form" style="display:none;" onsubmit="return submitSurgicalTreatment(event)">
                    <div class="neuro-form-grid">
                        <label class="neuro-full-width">Intervention r&eacute;alis&eacute;e
                            <input type="text" name="procedurePerformed"/>
                        </label>
                        <label>Type d'ex&eacute;r&egrave;se
                            <select name="resectionType">
                                <option value="">--</option>
                                <option value="Totale">Totale</option>
                                <option value="Partielle">Partielle</option>
                                <option value="Biopsie">Biopsie</option>
                            </select>
                        </label>
                        <label>Date op&eacute;ratoire
                            <input type="date" name="surgeryDate"/>
                        </label>
                        <label>Dur&eacute;e (minutes)
                            <input type="number" name="durationMinutes" min="0"/>
                        </label>
                        <label>Chirurgien
                            <input type="text" name="surgeon"/>
                        </label>
                        <label class="neuro-full-width">Complications perop&eacute;ratoires
                            <textarea name="intraoperativeComplications"></textarea>
                        </label>
                    </div>
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="drainPlaced"/> Drain pos&eacute;</label>
                        <label><input type="checkbox" name="catheterPlaced"/> Sonde pos&eacute;e</label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!surgicalTreatments) { %>
                        <div class="neuro-no-data">Aucune intervention enregistr&eacute;e</div>
                    <% } else { surgicalTreatments.each { s -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ s.surgeryDate ? ui.format(s.surgeryDate) : (s.dateCreated ? ui.format(s.dateCreated) : "") }</div>
                            <div><strong>Intervention r&eacute;alis&eacute;e :</strong> ${ s.procedurePerformed ? ui.format(s.procedurePerformed) : "\u2014" }</div>
                            <div><strong>Type d'ex&eacute;r&egrave;se :</strong> ${ s.resectionType ? ui.format(s.resectionType) : "\u2014" }</div>
                            <div><strong>Dur&eacute;e (minutes) :</strong> ${ s.durationMinutes ? ui.format(s.durationMinutes) : "\u2014" }</div>
                            <div><strong>Chirurgien :</strong> ${ s.surgeon ? ui.format(s.surgeon) : "\u2014" }</div>
                            <div><strong>Complications perop&eacute;ratoires :</strong> ${ s.intraoperativeComplications ? ui.format(s.intraoperativeComplications) : "\u2014" }</div>
                            <div class="neuro-flags">
                                <% if (s.drainPlaced) { %><span class="neuro-flag-tag">Drain pos&eacute;</span><% } %>
                                <% if (s.catheterPlaced) { %><span class="neuro-flag-tag">Sonde pos&eacute;e</span><% } %>
                            </div>
                            <% if (s.notes) { %><div>${ ui.format(s.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <% } %>
    </div>
</div>
