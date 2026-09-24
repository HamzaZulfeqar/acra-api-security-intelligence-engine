# Sprint 10 — Batch & Indirect Authorization Intelligence

Status: IN PROGRESS — Phase 1 candidate implemented, verification pending  
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

## Verification

Pending GitHub Actions execution on `s10-batch-indirect-authorization`.

Until that workflow passes, Phase 1 remains IMPLEMENTED / UNVERIFIED.
