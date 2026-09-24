# Sprint 10 — Batch & Indirect Authorization Intelligence

Status: IN PROGRESS — Phases 1–6 VERIFIED  
Branch: `s10-batch-indirect-authorization`  
Immutable Sprint 9 base: `ce81220eb9ea41009973b4072c08d59927ee8c6b`

## Dependency decision

Sprint 9 is SOFTWARE COMPLETE and its post-documentation final closure revalidation passed.

The historical roadmap assigned Sprint 10 to **Property / Batch / Indirect Authorization**. Property authorization
is now complete in Sprint 9, so Sprint 10 is narrowed to the two remaining dependency-ordered capabilities:

- Batch Authorization Intelligence;
- Indirect Reference Authorization Intelligence.

This is a roadmap reconciliation, not a replacement of the historical intent.

## Research / standards anchor

Sprint 10 treats both capabilities as object-authorization reasoning:

- batch requests must preserve authorization decisions for each referenced object/action independently;
- indirect references must be authorized after resolving the key/alias to the actual target resource.

Identifier opacity, randomness, successful lookup and batch-level HTTP success are not authorization evidence.

## Sprint boundary

Planned dependency order:

1. deterministic evidence-backed batch/indirect reasoning foundation;
2. controlled ACRA-Lab batch and indirect-reference ground truth;
3. safe planning/execution through the existing S4 active engine;
4. provenance-gated assessment/FindingCandidate projection;
5. batch/indirect coverage accounting;
6. product workspace and Burp UI;
7. deterministic report/export;
8. security hardening and bounded performance observations;
9. final traceability, regression, reproducible checkpoint and software audit.

Later phases remain gated on successful preceding verification.

## Phase 1 — batch and indirect authorization reasoning foundation

Candidate implementation:

### Batch

- `BatchItemPolicy`;
- `BatchItemObservation`;
- `BatchItemAuthorizationAssessment`;
- `S10BatchAuthorizationAnalysis`;
- `S10BatchAuthorizationAnalyzer`;
- item-level expected/observed decisions;
- role/tenant applicability;
- provenance validation through the existing `EvidenceReferenceValidator`;
- mixed batch decisions preserved;
- missing/ambiguous item policy fails closed;
- no aggregate batch authorization shortcut.

### Indirect references

- `IndirectReferenceSource`;
- `IndirectReferenceResolution`;
- `IndirectReferencePolicy`;
- `IndirectReferenceAuthorizationAssessment`;
- `S10IndirectReferenceAnalysis`;
- `S10IndirectReferenceAnalyzer`;
- SHA-256 fingerprint representation instead of raw key storage;
- resolved-resource authorization;
- resolution-conflict rejection;
- missing/ambiguous resolved-target policy fails closed;
- provenance validation through the existing evidence store.

## Phase 1 acceptance boundary

Phase 1 must prove:

1. a mixed batch retains every item-level decision;
2. explicit DENY + observed ALLOW on one batch item is not hidden by other successful items;
3. missing batch policy is INCONCLUSIVE;
4. ambiguous batch policy is not promoted;
5. cross-project batch provenance fails closed;
6. batch analysis identity is deterministic across input ordering;
7. an indirect fingerprint is authorized against its resolved resource;
8. raw reference material is not stored;
9. missing resolved-resource policy is INCONCLUSIVE;
10. one fingerprint resolving to multiple resources is explicit conflict and non-promotable;
11. cross-project indirect provenance fails closed;
12. indirect analysis identity is deterministic across input ordering;
13. secret-bearing metadata is rejected;
14. retained S5/S6/S7/S8/S9 foundations remain green;
15. exact Java 21 compilation with warnings as errors and Maven core test compilation pass.

## Phase 1 verification

GitHub Actions run `36004146212`: **SUCCESS** at commit
`568daea31e5e1cfba7639efa7135a094dad6fb5f`.

Verified gates:

- exact Temurin Java 21 source/test compilation with `-Xlint:all -Werror`: PASS;
- `Sprint10BatchIndirectFoundationTestSuite`: PASS, 25 assertions;
- retained Sprint 9 property foundation: PASS, 15 assertions;
- retained Sprint 8 routing-normalization foundation: PASS, 16 assertions;
- retained Sprint 6 policy foundation: PASS, 11 assertions;
- retained Sprint 5 final closure: PASS, 21 assertions;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 2 owns controlled localhost batch/indirect ground truth; no active
batch/indirect mutation is claimed yet.


