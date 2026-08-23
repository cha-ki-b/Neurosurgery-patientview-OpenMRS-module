# Handoff: completing the Patientview module

You're picking up an OpenMRS module (`patientview`, currently v1.2.2) that replaces the
default patient dashboard with a neurosurgery-specific one, built against the "Fiche de
Neurochirurgie" paper form. 5 of 10 tabs are done. **Read `README.md` first** — it documents
the architecture, the security model, and every non-obvious gotcha this module has already
hit in production. This file only covers what's left and how to add it safely.

## Non-negotiables (every one of these has caused a real bug or a real outage before)

1. **Append-only persistence.** Every entity is insert-only, never updated/deleted in place —
   a correction is a new row (non-repudiation). "Current" = most recent row per patient.
2. **Every REST controller** must open its GET method with `PatientviewPrivileges.requireView();`
   and its POST method with `PatientviewPrivileges.requireManage();` — do **not** rely on
   `@Authorized` alone (the Reference Application auto-grants those API-level privileges to
   every role; see README §4). `ModuleWiringTest.everyRestControllerMustEnforceAPrivilegeCheck()`
   will fail the build if you forget this.
3. **Every page controller** must check `Context.hasPrivilege(PatientviewPrivileges.APP_VIEW_DASHBOARD)`
   and set `accessDenied`/`canManage` model attributes exactly like the 5 existing
   `*PageController.java` files do. Every `.gsp` must gate its "+ Ajouter" buttons/forms
   behind `canManage` and show the standard access-denied panel when `accessDenied`.
4. **New privileges only ever go under the `App:` prefix** if they're meant to actually
   restrict anything (e.g. `App: patientview.neurosurgeryDashboard.someNewThing`). Plain,
   unprefixed privileges are effectively "everyone has this" on a Reference Application
   install — don't add another one expecting it to gate access.
5. **CSS classes are `neuro-`-prefixed, no exceptions** (`neuro-panel`, `neuro-btn`, etc.) —
   generic names collide with Bootstrap, which the Reference App loads globally. This broke
   the UI once already.
6. **Never interpolate a value in a `.gsp` without a guard that also treats the literal
   string `"null"` as missing**, not just true null:
   `(x && x.toString().trim().toLowerCase() != 'null') ? ui.format(x) : ''`. A plain `x ? ... : ...`
   is not enough.
7. **Page templates never use `config`** (fragment-only). `patient` is the raw
   `org.openmrs.Patient` object (no `.patient` nesting, no `.primaryIdentifiers` — use
   `.activeIdentifiers`). `patient.uuid` for `ui.pageLink(...)` navigation between pages;
   `patient.patientId` (integer) for the `patientId` JS variable used in REST/AJAX calls —
   these are not interchangeable, the REST controllers expect an integer.
8. **Liquibase changesets are immutable once shipped.** If a changeset has ever run
   successfully anywhere, never edit its content again — Liquibase permanently marks that ID
   "done" the moment any checksum for it is recorded, and editing it only produces a
   validation error for anyone who already ran it. Always add a new changeset instead. (Full
   war story with a MySQL FK/unique-index interaction in README §3, footnote 1 — read it
   before touching `MedicalHistory`-style migrations.)
9. **No Maven/OpenMRS jars are available in a typical sandboxed agent environment.** You
   likely can't run `mvn compile`. Compensate with static verification before calling
   anything done: every `.xml`/`.json` file parses; every `.gsp`'s `<% %>` scriptlets and
   `${ }` expressions are brace-balanced (a naive check must track `<% %>` and HTML text
   _separately_ — an `if`/`each` block's braces legitimately span multiple scriptlets with
   HTML in between); every DAO/Service interface method has a matching impl; every GSP field
   reference matches an actual key the DAO map-builder puts there. Extend `ModuleWiringTest`
   for any new packaging convention you introduce, the same way existing checks were added.

## The pattern for every new category (repeat this exactly, per tab)

Look at `NeurosurgicalDiagnosis`/`Pathology` (api) and `diagnostic.gsp`/`anatomopathologie.gsp`
(omod) as the reference implementation — they're the simplest complete example of the full
stack. For each new category:

1. **Model** (`api/.../model/Xxx.java`) — plain getters/setters, `id`/`uuid`/`patient`/
   `creator`/`dateCreated` at minimum.
2. **Hibernate mapping** (`api/.../model/Xxx.hbm.xml`) — register it in `config.xml`'s
   `<mappingFiles>`.
3. **Liquibase changeset** (append to `liquibase.xml`, new dated id, never touch old ones) —
   `createTable` + FK to `patient`/`users` + `createIndex` on `patient_id`.
4. **DAO** (`PatientviewDao` interface + `PatientviewDAOImpl`) — `getXxx(patient)` /
   `saveXxx(patient, data)`, ordered `desc` by date, mapped to/from `Map<String,Object>`.
