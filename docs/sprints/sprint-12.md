# Sprint 12 — Reproduction & Interoperability Exports

Status: **IN PROGRESS — Phase 1 VERIFIED**  
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

## Next dependency

Phase 2 owns deterministic SARIF 2.1.0 projection from the canonical reproduction package. It must preserve
review-only candidate semantics, severity/confidence separation, secret-safe provenance and stable rule/result
identity. Actual Burp Issue creation remains a later adapter-layer phase.
