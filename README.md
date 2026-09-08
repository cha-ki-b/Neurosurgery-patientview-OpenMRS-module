# Patientview (Neurosurgery Patient View) — OpenMRS Module

A custom OpenMRS module that replaces the default coreapps patient dashboard with a
neurosurgery-focused one: a multi-tab record covering the whole of the department's paper
"Fiche de Neurochirurgie" - admission, antecedents, clinical and neurological exam,
biology, imaging, diagnosis, management, anatomopathology, post-operative course,
sequelae, discharge and follow-up - with full CRUD backed by MySQL and a two-tier
privilege model so nurses and surgeons/radiologists see different levels of access.

**Target platform:** OpenMRS Platform 2.5.9 / Reference Application 2.12.2
**Module ID:** `patientview` · **Package:** `org.openmrs.module.patientview` · **Version:** `1.4.5`

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
- **76 of the 120 concept-backed fields have no concept yet** (§15.7). The export machinery is
  complete and tested; 44 fields and 14 of the 16 sets export today. What is left is dictionary
  work needing a clinician and a CIEL curator, not a developer, and `tools/ciel_match.py` turns
  it into reviewing a pre-filled table. Of the remainder, roughly a third are narrative free text
  with no natural coded equivalent, so a realistic ceiling is well short of 120.
- **The OCL subscription cannot work with the bundled `openconceptlab` 1.2.9** - its 2007-era
  HTTP client cannot authenticate to current OCL, and the import fails with a 403 whose body
  reads "Anonymous API access is disabled" even with a valid token. CIEL has to be loaded from an
  offline export instead, and future dictionary updates need that same manual refresh until the
  module is upgraded (§15.8).
- **`concept.true` / `concept.false` must be configured** or every coded boolean is silently
  skipped. They were unset on this installation - an OpenMRS-wide gap, not specific to this
  module (§15.9).
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

- **1.4.5** — **Curating a concept now re-projects the rows it affects, automatically.**
  Each ledger entry records a fingerprint of the mapping it was projected with; when that
  mapping changes the row is projected again and its previous encounter and conditions are
  voided, rather than staying stuck with whatever resolved at the time. Previously this
  needed voiding and clearing the ledger by hand, which had already been done twice.
  Rows written before 1.4.5 have no fingerprint and so count as stale, meaning the upgrade
  itself picks up every concept curated so far.
- **1.4.4** — Adds the `conditionFlag` field type: a true boolean comorbidity or
  complication now exports as an OpenMRS `Condition` with a coded diagnosis, not as an
  observation. CIEL carries these as Diagnosis-class concepts with datatype N/A, which no
  obs can hold - so the concept was right all along and the representation was wrong, and
  all 22 such fields were being reported as datatype mismatches. Nine verified fields
  converted (hypertension, stroke, renal failure, headache, visual disturbance,
  post-operative infection, hydrocephalus, CSF leak, aphasia); curation 35 → 44 of 120.
  Covered by `ObsProjectorConditionFlagTest`, the projector's first unit tests.
- **1.4.3** — **Fixes a syntax error that had disabled every CRUD form since 1.4.0.** An
  unescaped apostrophe in one error message meant `patientview-crud.js` never parsed, so
  *every* function it defines was undefined in the browser - not only the new one. Adds a
  build-time guard (`everyJavascriptFileMustParse`) that compiles each `.js` with Nashorn,
  since the build checked XML, JSON and GSP braces but never JavaScript. Also curates 13
  more fields against a freshly loaded CIEL, including Glasgow (`CIEL:160347`) and
  Karnofsky (`CIEL:5283`): curation 26 → 35 of 120, sets exporting 13 → 14.
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

## 15. Exposing the record through FHIR

Everything this module collects is also written into the core OpenMRS clinical model, where the
FHIR2 module serves it as standard FHIR resources. This section explains why it is built that
way, what comes out, and what you have to do to get the rest of it out.

### 15.1 Why the data had to move, not the code

FHIR2 serves resources out of the **core** schema. Its data-access layer queries `obs`,
`encounter` and `conditions` directly, and it documents no supported way for another module to
register a resource provider or contribute rows from its own tables.

So patientview's sixteen `patientview_*` tables were invisible to it, and no adapter written in
either module could have changed that. The question was never *where should the FHIR code live*
but *does this data enter the model FHIR2 reads at all* - and it did not.

The fix is therefore not an integration. Records keep being saved exactly as before, and a
projector additionally writes them into the core model:

```
saveXxx()  ->  patientview_* row      (unchanged - still what the module's own screens read)
           ->  ObsProjector  ->  Encounter + Obs   ->  FHIR2 serves Observation
                             ->  Condition         ->  FHIR2 serves Condition
```

