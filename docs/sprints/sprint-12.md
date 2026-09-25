# Sprint 12 — Reproduction & Interoperability Exports

Status: **IN PROGRESS — Phases 1–2 VERIFIED**  
Branch: `s12-reproduction-interoperability-exports`  
Immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`

## Purpose

Sprint 12 closes the long-standing FR-013 interoperability gap. Existing sprints already produce deterministic
JSON/Markdown reports, but the requirements register explicitly calls for reproduction packages supporting JSON,
Burp Issue and SARIF targets.

The sprint preserves one central rule: **export format must not change finding truth**. A `FindingCandidate`
remains CANDIDATE / REJECTED / INCONCLUSIVE regardless of where it is exported.

## Phase 1 — reproduction package + canonical JSON

Implemented:

- `ReproductionPackage`;
- `ReproductionPackageBuilder`;
- `ReproductionExportArtifact`;
- `ReproductionPackageExporter`;
- `ReproductionJsonReporter`;
- deterministic package ID and fingerprint;
- deterministic canonical JSON and SHA-256;
- candidate state, severity and confidence retained independently;
- `reviewOnly=true` enforced for every package;
- only CANDIDATE packages are `issueEligible`;
- raw principal replaced by SHA-256 fingerprint;
- candidate rationale and contradictory narrative structurally omitted;
- evidence, assessment and policy references retained;
- report/candidate project, state, dimension, assessment and evidence consistency validated fail-closed.

### Phase 1 verification

GitHub Actions run `36122252879`: **SUCCESS** at source commit
`46a43350d5357f78f0756b6ce20329ef750e0cfb`.

- `Sprint12ReproductionPackageFoundationTestSuite`: PASS, 30 assertions;
- Sprint 11 research report/export regression: PASS, 31 assertions;
- Sprint 10 batch/indirect report/export regression: PASS, 43 assertions;
- Sprint 9 property report/export regression: PASS, 28 assertions;
- Sprint 8 routing report/export regression: PASS, 19 assertions;
- Sprint 7 workflow report/export regression: PASS, 26 assertions;
- retained Sprint 6 authorization reporting/export: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

## Current boundary

Not yet implemented:

- SARIF;
- Burp Issue model/adapter;
- live Burp issue registration;
- real Burp desktop runtime validation.

## Phase 2 — SARIF 2.1.0 interoperability

Implemented:

- public `CanonicalJsonDocumentWriter` wrapper for deterministic standards documents;
- `ReproductionSarifExporter`;
- `ReproductionSarifReporter`;
- SARIF `version=2.1.0`;
- official OASIS 2.1.0 Errata 01 schema URI;
- ACRA driver + deterministic authorization rule descriptor;
- deterministic result fingerprints;
- candidate-state mapping:
  - CANDIDATE → `kind=review`, `level=none`;
  - REJECTED → `kind=pass`, `level=none`;
  - INCONCLUSIVE → `kind=open`, `level=none`;
- ACRA severity and confidence preserved as separate result properties;
- review-only and issue-eligibility properties preserved;
- endpoint/resource/evidence/assessment/policy lineage retained in minimized properties;
- no fabricated physical/source-code locations for API endpoints;
- secret-safe canonical SARIF + SHA-256;
- CI artifact upload.

### Phase 2 verification

GitHub Actions run `36122759287`: **SUCCESS** at source commit
`3e5c63962c007865a85dfdd23edac2165f3075d7`.

Verified:

- `Sprint12SarifExportTestSuite`: PASS, 30 assertions;
- independent Python SARIF structure parser: PASS;
- SARIF version/schema/run/tool/result/rule structure: PASS;
- CANDIDATE review semantics: PASS;
- level-none requirement for non-fail kinds: PASS;
- raw principal / bearer / rationale exclusion: PASS;
- Maven core `test-compile`: PASS;
- SARIF artifact archived by CI: PASS.

Phase 2 is **VERIFIED COMPLETE**.

## Next dependency

Phase 3 owns the Burp Issue adapter. Montoya-specific types must remain under
`extension/burp-extension`; the adapter must require an issue-eligible CANDIDATE reproduction package plus a real
`HttpRequestResponse` with a usable URL. REJECTED/INCONCLUSIVE packages must fail closed and no Burp issue may be
registered from core alone.
