<%
    def active = config.active ?: ""
    def patientUuid = config.patientUuid
%>
<nav class="neuro-sidebar">
    <a class="neuro-nav-item ${active == 'resume' ? 'active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/patient", [patientId: patientUuid]) }">
        R&eacute;sum&eacute;
    </a>
    <a class="neuro-nav-item ${active == 'antecedents' ? 'active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/antecedents", [patientId: patientUuid]) }">
        Ant&eacute;c&eacute;dents
    </a>
    <a class="neuro-nav-item ${active == 'examen' ? 'active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/examenClinique", [patientId: patientUuid]) }">
        Examen clinique
    </a>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Diagnostic neurochirurgical</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Prise en charge</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Anatomopathologie</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">&Eacute;volution &amp; s&eacute;quelles</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Biologie</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Sortie &amp; suivi</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Imagerie</span>
</nav>
