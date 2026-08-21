# Patientview (Neurosurgery Patient View) — OpenMRS Module

A custom OpenMRS module that replaces the default coreapps patient dashboard with a
neurosurgery-focused one: a multi-tab record covering the whole of the department's paper
"Fiche de Neurochirurgie" - admission, antecedents, clinical and neurological exam,
biology, imaging, diagnosis, management, anatomopathology, post-operative course,
sequelae, discharge and follow-up - with full CRUD backed by MySQL and a two-tier
privilege model so nurses and surgeons/radiologists see different levels of access.

**Target platform:** OpenMRS Platform 2.5.9 / Reference Application 2.12.2
**Module ID:** `patientview` · **Package:** `org.openmrs.module.patientview` · **Version:** `1.3.0`

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

Build produces `patientview1.3.0.omod` (see `omod/pom.xml`'s `finalName`:
`${project.parent.artifactId}${project.parent.version}`, deliberately no separator).

## 2. Feature map (Fiche de Neurochirurgie → module tabs)

| Sidebar tab | Fiche section(s) | Status |
| --- | --- | --- |
| Résumé | dashboard summary (alerts, GCS/Karnofsky, primary diagnosis) | done |
| Antécédents | §2 Motif d'hospitalisation, §3 Antécédents médicaux/chirurgicaux | done |
| Examen clinique | §4 Examen clinique général, §5 Examen neurologique (incl. GCS) | done |
| Diagnostic neurochirurgical | §8 | done |
| Anatomopathologie | §10 | done |
| Prise en charge | §9 (traitement médical + chirurgical) | done |
| Évolution & séquelles | §11-12 | done |
| Biologie | §7 | done |
| Sortie & suivi | §13-14 | done |
| Imagerie | §6 - local comptes rendus + DICOM studies read from the `imaging` module | done |

Admin info already covered by OpenMRS core (name, DOB, sex, phone, etc.) is intentionally
**not** duplicated anywhere in this module.

## 3. Domain model & persistence

Sixteen persistent entities, mapped with classic Hibernate `.hbm.xml` (not JPA
annotations), all FK'd to `patient` and `users`:

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
| `MedicalTreatment` | `patientview_medical_treatment` | append-only |
| `SurgicalTreatment` | `patientview_surgical_treatment` | append-only |
| `PostopEvolution` | `patientview_postop_evolution` | append-only |
| `Sequelae` | `patientview_sequelae` | append-only |
| `LabResult` | `patientview_lab_result` | append-only |
| `Discharge` | `patientview_discharge` | append-only |
| `FollowUp` | `patientview_follow_up` | append-only |
| `ImagingNote` | `patientview_imaging_note` | append-only |

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

`SurgicalTreatment` is deliberately **not** a reuse of `SurgicalHistory`: that one is §3,
the patient's *antécédents chirurgicaux* (operations predating this episode, often
elsewhere), while `SurgicalTreatment` is §9's intervention performed as part of the
current management, carrying operative detail (exérèse type, duration, per-operative
complications, drain/catheter) a history entry has no place for. `Sequelae` is likewise
separate from `PostopEvolution` despite sharing a tab - sequelae are assessed at follow-up,
months after the post-operative course is recorded, and with append-only rows a merged
table would force a clinician to re-enter the whole post-operative section to add a
deficit noticed later.

Mapping files are registered via `config.xml`'s `<mappingFiles>` (not a Spring
`parent="mappingResources"` bean - that bean doesn't exist in OpenMRS core and breaks module
startup). `ModuleWiringTest` enumerates the `.hbm.xml` files on disk and fails if any of
them is missing from that list, so a new entity cannot be left unmapped (which compiles,
packages and deploys fine, then fails at the first HQL query with "not mapped").

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

**Pages** (`pages/clinicianfacing/*.gsp` + matching `*PageController.java`), one per sidebar
tab: `patient.gsp` (Résumé), `antecedents.gsp`, `examenClinique.gsp`, `diagnostic.gsp`,
`priseEnCharge.gsp`, `anatomopathologie.gsp`, `evolution.gsp`, `biologie.gsp`, `sortie.gsp`,
`imagerie.gsp`. Two shared fragments (`sidebarNav.gsp`, `patientHeader.gsp`) are included on
every page.

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
  than a true null. Both have bitten this module in production, repeatedly - see the next
  bullet.
- **Patient names are never formatted in a template.** The dashboard header showed
  `null null` (or, after the v1.2.2 guard, a bare `, `) for patients whose `person_name`
  row holds the literal four-character string `"null"`. It was "fixed" twice in the
  templates and came back both times, because every page and every breadcrumb formatted
  the name inline: each fix had to be copy-pasted into five places and one was always
  missed. As of 1.3.0 the name is assembled once in
  `PatientviewDisplayName.of(Patient)` (in `api`, unit-tested by
  `PatientviewDisplayNameTest`), every page controller puts the result in the model as
  `patientDisplayName`, and the templates only render that. `ModuleWiringTest` fails the
  build if any `.gsp` reads `patient.familyName`/`patient.givenName` again, or if a page
  controller forgets to supply the attribute. **If you see a name problem, fix
  `PatientviewDisplayName`, not a template.**
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
  `appui`, matched against what Reference Application 2.12.2 bundles. A missing one of these
  stops the module from starting.
