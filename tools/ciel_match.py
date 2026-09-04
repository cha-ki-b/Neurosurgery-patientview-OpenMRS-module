#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Turn the FHIR mapping manifest's curation backlog into a review exercise.

patientview-fhir-mapping.json ships with most fields carrying no concept: the projector
skips those, so they never leave the module. Filling them in is dictionary work, and doing
it by hand means searching CIEL once per field. This does the searching.

It deliberately needs no Python database driver. Step 1 prints SQL you run against your own
OpenMRS database; step 2 reads the result back and writes the approved codes into the
manifest. That works whether the dictionary is in a running container or a restored dump.

    # 0. Is CIEL even loaded? Anything under a few thousand is the demo subset, not full CIEL.
    docker exec -i openmrs-mysql mysql -uopenmrs -pPASSWORD openmrs -N -e "
      SELECT crs.name, COUNT(*) FROM concept_reference_map crm
      JOIN concept_reference_term crt USING (concept_reference_term_id)
      JOIN concept_reference_source crs USING (concept_source_id)
      GROUP BY crs.name ORDER BY 2 DESC"

    # 1. Generate the candidate search and run it, saving the tab-separated result.
    python tools/ciel_match.py sql > /tmp/ciel_candidates.sql
    docker exec -i openmrs-mysql mysql -uopenmrs -pPASSWORD openmrs \
        < /tmp/ciel_candidates.sql > candidates.tsv

    # 2. Open candidates.tsv, KEEP AT MOST ONE ROW PER FIELD, delete the rest. A clinician
    #    should do this pass - a name match is a suggestion, not a decision.

    # 3. Write the approved codes into the manifest, then rebuild.
    python tools/ciel_match.py apply candidates.tsv

