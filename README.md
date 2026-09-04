# Patientview (Neurosurgery Patient View) — OpenMRS Module

A custom OpenMRS module that replaces the default coreapps patient dashboard with a
neurosurgery-focused one: a multi-tab record covering the whole of the department's paper
"Fiche de Neurochirurgie" - admission, antecedents, clinical and neurological exam,
biology, imaging, diagnosis, management, anatomopathology, post-operative course,
sequelae, discharge and follow-up - with full CRUD backed by MySQL and a two-tier
privilege model so nurses and surgeons/radiologists see different levels of access.

**Target platform:** OpenMRS Platform 2.5.9 / Reference Application 2.12.2
**Module ID:** `patientview` · **Package:** `org.openmrs.module.patientview` · **Version:** `1.4.2`

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

Every tab's data is additionally projected into the core OpenMRS clinical model so the
FHIR2 module can serve it - see §15 for what that covers today.

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
- **`FhirMappingManifestTest`** — checks the FHIR mapping manifest against the DAO in both
  directions and against the projector's wiring; see §15.
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
- **105 of the 120 concept-backed fields have no concept yet** (§15). The export machinery is
  complete and tested. Thirteen of the sixteen sets export *something* on a stock dictionary
  since 1.4.2, but for most of them that is only their free-text note: the Glasgow and Karnofsky
  scores still resolve to nothing, because a demo install carries 444 concepts and no Glasgow or
  Karnofsky at all (§15). This is dictionary work needing a clinician and a
  CIEL curator, not a developer - and `tools/ciel_match.py` turns it from 105 manual searches
  into reviewing a pre-filled table. OCL disabled anonymous API access, so the codes have to be
  resolved against the dictionary as loaded on your own server; that script does exactly that
  without needing a Python database driver.
- The lab panels (NFS, ionogramme, coagulation) and CRP/glycémie/créatinine are stored as
  text by this module, so mapping them to CIEL's *numeric* concepts will be reported as a
  datatype mismatch rather than silently coerced. Either map them to text-datatype concepts
  or split the values out first - a deliberate decision, not an oversight.
- FHIR2 does not serve `Procedure`, so the two surgical sets export as observations. Because
  the mapping lives in a manifest rather than in Java, retargeting them if FHIR2 adds it
  later is a data edit.
- FHIR2 4.x requires JDK 11; this module compiles at source level 1.8. Moving to 4.x is a
  Java migration for patientview, independent of anything in §15.
- The Imagerie tab's DICOM study list needs the separate `imaging` module installed and
  started. Without it the tab still works, showing this module's own comptes rendus and a
  message explaining the PACS link is unavailable - but there is no fallback path that
  talks to Orthanc directly, by design (§14).
- No CSRF token on the `.form` POST endpoints — matches the rest of the legacy OpenMRS UI
  framework's module conventions (a pre-existing ecosystem-wide gap, not introduced here).
- DICOM traffic itself is unencrypted; only the HTTP(S) layer is covered by
  `deployment/`. Orthanc's `DicomTlsEnabled` is a separate, more involved configuration
  step for later.
- **Data residue from the laterality bug fixed in 1.3.0.** Any
  `patientview_neurosurgical_diagnosis` row saved before 1.3.0 with laterality *Bilatérale*
  holds the literal text `Bilat\u00e9rale`. `diagnostic.gsp` had a Groovy-style unicode
  escape inside an HTML attribute value, and a `.gsp` emits an attribute value verbatim rather
  than decoding it, so that text is what the browser submitted and what got stored. The
  template is fixed (it uses `&eacute;` now, which the browser *does* decode before
  submitting, as every tab added in 1.3.0 already did), but historical rows still render the
  raw escape on the Diagnostic tab and in `medreport` output. Repairing them means an
  in-place `UPDATE` on a clinical table, which this module has never done (§3) - so it is
  deliberately left as an explicit decision rather than quietly migrated. To see whether any
  exist: `SELECT diagnosis_id, laterality FROM patientview_neurosurgical_diagnosis WHERE
  laterality LIKE '%\\u00%';`

