# Patientview (Neurosurgery Patient View) — OpenMRS Module

A custom OpenMRS module that replaces the default coreapps patient dashboard with a
neurosurgery-focused one: a multi-tab record covering the department's paper "Fiche de
Neurochirurgie" (antecedents, clinical/neurological exam, diagnosis, anatomopathology, and
more to come), full CRUD backed by MySQL, and a two-tier privilege model so nurses and
surgeons/radiologists see different levels of access.

**Target platform:** OpenMRS Platform 2.5.9 / Reference Application 2.12.2
**Module ID:** `patientview` · **Package:** `org.openmrs.module.patientview` · **Version:** `1.2.1`

Thanks to `hanyG175` and `bouzenaali` for starting the job: [Repo](https://github.com/hanyG175/openmrs-patientview-module)

---

## 1. Architecture

Standard two-module OpenMRS layout, built with the `maven-openmrs-plugin`:

```
patientview/
├── api/          → domain model, persistence, business logic (packaged as a plain jar,
│                    unpacked into the omod at build time)
├── omod/         → module descriptor, Spring MVC controllers, UI pages/fragments,
│                    static assets (packaged as the deployable .omod)
└── deployment/   → infra for the *separate* openmrs-orthanc-integration repo (nginx
                     reverse proxy + TLS, secrets-via-.env) - not part of the module build,
                     see deployment/DEPLOYMENT_SECURITY.md
```

`api` has no dependency on `omod`; `omod` depends on `api`. This keeps persistence/business
logic testable and reusable independently of the web layer.

Build produces `patientview1.2.1.omod` (see `omod/pom.xml`'s `finalName`:
`${project.parent.artifactId}${project.parent.version}`, deliberately no separator).

## 2. Feature map (Fiche de Neurochirurgie → module tabs)

| Sidebar tab | Fiche section(s) | Status |
| --- | --- | --- |
| Résumé | dashboard summary (alerts, GCS/Karnofsky, primary diagnosis) | done |
| Antécédents | §2 Motif d'hospitalisation, §3 Antécédents médicaux/chirurgicaux | done |
| Examen clinique | §4 Examen clinique général, §5 Examen neurologique (incl. GCS) | done |
| Diagnostic neurochirurgical | §8 | done |
| Anatomopathologie | §10 | done |
| Prise en charge | §9 | not started |
| Évolution & séquelles | §11-12 | not started |
| Biologie | §7 | not started |
| Sortie & suivi | §13-14 | not started |
| Imagerie | Orthanc-backed, §6 | not started |

Admin info already covered by OpenMRS core (name, DOB, sex, phone, etc.) is intentionally
**not** duplicated anywhere in this module.

## 3. Domain model & persistence

Eight persistent entities, mapped with classic Hibernate `.hbm.xml` (not JPA annotations),
all FK'd to `patient` and `users`:

| Entity | Table | Pattern |
| --- | --- | --- |
| `NeuroAssessment` | `patientview_neuro_assessment` | append-only (GCS + Karnofsky) |
| `SurgicalHistory` | `patientview_surgical_history` | append-only |
| `MedicalHistory` | `patientview_medical_history` | append-only¹ |
| `AdmissionContext` | `patientview_admission_context` | append-only |
| `VitalSigns` | `patientview_vital_signs` | append-only |
| `NeuroExamDetail` | `patientview_neuro_exam_detail` | append-only |
| `NeurosurgicalDiagnosis` | `patientview_neurosurgical_diagnosis` | append-only |
| `Pathology` | `patientview_pathology` | append-only |

**Append-only, everywhere, on purpose.** No entity is ever updated or deleted in place — a
correction is a new row, not an overwrite. This is a deliberate non-repudiation choice: the
full history of who recorded what and when is always preserved. "Current" state for any
category is simply its most recent row per patient.

¹ `MedicalHistory` originally shipped (v1.0.1) as a single mutable row per patient (`unique
patient_id`), which silently lost history on edit. Fixing that in place turned into a real
lesson in Liquibase/MySQL edge cases - see `liquibase.xml`'s `patientview-2026-08-10-*` and
`patientview-2026-08-12-01` changesets' comments for the full story if you're touching that
table: dropping a unique index that a foreign key depends on requires dropping the FK first
(MySQL error 1553), and editing an *already-executed* changeset's SQL never re-runs it no
matter how you fix it — Liquibase permanently marks a changeset ID as done the moment any
checksum for it is recorded. The actual fix lives in a fresh, `runOnChange="true"`,
fully idempotent changeset instead.

Mapping files are registered via `config.xml`'s `<mappingFiles>` (not a Spring
`parent="mappingResources"` bean - that bean doesn't exist in OpenMRS core and breaks module
startup). Every entry is checked against the classpath by `ModuleWiringTest`.

## 4. Security & authorization model

Two independent pairs of privileges, enforced at two different layers, because a Reference
Application install treats them very differently in practice:

