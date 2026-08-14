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
<<<<<<< HEAD
        { label: "${ patient.familyName ? ui.format(patient.familyName) : '' }, ${ patient.givenName ? ui.format(patient.givenName) : '' }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"},
=======
        { label: "${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"},
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
        { label: "Examen clinique" }
    ];

    jq(function(){
        initializePatientDashboard();
    });
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "examen"]) }

    <div class="neuro-content">
        ${ ui.includeFragment("patientview", "patientHeader", [patient: patient]) }

        <!-- Examen clinique general (Fiche section 4) -->
<<<<<<< HEAD
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Constantes</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('vitalSignsForm')">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <form id="vitalSignsForm" class="neuro-form" style="display:none;" onsubmit="return submitVitalSigns(event)">
                    <div class="neuro-form-grid">
=======
        <div class="panel">
            <div class="panel-header">
                <h2>Constantes</h2>
                <button type="button" class="btn btn-secondary" onclick="toggleForm('vitalSignsForm')">+ Ajouter</button>
            </div>
            <div class="panel-content">
                <form id="vitalSignsForm" class="neuro-form" style="display:none;" onsubmit="return submitVitalSigns(event)">
                    <div class="form-grid">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                        <label>Temp&eacute;rature (&deg;C) <input type="number" step="0.1" name="temperature"/></label>
                        <label>TA systolique (mmHg) <input type="number" name="bpSystolic"/></label>
                        <label>TA diastolique (mmHg) <input type="number" name="bpDiastolic"/></label>
                        <label>Fr&eacute;quence cardiaque (bpm) <input type="number" name="heartRate"/></label>
                        <label>Fr&eacute;quence respiratoire (/min) <input type="number" name="respiratoryRate"/></label>
                        <label>Saturation O2 (%) <input type="number" name="spo2" min="0" max="100"/></label>
                        <label>Poids (kg) <input type="number" step="0.1" name="weightKg"/></label>
                        <label>Taille (cm) <input type="number" step="0.1" name="heightCm"/></label>
                    </div>