## Phase 2 — controlled ACRA-Lab batch and indirect-reference ground truth

Implemented:

- `GT-S10-BATCH-INDIRECT-AUTHORIZATION.json`;
- fixed synthetic batch resources `resource-a` / `resource-b`;
- fixed synthetic aliases `share-a` / `share-b`;
- secure batch fixture evaluates authorization independently for every item;
- deliberately vulnerable batch fixture incorrectly reuses the first item's authorization across the batch;
- secure indirect fixture resolves the alias and then authorizes the resolved resource;
- deliberately vulnerable indirect fixture resolves the alias but skips resolved-target authorization;
- batch read fixture is explicitly non-persistent;
- authentication remains required for both fixture families;
- no key generation, guessing, alias enumeration or external targets.

### Phase 2 verification

GitHub Actions run `36004574331`: **SUCCESS** at commit
`95bdc02e53ffb06e49eaf1bfd8bbadb137b5a0e9`.

- Sprint 10 ground-truth contract: PASS, 4 cases;
- secure/vulnerable ACRA-Lab readiness: PASS;
- Sprint 10 foundation: PASS, 25 assertions;
- controlled batch/indirect live-lab suite: PASS, 16 assertions;
- retained authorization foundations: PASS;
- Maven core `test-compile`: PASS;
- controlled lab logs artifact upload: PASS.

Phase 2 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 3 owns safe planner/executor integration through the existing S4 engine.


## Phase 3 — safe S4 planner/executor integration

Implemented and verified:

- fixed policy-backed `BATCH` and `INDIRECT_REFERENCE` test seeds;
- reuse of the existing S4 planner, queue, request builder, executor and evidence store;
- reuse of existing `MutationType.BATCH` and `MutationType.INDIRECT_REFERENCE`;
- request-equivalence validation before dispatch;
- authorized `LAB` loopback execution only;
- scope, environment, consent, request-budget, mutation-budget, concurrency, rate-limit and kill-switch enforcement;
- non-persistent batch-read mutation adding only fixed `resource-b` to fixed owned `resource-a`;
- fixed indirect-reference path substitution `share-a → share-b`;
- secure and deliberately vulnerable controlled outcomes projected into the existing S10 analyzers;
- raw bearer material excluded from serialized test state;
- raw indirect alias excluded from the persisted resolution model; SHA-256 fingerprint retained instead;
- cross-project analysis remains fail-closed;
- candidate state remains review-only.

### Phase 3 verification

GitHub Actions run `36048381112`: **SUCCESS** at source commit
`608490cfc1211057e879f6c6457ea62fd648b405`.

Verified execution evidence:

- `Sprint10ControlledBatchExecutionTestSuite`: PASS, 25 assertions;
- secure batch: aggregate ALLOW, item candidates = 0;
- deliberately vulnerable batch: aggregate ALLOW, item candidates = 1;
- `Sprint10ControlledIndirectExecutionTestSuite`: PASS, 23 assertions;
- secure indirect resolved-target control: expected DENY, observed DENY, candidates = 0;
- deliberately vulnerable indirect control: expected DENY, observed ALLOW, candidates = 1;
- Sprint 10 foundation/lab and retained authorization regressions: PASS;
- Maven core `test-compile`: PASS.

Phase 3 is **VERIFIED COMPLETE**.

No identifier generation, guessing, alias enumeration, external target, destructive mutation or automatic confirmed-vulnerability behavior is introduced.

Sprint 10 remains IN PROGRESS. Phase 4 owns provenance-gated batch/indirect `FindingCandidate` projection.


## Phase 4 — provenance-gated FindingCandidate projection

Implemented:

- `S10BatchFindingRequest`;
- `S10IndirectFindingRequest`;
- `S10BatchFindingCandidateEvaluator`;
- `S10IndirectFindingCandidateEvaluator`;
- evidence-reference and observation-lineage revalidation before projection;
- request-to-assessment consistency checks;
- deterministic candidate IDs and finding fingerprints;
- explicit `BATCH_AUTHORIZATION/BATCH_ITEM` and
  `INDIRECT_REFERENCE_AUTHORIZATION/RESOLVED_TARGET` dimensions;
