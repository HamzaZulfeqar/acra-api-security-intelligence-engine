# Sprint 12 — Reproduction & Standards Export

Status: **IN PROGRESS — Phases 1–4 VERIFIED**  
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

## Phase 3 — explicit issue-publication boundary

Implemented:

- `S12MontoyaAuditIssueFactory`;
- `S12DefaultMontoyaAuditIssueFactory`;
- `S12AuditIssueSink`;
- `S12MontoyaSiteMapAuditIssueSink`;
- `S12BurpIssuePublisher`;
- deterministic `S12BurpIssuePublicationReceipt`.

Publication flow:

```text
non-publishable core projection
        +
candidate-bound approved request
        |
        v
Montoya issue spec
        |
        v
injected AuditIssue factory
        |
        v
injected issue sink
        |
        v
IMPORTED_REVIEW_CANDIDATE receipt
```

The production sink wraps Montoya `SiteMap.add(AuditIssue)`, but it is not registered in `ACRAExtension`.
Headless tests inject a fake issue factory and fake sink, so the software gate does not masquerade as real Burp
desktop validation.

### Phase 3 verification

GitHub Actions run `36069442716`: **SUCCESS** at source commit
`45554b8c912f9f5fb23b39267606c3fb5dd110a6`.

Verified:

- Phase 1 standards suite: PASS, 34 assertions;
- Phase 2 Montoya issue adapter: PASS, 13 assertions;
- Phase 3 publication boundary: PASS, 19 assertions;
- denied approval → 0 factory calls / 0 sink calls;
- mismatched approval → 0 factory calls / 0 sink calls;
- approved publication → exactly 1 factory call / 1 sink call;
- deterministic review-publication receipt: PASS;
- candidate/projection state remains review-only after publication;
- `ACRAExtension` publisher wiring absent: PASS;
- complete retained Sprint 11 verifier: PASS;
- Maven extension/core compilation: PASS.

Phase 3 is **VERIFIED COMPLETE**.

## Phase 4 — reproduction product workspace and read-only UI

Implemented:

- `S12ReproductionProductEntry`;
- immutable `S12ReproductionProductSnapshot`;
- synchronized `S12ReproductionWorkspace`;
- `S12ReproductionPanel`;
- top-level `Reproduction` tab in `AcraSuiteTab`;
- Packages / JSON Export / SARIF Export / Burp Review / Publication Receipts surfaces;
- extension-only receipt display separated from core package state;
- default constructor chaining preserves all earlier S4–S10 UI callers.

UI safety boundaries:

- core Burp projections remain `publishable=false`;
- no publish/import/add-issue action control exists in the Sprint 12 panel;
- recording a publication receipt does not mutate FindingCandidate/reproduction-package state;
- review candidate and rejected control counts remain distinct;
- real Burp runtime wording remains explicitly separate.

### Phase 4 verification

GitHub Actions run `36070071498`: **SUCCESS** at source commit
`a3328f7df32391f6aaa4a2f867de8590ba6fab52`.

Verified:

- Phase 1 standards suite: PASS, 34 assertions;
- Phase 2 Montoya adapter: PASS, 13 assertions;
- Phase 3 publication boundary: PASS, 19 assertions;
- Phase 4 reproduction UI: PASS, 34 assertions;
- complete retained Sprint 11 verifier: PASS;
- Maven core/extension compilation: PASS.

Phase 4 is **VERIFIED COMPLETE**.

## Next dependency

Phase 5 is the pre-closure hardening/reproducibility gate. It must adversarially test export minimization,
endpoint/URL/approval validation, receipt integrity, workspace collisions and publication side-effect isolation;
then produce deterministic JSON/SARIF evidence artifacts and bounded engineering observations before final closure.
