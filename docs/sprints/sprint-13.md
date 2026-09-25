# Sprint 13 — Finding Lifecycle Governance

Status: **IN PROGRESS — Phases 1–5 VERIFIED**  
Branch: `s13-finding-lifecycle-governance`  
Immutable Sprint 12 base: `9ced79ba0986ce90884b745342769e88c38a68c2`

## Purpose

FR-012 requires findings to implement a lifecycle. The repository already had evidence-backed
`FindingCandidate`, deterministic severity/confidence and reproduction/export capabilities, but no persisted
finding lifecycle state or governed transition history.

No authoritative lifecycle-state enumeration existed in the repository. Sprint 13 therefore defines the explicit
ACRA operational lifecycle while preserving the project’s strongest existing boundary:

> A FindingCandidate is not a confirmed vulnerability.

Software may create `REVIEW_REQUIRED`. Only an explicit human-reviewed transition may create `CONFIRMED`.

## Phase 1 — deterministic lifecycle foundation

Implemented states:

```text
REVIEW_REQUIRED
   | CONFIRM
   v
CONFIRMED
   | ACCEPT_RISK -----------------> ACCEPTED_RISK ----> CLOSED
   |
   | START_REMEDIATION
   v
REMEDIATION_IN_PROGRESS
   | REQUEST_RETEST
   v
RETEST_REQUIRED
   | PASS_RETEST ----> RESOLVED ----> CLOSED
   |
   | FAIL_RETEST
   +-----------------> CONFIRMED

REVIEW_REQUIRED
   | MARK_FALSE_POSITIVE
   v
FALSE_POSITIVE ---------------------> CLOSED
```

Implemented:

- `FindingLifecycleState`;
- `FindingLifecycleAction`;
- fail-closed `FindingLifecyclePolicy`;
- `FindingLifecycleTransitionRequest`;
- deterministic append-only `FindingLifecycleEvent`;
- immutable `GovernedFinding`;
- `FindingLifecycleService`;
- candidate/risk-assessment identity binding;
- independent severity and confidence preservation;
- evidence-required human transition requests;
- deterministic finding/event identities and fingerprints;
- history-chain validation;
- secret-bearing reviewer/decision/evidence-reference rejection;
- no automatic-confirmation API.

### Phase 1 invariants

1. Only `FindingCandidateState.CANDIDATE` can enter the governed lifecycle.
2. A new governed finding always starts `REVIEW_REQUIRED`.
3. Severity/confidence never decide lifecycle state.
4. Even CRITICAL severity cannot automatically create `CONFIRMED`.
5. Confirmation requires an explicit reviewer reference, decision reference and evidence.
6. Rejected/inconclusive candidates cannot enter the lifecycle.
7. Risk assessment must belong to the same candidate.
8. Lifecycle events are append-only, sequential and transition-policy validated.
9. Closed findings cannot silently reopen.
10. False-positive closure never becomes historically confirmed.
11. Resolved/accepted-risk closure retains the earlier confirmed history.
12. No network action or reproduction action is performed by lifecycle transitions.

### Phase 1 verification

GitHub Actions run `36077162629`: **SUCCESS** at source commit
`e2f4019f85270bab3498521a080b8e9ea59cf347`.

Verified:

- `Sprint13FindingLifecycleFoundationTestSuite`: PASS, 41 assertions;
- full Sprint 12 reproduction/standards verifier: PASS;
- Sprint 12 reproduction hardening: PASS, 14 assertions;
- Sprint 12 publication hardening: PASS, 14 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core test compilation: PASS.

Phase 1 is **VERIFIED COMPLETE**.

## Phase 2 — deterministic governance workspace

Implemented:

- `FindingGovernanceWorkspace`;
- `FindingGovernanceSnapshot`;
- `FindingGovernanceQueue`;
- one governed finding per source candidate;
- idempotent open for identical candidate/risk state;
- candidate/risk drift rejection;
- stale-snapshot protection using deterministic finding fingerprint;
- transition delegation exclusively through `FindingLifecycleService`;
- deterministic queue/state counts;
- deterministic snapshot ordering;
- confirmed-history accounting distinct from terminal queue membership;
- workspace clear/re-open semantics without identity leakage;
- no direct state/publish shortcut methods.

### Phase 2 verification

GitHub Actions run `36077517602`: **SUCCESS** at source commit
`0c9533c3a58aae4d5f3aa27cd2fb48e694616d6d`.

Verified:

- `Sprint13FindingGovernanceWorkspaceTestSuite`: PASS, 25 assertions;
- Phase 1 lifecycle suite: PASS, 41 assertions;
- full retained Sprint 12 reproduction/standards verifier: PASS;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS;
- exact-head Core / Sprint 2 / Sprint 3 workflows: PASS.

Phase 2 is **VERIFIED COMPLETE**.

## Phase 3 — read-only governance product UI

Implemented:

