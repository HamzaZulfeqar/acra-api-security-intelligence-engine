# Sprint 12 — Reproduction & Interchange Exports

Status: **IN PROGRESS — Phases 1–2 VERIFIED**  
Branch: `s12-reproduction-exports`  
Immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`

## Purpose

Close the longstanding FR-013 reproduction/export gap without weakening ACRA's review-only finding boundary.

Sprint 12 owns deterministic, secret-safe interchange artifacts for:

1. JSON;
2. SARIF;
3. Burp Issue projection.

Real Burp desktop issue submission is a separate runtime-validation lane.

## Phase 1 — reproduction package + JSON/SARIF

Implemented:

- `ReproductionPackage`;
- `ReproductionPackageFactory`;
- `ReproductionExportArtifact`;
- `ReproductionPackageExporter`;
- deterministic package identity;
- canonical secret-safe JSON export;
- deterministic SARIF 2.1.0 export;
- stable SARIF rule `ACRA-AUTHORIZATION-REVIEW`;
- explicit `confirmed=false`;
- candidate → warning; rejected/inconclusive → note;
- structural exclusion of principal, tenant and rationale fields.

### Verification

GitHub Actions run `36128103735`: **SUCCESS** at source commit
`b829c06cfe4c440984305192bd817eb44f5403d3`.

- `Sprint12ReproductionExportFoundationTestSuite`: PASS, 31 assertions;
- retained Sprint 11 research report export: PASS, 31 assertions;
- retained Sprint 10 reporting export: PASS, 43 assertions;
- retained Sprint 9 property reporting export: PASS, 28 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

## Claim boundary

Phase 1 does not claim:

- a confirmed vulnerability;
- real Burp Issue creation;
- real Burp desktop runtime validation;
- external target reproduction;
- network dispatch.

FR-013 is currently **PARTIAL**: JSON + SARIF verified; Burp Issue projection remains.

## Phase 2 — Burp Issue draft and Montoya projection boundary

Implemented:

- `BurpIssueDraftSeverity`;
- `BurpIssueDraftConfidence`;
- `BurpIssueSubmissionState`;
- `BurpIssueDraft`;
- `BurpIssueDraftFactory`;
- `BurpIssueDraftAdapter` in the Burp extension;
- compile contract against Montoya API 2026.7 `AuditIssue.auditIssue(...)`;
- informational/tentative review semantics;
- explicit non-confirmation and non-submission state;
- HTML encoding for dynamic issue detail fields;
- minimal Montoya audit-issue stubs for retained offline Sprint 2/3 verification.

### Phase 2 verification

GitHub Actions run `36129019615`: **SUCCESS** at source commit
`7437b7953ddf0f9140b776e8f92be3030702ad78`.

Verified:

- `Sprint12ReproductionExportFoundationTestSuite`: PASS, 31 assertions;
- `Sprint12BurpIssueDraftTestSuite`: PASS, 20 assertions;
- retained S11 report export: PASS, 31 assertions;
- retained S10 report export: PASS, 43 assertions;
- retained S9 report export: PASS, 28 assertions;
- Maven core + extension `test-compile` against Montoya 2026.7: PASS;
- retained Sprint 2 local gate: run `36129243039` — SUCCESS;
- retained Sprint 3 local gate: run `36129243006` — SUCCESS.

Phase 2 is **VERIFIED COMPLETE**.

### Runtime boundary

The adapter creates an `AuditIssue` projection but does not receive `MontoyaApi` and cannot call
`SiteMap.add(AuditIssue)`. Real Burp issue registration is therefore still unexecuted.

## Next dependency

Phase 3 will execute the projection factory against the real Montoya API dependency in CI and verify the resulting
`AuditIssue` fields (URL, severity, confidence, detail and evidence count) without adding the issue to a live Burp
site map. This completes the software export target while keeping desktop submission as a separate runtime gate.
