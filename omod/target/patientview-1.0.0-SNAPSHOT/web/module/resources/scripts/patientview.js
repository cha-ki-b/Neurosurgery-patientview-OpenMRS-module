/**
 * JavaScript functions for the Neurosurgery Patient Dashboard
 */

// Global variables
var currentPatientId = null;

/**
 * Initialize the patient dashboard
 */
function initializePatientDashboard() {
    currentPatientId = patientId;
    
    // Set up auto-refresh for critical data
    setInterval(refreshCriticalData, 300000); // Every 5 minutes
    
    // Initialize tooltips and interactive elements
    initializeTooltips();
    
    // Set up keyboard shortcuts
    setupKeyboardShortcuts();
    
    console.log('Neurosurgery Patient Dashboard initialized for patient:', currentPatientId);
}

/**
 * Refresh critical patient data without full page reload
 */
function refreshCriticalData() {
    if (!currentPatientId) return;
    
    // Refresh latest neurological assessment
    refreshNeuroStatus();
    
    // Check for new alerts
    checkForNewAlerts();
}

/**
 * Add new neurological assessment
 */
function addNeuroAssessment(patientId) {
    showLoadingModal();
    
    // Load the assessment form
    jQuery.ajax({
        url: openmrsContextPath + '/module/patientview/addNeuroAssessment.form',
        type: 'GET',
        data: { patientId: patientId },
        success: function(data) {
            hideLoadingModal();
            jQuery('#neuroAssessmentFormContent').html(data);
            jQuery('#neuroAssessmentModal').show();
            
            // Initialize form validation
            initializeNeuroAssessmentForm();
        },
        error: function(xhr, status, error) {
            hideLoadingModal();
            showErrorMessage('Failed to load assessment form: ' + error);
        }
    });
}

/**
 * Close the neurological assessment modal
 */
function closeNeuroAssessmentModal() {
    jQuery('#neuroAssessmentModal').hide();
    jQuery('#neuroAssessmentFormContent').empty();
}

/**
 * Initialize the neurological assessment form
 */
function initializeNeuroAssessmentForm() {
    // Glasgow Coma Scale calculator
    setupGCSCalculator();
    
    // Pupil assessment tools
    setupPupilAssessment();
    
    // Motor function testing
    setupMotorFunctionTesting();
    
    // Form validation
    setupFormValidation();
}

/**
 * Setup Glasgow Coma Scale calculator
 */
function setupGCSCalculator() {
    const eyeSelect = jQuery('#eyeResponse');
    const verbalSelect = jQuery('#verbalResponse');
    const motorSelect = jQuery('#motorResponse');
    const totalDisplay = jQuery('#gcsTotal');
    
    function calculateGCS() {
        const eye = parseInt(eyeSelect.val() || 0);
        const verbal = parseInt(verbalSelect.val() || 0);
        const motor = parseInt(motorSelect.val() || 0);
        const total = eye + verbal + motor;
        
        totalDisplay.text(total);
        totalDisplay.removeClass('gcs-severe gcs-moderate gcs-mild');
        
        if (total >= 13) {
            totalDisplay.addClass('gcs-mild');
        } else if (total >= 9) {
            totalDisplay.addClass('gcs-moderate');
        } else {
            totalDisplay.addClass('gcs-severe');
        }
    }
    
    eyeSelect.on('change', calculateGCS);
    verbalSelect.on('change', calculateGCS);
    motorSelect.on('change', calculateGCS);
}

/**
 * Setup pupil assessment tools
 */
function setupPupilAssessment() {
    // Pupil size sliders
    jQuery('.pupil-slider').on('input', function() {
        const value = jQuery(this).val();
        const eye = jQuery(this).data('eye');
        jQuery('#pupilSize' + eye).text(value + 'mm');
        
        // Update visual indicator
        const indicator = jQuery('#pupilIndicator' + eye);
        indicator.css('width', (value * 3) + 'px');
        indicator.css('height', (value * 3) + 'px');
    });
    
    // Light reflex buttons
    jQuery('.light-reflex-btn').on('click', function() {
        const eye = jQuery(this).data('eye');
        const response = jQuery(this).data('response');
        
        jQuery('.light-reflex-btn[data-eye="' + eye + '"]').removeClass('active');
        jQuery(this).addClass('active');
        
        jQuery('#lightReflex' + eye).val(response);
    });
}