- `S13FindingGovernancePanel`;
- top-level `Finding Governance` area in `AcraSuiteTab`;
- Overview;
- Review Required;
- Confirmed;
- Remediation;
- Retest;
- Terminal;
- History;
- deterministic queue/table projection from `FindingGovernanceSnapshot`;
- append-only lifecycle history projection;
- suite-tab access to the same governance workspace;
- no lifecycle action or publication controls.

### Phase 3 verification

GitHub Actions run `36079250303`: **SUCCESS** at source commit
`b109b58c0a6ac7de032035e55ae57daffe6a84f6`.

Verified:

- `Sprint13FindingGovernanceUiTestSuite`: PASS, 55 assertions;
- Phase 2 governance workspace: PASS, 25 assertions;
- Phase 1 lifecycle foundation: PASS, 41 assertions;
- retained Sprint 12 reproduction/standards foundation: PASS;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS;
- Core / Sprint 2 workflows: PASS on exact Phase 3 head.

Phase 3 is **VERIFIED COMPLETE**.

## Phase 4 — deterministic governance report/export

Implemented:

- `S13GovernanceReportStatus`;
- `S13GovernanceReportSummary`;
- `S13GovernanceFindingProjection`;
- `S13GovernanceEventProjection`;
- `S13GovernanceReport`;
- `S13GovernanceReportGenerator`;
- `S13GovernanceReportExporter`;
- `S13GovernanceJsonReporter`;
- workspace-level `report`, `exportJson` and `exportMarkdown`;
- read-only Report and JSON Export UI views;
- canonical report evidence artifact retention.

### Phase 4 verification

Source gate: GitHub Actions run `36079738535`: **SUCCESS** at source commit
`55d76ad0ff3f3536e5e76c9eefa8f3f6c1b00f04`.

Artifact-retention gate: run `36079852878`: **SUCCESS** at
`920d4dffde757e11009957990fa08930af0531e7`.

Verified:

- `Sprint13GovernanceReportingExportTestSuite`: PASS, 37 assertions;
- governance UI: PASS, 66 assertions;
- Phase 2 workspace: PASS, 25 assertions;
- Phase 1 lifecycle: PASS, 41 assertions;
- deterministic report ID independent of render timestamp: PASS;
- JSON/Markdown deterministic digests: PASS;
- Reporter adapter: PASS;
- report/export state mutation: NONE;
- source-candidate rationale/raw principal exclusion: PASS;
- canonical JSON/SHA-256/Markdown upload: PASS;
- Core / Sprint 2 / Sprint 3 workflows: PASS at source verification head.

Phase 4 is **VERIFIED COMPLETE**.

## Phase 5 — security hardening and bounded engineering observations

Security-hardening coverage:

- secret-bearing reviewer references rejected;
- secret-bearing decision references rejected;
- secret-bearing lifecycle evidence references rejected;
- evidence-free transition requests rejected;
- CRITICAL prioritization remains REVIEW_REQUIRED until explicit reviewed confirmation;
- invalid transition paths fail closed;
- stale governed-finding fingerprints fail closed;
- candidate-fingerprint drift fails closed;
- risk-assessment drift fails closed;
- false-positive closure remains historically unconfirmed;
- closed findings cannot silently reopen;
- report/export cannot mutate governed-finding state;
- report schema excludes raw candidate principal and rationale fields;
- report summary queue-denominator mismatches fail closed;
- Reporter rejects invalid model input.

Bounded engineering observations cover 100 / 1,000 / 10,000 REVIEW_REQUIRED governed findings across:

- workspace population;
- immutable snapshot creation;
- deterministic governance report generation.

### Phase 5 verification

GitHub Actions run `36080369571`: **SUCCESS** at source commit
`3ba4f96f8fbf43b0e025a280cd8763fd28d6f75a`.

Verified evidence:

- `Sprint13GovernanceSecurityHardeningTestSuite`: PASS, 25 assertions;
- `Sprint13GovernancePerformanceObservationTestSuite`: PASS, 19 assertions;
- governance reporting regression: PASS, 37 assertions;
- governance UI regression: PASS, 66 assertions;
- lifecycle foundation: PASS, 41 assertions;
- governance workspace: PASS, 25 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS;
- Core / Sprint 2 / Sprint 3 workflows: PASS at exact Phase 5 head;
- report artifacts + `performance-s13.csv`: uploaded by Sprint 13 workflow.

Observed CI values:

| Governed findings | Population | Snapshot | Report | Approx JVM memory delta |
|---:|---:|---:|---:|---:|
| 100 | 144 ms | 3 ms | 56 ms | 8,642,448 bytes |
| 1,000 | 193 ms | 0 ms | 31 ms | 14,033,624 bytes |
| 10,000 | 891 ms | 5 ms | 283 ms | 11,939,608 bytes |

These values are environment-specific engineering observations from one CI run. They are not benchmarks, SLOs,
scanner-accuracy evidence, production capacity guarantees or real-world workload claims.

Phase 5 is **VERIFIED COMPLETE**.

## Next dependency

Remaining Sprint 13 work is final requirements traceability, retained release/regression verification,
deterministic source packaging and final software audit. Real Burp desktop runtime/publication remains a separate
**UNVERIFIED / DEFERRED** validation lane.