## 12. Version history

- **1.4.2** — Curates the free-text `notes` field on all eleven sets that have one to
  `CIEL:162169` (Text of encounter note), verified against a live dictionary. Takes curation
  from 15 fields to 26 and the sets exporting anything from 2 to 13 on a stock Reference
  Application. Also fixes two real defects in `ciel_match.py` found by running it against
  that dictionary: it matched on only the longest word of a label (picking "clinical" over
  "note", so it missed the very concept this release curates), and it would have ingested a
  UNION's heading row as data.
- **1.4.1** — Medical history now exports every recorded version rather than only the
  current one, so each becomes its own dated encounter. Adds `tools/ciel_match.py`, which
  turns the concept backlog from 105 manual dictionary searches into a review pass, and
  relaxes the curation assertions from equalities to floors so a curation batch no longer
  fails the build (§15).
- **1.4.0** — Exposes the whole record through the FHIR2 API, without depending on FHIR2:
  an append-only projection of every set into core `Encounter`/`Obs`/`Condition`, driven by a
  declarative CIEL mapping manifest, with an idempotency ledger, a per-patient and server-wide
  backfill, a coverage report, and build-time guards against manifest/DAO/projector drift. 15
  of 121 fields ship with verified concepts; the rest is dictionary curation (§15).
- **1.3.0** — Phase 3, completing the Fiche: Prise en charge (§9), Évolution
  postopératoire & Séquelles (§11-12), Biologie (§7), Sortie & Suivi (§13-14) and
  Imagerie (§6) - eight new append-only entities, their REST/page controllers and tabs, and
  eight new `medreport` sections. The Imagerie tab reads DICOM studies from the separate
  `imaging` module reflectively rather than reimplementing an Orthanc client (§14). Fixes the
  `null null` patient-name header for good by moving name assembly out of the templates into
  `PatientviewDisplayName`, with build-time guards against the pattern that regressed twice
  (§7). Also clears two older latent defects: `MedicalHistory.hbm.xml`'s stale
  `unique="true"` on `patient`, left from the v1.0.1 single-row design that changeset
  `patientview-2026-08-12-01` undid and a live hazard for any `hbm2ddl` run against this
  schema; and `diagnostic.gsp`'s laterality option, which stored a literal `\u00e9` escape
  instead of the accented text (§11 covers the rows already saved that way).
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

## 15. Exposing the record through FHIR (1.4.0)

The FHIR2 module serves FHIR resources out of the **core** OpenMRS schema: its data-access
layer queries `obs`, `encounter`, `conditions` and friends directly, and it documents no way
for another module to register a resource provider or contribute rows from its own tables.
patientview's sixteen `patientview_*` tables were therefore invisible to it - which is the
actual reason nothing was exported before 1.4.0, and no amount of adapter code in either
module would have changed it.

So this release does not integrate with FHIR2 at all. It makes the data reachable through the
core clinical model, and lets FHIR2 do its own job:

```
saveXxx()  ->  patientview_* row (unchanged, still the system of record for the UI)
           ->  ObsProjector      ->  Encounter + Obs        ->  FHIR2 serves Observation
                                 ->  Condition (section 8)  ->  FHIR2 serves Condition
```

**`ObsProjector` contains no reference to FHIR2, deliberately.** It calls only
`ConceptService`, `EncounterService`, `ConditionService` and the encounter's obs cascade - core
platform APIs, stable across the whole 2.x line. That indirection is the forward-compatibility
story: across FHIR2's major versions the one thing that has broken downstream modules is its
**DAO layer** (2.x → 3.x replaced the Hibernate `Criteria` object with
`OpenmrsFhirCriteriaContext`, and the core team's guidance is that modules which "leveraged the
FHIR2 DAO API classes to implement their own DAOs will be impacted"; 3.x is otherwise
"basically backwards compatible"). Nothing here touches that layer, so an upgrade to FHIR2 4.x
is a no-op for this module. **Do not add an fhir2 dependency to either `pom.xml`** - it would
throw that property away, and `FhirMappingManifestTest` is not a substitute for the discipline.

