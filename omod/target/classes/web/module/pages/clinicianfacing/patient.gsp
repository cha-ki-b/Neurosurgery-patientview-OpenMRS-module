<%
    ui.decorateWith("appui", "standardEmrPage")
%>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }" , link: "${ ui.pageLink("patientview", "clinicianfacing/patient", [ patientId: patient.uuid ]) }"}
    ];
    
    jq(function(){
        jq(".tabs").tabs();
        
        // Auto-refresh critical data every 5 minutes
        setInterval(function() {
            // Refresh neurological status
            refreshNeuroStatus();
        }, 300000);
    });
    
    function refreshNeuroStatus() {
        // Implementation for refreshing neuro data
        console.log('Refreshing neurological status...');
    }
</script>

<div class="neurosurgery-patient-view">
    
    <!-- Patient Header -->
    <div class="patient-header" style="background: linear-gradient(135deg, #2c5aa0 0%, #1e4176 100%); color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
                <h1 style="margin: 0; font-size: 1.8em;">${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }</h1>
                <div style="margin-top: 8px;">
                    <% patient.activeIdentifiers.each { %>
                        <span style="background: rgba(255,255,255,0.2); padding: 4px 8px; border-radius: 4px; margin-right: 10px;">
                            ${ ui.format(it.identifier) }
                        </span>
                    <% } %>
                </div>
                <div style="margin-top: 8px; color: rgba(255,255,255,0.9);">
                    Age: ${ patient.birthdate ? ui.format(patient.age) : "Unknown" } years |
                    Gender: ${ ui.format(patient.gender) } |
                    DOB: ${ patient.birthdate ? ui.format(patient.birthdate) : "Unknown" }
                </div>
            </div>
            <div>
                <% if (patient.dead) { %>
                    <div style="background: #dc3545; padding: 10px; border-radius: 4px;">
                        <strong>⚠️ DECEASED</strong><br/>
                        ${ patient.deathDate ? ui.format(patient.deathDate) : "" }
                    </div>
                <% } %>
            </div>
        </div>
    </div>
    
    <!-- Main Content Grid -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
        
        <!-- Neurological Status Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                🧠 Neurological Status
            </h3>
            <div id="neuro-status-content">
                <!-- Sample GCS Display -->
                <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 15px;">
                    <div style="background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 15px; border-radius: 8px; text-align: center; min-width: 80px;">
                        <div style="font-size: 1.8em; font-weight: bold; line-height: 1;">${ latestGCS.totalScore ? ui.format(latestGCS.totalScore) : "—" }</div>
                        <div style="font-size: 0.8em; margin-top: 4px;">GCS</div>
                    </div>
                    <div style="font-family: monospace;">
                        <div>E: ${ latestGCS.eyeResponse ?: "—" }</div>
                        <div>V: ${ latestGCS.verbalResponse ?: "—" }</div>
                        <div>M: ${ latestGCS.motorResponse ?: "—" }</div>
                    </div>
                </div>
                <div style="color: #6c757d; font-size: 0.9em;">
                    Last assessed: ${ latestGCS.dateRecorded ? ui.format(latestGCS.dateRecorded) : "Aucune évaluation" }
                </div>
            </div>
        </div>
        
        <!-- Primary Diagnosis Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                🏥 Primary Diagnosis
            </h3>
            <div style="font-size: 1.1em; color: #495057; font-weight: 500;">
                ${ ui.format(diagnosis) }
            </div>
            <div style="color: #6c757d; font-size: 0.9em; margin-top: 8px;">
                Active since: ${ new Date().format('dd/MM/yyyy') }
            </div>
        </div>
        
        <!-- Recent Vitals Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                📊 Recent Vitals
            </h3>
            <div style="display: flex; flex-direction: column; gap: 8px;">
                <div style="display: flex; justify-content: space-between;">
                    <span>Blood Pressure:</span>
                    <strong>120/80 mmHg</strong>
                </div>
                <div style="display: flex; justify-content: space-between;">
                    <span>Heart Rate:</span>
                    <strong>72 bpm</strong>
                </div>
                <div style="display: flex; justify-content: space-between;">
                    <span>Temperature:</span>
                    <strong>36.5°C</strong>
                </div>
                <div style="display: flex; justify-content: space-between;">
                    <span>O2 Sat:</span>
                    <strong>98%</strong>
                </div>
            </div>
        </div>
        
        <!-- Current Medications Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                💊 Current Medications
            </h3>
            <div style="display: flex; flex-direction: column; gap: 10px;">
                <% if (!medications) { %>
                    <div class="no-data">Aucun médicament enregistré</div>
                <% } else { medications.each { med -> %>
                    <div style="padding: 10px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #17a2b8;">
                        <div style="font-weight: 600; color: #495057;">${ ui.format(med.name) }</div>
                        <div style="color: #17a2b8; font-weight: 500;">${ ui.format(med.dosage) }</div>
                        <div style="font-size: 0.9em; color: #6c757d;">${ ui.format(med.frequency) }</div>
                    </div>
                <% } } %>
            </div>
        </div>
        
        <!-- Recent Imaging Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                🔬 Recent Imaging
            </h3>
            <div style="display: flex; flex-direction: column; gap: 10px;">
                <% if (!imaging) { %>
                    <div class="no-data">Aucune imagerie récente</div>
                <% } else { imaging.each { img -> %>
                    <div style="padding: 10px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #ffc107;">
                        <div style="font-weight: 600; color: #495057;">${ ui.format(img.type) }</div>
                        <div style="color: #ffc107; font-weight: 500; margin: 4px 0;">${ img.date ? ui.format(img.date) : "" }</div>
                        <div style="font-size: 0.9em; color: #6c757d;">${ ui.format(img.findings) }</div>
                    </div>
                <% } } %>
            </div>
        </div>
        
        <!-- Upcoming Appointments Panel -->
        <div class="info-section" style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="margin-top: 0; color: #495057; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;">
                📅 Next Appointments
            </h3>
            <div style="display: flex; flex-direction: column; gap: 10px;">
                <div style="padding: 10px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #6f42c1;">
                    <div style="font-weight: 600; color: #6f42c1;">${ new Date().plus(3).format('dd/MM/yyyy HH:mm') }</div>
                    <div style="color: #495057; margin: 4px 0;">Neurosurgery Follow-up</div>
                    <div style="font-size: 0.9em; color: #6c757d;">Dr. Smith</div>
                </div>
                <div style="padding: 10px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #6f42c1;">
                    <div style="font-weight: 600; color: #6f42c1;">${ new Date().plus(7).format('dd/MM/yyyy HH:mm') }</div>
                    <div style="color: #495057; margin: 4px 0;">Physical Therapy</div>
                    <div style="font-size: 0.9em; color: #6c757d;">PT Department</div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Quick Actions -->
    <div style="display: flex; justify-content: center; gap: 15px; margin-top: 25px; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
        <button onclick="addNeuroAssessment()" style="background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: 500;">
            Add Neuro Assessment
        </button>
        <button onclick="scheduleAppointment()" style="background: linear-gradient(135deg, #007bff 0%, #0056b3 100%); color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: 500;">
            Schedule Follow-up
        </button>
        <button onclick="viewHistory()" style="background: linear-gradient(135deg, #6f42c1 0%, #563d7c 100%); color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: 500;">
            View Full History
        </button>
    </div>
</div>

<script type="text/javascript">
    function addNeuroAssessment() {
        alert('Neurological assessment form would open here');
        // Implement actual form opening
    }
    
    function scheduleAppointment() {
        alert('Appointment scheduling would open here');
        // Implement actual appointment scheduling
    }
    
    function viewHistory() {
        alert('Full patient history would open here');
        // Implement full history view
    }
</script>