- secure verified controls → `REJECTED`;
- verified DENY→ALLOW mismatch → review-only `CANDIDATE`;
- missing/invalid provenance or projection mismatch → `INCONCLUSIVE`;
- no raw indirect alias storage in projected candidates;
- no confirmed-vulnerability state.

### Phase 4 verification

GitHub Actions run `36048837246`: **SUCCESS** at source commit
`abb57d0320fcdc161992297415978bde6c769bee`.

- `Sprint10FindingCandidateProjectionTestSuite`: PASS, 22 assertions;
- retained controlled batch execution: PASS, 25 assertions;
- retained controlled indirect execution: PASS, 23 assertions;
- retained Sprint 10 foundation/lab and earlier authorization regressions: PASS;
- Maven core `test-compile`: PASS.

Phase 4 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 5 owns deterministic batch/indirect coverage accounting.


## Phase 5 — deterministic batch/indirect coverage accounting

Implemented:

- `S10CoverageFamily`;
- `S10CoverageDisposition`;
- `S10AuthorizationCoverageEntry`;
- `S10AuthorizationCoverageSummary`;
- `S10AuthorizationCoverageTracker`;
- explicit combined coverage universe for batch-item and indirect resolved-target policies;
- duplicate-safe deterministic policy registration;
- explicit observation-before-assessment lifecycle;
- finding/assessment/observation consistency validation;
- `UNOBSERVED`, `OBSERVED_UNASSESSED`, `REJECTED`, `INCONCLUSIVE` and `CANDIDATE` dispositions;
- deterministic aggregate counts and observation/assessment ratios.

### Phase 5 verification

GitHub Actions run `36049247018`: **SUCCESS** at source commit
`16169367c3e7c3b179a6d8541dfc88698fde6b81`.

- `Sprint10CoverageAccountingTestSuite`: PASS, 22 assertions;
- explicit four-context fixture: 2 batch + 2 indirect policies;
- observed/assessed gaps preserved instead of inferred;
- candidate, rejected and inconclusive contexts accounted separately;
- mismatched batch resource and indirect resolved-target observations rejected;
- deterministic coverage ordering: PASS;
- retained Phase 3 active execution and Phase 4 finding projection: PASS;
- retained Sprint 10 foundation/lab and earlier authorization regressions: PASS;
- Maven core `test-compile`: PASS.

Phase 5 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 6 owns the read-only batch/indirect product workspace and headless Burp UI projection.


## Phase 6 — read-only product workspace and Burp UI

Implemented:

- `S10BatchIndirectProductSnapshot`;
- `S10BatchIndirectWorkspace`;
- `S10BatchIndirectPanel`;
- `AcraSuiteTab` integration with backward-compatible constructor chaining;
- top-level `Batch & Indirect` product area;
- `Overview`, `Policies`, `Observations`, `Assessments`, `Candidates` and `Coverage` views;
- separate batch vs indirect policy, observation and assessment rows;
- shared review-only finding and coverage lifecycle;
- raw indirect aliases excluded from UI projection;
- no Phase 7 report/export surface exposed early;
- real Burp desktop validation remains separately unverified.

### Phase 6 verification

GitHub Actions run `36051856008`: **SUCCESS** at source commit
`a33aaffccce80ba8251284a0b4de70f2f76e7a8a`.

Verified UI evidence:

- extension Maven `test-compile`: PASS;
- retained Sprint 6 authorization UI: PASS, 27 assertions;
- retained Sprint 7 workflow UI: PASS, 20 assertions;
- retained Sprint 8 routing UI: PASS, 23 assertions;
- retained Sprint 9 property UI: PASS, 59 assertions;
- `Sprint10BatchIndirectUiTestSuite`: PASS, 218 assertions;
- explicit four-policy denominator: 2 batch + 2 indirect;
- three evidence-backed observed/assessed contexts and one explicit unobserved context;
- one review candidate and two rejected controls;
- raw `share-a` / `share-b` material absent from all S10 UI tables;
- candidate/non-confirmation, aggregate-HTTP/per-item and headless/desktop boundaries preserved.

Phase 6 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 7 owns deterministic report/export generation and read-only UI projection.
