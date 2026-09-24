# Sprint 10 — Batch & Indirect Authorization Intelligence

Status: IN PROGRESS — Phase 1 VERIFIED  
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
