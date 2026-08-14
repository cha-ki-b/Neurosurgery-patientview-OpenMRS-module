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
        { label: "Ant\u00e9c\u00e9dents" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "antecedents"]) }

    <div class="neuro-content">
        ${ ui.includeFragment("patientview", "patientHeader", [patient: patient]) }

        <!-- Contexte d'admission (Fiche section 2, + partie non redondante de la section 1) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Contexte d'admission</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('admissionContextForm')">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <form id="admissionContextForm" class="neuro-form" style="display:none;" onsubmit="return submitAdmissionContext(event)">
                    <div class="neuro-form-grid">
                        <label>Motif d'admission
                            <input type="text" name="admissionReason"/>
                        </label>
                        <label>Date de d&eacute;but des sympt&ocirc;mes
                            <input type="date" name="symptomOnsetDate"/>
                        </label>
                        <label>D&eacute;lai avant diagnostic (jours)
                            <input type="number" name="diagnosisDelayDays" min="0"/>
                        </label>
                        <label>Diagnostic d'admission
                            <input type="text" name="admissionDiagnosis"/>
                        </label>
                        <label>Diagnostic principal
                            <input type="text" name="primaryDiagnosis"/>
                        </label>
                        <label>Profession
                            <input type="text" name="profession"/>
                        </label>
                        <label>M&eacute;decin r&eacute;f&eacute;rent
                            <input type="text" name="referringPhysician"/>
                        </label>
                        <label>Neurochirurgien responsable
                            <input type="text" name="responsibleNeurosurgeon"/>
                        </label>
                        <label class="neuro-full-width">Diagnostics secondaires
                            <textarea name="secondaryDiagnoses"></textarea>
                        </label>
                    </div>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>

                <div class="neuro-record-list">
                    <% if (!admissionContexts) { %>
                        <div class="neuro-no-data">Aucun contexte d'admission enregistr&eacute;</div>
                    <% } else { admissionContexts.each { ctx -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ ctx.dateCreated ? ui.format(ctx.dateCreated) : "" }</div>
                            <div><strong>Motif :</strong> ${ ctx.admissionReason ? ui.format(ctx.admissionReason) : "\u2014" }</div>
                            <div><strong>Diagnostic principal :</strong> ${ ctx.primaryDiagnosis ? ui.format(ctx.primaryDiagnosis) : "\u2014" }</div>
                            <div><strong>Diagnostic d'admission :</strong> ${ ctx.admissionDiagnosis ? ui.format(ctx.admissionDiagnosis) : "\u2014" }</div>
                            <% if (ctx.secondaryDiagnoses) { %>
                                <div><strong>Diagnostics secondaires :</strong> ${ ui.format(ctx.secondaryDiagnoses) }</div>
                            <% } %>
                            <div>
                                <strong>D&eacute;but des sympt&ocirc;mes :</strong> ${ ctx.symptomOnsetDate ? ui.format(ctx.symptomOnsetDate) : "\u2014" }
                                &nbsp;|&nbsp;
                                <strong>D&eacute;lai :</strong> ${ ctx.diagnosisDelayDays != null ? ui.format(ctx.diagnosisDelayDays) + " j" : "\u2014" }
                            </div>
                            <div>
                                <strong>Profession :</strong> ${ ctx.profession ? ui.format(ctx.profession) : "\u2014" }
                                &nbsp;|&nbsp;
                                <strong>M&eacute;decin r&eacute;f&eacute;rent :</strong> ${ ctx.referringPhysician ? ui.format(ctx.referringPhysician) : "\u2014" }
                                &nbsp;|&nbsp;
                                <strong>Neurochirurgien :</strong> ${ ctx.responsibleNeurosurgeon ? ui.format(ctx.responsibleNeurosurgeon) : "\u2014" }
                            </div>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <!-- Antecedents medicaux (Fiche section 3) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Ant&eacute;c&eacute;dents m&eacute;dicaux</h2>
            </div>
            <div class="neuro-panel-content">
                <form id="medicalHistoryForm" class="neuro-form" onsubmit="return submitMedicalHistory(event)">
                    <div class="neuro-checklist-grid">
                        <label><input type="checkbox" name="diabetes" ${ medicalHistory.diabetes ? "checked" : "" }/> Diab&egrave;te</label>
                        <label><input type="checkbox" name="hypertension" ${ medicalHistory.hypertension ? "checked" : "" }/> Hypertension</label>
                        <label><input type="checkbox" name="epilepsy" ${ medicalHistory.epilepsy ? "checked" : "" }/> &Eacute;pilepsie</label>
                        <label><input type="checkbox" name="stroke" ${ medicalHistory.stroke ? "checked" : "" }/> AVC</label>
                        <label><input type="checkbox" name="heartDisease" ${ medicalHistory.heartDisease ? "checked" : "" }/> Cardiopathie</label>
                        <label><input type="checkbox" name="renalFailure" ${ medicalHistory.renalFailure ? "checked" : "" }/> Insuffisance r&eacute;nale</label>
                    </div>
                    <div class="neuro-form-grid">
                        <label class="neuro-full-width">Allergies
                            <input type="text" name="allergies" value="${ medicalHistory.allergies ? ui.format(medicalHistory.allergies) : '' }"/>
                        </label>
                        <label class="neuro-full-width">Traitement chronique
                            <input type="text" name="chronicTreatment" value="${ medicalHistory.chronicTreatment ? ui.format(medicalHistory.chronicTreatment) : '' }"/>
                        </label>
                        <label class="neuro-full-width">Autres ant&eacute;c&eacute;dents
                            <textarea name="otherHistory">${ medicalHistory.otherHistory ? ui.format(medicalHistory.otherHistory) : '' }</textarea>
                        </label>
                    </div>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                    <% if (medicalHistory.dateChanged) { %>
                        <span class="neuro-assessment-date">Derni&egrave;re mise &agrave; jour : ${ ui.format(medicalHistory.dateChanged) }</span>
                    <% } %>
                </form>
            </div>
        </div>

        <!-- Antecedents chirurgicaux (Fiche section 3) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Ant&eacute;c&eacute;dents chirurgicaux</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('surgicalHistoryForm')">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <form id="surgicalHistoryForm" class="neuro-form" style="display:none;" onsubmit="return submitSurgicalHistory(event)">
                    <div class="neuro-form-grid">
                        <label>Intervention
                            <input type="text" name="procedureName" required="required"/>
                        </label>
                        <label>Date
                            <input type="date" name="datePerformed"/>
                        </label>
                        <label>Localisation
                            <input type="text" name="location"/>
                        </label>
                        <label>Chirurgien
                            <input type="text" name="surgeon"/>
                        </label>
                        <label class="neuro-full-width">R&eacute;sultat
                            <input type="text" name="outcome"/>
                        </label>
                        <label class="neuro-full-width">Notes
                            <textarea name="notes"></textarea>
                        </label>
                    </div>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>

                <div class="neuro-surgical-timeline">
                    <% if (!surgicalHistory) { %>
                        <div class="neuro-no-data">Aucun ant&eacute;c&eacute;dent chirurgical enregistr&eacute;</div>
                    <% } else { surgicalHistory.each { s -> %>
                        <div class="neuro-surgery-item">
                            <div class="neuro-surgery-date">${ s.date ? ui.format(s.date) : "\u2014" }</div>
                            <div>
                                <div class="neuro-procedure-name">${ s.procedure ? ui.format(s.procedure) : "\u2014" }</div>
                                <% if (s.location) { %><div class="neuro-surgeon">Localisation : ${ ui.format(s.location) }</div><% } %>
                                <% if (s.surgeon) { %><div class="neuro-surgeon">Chirurgien : ${ ui.format(s.surgeon) }</div><% } %>
                                <% if (s.outcome) { %><div class="neuro-outcome">${ ui.format(s.outcome) }</div><% } %>
                                <% if (s.notes) { %><div class="neuro-outcome">${ ui.format(s.notes) }</div><% } %>
                            </div>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>
    </div>
</div>
