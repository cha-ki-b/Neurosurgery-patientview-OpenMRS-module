<%
    def patient = config.patient
%>
<div class="patient-header">
    <div class="patient-info">
        <h1>${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }</h1>
        <div class="patient-identifiers">
            <% patient.activeIdentifiers.each { %>
                <span class="identifier">${ ui.format(it.identifier) }</span>
            <% } %>
        </div>
        <div class="patient-demographics">
            ${ patient.birthdate ? ui.format(patient.age) + " ans" : "\u00c2ge inconnu" } |
            ${ ui.format(patient.gender) } |
            ${ patient.birthdate ? ui.format(patient.birthdate) : "Date de naissance inconnue" }
        </div>
    </div>
    <% if (patient.dead) { %>
        <div class="patient-alerts">
            <div class="alert-panel">
                <strong>&#9888; D&Eacute;C&Eacute;D&Eacute;</strong><br/>
                ${ patient.deathDate ? ui.format(patient.deathDate) : "" }
            </div>
        </div>
    <% } %>
</div>
