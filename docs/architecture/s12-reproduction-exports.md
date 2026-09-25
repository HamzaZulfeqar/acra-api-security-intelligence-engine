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

## Phase 2 Burp projection boundary

```text
ReproductionPackage
        |
        v
BurpIssueDraft (core; no Montoya dependency)
        |
        v
BurpIssueDraftAdapter (extension; Montoya 2026.7)
        |
        v
AuditIssue object
        |
        X  SiteMap.add(...)  NOT CALLED
```

Phase 2 invariants:

1. Core remains independent of Montoya types.
2. Draft severity is INFORMATION because candidate severity is not a confirmed vulnerability severity.
3. Draft confidence is TENTATIVE.
4. `confirmed` is false.
5. submission state is `NOT_SUBMITTED`.
6. Dynamic detail values are HTML encoded.
7. Adapter requires an absolute HTTP(S) base URL.
8. Adapter creates a Montoya `AuditIssue` projection only.
9. Adapter has no site-map submission capability.
10. Real Burp desktop registration remains separately unverified.

Phase 2 verification: GitHub Actions run `36129019615` — SUCCESS. Retained Sprint 2 and Sprint 3 gates also
pass after updating their local Montoya stubs to the same minimal 2026.7 audit-issue contract.

## Phase 3 dependency

Execute the adapter against the official Montoya API dependency in CI and verify the projected `AuditIssue`
properties without registering it with a live site map.
