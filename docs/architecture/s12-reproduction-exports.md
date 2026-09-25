# Sprint 12 Reproduction & Interchange Export Architecture

## Boundary

```text
FindingCandidate (review-only)
        |
        v
ReproductionPackageFactory
        |
        v
ReproductionPackage
        |
        +--> JSON
        +--> SARIF 2.1.0
        +--> future BurpIssueDraft
```

## Phase 1 invariants

1. Export never promotes CANDIDATE to a confirmed vulnerability.
2. Principal and tenant identifiers are excluded from the reproduction-package schema.
3. Free-form rationale is excluded from the reproduction-package schema.
4. Raw HTTP request/response material is excluded.
5. Evidence/policy references remain available for review.
6. Package identity and artifact SHA-256 are deterministic.
7. SARIF candidate results use `warning`, not `error`.
8. Rejected/inconclusive controls use `note`.
9. SARIF explicitly carries `confirmed=false`.
10. Core remains independent of Montoya/Burp types.

Phase 1 verification: GitHub Actions run `36128103735` — SUCCESS.

## Phase 2 dependency

Burp compatibility must be introduced through a minimized `BurpIssueDraft` and extension adapter. The core draft
must not import Montoya types, and extension projection must not imply desktop issue submission has executed.