Nothing here runs at module runtime; it is a curation aid that lives outside the build.
"""
from __future__ import print_function

import io
import json
import os
import sys
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
MANIFEST = os.path.join(HERE, os.pardir, "api", "src", "main", "resources",
                        "patientview-fhir-mapping.json")

# What concept datatype each manifest field type can actually be written to. Mirrors
# ObsProjector.buildObs - a candidate outside this set would be reported as a datatype
# mismatch at projection time, so it is worth flagging during review instead.
COMPATIBLE = {
    "numeric": {"Numeric"},
    "text": {"Text"},
    "boolean": {"Boolean", "Coded"},
    "date": {"Date", "Datetime"},
}

COLUMNS = ["set_id", "field_id", "label", "field_type", "match", "ciel_code",
           "concept_name", "datatype"]


def load_manifest():
    with io.open(MANIFEST, encoding="utf-8") as handle:
        return json.load(handle, object_pairs_hook=OrderedDict)


def uncurated_fields(manifest):
    """(set_id, field_id, label, type) for every field still lacking a concept."""
    for entry in manifest["sets"]:
        for field in entry["fields"]:
            if field["type"] == "condition":
                continue  # Condition takes free text; it needs no concept at all.
            if not field["concept"]:
                yield entry["id"], field["id"], field["name"], field["type"]


def sql_escape(value):
    return value.replace("\\", "\\\\").replace("'", "''")


def emit_sql(manifest):
    fields = list(uncurated_fields(manifest))
    if not fields:
        print("-- Nothing to curate: every field already carries a concept.")
        return
    print("-- Candidate CIEL concepts for the %d uncurated patientview fields." % len(fields))
    print("-- Matches on ANY non-voided concept name (synonyms included, which is where a")
    print("-- clinician's wording usually lands), but reports the fully specified name so the")
    print("-- reviewer sees what the concept really is.")
    print("-- Review before applying: a name match is a suggestion, not a decision.")
    print("SELECT %s;" % ", ".join("'%s'" % c for c in COLUMNS))
    branches = []
    for set_id, field_id, label, field_type in fields:
        needle = sql_escape(label)
        branches.append("""(
SELECT '{set_id}', '{field_id}', '{label}', '{field_type}',
       CASE WHEN LOWER(m.name) = LOWER('{needle}') THEN 'exact' ELSE 'partial' END,
       crt.code, fsn.name, cdt.name
FROM concept c
JOIN concept_name m ON m.concept_id = c.concept_id AND m.voided = 0
JOIN concept_name fsn ON fsn.concept_id = c.concept_id AND fsn.voided = 0
     AND fsn.concept_name_type = 'FULLY_SPECIFIED'
JOIN concept_datatype cdt ON cdt.concept_datatype_id = c.datatype_id
JOIN concept_reference_map crm ON crm.concept_id = c.concept_id
JOIN concept_reference_term crt ON crt.concept_reference_term_id = crm.concept_reference_term_id
JOIN concept_reference_source crs ON crs.concept_source_id = crt.concept_source_id
WHERE c.retired = 0 AND crs.name = 'CIEL'
  AND (LOWER(m.name) = LOWER('{needle}') OR LOWER(m.name) LIKE LOWER('%{needle}%'))
GROUP BY crt.code, fsn.name, cdt.name, m.name
ORDER BY 5, CHAR_LENGTH(fsn.name)
LIMIT 4
)""".format(set_id=sql_escape(set_id), field_id=sql_escape(field_id),
            label=needle, field_type=field_type, needle=needle))
    print("\nUNION ALL\n".join(branches) + ";")


def read_tsv(path):
    rows = []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.rstrip("\r\n")
            if not line:
                continue
            cells = line.split("\t")
            if cells[0] in ("set_id", "'set_id'"):
                continue  # header emitted by the SELECT, or by mysql itself
            if len(cells) != len(COLUMNS):
                print("  ! skipping malformed line (%d columns, expected %d): %s"
                      % (len(cells), len(COLUMNS), line[:70]))
                continue
            rows.append(dict(zip(COLUMNS, cells)))
    return rows


def apply_rows(manifest, rows):
    chosen = {}
    duplicates = []
    for row in rows:
        key = (row["set_id"], row["field_id"])
        if key in chosen:
            duplicates.append("%s.%s" % key)
            continue
        chosen[key] = row
    if duplicates:
        print("REFUSING TO APPLY: more than one candidate is still present for:")
        for name in sorted(set(duplicates)):
            print("  - " + name)
        print("\nKeep at most one row per field in the TSV, then run this again.")
        return None

    applied = 0
    warnings = []
    for entry in manifest["sets"]:
        for field in entry["fields"]:
            row = chosen.get((entry["id"], field["id"]))
            if row is None:
                continue
            if field["concept"]:
                warnings.append("%s.%s already had a concept; left as it was"
                                % (entry["id"], field["id"]))
                continue
            allowed = COMPATIBLE.get(field["type"], set())
            if row["datatype"] not in allowed:
                warnings.append(
                    "%s.%s is %s but CIEL:%s is %s - the projector will report this as a "
                    "datatype mismatch and export nothing"
                    % (entry["id"], field["id"], field["type"], row["ciel_code"],
                       row["datatype"]))
            field["concept"] = [OrderedDict([("source", "CIEL"), ("code", row["ciel_code"])])]
            applied += 1
    return applied, warnings


def write_manifest(manifest):
    text = json.dumps(manifest, indent=2, ensure_ascii=False)
    with io.open(MANIFEST, "w", encoding="utf-8", newline="") as handle:
        handle.write(text.replace("\n", "\r\n") + "\r\n")


def counts(manifest):
    total = curated = 0
    exporting = 0
    for entry in manifest["sets"]:
        entry_exports = False
        for field in entry["fields"]:
            if field["type"] == "condition":
                entry_exports = True
                continue
            total += 1
            if field["concept"]:
                curated += 1
                entry_exports = True
        if entry_exports:
            exporting += 1
    return total, curated, exporting


def main(argv):
    if len(argv) < 2 or argv[1] not in ("sql", "apply"):
        print(__doc__)
        return 2
    manifest = load_manifest()

    if argv[1] == "sql":
        emit_sql(manifest)
        return 0

    if len(argv) < 3:
        print("usage: ciel_match.py apply <candidates.tsv>")
        return 2
    rows = read_tsv(argv[2])
    if not rows:
        print("No candidate rows found in %s" % argv[2])
        return 1
    before = counts(manifest)
    result = apply_rows(manifest, rows)
    if result is None:
        return 1
    applied, warnings = result
    write_manifest(manifest)
    after = counts(manifest)

    print("Applied %d concept(s)." % applied)
    for warning in warnings:
        print("  ! " + warning)
    print("\nCuration: %d of %d fields carry a concept (was %d); %d of 16 sets now export."
          % (after[1], after[0], before[1], after[2]))
    print("\nNext: rebuild (mvn -o clean install), then re-run the backfill so existing rows")
    print("pick up the new concepts:")
    print("  POST /module/patientview/fhirProjection.form?allPatients=true")
    if after[1] > before[1]:
        print("\nFhirMappingManifestTest asserts curation never regresses below a floor. Raise")
        print("that floor to %d to lock in this progress." % after[1])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