/**
 * Setup motor function testing
 */
function setupMotorFunctionTesting() {
    jQuery('.motor-test').on('click', function() {
        const limb = jQuery(this).data('limb');
        const strength = jQuery(this).data('strength');
        
        jQuery('.motor-test[data-limb="' + limb + '"]').removeClass('selected');
        jQuery(this).addClass('selected');
        
        jQuery('#motorStrength' + limb).val(strength);
    });
}

/**
 * Setup form validation
 */
function setupFormValidation() {
    jQuery('#neuroAssessmentForm').on('submit', function(e) {
        e.preventDefault();
        
        if (validateNeuroAssessmentForm()) {
            submitNeuroAssessment();
        }
    });
}

/**
 * Validate neurological assessment form
 */
function validateNeuroAssessmentForm() {
    let isValid = true;
    const requiredFields = ['#eyeResponse', '#verbalResponse', '#motorResponse'];
    
    // Clear previous errors
    jQuery('.form-error').remove();
    
    // Check required fields
    requiredFields.forEach(function(fieldId) {
        const field = jQuery(fieldId);
        if (!field.val()) {
            field.after('<div class="form-error">This field is required</div>');
            isValid = false;
        }
    });
    
    // Validate GCS total
    const total = parseInt(jQuery('#gcsTotal').text());
    if (total < 3 || total > 15) {
        showErrorMessage('Invalid Glasgow Coma Scale total');
        isValid = false;
    }
    
    return isValid;
}

/**
 * Submit neurological assessment
 */
function submitNeuroAssessment() {
    const formData = jQuery('#neuroAssessmentForm').serialize();
    
    showLoadingMessage('Saving assessment...');
    
    jQuery.ajax({
        url: openmrsContextPath + '/module/patientview/addNeuroAssessment.form',
        type: 'POST',
        data: formData,
        success: function(response) {
            hideLoadingMessage();
            showSuccessMessage('Neurological assessment saved successfully');
            closeNeuroAssessmentModal();
            
            // Refresh the neurological status panel
            refreshNeuroStatus();
        },
        error: function(xhr, status, error) {
            hideLoadingMessage();
            showErrorMessage('Failed to save assessment: ' + error);
        }
    });
}

/**
 * Refresh neurological status panel
 */
function refreshNeuroStatus() {
    if (!currentPatientId) return;
    
    jQuery.ajax({
        url: openmrsContextPath + '/module/patientview/neuroAssessment.form',
        type: 'GET',
        data: { patientId: currentPatientId },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                updateNeuroStatusPanel(response.data);
            }
        },
        error: function(xhr, status, error) {
            console.error('Failed to refresh neuro status:', error);
        }
    });
}

/**
 * Update neurological status panel with new data
 */
function updateNeuroStatusPanel(data) {
    if (data.latestGCS) {
        const gcs = data.latestGCS;
        jQuery('.score-value').text(gcs.totalScore);
        jQuery('.gcs-breakdown').html(
            '<div>E: ' + gcs.eyeResponse + '</div>' +
            '<div>V: ' + gcs.verbalResponse + '</div>' +
            '<div>M: ' + gcs.motorResponse + '</div>'
        );
        
        const date = new Date(gcs.dateRecorded);
        jQuery('.assessment-date').text(date.toLocaleString());
    }
}

/**
 * View imaging history
 */
function viewImagingHistory(patientId) {
    window.open(
        openmrsContextPath + '/module/patientview/imagingHistory.form?patientId=' + patientId,
        '_blank',
        'width=1000,height=700,scrollbars=yes'
    );
}

/**
 * Schedule follow-up appointment
 */
function scheduleFollowup(patientId) {
    window.location.href = openmrsContextPath + '/module/patientview/scheduleAppointment.form?patientId=' + patientId;
}

