# Sprint 10 Batch & Indirect Authorization Architecture

## Roadmap reconciliation

The historical ACRA roadmap assigned Sprint 10 to **Property / Batch / Indirect Authorization**.

Property-level authorization was pulled forward and completed in Sprint 9 with evidence-backed property
observations, controlled localhost validation, active execution, finding projection, coverage, product UI,
reporting and final closure.

Sprint 10 therefore owns the remaining roadmap scope:

- batch authorization;
- indirect-reference authorization.

Sprint 10 must reuse the Sprint 9 property layer and must not recreate it.

## Standards boundary

The batch/indirect work remains an object-authorization problem rather than a new vulnerability family.

- OWASP API1:2023 requires object-level authorization for API endpoints that receive an object identifier and
  perform an action on the referenced object.
- CWE-639 describes authorization bypass where a user-controlled key identifies another user's record.

Sprint 10 applies those principles to two structures that are easy to mis-handle:

1. a single API request that represents multiple object operations;
2. a client-controlled indirect key or alias that resolves to a target resource.

## Phase 1 processing models

### Batch authorization

```text
Explicit per-item BatchItemPolicy
          +
Evidence-backed BatchItemObservation
          +
Resolved actor authorization context
          |
          v
S10BatchAuthorizationAnalyzer
          |
          +--> evidence/project/test/execution/observation validation
          +--> exact endpoint/resource/action matching
          +--> role/tenant applicability
          +--> ambiguity rejection
          +--> per-item expected vs observed decision
          |
          v
BatchItemAuthorizationAssessment[]
```

No overall HTTP success state is allowed to override a denied item's decision.

### Indirect reference authorization

```text
Raw client reference
      |
      +--> NOT PERSISTED by S10
      |
      v
SHA-256 reference fingerprint
      +
Evidence-backed resolved resource
      +
Explicit policy for resolved target
      |
      v
S10IndirectReferenceAnalyzer
      |
      +--> provenance validation
      +--> one-reference -> one-resolved-target consistency
      +--> authorization against resolved resource
      |
      v
IndirectReferenceAuthorizationAssessment[]
```

Authorization is evaluated against the resolved target resource, not the apparent safety or opacity of the key.

## Phase 1 invariants

1. Batch authorization is item-level; aggregate HTTP success is not an authorization decision for every item.
2. Each batch item requires explicit resource/action identity and evidence lineage.
3. Missing batch item policy is INCONCLUSIVE.
4. Multiple applicable item policies remain ambiguous and are not promoted.
5. Cross-project provenance fails closed.
6. Mixed ALLOW/DENY item outcomes remain explicit.
7. Indirect raw reference values are not stored by the Phase 1 model.
8. Indirect references are represented by SHA-256 fingerprints.
9. Every indirect fingerprint must resolve to one unambiguous target before authorization can be assessed.
10. One fingerprint resolving to multiple resources is INCONCLUSIVE.
11. Indirect authorization policy is matched to the resolved target resource, action, actor role and tenant.
12. Missing or ambiguous resolved-target policy fails closed.
13. DENY -> ALLOW may become a review candidate at the assessment layer only.
14. Phase 1 creates no automatically confirmed vulnerability state.

## Explicit Phase 1 exclusions

Phase 1 does not:

- generate or guess object identifiers;
- enumerate aliases, invitation codes, short links, UUIDs or lookup keys;
- mutate or execute batch requests;
- perform indirect-key substitution;
- probe external targets;
- infer policy from a batch-level HTTP status;
- infer policy from identifier format, opacity or randomness;
- treat unpredictable identifiers as authorization controls;
- implement GraphQL/gRPC/WebSocket batch testing;
- auto-confirm a vulnerability;
- replace existing S4 scope, consent, budget, rate or safety gates;
- claim real Burp desktop runtime validation.

## Future Sprint 10 slices

Later phases may add controlled ACRA-Lab ground truth, safe S4 planner/executor integration, provenance-gated
FindingCandidate projection, coverage accounting, product UI, deterministic report/export, security hardening,
bounded performance observations and reproducible final closure.

Those capabilities are not claimed until separately implemented and verified.
