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
        { label: "&Eacute;volution &amp; s&eacute;quelles" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "evolution"]) }

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

        <!-- &Eacute;volution postop&eacute;ratoire (Fiche section 11) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>&Eacute;volution postop&eacute;ratoire</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('postopEvolutionForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="postopEvolutionForm" class="neuro-form" style="display:none;" onsubmit="return submitPostopEvolution(event)">
                    <div class="neuro-form-grid">
                        <label>Glasgow postop&eacute;ratoire
                            <input type="number" name="postopGcs" min="3" max="15"/>
                        </label>
                        <label>Karnofsky postop&eacute;ratoire (%)
                            <input type="number" name="postopKarnofsky" min="0" max="100" step="10"/>
                        </label>
                        <label>&Eacute;tat neurologique
                            <input type="text" name="neurologicalStatus"/>
                        </label>
                        <label>Date du d&eacute;c&egrave;s
                            <input type="date" name="deathDate"/>
                        </label>
                    </div>
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="infection"/> Infection</label>
                        <label><input type="checkbox" name="hemorrhage"/> H&eacute;morragie</label>
                        <label><input type="checkbox" name="hydrocephalus"/> Hydroc&eacute;phalie</label>
                        <label><input type="checkbox" name="csfLeak"/> Fuite de LCR</label>
                        <label><input type="checkbox" name="seizures"/> Convulsions</label>
                        <label><input type="checkbox" name="deceased"/> D&eacute;c&egrave;s</label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!postopEvolutions) { %>
                        <div class="neuro-no-data">Aucune &eacute;volution postop&eacute;ratoire enregistr&eacute;e</div>
                    <% } else { postopEvolutions.each { e -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ e.dateCreated ? ui.format(e.dateCreated) : "" }</div>
                            <div><strong>Glasgow postop&eacute;ratoire :</strong> ${ e.postopGcs ? ui.format(e.postopGcs) : "\u2014" }</div>
                            <div><strong>Karnofsky postop&eacute;ratoire (%) :</strong> ${ e.postopKarnofsky ? ui.format(e.postopKarnofsky) : "\u2014" }</div>
                            <div><strong>&Eacute;tat neurologique :</strong> ${ e.neurologicalStatus ? ui.format(e.neurologicalStatus) : "\u2014" }</div>
                            <div><strong>Date du d&eacute;c&egrave;s :</strong> ${ e.deathDate ? ui.format(e.deathDate) : "\u2014" }</div>
                            <div class="neuro-flags">
                                <% if (e.infection) { %><span class="neuro-flag-tag">Infection</span><% } %>
                                <% if (e.hemorrhage) { %><span class="neuro-flag-tag">H&eacute;morragie</span><% } %>
                                <% if (e.hydrocephalus) { %><span class="neuro-flag-tag">Hydroc&eacute;phalie</span><% } %>
                                <% if (e.csfLeak) { %><span class="neuro-flag-tag">Fuite de LCR</span><% } %>
                                <% if (e.seizures) { %><span class="neuro-flag-tag">Convulsions</span><% } %>
                                <% if (e.deceased) { %><span class="neuro-flag-tag">D&eacute;c&egrave;s</span><% } %>
                            </div>
                            <% if (e.notes) { %><div>${ ui.format(e.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <!-- S&eacute;quelles (Fiche section 12) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>S&eacute;quelles</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('sequelaeForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="sequelaeForm" class="neuro-form" style="display:none;" onsubmit="return submitSequelae(event)">
                    <div class="neuro-form-grid">
                        <label>D&eacute;ficit moteur
                            <input type="text" name="motorDeficit"/>
                        </label>
                        <label>D&eacute;ficit sensitif
                            <input type="text" name="sensoryDeficit"/>
                        </label>
                        <label class="neuro-full-width">Handicap r&eacute;siduel
                            <textarea name="residualDisability"></textarea>
                        </label>
                    </div>
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="aphasia"/> Aphasie</label>
                        <label><input type="checkbox" name="cognitiveDisorders"/> Troubles cognitifs</label>
                        <label><input type="checkbox" name="secondaryEpilepsy"/> &Eacute;pilepsie secondaire</label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!sequelae) { %>
                        <div class="neuro-no-data">Aucune s&eacute;quelle enregistr&eacute;e</div>
                    <% } else { sequelae.each { q -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ q.dateCreated ? ui.format(q.dateCreated) : "" }</div>
                            <div><strong>D&eacute;ficit moteur :</strong> ${ q.motorDeficit ? ui.format(q.motorDeficit) : "\u2014" }</div>
                            <div><strong>D&eacute;ficit sensitif :</strong> ${ q.sensoryDeficit ? ui.format(q.sensoryDeficit) : "\u2014" }</div>
                            <div><strong>Handicap r&eacute;siduel :</strong> ${ q.residualDisability ? ui.format(q.residualDisability) : "\u2014" }</div>
                            <div class="neuro-flags">
                                <% if (q.aphasia) { %><span class="neuro-flag-tag">Aphasie</span><% } %>
                                <% if (q.cognitiveDisorders) { %><span class="neuro-flag-tag">Troubles cognitifs</span><% } %>
                                <% if (q.secondaryEpilepsy) { %><span class="neuro-flag-tag">&Eacute;pilepsie secondaire</span><% } %>
                            </div>
                            <% if (q.notes) { %><div>${ ui.format(q.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <% } %>
    </div>
</div>
