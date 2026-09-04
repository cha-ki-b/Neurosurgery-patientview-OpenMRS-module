<%
    def active = config.active ?: ""
    def patientUuid = config.patientUuid
%>
<nav class="neuro-sidebar">
    <a class="neuro-nav-item ${active == 'resume' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/patient", [patientId: patientUuid]) }">
        R&eacute;sum&eacute;
    </a>
    <a class="neuro-nav-item ${active == 'antecedents' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patientUuid]) }">
        Ant&eacute;c&eacute;dents
    </a>
    <a class="neuro-nav-item ${active == 'examen' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patientUuid]) }">
        Examen clinique
    </a>
    <a class="neuro-nav-item ${active == 'diagnostic' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/diagnostic", [patientId: patientUuid]) }">
        Diagnostic neurochirurgical
    </a>
    <a class="neuro-nav-item ${active == 'priseEnCharge' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/priseEnCharge", [patientId: patientUuid]) }">
        Prise en charge
    </a>
    <a class="neuro-nav-item ${active == 'anatomopathologie' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/anatomopathologie", [patientId: patientUuid]) }">
        Anatomopathologie
    </a>
    <a class="neuro-nav-item ${active == 'evolution' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/evolution", [patientId: patientUuid]) }">
        &Eacute;volution &amp; s&eacute;quelles
    </a>
    <a class="neuro-nav-item ${active == 'biologie' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/biologie", [patientId: patientUuid]) }">
        Biologie
    </a>
    <a class="neuro-nav-item ${active == 'sortie' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/sortie", [patientId: patientUuid]) }">
        Sortie &amp; suivi
    </a>
    <a class="neuro-nav-item ${active == 'imagerie' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/imagerie", [patientId: patientUuid]) }">
        Imagerie
    </a>
</nav>