- `aware_of_modules` — `imaging`, an **optional** companion: start it first if it is
  present, make its classes visible, but load patientview fine without it. This is what
  keeps the Imagerie tab's PACS list from becoming a hard dependency — see §14.
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
  service/DAO delegation logic and the diagnosis-precedence/alert-threshold rules. Also
  covers `DicomStudyBridge`'s degradation path: the `imaging` module is not on the test
  classpath, which is exactly the "not installed" case the Imagerie tab has to survive.
- **`PatientviewDisplayNameTest`** — the patient-name rendering rules (§7), including the
  literal-`"null"` inputs that caused the original bug and the one-half-of-the-name case
  that the template-only fix rendered as a stray comma.
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

- All ten tabs are built (§2); the Fiche de Neurochirurgie is fully covered.
- The Imagerie tab's DICOM study list needs the separate `imaging` module installed and
  started. Without it the tab still works, showing this module's own comptes rendus and a
  message explaining the PACS link is unavailable - but there is no fallback path that
  talks to Orthanc directly, by design (§14).
- No CSRF token on the `.form` POST endpoints — matches the rest of the legacy OpenMRS UI
  framework's module conventions (a pre-existing ecosystem-wide gap, not introduced here).
- DICOM traffic itself is unencrypted; only the HTTP(S) layer is covered by
  `deployment/`. Orthanc's `DicomTlsEnabled` is a separate, more involved configuration
  step for later.
- `MedicalHistory.hbm.xml` still carries `unique="true"` on its `patient` `many-to-one`,
  left over from the v1.0.1 single-row-per-patient design that changeset
  `patientview-2026-08-12-01` undid. It is inert (Hibernate only uses it for DDL generation,
  and OpenMRS migrates with Liquibase), but it would resurrect the constraint for anyone who
  ever pointed `hbm2ddl` at this schema.
- `diagnostic.gsp`'s laterality `<option value="Bilat\u00e9rale">` stores the literal
  text `Bilat\u00e9rale`: a `.gsp` emits an attribute value verbatim, so a Groovy-style
  unicode escape is not decoded there. Existing rows already hold that string, so changing
  it is a data decision, not just a template fix - the newer tabs use HTML entities in
  `value="..."` instead, which browsers do decode.

## 12. Version history

- **1.3.0** — Phase 3, completing the Fiche: Prise en charge (§9), Évolution
  postopératoire & Séquelles (§11-12), Biologie (§7), Sortie & Suivi (§13-14) and
  Imagerie (§6) - eight new append-only entities, their REST/page controllers and tabs, and
  eight new `medreport` sections. The Imagerie tab reads DICOM studies from the separate
  `imaging` module reflectively rather than reimplementing an Orthanc client (§14). Fixes the
  `null null` patient-name header for good by moving name assembly out of the templates into
  `PatientviewDisplayName`, with build-time guards against the pattern that regressed twice
  (§7).
- **1.0.1** — Phase 1: sidebar navigation, Antécédents, Examen clinique.
- **1.0.2 / 1.2.0** — Phase 2: Diagnostic neurochirurgical, Anatomopathologie; security/NFR
  foundation (privileges, append-only non-repudiation, DB indexes, `deployment/`); CSS
  namespace fix; null-render guard fix.
- **1.2.2** — Contributes this module's clinical data to the `medreport` reporting module via
  a single new resource, `api/src/main/resources/medreport-datasource.json`. **No Java, no
  new dependency, no schema change** — see §13.
- **1.2.1** — Two-tier `App:` privilege model (view/manage) with explicit enforcement at
  every controller, since the Reference Application's auto-grant convention made the
  original API-level privileges an ineffective boundary; second, more defensive round of
  the null-render guard fix (literal string `"null"`, not just true null).

---

## 13. Contributing data to `medreport` (1.2.2)

`medreport` renders patient reports from whatever data other modules declare. This module
declares its own through **one file**:

```
api/src/main/resources/medreport-datasource.json
```

It lists the sixteen clinical sets (admission, medical history, surgical history, vitals,
neurological exam, scores, diagnosis, pathology, imaging notes, biology, medical and
surgical treatment, post-operative course, sequelae, discharge, follow-up), their fields
with **fr/en/ar labels**, the privilege each requires, and which `PatientviewService`
method returns each set.

The DICOM study list is deliberately **not** in the manifest: those rows come from the
`imaging` module rather than from a patientview table, and `medreport` already links to
its own imaging reports page.

### What this module did *not* have to do

- No dependency on `medreport` in any `pom.xml`.
- No Java code, no interface to implement, no Spring bean.
- No schema change.

`medreport` discovers the manifest on the classpath of every started module and calls the
named service methods reflectively. The contract is the manifest schema plus the fact that
those methods already return `Map<String,Object>` / `List<Map<String,Object>>` — JDK types on
both sides. **If `medreport` is not installed, nothing reads the file and nothing changes.**