/**
 * Print patient summary
 */
function printSummary(patientId) {
    const printWindow = window.open(
        openmrsContextPath + '/module/patientview/printSummary.form?patientId=' + patientId,
        '_blank',
        'width=800,height=600'
    );
    
    printWindow.onload = function() {
        printWindow.print();
    };
}

/**
 * Check for new alerts
 */
function checkForNewAlerts() {
    if (!currentPatientId) return;
    
    jQuery.ajax({
        url: openmrsContextPath + '/module/patientview/checkAlerts.form',
        type: 'GET',
        data: { patientId: currentPatientId },
        dataType: 'json',
        success: function(response) {
            if (response.newAlerts && response.newAlerts.length > 0) {
                showNewAlerts(response.newAlerts);
            }
        },
        error: function(xhr, status, error) {
            console.error('Failed to check for alerts:', error);
        }
    });
}

/**
 * Show new alerts to user
 */
function showNewAlerts(alerts) {
    alerts.forEach(function(alert) {
        showWarningMessage(alert.message, 10000); // Show for 10 seconds
    });
}

/**
 * Initialize tooltips
 */
function initializeTooltips() {
    // Add tooltips to GCS components
    jQuery('[data-tooltip]').each(function() {
        const element = jQuery(this);
        const tooltip = element.attr('data-tooltip');
        
        element.on('mouseenter', function() {
            showTooltip(element, tooltip);
        }).on('mouseleave', function() {
            hideTooltip();
        });
    });
}

/**
 * Show tooltip
 */
function showTooltip(element, text) {
    const tooltip = jQuery('<div class="custom-tooltip">' + text + '</div>');
    jQuery('body').append(tooltip);
    
    const offset = element.offset();
    tooltip.css({
        left: offset.left + element.outerWidth() / 2 - tooltip.outerWidth() / 2,
        top: offset.top - tooltip.outerHeight() - 10
    }).fadeIn(200);
}

/**
 * Hide tooltip
 */
function hideTooltip() {
    jQuery('.custom-tooltip').fadeOut(200, function() {
        jQuery(this).remove();
    });
}

/**
 * Setup keyboard shortcuts
 */
function setupKeyboardShortcuts() {
    jQuery(document).keydown(function(e) {
        // Ctrl+N: New neurological assessment
        if (e.ctrlKey && e.which === 78) {
            e.preventDefault();
            addNeuroAssessment(currentPatientId);
        }
        
        // Escape: Close modal
        if (e.which === 27) {
            closeNeuroAssessmentModal();
        }
    });
}

/**
 * Utility functions for messages and loading states
 */
function showLoadingModal() {
    jQuery('body').append('<div id="loadingModal" class="modal"><div class="modal-content"><div style="text-align:center;padding:40px;"><div class="spinner"></div><p>Loading...</p></div></div></div>');
}

function hideLoadingModal() {
    jQuery('#loadingModal').remove();
}

function showLoadingMessage(message) {
    jQuery('body').append('<div id="loadingMessage" class="loading-message">' + message + '</div>');
}

function hideLoadingMessage() {
    jQuery('#loadingMessage').remove();
}

function showSuccessMessage(message) {
    const messageDiv = jQuery('<div class="success-message">' + message + '</div>');
    jQuery('body').append(messageDiv);
    messageDiv.fadeIn(300).delay(3000).fadeOut(300, function() {
        jQuery(this).remove();
    });
}

function showErrorMessage(message) {
    const messageDiv = jQuery('<div class="error-message">' + message + '</div>');
    jQuery('body').append(messageDiv);
    messageDiv.fadeIn(300).delay(5000).fadeOut(300, function() {
        jQuery(this).remove();
    });
}

function showWarningMessage(message, duration) {
    duration = duration || 5000;
    const messageDiv = jQuery('<div class="warning-message">' + message + '</div>');
    jQuery('body').append(messageDiv);
    messageDiv.fadeIn(300).delay(duration).fadeOut(300, function() {
        jQuery(this).remove();
    });
}