| Privilege | Layer | Reference-App behavior |
| --- | --- | --- |
| `View Neurosurgery Data`, `Manage Neurosurgery Data` | API-level, `@Authorized` on every `PatientviewService` method | **Auto-granted to all roles.** The Reference Application deliberately assigns every plain (non-`App:`/`Task:`) privilege to all roles and gates real access via UI-level privileges instead - see its own docs on "API level vs UI level privileges". These two still matter for non-Reference-Application OpenMRS distributions, but are not an effective boundary here. |
| `App: patientview.neurosurgeryDashboard`, `App: patientview.neurosurgeryDashboard.manage` | UI-level, checked explicitly (`PatientviewPrivileges.requireView()`/`requireManage()`) at the top of every REST endpoint and page controller | **Not** auto-granted - this is the real access boundary. |

**Intended role assignment:**
- Grant `App: patientview.neurosurgeryDashboard` alone to nurses → read-only access (every
  "+ Ajouter" button, form, and write REST endpoint is hidden/rejected).
- Grant **both** `App:` privileges to surgeons/radiologists → full read-write access.

`App: patientview.neurosurgeryDashboard` also gates the "Neurosurgery Dashboard" link itself
via `patientview_extension.json`'s `requiredPrivilege` - a user without it never sees the
button at all. `PatientviewExceptionHandler` (`@ControllerAdvice`) turns a failed check into
a clean 403 JSON response instead of a stack trace; page controllers set an `accessDenied`
model flag that each `.gsp` renders as a plain message instead of the dashboard content.

`ModuleWiringTest.everyRestControllerMustEnforceAPrivilegeCheck()` scans every
`*RestController.java` at build time so a newly added controller can't accidentally skip
this - don't rely on `@Authorized` alone for a new endpoint.

## 5. Service layer

```
PatientviewService (interface, @Authorized on every method)
   └─ PatientviewServiceImpl
         └─ PatientviewDao → PatientviewDAOImpl (Hibernate, via sessionFactory)
```

`PatientviewService` is the single entry point consumed by every controller. All data-backed
methods query the database via `PatientviewDao`; `getGlasgowComaScaleOptions`,
`getMotorFunctionOptions`, `getPupilResponseOptions` are intentional static reference/lookup
data (dropdown options), not placeholders.

`getPrimaryNeurologicalDiagnosis` prefers the latest `NeurosurgicalDiagnosis` entry (the
formal diagnosis) and falls back to the admission-time impression from `AdmissionContext`
only if none exists yet. `getActiveAlerts` flags Glasgow ≤ 12 automatically.

## 6. Spring wiring (`api/src/main/resources/moduleApplicationContext.xml`)

Three things happen here, and all three are required for a service to work end-to-end in
OpenMRS — missing any one produces a different failure mode:

1. **DAO bean**, wired with `sessionFactory` (OpenMRS's shared Hibernate session factory).
2. **Service bean**, wrapped in a `TransactionProxyFactoryBean` so `@Transactional` semantics
   apply.
3. **`<bean parent="serviceContext">`** — registers the service against its interface with
   OpenMRS's `ServiceContext`. Without this, the bean exists in Spring but
   `Context.getService(PatientviewService.class)` throws `ServiceNotFoundException` — the
   Spring bean and the OpenMRS service registry are two separate things.

## 7. Web layer (`omod`)

**Pages** (`pages/clinicianfacing/*.gsp` + matching `*PageController.java`): `patient.gsp`
(Résumé), `antecedents.gsp`, `examenClinique.gsp`, `diagnostic.gsp`, `anatomopathologie.gsp`.
Two shared fragments (`sidebarNav.gsp`, `patientHeader.gsp`) are included on every page.

Things worth knowing if you touch these templates:
- Page templates have **no `config` variable** — that's a fragment-only concept (`config`
  holds parameters passed via `ui.includeFragment(provider, id, configMap)`). Fragments *do*
  use `config`; pages never should. Both directions are checked by `ModuleWiringTest`.
- `patient` is the **raw `Patient`/`Person` object** in every page controller's model, not a
  `PatientDomainWrapper` — so `patient.familyName`, `patient.givenName`, `patient.gender`,
  `patient.birthdate`, `patient.age`, `patient.dead`, `patient.deathDate`,
  `patient.activeIdentifiers` are valid direct properties, but there is no nested
  `patient.patient` and no `patient.primaryIdentifiers` (a `PatientDomainWrapper`-only
  getter). Both mistakes throw `MissingPropertyException` at render time.
- **Never interpolate a possibly-null/empty value directly**: Groovy's GString renders a
  true `null` as the literal text `"null"`, and a plain truthy check (`x ? ... : ...`)
  doesn't catch a stored value that's the literal *string* `"null"` (four characters) rather
  than a true null - both have bitten this module in production. The name-display pattern
  used everywhere now guards against both:
  `(patient.familyName && patient.familyName.toString().trim().toLowerCase() != 'null') ? ui.format(patient.familyName) : ''`.
