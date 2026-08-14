<%
    def patient = config.patient
%>
<<<<<<< HEAD
<div class="neuro-patient-header">
    <div class="neuro-patient-info">
        <h1>${ patient.familyName ? ui.format(patient.familyName) : "" }, ${ patient.givenName ? ui.format(patient.givenName) : "" }</h1>
        <div class="neuro-patient-identifiers">
            <% patient.activeIdentifiers.each { %>
                <span class="neuro-identifier">${ it.identifier ? ui.format(it.identifier) : "" }</span>
            <% } %>
        </div>
        <div class="neuro-patient-demographics">
            ${ patient.birthdate ? ui.format(patient.age) + " ans" : "\u00c2ge inconnu" } |
            ${ patient.gender ? ui.format(patient.gender) : "Sexe non renseign\u00e9" } |
=======
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
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
            ${ patient.birthdate ? ui.format(patient.birthdate) : "Date de naissance inconnue" }
        </div>
    </div>
    <% if (patient.dead) { %>
<<<<<<< HEAD
        <div class="neuro-patient-alerts">
            <div class="neuro-alert-panel">
=======
        <div class="patient-alerts">
            <div class="alert-panel">
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
                <strong>&#9888; D&Eacute;C&Eacute;D&Eacute;</strong><br/>
                ${ patient.deathDate ? ui.format(patient.deathDate) : "" }
            </div>
        </div>
    <% } %>
</div>
