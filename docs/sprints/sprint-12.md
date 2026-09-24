# Sprint 12 — Reproduction & Standards Export

Status: **IN PROGRESS — Phases 1–2 VERIFIED**  
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

## Phase 2 — explicit-approval Montoya AuditIssue adapter

Implemented:

- `S12BurpIssuePublicationApproval`;
- `S12MontoyaAuditIssueSpec`;
- `S12MontoyaAuditIssueAdapter`;
- direct compile-time use of Montoya 2026.7 `AuditIssue`, `AuditIssueSeverity` and `AuditIssueConfidence`;
- INFORMATION severity and TENTATIVE confidence for review-only imports;
- explicit approved boolean;
- candidate-ID binding between approval and projection;
- absolute HTTP(S) base URL validation;
- default issue factory method using `AuditIssue.auditIssue(...)`;
- no `SiteMap.add` or other publication method.

### Phase 2 verification

GitHub Actions run `36068996138`: **SUCCESS** at source commit
`e57c8dbb0e62b4f1acc59210ff04ee8e8333b85a`.

Verified:

- `Sprint12MontoyaIssueAdapterTestSuite`: PASS, 13 assertions;
- Sprint 12 Phase 1 standards suite: PASS, 34 assertions;
- complete retained Sprint 11 foundation: PASS;
- Maven extension/core test compilation: PASS.

Phase 2 is **VERIFIED COMPLETE**.

## Next dependency

Phase 3 may add an explicit publication service around Montoya `SiteMap.add(AuditIssue)`, but that service must:

1. require the Phase 2 approval object;
2. remain entirely unregistered from automatic scanning/bootstrap flows;
3. preserve INFORMATION/TENTATIVE review semantics;
4. produce a publication receipt without changing FindingCandidate state;
5. be headless-tested with an injected issue factory/sink while real Burp desktop publication remains separately unverified.