**`ObsProjector` contains no reference to FHIR2, deliberately.** It calls only `ConceptService`,
`EncounterService`, `ConditionService` and the encounter's obs cascade - core platform APIs,
stable across the whole 2.x line.

That indirection is the forward-compatibility story. Across FHIR2's major versions the one thing
that has broken downstream modules is its **DAO layer**: 2.x to 3.x replaced the Hibernate
`Criteria` object with `OpenmrsFhirCriteriaContext`, and the core team's guidance is that modules
which "leveraged the FHIR2 DAO API classes to implement their own DAOs will be impacted", while
3.x is otherwise "basically backwards compatible". Nothing here touches that layer, so upgrading
FHIR2 is a no-op for this module - which was confirmed in practice when this instance moved from
fhir2 1.2.2 to 1.6.0 mid-development and nothing needed changing.

**Do not add an fhir2 dependency to either `pom.xml`.** It would throw that property away. A
static check in `ModuleWiringTest` fails the build if one ever appears.

### 15.2 The three ways a field can be exported

Which one a field uses is declared by its `type` in the mapping manifest.

| Manifest `type` | Becomes | Used for |
| --- | --- | --- |
| `numeric` `text` `date` `boolean` | an `Obs` on the encounter | measurements, scores, narrative, yes/no answers |
| `conditionFlag` | a `Condition` with a **coded** diagnosis | comorbidities and complications |
| `condition` | a `Condition` with **free text** | the section 8 neurosurgical diagnosis |

`conditionFlag` exists because of a real modelling mistake found during curation. CIEL carries
comorbidities and complications - hypertension, hydrocephalus, CSF leak - as **Diagnosis-class
concepts with datatype `N/A`**. No observation can hold a value of that datatype, so every one of
those fields was being reported as a datatype mismatch and exported nothing. The concept was
right all along; representing "this patient has hydrocephalus" as an observation was wrong.
A Condition is both the only thing that works and the correct FHIR modelling.

Two rules the projector applies, and the reasoning matters more than the code:

- **A false boolean exports nothing.** The forms cannot distinguish "no" from "not assessed", so
  asserting a negative would invent a clinical finding nobody recorded.
- **A datatype mismatch is reported, never coerced.** `"0.95 g/L"` and `"95 mg/dL"` are the same
  glucose; a parser guessing units in a neurosurgery record is a safety bug, not a convenience.

Section 8 needs no dictionary at all: OpenMRS `Condition` accepts free text through
`CodedOrFreeText.setNonCoded`, which FHIR2 renders as `Condition.code.text`. The lesion
descriptors are folded into `Condition.additionalDetail`.

### 15.3 The ledger: exported once, re-exported when the mapping changes

`patientview_fhir_projection` records, per source row, which encounter it produced and a
**fingerprint of the mapping it was projected with**.

Because every patientview entity is append-only, *"project the rows whose fingerprint is not
current"* completely describes the work. That single property buys a lot:

- the live path (after a save) and the backfill of historical rows are the **same operation**;
- re-running either is safe, so the "Export FHIR" button cannot double-write;
- ordering is irrelevant - a back-dated record is picked up as reliably as the newest;
- **curating a concept later re-exports the rows it affects, automatically.**

That last point was a genuine design gap until 1.4.5. The ledger used to record only *that* a row
was done, so a row projected before its concept existed kept whatever little it had managed to
export - forever. Working around it meant voiding the encounter and clearing the ledger by hand.

The fingerprint is a SHA-256 digest over each field's id, type and ordered concept references,
plus the group concept and date key: everything that changes what a projection would produce, and
nothing else. Editing a label or a comment leaves it identical, so cosmetic manifest edits do not
churn every record in the database.

When a fingerprint has moved, the previous output is **superseded**: voided, not deleted, so the
void reason preserves the audit trail. Conditions are voided explicitly, because `voidEncounter`
cascades only to observations and would otherwise leave them behind as duplicates of the ones the
re-projection is about to create.

`manifest_fingerprint` is nullable and **null counts as stale**, so upgrading to 1.4.5 re-exports
every pre-existing row once, picking up all curation done to date.

### 15.4 Transaction propagation, which is load-bearing

`projectSet` runs `REQUIRED` - in the **caller's** transaction - because it runs straight after a
save and must see the row that save just wrote. An earlier draft used `REQUIRES_NEW` for
isolation; that suspends the caller's transaction, leaves the new row invisible, and silently
pushes every projection one save behind, permanently.

`projectPatient` *does* take `REQUIRES_NEW`: it reads only committed rows, and a server-wide
backfill must not let one patient's failure abort the sweep. See `moduleApplicationContext.xml`.

**A dictionary gap can never fail a clinical save.** Every reason a field might not be exportable
- no concept curated, a code that does not resolve here, a datatype disagreement, a coded boolean
with no `concept.true` configured - is checked *before* any obs is built, and reported instead of
thrown.