### Idempotent because the module is append-only

Every entity here is insert-only, so *"project the rows that are not in the ledger yet"* is a
complete description of the work. That single property buys a lot:

- the live path (after a save) and the backfill of historical rows are the **same operation**;
- re-running either is safe, so the "Export FHIR" button and the server-wide backfill cannot
  double-write;
- it is self-healing - a row whose export was skipped for want of a concept is retried the next
  time anything for that patient is saved, or on the next backfill;
- ordering is irrelevant, so a back-dated record is picked up as reliably as the newest one.

The ledger is `patientview_fhir_projection` (source set, source row uuid, encounter uuid), with
a unique index on `(source_set, source_uuid)` so the guarantee is enforced by the database and
not only by the code that checks before inserting. Every DAO getter now also returns the row's
`uuid`, which is what the ledger keys on.

### Transaction propagation, which is load-bearing

`projectSet` runs `REQUIRED` - in the **caller's** transaction - because it runs straight after
a save and must see the row that save just wrote. An earlier draft of this used `REQUIRES_NEW`
for isolation; that suspends the caller's transaction, leaves the new row invisible, and
silently pushes every projection one save behind forever. `projectPatient` *does* take
`REQUIRES_NEW`, because it only reads already-committed rows and the server-wide backfill needs
one patient's failure not to abandon the sweep. See `moduleApplicationContext.xml`.

A dictionary gap can never fail a clinical save: every reason a field might not be exportable -
no concept curated, a declared concept that does not resolve here, a datatype disagreement, a
coded boolean with no `concept.true` configured - is checked *before* any obs is built, and
reported instead of thrown.

### Concepts: CIEL, by mapping, never by id

`api/src/main/resources/patientview-fhir-mapping.json` declares all 16 sets and 121 projectable
fields. Concepts are resolved at runtime with
`ConceptService.getConceptByMapping(code, source)` - **never by numeric `concept_id`**, because
those are install-specific: an id that is correct here would point at a different concept, or
none, on another server.

**26 of the 121 fields carry a concept.** Those were each verified against a public
source - the CIEL vitals codes against `openmrs-module-referenceapplication`'s own
`htmlforms/vitals.xml`, and the Glasgow/Karnofsky codes against loinc.org:

| Fiche | Fields | Codes |
| --- | --- | --- |
| §4 Constantes | temperature, TA systolique/diastolique, pouls, FR, SpO2, poids, taille | `CIEL:5088 5085 5086 5087 5242 5092 5089 5090` |
| §5 Scores | Glasgow E / V / M / total, Karnofsky | `LOINC:9267-6 9270-0 9268-4 9269-2 89243-0` |
| §11 Évolution | Glasgow et Karnofsky postopératoires | the **same** codes as §5 |
| 11 sets | the free-text `notes` field on each | `CIEL:162169` Text of encounter note |

That last row is deliberate: in FHIR a post-operative score is the same `Observation.code` at a
later `effectiveDateTime`, not a different code.

`bmi` is deliberately **not** projected - the reference application's vitals form stores no
concept for it either, because it is derived from height and weight and FHIR consumers
recompute it.

Medical history exports **every** recorded version, not just the current one: the table is
append-only, so each version becomes its own dated encounter, which is what makes a
"comorbidities as of this admission" query answerable. `getMedicalHistoryVersions` returns them
all and `getMedicalHistory` delegates to it for the first, so there is still one map-builder.

