# Sprint 12 — Reproduction & Standards Export

Status: **IN PROGRESS — Phase 1 VERIFIED**  
Branch: `s12-reproduction-standards-export`  
Immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`

## Purpose

FR-013 requires reproduction packages to support JSON, Burp Issue and SARIF export targets. Sprint 12 closes that
remaining standards/export gap without turning review candidates into confirmed vulnerabilities.

## Phase 1 — deterministic review-only reproduction package

Implemented:

- `S12ReproductionPackage` and deterministic factory;
- canonical JSON export with SHA-256;
- SARIF 2.1.0 export with SHA-256;
- SARIF CANDIDATE/INCONCLUSIVE → `kind=review`;
- SARIF REJECTED → `kind=informational`;
- all non-fail SARIF results → `level=none`;
- `S12BurpIssueProjection` / projector;
- Burp projection severity INFORMATION, confidence TENTATIVE, `publishable=false`;
- raw principal identifiers, candidate rationale and secret-bearing text excluded;
- review candidates without supporting evidence fail closed.

### Phase 1 verification

GitHub Actions run `36068186039`: **SUCCESS** at
`a8d442aa95c5d8946035586cf2b212e27f9ebf79`.

- Sprint 12 foundation suite: PASS, 34 assertions;
- retained Sprint 11 foundation: PASS;
- Maven core test compilation: PASS.

## Current claim boundary

Phase 1 does not create or publish a real Burp Scanner issue, does not validate a real Burp desktop runtime, and
does not convert a FindingCandidate into a confirmed vulnerability.

## Next dependency

Phase 2 must implement Montoya-specific conversion only in the extension module. Publication must require an
explicit approval object that is separate from FindingCandidate and reproduction-package state.