<<<<<<< HEAD
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                    <span class="neuro-assessment-date">L'IMC est calcul&eacute; automatiquement &agrave; partir du poids et de la taille.</span>
                </form>

                <% if (!vitalSigns) { %>
                    <div class="neuro-no-data">Aucune constante enregistr&eacute;e</div>
=======
                    <button type="submit" class="btn btn-primary">Enregistrer</button>
                    <span class="assessment-date">L'IMC est calcul&eacute; automatiquement &agrave; partir du poids et de la taille.</span>
                </form>

                <% if (!vitalSigns) { %>
                    <div class="no-data">Aucune constante enregistr&eacute;e</div>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                <% } else { %>
                    <table class="neuro-table">
                        <thead>
                            <tr>
                                <th>Date</th><th>Temp.</th><th>TA</th><th>FC</th><th>FR</th><th>SpO2</th><th>Poids</th><th>Taille</th><th>IMC</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% vitalSigns.each { v -> %>
                                <tr>
                                    <td>${ v.examDate ? ui.format(v.examDate) : "\u2014" }</td>
                                    <td>${ v.temperature ? ui.format(v.temperature) + " \u00b0C" : "\u2014" }</td>
                                    <td>${ (v.bpSystolic && v.bpDiastolic) ? ui.format(v.bpSystolic) + "/" + ui.format(v.bpDiastolic) : "\u2014" }</td>
                                    <td>${ v.heartRate ? ui.format(v.heartRate) : "\u2014" }</td>
                                    <td>${ v.respiratoryRate ? ui.format(v.respiratoryRate) : "\u2014" }</td>
                                    <td>${ v.spo2 ? ui.format(v.spo2) + "%" : "\u2014" }</td>
                                    <td>${ v.weightKg ? ui.format(v.weightKg) + " kg" : "\u2014" }</td>
                                    <td>${ v.heightCm ? ui.format(v.heightCm) + " cm" : "\u2014" }</td>
                                    <td>${ v.bmi ? ui.format(v.bmi) : "\u2014" }</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <!-- Conscience : GCS + Karnofsky (Fiche section 5) -->
<<<<<<< HEAD
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Glasgow Coma Scale &amp; Karnofsky</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="addNeuroAssessment(patientId)">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <% if (!neuroAssessments) { %>
                    <div class="neuro-no-data">Aucune &eacute;valuation enregistr&eacute;e</div>
=======
        <div class="panel">
            <div class="panel-header">
                <h2>Glasgow Coma Scale &amp; Karnofsky</h2>
                <button type="button" class="btn btn-secondary" onclick="addNeuroAssessment(patientId)">+ Ajouter</button>
            </div>
            <div class="panel-content">
                <% if (!neuroAssessments) { %>
                    <div class="no-data">Aucune &eacute;valuation enregistr&eacute;e</div>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                <% } else { %>
                    <table class="neuro-table">
                        <thead>
                            <tr>
                                <th>Date</th><th>E</th><th>V</th><th>M</th><th>Total</th><th>Karnofsky</th><th>Notes</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% neuroAssessments.each { a -> %>
                                <tr>
                                    <td>${ a.date ? ui.format(a.date) : "\u2014" }</td>
                                    <td>${ a.eyeResponse ?: "\u2014" }</td>
                                    <td>${ a.verbalResponse ?: "\u2014" }</td>
                                    <td>${ a.motorResponse ?: "\u2014" }</td>
                                    <td><strong>${ a.gcs ?: "\u2014" }</strong></td>
                                    <td>${ a.karnofskyScore ?: "\u2014" }</td>
                                    <td>${ a.notes ? ui.format(a.notes) : "" }</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <!-- Examen neurologique detaille (Fiche section 5) -->
<<<<<<< HEAD
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Examen neurologique d&eacute;taill&eacute;</h2>
                <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('neuroExamDetailForm')">+ Ajouter</button>
            </div>
            <div class="neuro-panel-content">
                <form id="neuroExamDetailForm" class="neuro-form" style="display:none;" onsubmit="return submitNeuroExamDetail(event)">
                    <div class="neuro-form-grid">
=======
        <div class="panel">
            <div class="panel-header">
                <h2>Examen neurologique d&eacute;taill&eacute;</h2>
                <button type="button" class="btn btn-secondary" onclick="toggleForm('neuroExamDetailForm')">+ Ajouter</button>
            </div>
            <div class="panel-content">
                <form id="neuroExamDetailForm" class="neuro-form" style="display:none;" onsubmit="return submitNeuroExamDetail(event)">
                    <div class="form-grid">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                        <label>Orientation <input type="text" name="orientation"/></label>
                        <label>Langage <input type="text" name="language"/></label>
                        <label>M&eacute;moire <input type="text" name="memory"/></label>
                        <label>Nerfs cr&acirc;niens <input type="text" name="cranialNerves"/></label>
                        <label>D&eacute;ficit moteur <input type="text" name="motorDeficit"/></label>
                        <label>D&eacute;ficit sensitif <input type="text" name="sensoryDeficit"/></label>
                        <label>R&eacute;flexes ost&eacute;o-tendineux <input type="text" name="reflexes"/></label>
                        <label>R&eacute;flexe plantaire <input type="text" name="plantarReflex"/></label>
                        <label>Coordination <input type="text" name="coordination"/></label>
                        <label>Marche <input type="text" name="gait"/></label>
                        <label>&Eacute;quilibre <input type="text" name="balance"/></label>
                    </div>
<<<<<<< HEAD
                    <div class="neuro-checklist-grid">
=======
                    <div class="checklist-grid">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                        <label><input type="checkbox" name="cerebellarSigns"/> Signes c&eacute;r&eacute;belleux</label>
                        <label><input type="checkbox" name="pyramidalSyndrome"/> Syndrome pyramidal</label>
                        <label><input type="checkbox" name="meningealSyndrome"/> Syndrome m&eacute;ning&eacute;</label>
                        <label><input type="checkbox" name="seizures"/> Crises &eacute;pileptiques</label>
                        <label><input type="checkbox" name="headache"/> C&eacute;phal&eacute;es</label>
                        <label><input type="checkbox" name="vomiting"/> Vomissements</label>
                        <label><input type="checkbox" name="visualDisturbances"/> Troubles visuels</label>
                        <label><input type="checkbox" name="sphincterDisturbances"/> Troubles sphinct&eacute;riens</label>
                    </div>
<<<<<<< HEAD
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>

                <div class="neuro-record-list">
                    <% if (!neuroExamDetails) { %>
                        <div class="neuro-no-data">Aucun examen neurologique d&eacute;taill&eacute; enregistr&eacute;</div>
                    <% } else { neuroExamDetails.each { n -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ n.examDate ? ui.format(n.examDate) : "" }</div>
=======
                    <label class="full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="btn btn-primary">Enregistrer</button>
                </form>

                <div class="record-list">
                    <% if (!neuroExamDetails) { %>
                        <div class="no-data">Aucun examen neurologique d&eacute;taill&eacute; enregistr&eacute;</div>
                    <% } else { neuroExamDetails.each { n -> %>
                        <div class="record-item">
                            <div class="record-item-date">${ n.examDate ? ui.format(n.examDate) : "" }</div>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                            <div>
                                <strong>Orientation :</strong> ${ n.orientation ? ui.format(n.orientation) : "\u2014" }
                                &nbsp;|&nbsp; <strong>Langage :</strong> ${ n.language ? ui.format(n.language) : "\u2014" }
                                &nbsp;|&nbsp; <strong>M&eacute;moire :</strong> ${ n.memory ? ui.format(n.memory) : "\u2014" }
                            </div>
                            <% if (n.motorDeficit || n.sensoryDeficit) { %>
                                <div>
                                    <strong>D&eacute;ficit moteur :</strong> ${ n.motorDeficit ? ui.format(n.motorDeficit) : "\u2014" }
                                    &nbsp;|&nbsp; <strong>D&eacute;ficit sensitif :</strong> ${ n.sensoryDeficit ? ui.format(n.sensoryDeficit) : "\u2014" }
                                </div>
                            <% } %>
                            <div>
                                <strong>Marche :</strong> ${ n.gait ? ui.format(n.gait) : "\u2014" }
                                &nbsp;|&nbsp; <strong>&Eacute;quilibre :</strong> ${ n.balance ? ui.format(n.balance) : "\u2014" }
                                &nbsp;|&nbsp; <strong>R&eacute;flexe plantaire :</strong> ${ n.plantarReflex ? ui.format(n.plantarReflex) : "\u2014" }
                            </div>
                            <div class="neuro-flags">
<<<<<<< HEAD
                                <% if (n.cerebellarSigns) { %><span class="neuro-flag-tag">Signes c&eacute;r&eacute;belleux</span><% } %>
                                <% if (n.pyramidalSyndrome) { %><span class="neuro-flag-tag">Syndrome pyramidal</span><% } %>
                                <% if (n.meningealSyndrome) { %><span class="neuro-flag-tag">Syndrome m&eacute;ning&eacute;</span><% } %>
                                <% if (n.seizures) { %><span class="neuro-flag-tag">Crises &eacute;pileptiques</span><% } %>
                                <% if (n.headache) { %><span class="neuro-flag-tag">C&eacute;phal&eacute;es</span><% } %>
                                <% if (n.vomiting) { %><span class="neuro-flag-tag">Vomissements</span><% } %>
                                <% if (n.visualDisturbances) { %><span class="neuro-flag-tag">Troubles visuels</span><% } %>
                                <% if (n.sphincterDisturbances) { %><span class="neuro-flag-tag">Troubles sphinct&eacute;riens</span><% } %>
=======
                                <% if (n.cerebellarSigns) { %><span class="flag-tag">Signes c&eacute;r&eacute;belleux</span><% } %>
                                <% if (n.pyramidalSyndrome) { %><span class="flag-tag">Syndrome pyramidal</span><% } %>
                                <% if (n.meningealSyndrome) { %><span class="flag-tag">Syndrome m&eacute;ning&eacute;</span><% } %>
                                <% if (n.seizures) { %><span class="flag-tag">Crises &eacute;pileptiques</span><% } %>
                                <% if (n.headache) { %><span class="flag-tag">C&eacute;phal&eacute;es</span><% } %>
                                <% if (n.vomiting) { %><span class="flag-tag">Vomissements</span><% } %>
                                <% if (n.visualDisturbances) { %><span class="flag-tag">Troubles visuels</span><% } %>
                                <% if (n.sphincterDisturbances) { %><span class="flag-tag">Troubles sphinct&eacute;riens</span><% } %>
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                            </div>
                            <% if (n.notes) { %><div>${ ui.format(n.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Neuro assessment modal (Glasgow Coma Scale + Karnofsky quick entry) -->
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
