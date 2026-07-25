# Patientview (Neurosurgery Patient View) — OpenMRS Module

A custom OpenMRS module that adds a neurosurgery-focused patient dashboard: Glasgow Coma
Scale (GCS) tracking, surgical history, and a supporting REST/AJAX layer for capturing new
neurological assessments from the UI.

**Target platform:** OpenMRS Platform 2.5.9 / Reference Application 2.12.2
**Module ID:** `neuro-patient-view` · **Package:** `org.openmrs.module.patientview`

---

## 1. Architecture

Standard two-module OpenMRS layout, built with the `maven-openmrs-plugin`:

```
patientview/
├── api/    → domain model, persistence, business logic (packaged as a plain jar,
│              unpacked into the omod at build time)
└── omod/   → module descriptor, Spring MVC controllers, UI pages/fragments,
               static assets (packaged as the deployable .omod)
```

`api` has no dependency on `omod`; `omod` depends on `api`. This keeps persistence/business
logic testable and reusable independently of the web layer.

## 2. Domain model & persistence

Two persistent entities, mapped with classic Hibernate `.hbm.xml` (not JPA annotations):

| Entity            | Table                          | Notes                                    |
|--------------------|--------------------------------|-------------------------------------------|
| `NeuroAssessment`  | `patientview_neuro_assessment` | GCS eye/verbal/motor scores, notes, date  |
| `SurgicalHistory`  | `patientview_surgical_history` | Procedure, surgeon, outcome, date         |

Schema is created via `api/src/main/resources/liquibase.xml` (two changesets, FK'd to
`patient` and `users`). **The filename matters**: OpenMRS looks up this changelog by the
exact classpath resource name `liquibase.xml` at module startup — anything else is silently
never run.

Mapping files are registered the OpenMRS-documented way, via `config.xml`:

```xml
<mappingFiles>
    org/openmrs/module/patientview/api/model/NeuroAssessment.hbm.xml
    org/openmrs/module/patientview/api/model/SurgicalHistory.hbm.xml
</mappingFiles>
```

*(Not* via a Spring `parent="mappingResources"` bean — that bean doesn't exist in OpenMRS
core and will break module startup.)

## 3. Service layer

```
PatientviewService (interface)
   └─ PatientviewServiceImpl
         └─ PatientviewDao → PatientviewDAOImpl (Hibernate, via sessionFactory)
```

`PatientviewService` is the single entry point consumed by both the UI controllers and the
AJAX controller. Method-level status, as of this build:

**Backed by the database** (via `PatientviewDao`):
`getRecentNeuroAssessments`, `getLatestNeuroAssessment`, `saveNeuroAssessment`,
`getSurgicalHistory`, `getDetailedSurgicalHistory`, `getLatestGCS` (derived from the latest
assessment).

**Static placeholder data — not yet DB-backed** (safe to call, but always return the same
hardcoded value regardless of patient):
`getCurrentNeurologicalMedications`, `getUpcomingAppointments`, `getRecentImaging`,
`getPrimaryNeurologicalDiagnosis`, `getActiveAlerts`. These are the natural next extension
points — each would need its own DAO method backed by the relevant OpenMRS domain object
(`DrugOrder`, `Appointment`, `Obs`/`Order` for imaging, `Obs` or a coded diagnosis concept,
respectively) instead of an in-memory `HashMap`.

**Static reference/lookup data — intentional, not placeholders**:
`getGlasgowComaScaleOptions`, `getMotorFunctionOptions`, `getPupilResponseOptions` — these
back dropdowns in the assessment form and are meant to be constant.

## 4. Spring wiring (`api/src/main/resources/moduleApplicationContext.xml`)

Three things happen here, and all three are required for a service to work end-to-end in
OpenMRS — missing any one produces a different failure mode:

