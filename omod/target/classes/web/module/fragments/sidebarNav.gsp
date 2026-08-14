<%
    def active = config.active ?: ""
    def patientUuid = config.patientUuid
%>
<nav class="neuro-sidebar">
<<<<<<< HEAD
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
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Prise en charge</span>
    <a class="neuro-nav-item ${active == 'anatomopathologie' ? 'neuro-active' : ''}"
       href="${ ui.pageLink("patientview", "clinicianfacing/anatomopathologie", [patientId: patientUuid]) }">
        Anatomopathologie
    </a>
=======
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
>>>>>>> 72357f4195852fcce719c2ae19b1a4a78c735bde
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">&Eacute;volution &amp; s&eacute;quelles</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Biologie</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Sortie &amp; suivi</span>
    <span class="neuro-nav-item neuro-nav-disabled" title="Disponible dans une prochaine phase">Imagerie</span>
</nav>