Because the calls go through `Context.getService(PatientviewService.class)`, this module's own
`@Authorized` advice still runs on every fetch: `medreport` cannot be used to bypass
patientview's access rules. The manifest's `requiredPrivilege` is an additional filter that
also hides a set from the report's selection tree, so a user who may not view data cannot even
ask for it.

### The guard against drift

The integration is loose by design, which means nothing at compile time notices when the
manifest and this module's code diverge — a renamed DAO map key would leave a field selectable
in the report window but permanently blank in the document.

`MedreportDatasourceManifestTest` closes that gap from this side, with no OpenMRS context:

- every declared `serviceClass`/`method` exists on `PatientviewService` with the expected
  signature, and returns a `Map` for a flat set or a `List` for a repeating one;
- every declared field id (and every `recordTitleKey`) is a key `PatientviewDAOImpl` actually
  puts into its map;
- the advertised privilege is one this module really defines;
- no `pom.xml` or source file references `medreport`.

It is the only place this module mentions `medreport`, and it does so without importing
anything from it.

### Upgrading

Rebuild and upload `patientview1.3.0.omod`. **Uninstall the previous version first** — the
filename carries the version, so OpenMRS would otherwise keep both. If `medreport` was already running, open
`/openmrs/medreport/settings.page?refresh=true` afterwards so it rescans data sources; module
start order is not fixed and it may have scanned before this module started.

---

## 14. Reading DICOM studies from the `imaging` module (1.3.0)

The Imagerie tab (Fiche §6) has two halves, and only one of them is this module's data.

**What patientview owns.** `ImagingNote` / `patientview_imaging_note`: the *compte rendu* -
type d'examen (TDM / IRM / Angio-TDM / Angio-IRM), date, and the clinician's reading. This is
a clinical note *about* a study, and it exists whether or not the images ever reached a PACS.
Ordinary append-only entity, same pattern as every other tab.

**What patientview does not own.** The studies themselves. The roadmap originally called for an
HTTP client in this module hitting Orthanc's `/patients` and `/studies`, with the base URL and
credentials as OpenMRS global properties. That was dropped once it became clear the separate
[`imaging`](https://github.com/UCLouvain-ICTEAM) module already does all of it: it stores the
Orthanc base URL, credentials and proxy URL in its own `OrthancConfiguration` entity, syncs
studies, resolves DICOM PatientIDs to OpenMRS patients, and serves its own series / instances /
viewer pages. Reimplementing that here would have meant a second place to configure Orthanc, a
second copy of the patient-matching rule, and a second set of credentials to rotate - three
things guaranteed to drift apart.

So the tab reads from `imaging` instead, through
`api/.../api/imaging/DicomStudyBridge.java`:

- `Context.loadClass("org.openmrs.module.imaging.api.DicomStudyService")` (the global
  `OpenmrsClassLoader`, which searches every started module - a plain `Class.forName` from this
  module's own loader would not find it), then `getStudiesOfPatient(patient)` reflectively.
- Each study is flattened to a `Map` (`studyId`, `studyDate`, `studyTime`, `studyDescription`,
  `studyInstanceUID`). `studyDate` is DICOM's raw `YYYYMMDD` reformatted for reading; each row
  links to `imaging/series.page?patientId=..&studyId=..`, and the panel header links to
  `imaging/studies.page`. Nothing is cached or copied locally - Orthanc stays the source of
  truth for images, `imaging` stays the source of truth for how to reach it.
- Exposed as `PatientviewService.getImagingStudies(Patient)` /
  `isImagingModuleAvailable()` rather than called from the controller directly, so controllers
  keep a single entry point and **this module's own view privilege is still enforced before
  another module's data is rendered inside a patientview page**.

### Why reflection, and what it costs

Same trade as the `medreport` integration in §13, in the opposite direction: no `pom.xml`
dependency, no shared type, no compile-time coupling. `imaging` is declared only under
`config.xml`'s `<aware_of_modules>`, which is OpenMRS's way of saying *start it before me if
it's there, and let me see its classes - but load me fine if it isn't*.

The cost is that nothing at compile time notices if `imaging` renames
`getStudiesOfPatient`. Every failure mode - module absent, module stopped, signature moved,
classloader can't see it - is caught and degrades to an empty list, because all of them mean
the same thing to a clinician looking at this tab: there are no studies to show. None is worth
turning a patient record into a stack trace.
`PatientviewServiceImplConnectionTest.imagingStudiesShouldDegradeToAnEmptyListWhenTheImagingModuleIsAbsent()`
pins that behaviour: `imaging` is genuinely not on the test classpath, so the test exercises
the real not-installed path rather than a mock of it.

**If the tab shows "le module d'imagerie n'est pas installé"** on a server that does run
`imaging`, check in this order: the module is *started* (not merely uploaded) in
`/openmrs/admin/modules/module.list`; `imaging`'s own Orthanc configuration points at a
reachable Orthanc; and its study sync has run (patientview only reads what `imaging` has
already synced - it never triggers a fetch itself).