The remaining **94 fields await curation**, which is a dictionary task rather than a coding
one: search your loaded CIEL for the clinical label in the field's `name`, then add
`{"source": "CIEL", "code": "<id>"}` to its `concept` array. The file is packaged in the omod,
so shipping new codes means rebuilding.

Do not do that by hand. **`tools/ciel_match.py`** exists to make curation routine: `sql` mode
emits one query that searches your dictionary for every uncurated label at once - matching on
any synonym, since that is where a clinician's wording usually lands, while reporting the fully
specified name so the reviewer sees what the concept really is - and `apply` mode reads the
approved rows back into the manifest. It needs no Python database driver: you run the SQL
through `docker exec`, so it works against a running container or a restored dump. It refuses
to apply if more than one candidate is still present for a field, and warns when a candidate's
datatype cannot hold the field's value, which catches a mismatch during review rather than
after a rebuild. A name match is a suggestion, not a decision - a clinician makes the review
pass.

Ask the running server what is left:

```
GET  /openmrs/ws/rest/../module/patientview/fhirProjection.form   # coverage report
POST /module/patientview/fhirProjection.form?patientId=123        # backfill one patient
POST /module/patientview/fhirProjection.form?allPatients=true     # backfill everything
```

Section 8 is the exception that needs no dictionary at all: OpenMRS `Condition` accepts free
text through `CodedOrFreeText.setNonCoded`, and FHIR2 renders that as `Condition.code.text`. So
the neurosurgical diagnosis exports correctly today, with the lesion descriptors folded into
`Condition.additionalDetail`.

### What is exportable today, without curation

At most four of the sixteen sets - §4 Constantes, §5 Scores, §11 (the two scores only) and §8
Diagnostic - and **which of those actually export depends on the dictionary loaded on your
server**, not on this module. A declared code that your dictionary does not carry resolves to
nothing, and the field is skipped and reported.

Measured against a stock Reference Application (444 concepts, the demo subset), it is
**thirteen of sixteen** as of 1.4.2: §4 Constantes, whose eight CIEL vitals codes all resolve;
§8 Diagnostic, which needs no dictionary at all; and the eleven sets carrying a `notes` field,
which resolves through `CIEL:162169`. Only Motif d'hospitalisation, Antécédents médicaux and
Anatomopathologie export nothing at all, having neither.

That is thirteen sets exporting *something*, not thirteen sets exporting *fully* - most contribute
only their note. §5 and §11's scores are declared against LOINC, and a stock install carries only
11 LOINC mappings, all vitals, so Glasgow and Karnofsky resolve to nothing there. That is not a
defect in the mapping; those are the correct LOINC codes. It means **full CIEL still has to be
loaded** before the clinically interesting half of the Fiche can export.

Run the coverage report against your own server rather than trusting this paragraph - it
distinguishes "no concept declared" from "declared but not resolvable here", which is exactly
this distinction.

### The guard against drift

`FhirMappingManifestTest` (in `api`, no OpenMRS context) checks the manifest against the code
in both directions, because a string-keyed manifest drifts silently:

- every declared field is a key its named DAO getter really produces, **and** every key that
  getter produces is either mapped or explicitly excluded with a reason - the second direction
  is what stops a newly added clinical field from never being exported;
- every set has a `case` in `ObsProjector.fetchRows`, or it would read no rows at all;
- every getter exposes the `uuid` the ledger keys on;
- concepts are declared with a mapping source, never a bare local id;
- the field count (120) is asserted exactly, since it can only change with a schema change;
  the curation counts are asserted as **floors** (≥ 15 coded, ≥ 4 sets exporting) rather than
  equalities, because curation is expected routine work and an exact assertion would fail the
  build on every batch and train people to edit the number without reading it. A floor still
  catches the regression that matters - concepts disappearing from the manifest. `ciel_match.py`
  prints the new figure to raise it to.

`QuickDeploymentTest` covers the other half: it boots a real Spring/Hibernate context, so the
new mapping file, the projector bean and its transaction proxy are exercised rather than assumed.
