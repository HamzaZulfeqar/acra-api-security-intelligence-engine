# Sprint 12 — Reproduction & Interchange Exports

Status: **IN PROGRESS — Phase 1 VERIFIED**  
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

## Next dependency

Phase 2 must implement a deterministic core `BurpIssueDraft` and a non-submitting extension projection boundary.
Real issue submission remains disabled/unverified until an explicit Burp runtime exercise.