1. **DAO bean**, wired with `sessionFactory` (OpenMRS's shared Hibernate session factory).
2. **Service bean**, wrapped in a `TransactionProxyFactoryBean` so `@Transactional` semantics
   apply.
3. **`<bean parent="serviceContext">`** — registers the service against its interface with
   OpenMRS's `ServiceContext`. Without this, the bean exists in Spring but
   `Context.getService(PatientviewService.class)` throws `ServiceNotFoundException` — the
   Spring bean and the OpenMRS service registry are two separate things.

## 5. Web layer (`omod`)

- **Page**: `pages/clinicianfacing/patient.gsp` + `PatientPageController` — renders the
  dashboard. `PatientPageController` puts the model attributes directly on `PageModel`
  (`patient` is the raw `org.openmrs.Patient`, plus `latestGCS`, `diagnosis`, `medications`,
  `imaging`). Two things worth knowing if you touch this template:
  - Page templates have **no `config` variable** — that's a fragment-only concept
    (`config` holds parameters passed via `ui.includeFragment(provider, id, configMap)`).
    Reference model attributes directly, e.g. `patient.familyName`, not `config.patient`.
  - `patient` is the **raw `Patient`/`Person` object**, not a `PatientDomainWrapper` — so
    `patient.familyName`, `patient.givenName`, `patient.gender`, `patient.birthdate`,
    `patient.age`, `patient.dead`, `patient.deathDate`, `patient.activeIdentifiers` are all
    valid direct properties (confirmed against openmrs-core's `Person.java`/`Patient.java`),
    but there is no nested `patient.patient` and no `patient.primaryIdentifiers` (that's a
    `PatientDomainWrapper`-only convenience getter). Both mistakes throw
    `MissingPropertyException` at render time. Guarded by `ModuleWiringTest` (§7).
- **Extension**: `apps/patientview_extension.json` — adds the "Neurosurgery Dashboard" link
  to the patient dashboard. Every detail here was verified against `appframework`'s and
  `coreapps`' actual source, because each one fails *silently* (no startup error, the link
  just never appears) if wrong:
  - **Filename matters more than content.** `AppConfigurationLoaderFactory` routes files
    purely by glob, independent of what's inside them:
    `classpath*:/apps/*app.json` → deserialized as `List<AppDescriptor>` (a whole
    standalone app); `classpath*:/apps/*extension.json` → deserialized as `List<Extension>`
    (a pluggable extension). A file with `extensionPointId`/`type`/`url` content but named
    `..._app.json` loads without error as an (functionally empty) `AppDescriptor` and is
    never registered as an extension. The file **must** end in `extension.json`.
  - The JSON must be a **top-level array** (`[ {...} ]`), even for one entry — matches the
    shape `objectMapper.readValue(..., List<Extension>...)` expects.
  - `extensionPointId` must be the bare `patientDashboard.overallActions` — **not**
    `coreapps.patientDashboard.overallActions`. `PatientPageController` builds this id as
    `dashboard + ".overallActions"`, and `dashboard` defaults to the literal string
    `"patientDashboard"` when no `dashboard` request parameter is given (the normal case).

  All three are covered by `ModuleWiringTest` (§7).
- **AJAX endpoints** (`NeuroAssessmentRestController`, plain Spring MVC `@Controller`, not a
  DispatcherServlet REST resource): `neuroAssessment.form` (latest GCS), `checkAlerts.form`,
  `addNeuroAssessment.form` (GET renders a form fragment, POST saves via
  `PatientviewService`).
- **Fragment**: `UsersFragmentController` — unrelated demo fragment listing OpenMRS users.
- **Assets**: `resources/scripts/patientview.js` (GCS live calculator, AJAX wiring),
  `resources/css/patientview.css`.

## 6. Module descriptor (`omod/src/main/resources/config.xml`)

- `require_version 2.4.3` — minimum OpenMRS Platform version (floor check; running on a
  higher version, e.g. 2.5.9, is fine).
- `require_modules` — minimum versions of `uiframework`, `webservices.rest`, `coreapps`,
  `appui`, matched against what Reference Application 2.12.2 bundles.
- `mappingFiles` — see §2.

## 7. Build & test

```bash
mvn clean install
```

Test layers:

- **`PatientviewServiceSpringTest` / `PatientviewServiceImplConnectionTest`** — plain
  Mockito unit tests (no OpenMRS context), verifying service/DAO delegation logic.
- **`QuickDeploymentTest`** — extends `BaseModuleContextSensitiveTest`, boots a real
  in-memory OpenMRS Spring/Hibernate context (needs `org.openmrs.api:openmrs-api` with
  `classifier=tests`/`type=test-jar` on the test classpath). This is the only test that
  exercises the full startup wiring (§4), so it's the one that catches Spring
  misconfiguration before it reaches a real deployment.
- **`ModuleWiringTest`** (in `omod`) — fast, no-OpenMRS-context static checks on the
  packaging files themselves (`config.xml`, `moduleApplicationContext.xml`,
  `liquibase.xml`), added as a regression guard against the exact class of packaging bugs
  this module has previously shipped with.

## 8. Known limitations

- Several `PatientviewService` methods return static sample data rather than querying the
  database (§3) — functional as an API contract, not yet wired to real records.
- `getDetailedSurgicalHistory` currently just delegates to `getSurgicalHistory` — the
  distinction (presumably richer surgical detail) isn't yet implemented.
"# Neurosurgery-patientview-OpenMRS-module" 