### 15.5 Concepts are referenced by mapping, never by id

`api/src/main/resources/patientview-fhir-mapping.json` declares all 16 sets and 121 fields.
Concepts resolve at runtime through `ConceptService.getConceptByMapping(code, source)` -
**never by numeric `concept_id`**, because those are install-specific: an id correct on one
server points at a different concept, or none, on the next.

Each field's `concept` is an **ordered list**, and the first that resolves wins. That is how one
build works on dictionaries that differ:

```json
{ "id": "gcs", "name": "Glasgow coma score total", "type": "numeric",
  "concept": [ { "source": "CIEL",  "code": "160347" },
               { "source": "LOINC", "code": "9269-2" } ] }
```

It is also how a **local dictionary** fits in, for the fields CIEL does not carry: declare the
standard code first and your own second. Two caveats worth knowing before you start. A local code
makes the data FHIR-*shaped*, not FHIR-*interoperable* - any client can read it, nothing outside
your hospital knows what it means. And observations are append-only, so rows exported today
against a local code keep it forever; adding CIEL later changes only new rows. Prefer loading
CIEL first and using local concepts only for the genuine residue.

### 15.6 Curation status

**44 of the 120 concept-backed fields carry a concept** (a 121st, the section 8 diagnosis, needs
none). Every code was verified against the loaded dictionary or a public source before being
committed:

| Fiche | Fields | Codes |
| --- | --- | --- |
| §4 Constantes | temperature, TA systolique/diastolique, pouls, FR, SpO2, poids, taille | `CIEL:5088 5085 5086 5087 5242 5092 5089 5090` |
| §5 Scores | Glasgow total, Karnofsky | `CIEL:160347` `CIEL:5283`, then LOINC |
| §11 Évolution | Glasgow et Karnofsky postopératoires | the **same** codes as §5 |
| 11 sets | the free-text `notes` field on each | `CIEL:162169` Text of encounter note |
| 9 fields | comorbidities and complications, as `conditionFlag` | `CIEL:117399 111103 113338 139084 123074 129346 117470 155486 121529` |

Reusing the §5 codes for §11 is deliberate: in FHIR a post-operative score is the same
`Observation.code` at a later `effectiveDateTime`, not a different code.

Three things are deliberately **not** exported:

- `bmi` - derived from height and weight. The reference application's own vitals form stores no
  concept for it either, and FHIR consumers recompute it.
- Glasgow **eye / verbal / motor** components - CIEL models GCS as a single total and has no
  component concepts, so these have no standard equivalent anywhere. They are the concrete case
  for a local dictionary (§15.5).
- `deceased` - death is a patient *status* (`Patient.deceasedBoolean` in FHIR), not a condition.

Medical history exports **every** recorded version, not only the current one: the table is
append-only, so each version becomes its own dated encounter, which is what makes "comorbidities
as of this admission" answerable.

### 15.7 Curating the rest

The remaining **76 fields** are a dictionary task, not a coding one. Do not do it by hand.

**`tools/ciel_match.py`** turns it into a review pass:

```bash
python tools/ciel_match.py sql > candidates.sql          # one query for every uncurated label
docker exec -i openmrs-mysql mysql -uopenmrs -p openmrs -N < candidates.sql > candidates.tsv
#   open candidates.tsv, KEEP AT MOST ONE ROW PER FIELD, delete the rest
python tools/ciel_match.py apply candidates.tsv          # writes the approved codes back
```

It needs no Python database driver - the SQL runs through `docker exec`, so it works against a
running container or a restored dump. It refuses to apply while two candidates remain for a
field, and warns when a candidate's datatype cannot hold the field's value.

**A clinician must make the review pass.** This is not a formality. Run against a real CIEL, the
tool's loose-match tier suggested *Psyllium* - a laxative - for both `corticosteroids` and
`analgesics`, *disomnia* (a sleep disorder) for `sphincterDisturbances`, and *hemobilia* for
`hemorrhage`. The tool applies nothing on its own, for exactly this reason.

Then rebuild, redeploy, and press **Export FHIR** once: §15.3 means the affected rows re-export
themselves.

### 15.8 Loading CIEL

A stock Reference Application carries about 444 concepts - a demo subset with no neurosurgical
vocabulary at all, and no Glasgow or Karnofsky by any name. Full CIEL is roughly 50,000. Check
what you have before curating anything:

```bash
docker exec openmrs-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" openmrs -N -e "
  SELECT (SELECT COUNT(*) FROM concept WHERE retired=0), crs.name, COUNT(*)
  FROM concept_reference_map crm
  JOIN concept_reference_term crt USING (concept_reference_term_id)
  JOIN concept_reference_source crs USING (concept_source_id)
  GROUP BY crs.name ORDER BY 3 DESC LIMIT 5"'
```

