package org.openmrs.module.patientview.api;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;

/**
 * Privilege names used to gate access to neurosurgery patient-view data.
 * <p>
 * There are two independent pairs of privileges here, because they enforce access at two
 * different layers and, in an OpenMRS Reference Application installation, behave very
 * differently in practice:
 * <ul>
 *   <li>{@link #VIEW_NEURO_DATA} / {@link #MANAGE_NEURO_DATA} - plain API-level privileges,
 *       enforced via {@code @Authorized} on every {@link PatientviewService} method. <b>The
 *       Reference Application deliberately auto-grants every plain (non "App:"/"Task:")
 *       privilege to all roles</b> (see its documentation on API vs UI level privileges), so in
 *       a Reference Application install these two behave as a baseline everyone has - they still
 *       matter for other OpenMRS distributions that don't follow that convention, but they are
 *       not the real access-control boundary here.</li>
 *   <li>{@link #APP_VIEW_DASHBOARD} / {@link #APP_MANAGE_DASHBOARD} - "App:"-prefixed UI-level
 *       privileges, following the exact convention the Reference Application uses everywhere
 *       else (e.g. "App: registrationapp.registerPatient"). These are NOT auto-granted, so they
 *       are the privileges that should actually be assigned to roles: APP_VIEW_DASHBOARD to
 *       nurses (read-only), both to surgeons/radiologists (read-write). APP_VIEW_DASHBOARD also
 *       gates whether the "Neurosurgery Dashboard" link even appears on the patient dashboard
 *       (see patientview_extension.json's requiredPrivilege), and APP_MANAGE_DASHBOARD gates
 *       every add/edit form and REST write endpoint.</li>
 * </ul>
 */
public final class PatientviewPrivileges {

    public static final String VIEW_NEURO_DATA = "View Neurosurgery Data";

    public static final String MANAGE_NEURO_DATA = "Manage Neurosurgery Data";

    /** Gates the dashboard link's visibility and every read (GET) REST endpoint. Grant to nurses. */
    public static final String APP_VIEW_DASHBOARD = "App: patientview.neurosurgeryDashboard";

    /** Gates every add/edit form and write (POST) REST endpoint. Grant to surgeons/radiologists. */
    public static final String APP_MANAGE_DASHBOARD = "App: patientview.neurosurgeryDashboard.manage";

    private PatientviewPrivileges() {
    }

    /**
     * Throws if the current user lacks {@link #APP_VIEW_DASHBOARD}. Call this explicitly at the
     * top of every REST GET handler and page controller - do not rely on {@code @Authorized}
     * alone for this check, since the Reference Application's auto-grant convention (see class
     * doc) makes the plain API-level privilege ineffective as an actual boundary.
     */
    public static void requireView() {
        if (!Context.hasPrivilege(APP_VIEW_DASHBOARD)) {
            throw new APIAuthenticationException("Requires privilege: " + APP_VIEW_DASHBOARD);
        }
    }

    /**
     * Throws if the current user lacks {@link #APP_MANAGE_DASHBOARD}. Call this explicitly at the
     * top of every REST POST handler. Does not also require {@link #APP_VIEW_DASHBOARD} - an
     * administrator is expected to grant both privileges together to manage-capable roles.
     */
    public static void requireManage() {
        if (!Context.hasPrivilege(APP_MANAGE_DASHBOARD)) {
            throw new APIAuthenticationException("Requires privilege: " + APP_MANAGE_DASHBOARD);
        }
    }
}
