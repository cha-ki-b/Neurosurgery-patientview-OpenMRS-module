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
        { label: "Biologie" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "biologie"]) }

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

        <!-- Biologie (Fiche section 7) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Biologie</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('labResultForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="labResultForm" class="neuro-form" style="display:none;" onsubmit="return submitLabResult(event)">
                    <div class="neuro-form-grid">
                        <label>Date du pr&eacute;l&egrave;vement
                            <input type="date" name="sampleDate"/>
                        </label>
                        <label>Groupe sanguin
                            <select name="bloodGroup">
                                <option value="">--</option>
                                <option value="A+">A+</option>
                                <option value="A-">A-</option>
                                <option value="B+">B+</option>
                                <option value="B-">B-</option>
                                <option value="AB+">AB+</option>
                                <option value="AB-">AB-</option>
                                <option value="O+">O+</option>
                                <option value="O-">O-</option>
                            </select>
                        </label>
                        <label>CRP
                            <input type="text" name="crp" placeholder="ex : 12 mg/L"/>
                        </label>
                        <label>Glyc&eacute;mie
                            <input type="text" name="glycemia" placeholder="ex : 0.95 g/L"/>
                        </label>
                        <label>Cr&eacute;atinine
                            <input type="text" name="creatinine" placeholder="ex : 8 mg/L"/>
                        </label>
                        <label class="neuro-full-width">NFS
                            <textarea name="completeBloodCount" placeholder="ex : Hb 12.4 g/dL, GB 8.2 G/L, Plq 250 G/L"></textarea>
                        </label>
                        <label class="neuro-full-width">Ionogramme
                            <textarea name="ionogram" placeholder="ex : Na 138, K 4.1, Cl 102 mmol/L"></textarea>
                        </label>
                        <label class="neuro-full-width">Bilan de coagulation
                            <textarea name="coagulation" placeholder="ex : TP 92%, INR 1.05, TCA 32 s"></textarea>
                        </label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!labResults) { %>
                        <div class="neuro-no-data">Aucun bilan biologique enregistr&eacute;</div>
                    <% } else { labResults.each { b -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ b.sampleDate ? ui.format(b.sampleDate) : (b.dateCreated ? ui.format(b.dateCreated) : "") }</div>
                            <div><strong>Groupe sanguin :</strong> ${ b.bloodGroup ? ui.format(b.bloodGroup) : "\u2014" }</div>
                            <div><strong>CRP :</strong> ${ b.crp ? ui.format(b.crp) : "\u2014" }</div>
                            <div><strong>Glyc&eacute;mie :</strong> ${ b.glycemia ? ui.format(b.glycemia) : "\u2014" }</div>
                            <div><strong>Cr&eacute;atinine :</strong> ${ b.creatinine ? ui.format(b.creatinine) : "\u2014" }</div>
                            <div><strong>NFS :</strong> ${ b.completeBloodCount ? ui.format(b.completeBloodCount) : "\u2014" }</div>
                            <div><strong>Ionogramme :</strong> ${ b.ionogram ? ui.format(b.ionogram) : "\u2014" }</div>
                            <div><strong>Bilan de coagulation :</strong> ${ b.coagulation ? ui.format(b.coagulation) : "\u2014" }</div>
                            <% if (b.notes) { %><div>${ ui.format(b.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <% } %>
    </div>
</div>