**The OCL subscription does not work with the bundled `openconceptlab` 1.2.9.** Its HTTP client
(commons-httpclient 3.1, from 2007) cannot get its credentials onto the wire against current OCL:
the import fails after three seconds with `HTTP/1.1 403 Forbidden`, and OCL's body says
`"Authentication required. Anonymous API access is disabled."` - it saw the request as anonymous.
The token is not the problem; the same token and URL return `HTTP 200` from `curl` inside the
same container.

Use the **offline load** instead, which needs no authentication from the module at all:

```bash
# 1. Fetch the export with your own OCL token. The API redirects to a pre-signed S3 URL, and
#    the Authorization header must NOT be forwarded to S3 or it returns 403.
LOC=$(curl -s -i -H "Authorization: Token $OCL_TOKEN"   "https://api.openconceptlab.org/orgs/CIEL/sources/CIEL/v2026-08-24/export/"   | grep -i "^location:" | sed "s/^[Ll]ocation: //" | tr -d "
")
curl -o ciel.zip "$LOC"

# 2. Drop it in the module's load-at-startup folder and restart.
docker cp ciel.zip openmrs-app:/usr/local/tomcat/.OpenMRS/ocl/configuration/loadAtStartup/
docker restart openmrs-app
```

Set `openconceptlab.validationType` to `NONE` for a bulk load first. `FULL` re-validates all
50,000 concepts against OpenMRS rules and fails on forward references - concept A citing concept
B not yet imported. CIEL is already validated upstream.

Expect the import to take tens of minutes, and expect the startup that triggers it to be slow.

**Consequence worth planning for:** future CIEL updates need this same manual refresh, unless the
`openconceptlab` module is upgraded to a version that can authenticate.

### 15.9 Server configuration this depends on

Two things live outside the module and will silently reduce what it exports if they are missing.
The coverage report (§15.10) tells you about both.

- **`concept.true` / `concept.false`** must point at concepts, or every coded boolean is skipped.
  They were unset on this installation, which meant *no* coded boolean anywhere in OpenMRS could
  record a positive value. Set to `1065` / `1066` (Yes / No) per CIEL convention rather than
  `1` / `2` (True / False).
- **The `Neurosurgery Fiche` encounter type** is created by the module's activator on startup,
  looked up by a fixed uuid so a restart never creates a second one. If it is missing, nothing
  can be exported, because there is nothing to hang an encounter on.

### 15.10 Operating it

Records export automatically as they are saved. These endpoints cover what automation does not -
backfilling rows created before the export existed, and re-running after curation:

```
GET  /module/patientview/fhirProjection.form                  # coverage report
POST /module/patientview/fhirProjection.form?patientId=123    # one patient
POST /module/patientview/fhirProjection.form?allPatients=true # everything
```

The **coverage report** is the thing to trust over any number in this file, because it measures
your server rather than describing this one. It distinguishes "no concept declared" from
"declared but not resolvable here" - the difference between work still to do and a dictionary
that lacks a code you already chose.

There is also an **Export FHIR** button on the Résumé tab, which runs the per-patient export.

To confirm data is really reaching FHIR, read it back:

```bash
curl -u admin:PASSWORD "http://localhost:8080/openmrs/ws/fhir2/R4/Observation?patient=PATIENT_UUID"
curl -u admin:PASSWORD "http://localhost:8080/openmrs/ws/fhir2/R4/Condition?patient=PATIENT_UUID"
```

### 15.11 The guards against drift

A string-keyed manifest drifts silently, so `FhirMappingManifestTest` (in `api`, no OpenMRS
context needed) checks it against the code in both directions:

- every declared field is a key its named DAO getter really produces, **and** every key that
  getter produces is either mapped or explicitly excluded with a reason - the second direction is
  what stops a newly added clinical field from never being exported;
- every set has a `case` in `ObsProjector.fetchRows`, or it would read no rows at all;
- every getter exposes the `uuid` the ledger keys on;
- concepts are declared with a mapping source, never a bare local id;
- the field count (120) is asserted exactly, since it can only change with a schema change, while
  the curation counts are **floors** (≥ 44 curated, ≥ 14 sets exporting). Curation is expected
  routine work, so an exact assertion would fail the build on every batch and train people to
  edit the number without reading it. A floor still catches concepts disappearing from the
  manifest; `ciel_match.py` prints the new figure to raise it to.

`ObsProjectorConditionFlagTest` covers the projector's own logic over a static-mocked `Context`:
a true flag becomes a coded Condition, a false one exports nothing at all, an unresolvable
concept is reported rather than thrown, an unchanged fingerprint leaves a row alone, and a
changed one supersedes and re-exports it.

`QuickDeploymentTest` boots a real Spring/Hibernate context, so the mapping file, the projector
bean and its transaction proxy are exercised rather than assumed.
