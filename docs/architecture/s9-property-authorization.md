# Sprint 9 Property Authorization Architecture

## Purpose

Sprint 9 extends ACRA's existing authorization model to make object-property authorization a first-class,
evidence-backed analysis surface.

The existing Sprint 5 implementation already provides:

- `PropertyAuthorizationAssessment`;
- `PolicyValidationEvaluator.PropertyPolicy`;
- `PolicyValidationEvaluator.PropertyOperation`;
- property-policy review through `AuthorizationDimensionEvaluator`;
- correlation into the existing authorization finding-candidate pipeline.

Sprint 9 MUST reuse those contracts. It does not create a second property-policy engine.

## Standards boundary

The research and product boundary aligns with OWASP API3:2023 Broken Object Property Level Authorization,
which combines the historical excessive-data-exposure and mass-assignment failure modes around missing or
incorrect authorization at individual object properties.

This mapping is descriptive only. ACRA does not label an observation a confirmed API3 vulnerability solely
because a property differs.

## Phase 1 processing model

```text
Explicit PropertyPolicy
        +
Evidence-backed PropertyAccessObservation
        +
Existing AuthorizationContext
        |
        v
S9PropertyAuthorizationAnalyzer
        |
        +--> provenance validation
        +--> exact endpoint/property/operation matching
        +--> role/tenant applicability
        +--> ambiguity rejection
        |
        v
Existing PropertyAuthorizationAssessment
```

## Invariants

1. Property values are not stored by the Phase 1 observation model.
2. Every property observation requires evidence IDs and an existing execution/test/observation lineage.
3. Cross-project evidence fails closed.
4. Missing policy remains INCONCLUSIVE.
5. Multiple applicable policies remain explicitly ambiguous; no hidden precedence is invented.
6. Property-specific expected decisions come from explicit supplied property policy, not the object-level
   decision for the request as a whole.
7. DENY→ALLOW may create a review-only property candidate through the existing assessment contract.
8. No Phase 1 result is an automatically confirmed vulnerability.
9. Phase 1 does not generate property names, fuzz request bodies, or execute external targets.

## Future Sprint 9 slices

Later phases may add controlled ACRA-Lab read/update ground truth, safe mutation planning, provenance-gated
finding projection, property coverage, product UI, deterministic report/export, security hardening,
bounded performance observations, and final reproducible closure.

Those capabilities are not claimed until separately implemented and verified.
