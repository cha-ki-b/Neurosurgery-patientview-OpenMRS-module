<%
    def patient = config.patient
    def safeFamilyName = (patient.familyName && patient.familyName.toString().trim().toLowerCase() != 'null') ? ui.format(patient.familyName) : ""
    def safeGivenName = (patient.givenName && patient.givenName.toString().trim().toLowerCase() != 'null') ? ui.format(patient.givenName) : ""
%>
<div class="neuro-patient-header">
    <div class="neuro-patient-info">
        <h1>${ safeFamilyName }, ${ safeGivenName }</h1>
        <div class="neuro-patient-identifiers">
            <% patient.activeIdentifiers.each { %>
                <span class="neuro-identifier">${ it.identifier ? ui.format(it.identifier) : "" }</span>
            <% } %>
        </div>
        <div class="neuro-patient-demographics">
            ${ patient.birthdate ? ui.format(patient.age) + " ans" : "\u00c2ge inconnu" } |
            ${ patient.gender ? ui.format(patient.gender) : "Sexe non renseign\u00e9" } |
            ${ patient.birthdate ? ui.format(patient.birthdate) : "Date de naissance inconnue" }
        </div>
    </div>
    <% if (patient.dead) { %>
        <div class="neuro-patient-alerts">
            <div class="neuro-alert-panel">
                <strong>&#9888; D&Eacute;C&Eacute;D&Eacute;</strong><br/>
                ${ patient.deathDate ? ui.format(patient.deathDate) : "" }
            </div>
        </div>
    <% } %>
</div>
