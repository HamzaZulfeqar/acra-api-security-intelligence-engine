# Sprint 12 — Reproduction & Interoperability Exports

Status: **IN PROGRESS — Phases 1–3 VERIFIED**  
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

## Phase 3 — Burp AuditIssue adapter

Implemented:

- `ReproductionAuditIssueAdapter` in the Montoya extension module only;
- extension-owned `AuditIssue` implementation, avoiding a core Burp dependency;
- required real `HttpRequestResponse` evidence;
- URL derived from `request().url()`;
- issue text HTML escaping;
- explicit review-only / human-validation wording;
- conservative severity mapping:
  - ACRA CRITICAL/HIGH → Burp HIGH;
  - MEDIUM → MEDIUM;
  - LOW → LOW;
  - INFO → INFORMATION;
- conservative confidence mapping:
  - ACRA HIGH → Burp FIRM;
  - MEDIUM/LOW/INSUFFICIENT → TENTATIVE;
  - never CERTAIN for review-only candidates;
- original request/response evidence retained;
- no fabricated Collaborator interactions;
- fail-closed REJECTED/INCONCLUSIVE/null evidence/no response/blank URL behavior;
- expanded local Montoya stubs so retained Sprint 2/3 offline compilation covers the new Scanner issue surface.

### Phase 3 verification

GitHub Actions run `36123760660`: **SUCCESS** at source commit
`be10c1c406bb475e12446ad9cff23056c3cc352a`.

Verified:

- `Sprint12BurpIssueAdapterTestSuite`: PASS, 32 assertions;
- Phase 1 reproduction package: PASS, 30 assertions;
- Phase 2 SARIF: PASS, 30 assertions;
- independent SARIF structure validation: PASS;
- Maven core + extension test compilation: PASS;
- Sprint 2 retained CI: PASS after stub compatibility update;
- Sprint 3 retained CI: PASS after stub compatibility update.

Phase 3 is **VERIFIED COMPLETE**.

This verifies construction of a standards-aligned Burp `AuditIssue` object. It does not establish that a real Burp
desktop instance has accepted/displayed the issue.

## Next dependency

Phase 4 owns a controlled Site Map publisher contract using Montoya `SiteMap.add(AuditIssue)` with deterministic
duplicate suppression based on the reproduction fingerprint. Publishing must remain explicit and must not occur
implicitly during passive observation. Real desktop runtime validation remains a separate external gate.
