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
        { label: "Imagerie" }
    ];
</script>

<div class="neuro-layout">
    ${ ui.includeFragment("patientview", "sidebarNav", [patientUuid: patient.uuid, active: "imagerie"]) }

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

        <!-- Comptes rendus d'imagerie (Fiche section 6) -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Comptes rendus d'imagerie</h2>
                <% if (canManage) { %>
                    <button type="button" class="neuro-btn neuro-btn-secondary" onclick="toggleForm('imagingNoteForm')">+ Ajouter</button>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (canManage) { %>
                <form id="imagingNoteForm" class="neuro-form" style="display:none;" onsubmit="return submitImagingNote(event)">
                    <div class="neuro-form-grid">
                        <label>Type d'examen
                            <select name="examType">
                                <option value="">--</option>
                                <option value="TDM">TDM</option>
                                <option value="IRM">IRM</option>
                                <option value="Angio-TDM">Angio-TDM</option>
                                <option value="Angio-IRM">Angio-IRM</option>
                            </select>
                        </label>
                        <label>Date de l'examen
                            <input type="date" name="examDate"/>
                        </label>
                        <label class="neuro-full-width">R&eacute;sultat / compte rendu
                            <textarea name="findings"></textarea>
                        </label>
                    </div>
                    <label class="neuro-full-width">Notes <textarea name="notes"></textarea></label>
                    <button type="submit" class="neuro-btn neuro-btn-primary">Enregistrer</button>
                </form>
                <% } %>

                <div class="neuro-record-list">
                    <% if (!imagingNotes) { %>
                        <div class="neuro-no-data">Aucun compte rendu d'imagerie enregistr&eacute;</div>
                    <% } else { imagingNotes.each { n -> %>
                        <div class="neuro-record-item">
                            <div class="neuro-record-item-date">${ n.examDate ? ui.format(n.examDate) : (n.dateCreated ? ui.format(n.dateCreated) : "") }</div>
                            <div><strong>Type d'examen :</strong> ${ n.examType ? ui.format(n.examType) : "\u2014" }</div>
                            <div><strong>R&eacute;sultat / compte rendu :</strong> ${ n.findings ? ui.format(n.findings) : "\u2014" }</div>
                            <% if (n.notes) { %><div>${ ui.format(n.notes) }</div><% } %>
                        </div>
                    <% } } %>
                </div>
            </div>
        </div>

        <!-- Studies held in Orthanc, read through the companion imaging module (Fiche section 6).
             Read-only by design: Orthanc is the source of truth for images, and imaging owns the
             viewer. Nothing here is stored by patientview. -->
        <div class="neuro-panel">
            <div class="neuro-panel-header">
                <h2>Examens d'imagerie (PACS)</h2>
                <% if (imagingModuleAvailable) { %>
                    <a class="neuro-btn neuro-btn-secondary"
                       href="${ ui.pageLink("imaging", "studies", [patientId: patient.patientId]) }">Ouvrir dans le PACS</a>
                <% } %>
            </div>
            <div class="neuro-panel-content">
                <% if (!imagingModuleAvailable) { %>
                    <div class="neuro-no-data">
                        Le module d'imagerie (PACS Orthanc) n'est pas install&eacute; sur ce serveur.
                        Les comptes rendus ci-dessus restent disponibles.
                    </div>
                <% } else if (!imagingStudies) { %>
                    <div class="neuro-no-data">Aucun examen DICOM associ&eacute; &agrave; ce patient dans le PACS</div>
                <% } else { %>
                    <table class="neuro-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Description</th>
                                <th>Study Instance UID</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% imagingStudies.each { st -> %>
                                <tr>
                                    <td>${ st.studyDate ? ui.format(st.studyDate) : "\u2014" }</td>
                                    <td>${ st.studyDescription ? ui.format(st.studyDescription) : "\u2014" }</td>
                                    <td>${ st.studyInstanceUID ? ui.format(st.studyInstanceUID) : "\u2014" }</td>
                                    <td>
                                        <% if (st.studyId) { %>
                                            <a href="${ ui.pageLink("imaging", "series", [patientId: patient.patientId, studyId: st.studyId]) }">S&eacute;ries</a>
                                        <% } %>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <% } %>
    </div>
</div>