- Two page-controller-model attributes drive access control on every page: `accessDenied`
  (skip rendering content, show a message instead) and `canManage` (hide every "+ Ajouter"
  button and `<form>`, falling back to a read-only summary where relevant - see
  `antecedents.gsp`'s medical-history panel for the pattern). See §4.
- `patient.patientId` (an `Integer`) is used for REST/AJAX calls (`var patientId = ...` in
  each page's `<script>`); `patient.uuid` is used for page-to-page navigation
  (`ui.pageLink(..., [patientId: patient.uuid])`, matching what each `PageController`
  expects via `getPatientByUuid`). Don't mix the two - the REST controllers reject a UUID
  where they expect an integer.
- **CSS class names are all `neuro-`-prefixed** (`neuro-btn`, `neuro-panel`, `neuro-modal`,
  ...), deliberately avoiding collision with Bootstrap's identically-named classes that the
  Reference Application loads globally (`.btn`, `.panel`, `.modal`, `.close` are all real
  Bootstrap classes). Follow this convention for any new class.

**Extension** (`apps/patientview_extension.json`) — adds the "Neurosurgery Dashboard" link,
gated by `requiredPrivilege` (§4). Filename and shape matter more than you'd expect:
`AppConfigurationLoaderFactory` routes purely by glob (`*app.json` → `AppDescriptor`,
`*extension.json` → `Extension`), independent of content — a correctly-shaped file with the
wrong filename suffix loads without error and is simply never registered. Must be a
top-level JSON array. `extensionPointId` must be the bare `patientDashboard.overallActions`.
All checked by `ModuleWiringTest`.

**REST controllers** (`web/controller/*RestController.java`, plain Spring MVC
`@Controller`s, not DispatcherServlet REST resources) — one pair of GET/POST `.form`
endpoints per category, each starting with a `PatientviewPrivileges.requireView()` /
`requireManage()` call (§4). `PatientviewExceptionHandler` is a shared `@ControllerAdvice`
turning a failed check into 403 JSON.

**Assets**: `resources/scripts/patientview.js` (GCS calculator/modal, alerts polling),
`resources/scripts/patientview-crud.js` (generic "POST a form, reload the page" helper used
by every add-form), `resources/css/patientview.css`.

## 8. Module descriptor (`omod/src/main/resources/config.xml`)

- `require_version 2.4.3` — minimum OpenMRS Platform version (floor check; running on a
  higher version, e.g. 2.5.9, is fine).
- `require_modules` — minimum versions of `uiframework`, `webservices.rest`, `coreapps`,
  `appui`, matched against what Reference Application 2.12.2 bundles.
- `mappingFiles` — see §3.
- `privilege` × 4 — see §4.

## 9. Build & test

```bash
mvn clean install
```

Test layers:

- **`PatientviewServiceSpringTest` / `PatientviewServiceImplConnectionTest`** — plain
  Mockito unit tests (no OpenMRS context; uses `MockitoJUnitRunner`'s strict-stubs mode, so
  every `when(...)` must actually be exercised by the test or the build fails), verifying
  service/DAO delegation logic and the diagnosis-precedence/alert-threshold rules.
- **`QuickDeploymentTest`** — extends `BaseModuleContextSensitiveTest`, boots a real
  in-memory OpenMRS Spring/Hibernate context. The only test that exercises the full startup
  wiring (§6), so it's the one that would catch Spring/Liquibase misconfiguration before it
  reaches a real deployment.
- **`ModuleWiringTest`** (in `omod`) — fast, no-OpenMRS-context static checks on the
  packaging files themselves (`config.xml`, `moduleApplicationContext.xml`,
  `liquibase.xml`, every `.gsp`, `patientview_extension.json`), added as a regression guard
  against the exact classes of packaging/security bugs this module has previously shipped
  with. Includes source-scanning checks (e.g. every REST controller must call a privilege
  helper) so new code can't silently regress.

## 10. Deployment / infrastructure security

See `deployment/DEPLOYMENT_SECURITY.md` — nginx reverse proxy with TLS termination,
credentials moved from hardcoded YAML into a `.env` file. These files update the *separate*
`openmrs-orthanc-integration` repo, not this module.

## 11. Known limitations / roadmap

- Prise en charge (§9), Évolution & séquelles (§11-12), Biologie (§7), Sortie & suivi
  (§13-14), and the Orthanc-backed Imagerie tab are not yet built (§2).
- No CSRF token on the `.form` POST endpoints — matches the rest of the legacy OpenMRS UI
  framework's module conventions (a pre-existing ecosystem-wide gap, not introduced here).
- DICOM traffic itself is unencrypted even once the planned Imagerie tab exists; only the
  HTTP(S) layer is covered by `deployment/`. Orthanc's `DicomTlsEnabled` is a separate,
  more involved configuration step for later.

## 12. Version history

- **1.0.1** — Phase 1: sidebar navigation, Antécédents, Examen clinique.
- **1.0.2 / 1.2.0** — Phase 2: Diagnostic neurochirurgical, Anatomopathologie; security/NFR
  foundation (privileges, append-only non-repudiation, DB indexes, `deployment/`); CSS
  namespace fix; null-render guard fix.
- **1.2.1** — Two-tier `App:` privilege model (view/manage) with explicit enforcement at
  every controller, since the Reference Application's auto-grant convention made the
  original API-level privileges an ineffective boundary; second, more defensive round of
  the null-render guard fix (literal string `"null"`, not just true null).