5. **Service** (`PatientviewService` interface + impl) — delegate to DAO, add
   `@Authorized({PatientviewPrivileges.VIEW_NEURO_DATA})` /
   `@Authorized({PatientviewPrivileges.MANAGE_NEURO_DATA})` on the interface methods (belt
   and suspenders per non-negotiable #2), null-guard the patient/data args.
6. **REST controller** (`web/controller/XxxRestController.java`) — GET + POST `.form`
   endpoints, privilege checks per non-negotiable #2.
7. **Page controller** (`page/controller/clinicianfacing/XxxPageController.java`) —
   access-denied/canManage pattern per non-negotiable #3.
8. **GSP page** (`webapp/pages/clinicianfacing/xxx.gsp`) — form + record list, following the
   exact markup/CSS-class conventions of `diagnostic.gsp`.
9. **Sidebar** (`fragments/sidebarNav.gsp`) — turn that tab's `<span class="neuro-nav-item
neuro-nav-disabled">` placeholder into a real `<a>` link.
10. **JS** (`resources/scripts/patientview-crud.js`) — one `submitXxx(event)` function using
    the existing `postPatientviewForm` helper.
11. **Tests** — extend `ModuleWiringTest` (mapping file registered + exists on disk, page
    scoping rules) and add Mockito delegation/null-guard tests to
    `PatientviewServiceImplConnectionTest`, matching the existing ones. Watch out for
    `MockitoJUnitRunner`'s strict-stubs mode: don't stub a DAO call your test's code path
    won't actually reach, or the build fails with `UnnecessaryStubbingException`.
12. **Bump the version** (root/api/omod `pom.xml` + `config.xml`, all four, same value) and
    update `README.md`'s feature table and version history.

## Remaining tabs, in suggested order (all fields per the Fiche de Neurochirurgie)

### 1. Prise en charge — Fiche §9

Two related record types, or one combined entity — your call:

- **Traitement médical**: corticothérapie, antiépileptiques, antibiothérapie,
  anticoagulants, antalgiques, autres traitements (free text or booleans, match the
  `MedicalHistory` checklist style).
- **Traitement chirurgical**: intervention réalisée, type d'exérèse (Totale/Partielle/
  Biopsie — select), date opératoire, durée, chirurgien, complications peropératoires, drain
  posé (bool), sonde posée (bool). Consider linking to `SurgicalHistory` by date/procedure
  rather than duplicating it outright — read that entity first.

### 2. Anatomopathologie ↔ Diagnostic cross-check

Already done — skip. (Listed here only so you don't recreate it.)

### 3. Évolution postopératoire & Séquelles — Fiche §11-12

One tab, two logical sections:

- **Évolution**: Glasgow postopératoire, Karnofsky postopératoire, état neurologique,
  infection (bool), hémorragie (bool), hydrocéphalie (bool), fuite de LCR (bool), convulsions
  (bool), décès (bool + date).
- **Séquelles**: déficit moteur, déficit sensitif, aphasie (bool), troubles cognitifs (bool),
  épilepsie secondaire (bool), handicap résiduel (free text).

### 4. Biologie — Fiche §7

NFS, CRP, ionogramme, glycémie, créatinine, bilan de coagulation, groupe sanguin — all
free-text/decimal result fields with a date, one row per lab draw. Straightforward, smallest
remaining tab.

### 5. Sortie & Suivi — Fiche §13-14

- **Sortie**: date de sortie, mode de sortie (Guérison/Amélioration/Stable/Aggravation/Décès
  — select), one row per discharge episode.
- **Suivi**: date de consultation, examen clinique, examen neurologique, IRM/scanner de
  contrôle, récidive (bool), traitement en cours, prochaine consultation (date) — repeating,
  one row per follow-up visit.

### 6. Imagerie — Fiche §6, Orthanc-backed (do this last, it's the most involved)

Different shape from everything else — read-heavy, external system, no local write form for
the imaging data itself:

- Local fields (small, module-owned, same pattern as above): type d'examen (TDM/IRM/
  Angio-TDM/Angio-IRM), date, résultat/compte-rendu — these are clinical _notes about_ an
  imaging study, not the images.
- Orthanc integration (the real work): call Orthanc's REST API (`/patients`, `/studies`) to
  list studies whose DICOM PatientID `(0010,0020)` matches this patient's OpenMRS identifier,
  show a thumbnail/study list, link out to Orthanc's viewer. Store Orthanc's base URL +
  credentials as OpenMRS **global properties** (`config.xml`'s `<globalProperty>`, not
  hardcoded) — read `deployment/DEPLOYMENT_SECURITY.md` first, since Orthanc's own auth and
  the reverse-proxy setup are already defined there and this tab needs to match them, not
  reinvent them. Orthanc stays the source of truth for images — don't cache/duplicate DICOM
  metadata locally beyond what's needed to render the list.
- This is the one place where you'll need an actual HTTP client from Java (Orthanc's REST
  API), not just Hibernate — check what's already on the classpath before adding a new
  dependency.

## When you're done with a tab

Re-verify with the same discipline used throughout this module: XML/JSON well-formed,
brace-balanced Java and GSP, DAO/Service method parity, every GSP field reference matches a
real map key, `ModuleWiringTest` extended and (conceptually) passing, version bumped,
README updated. Package as a zip excluding `target/` directories and hand it back with a
short changelog, the same style as this module's existing version history.
