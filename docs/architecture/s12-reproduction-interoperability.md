# Sprint 12 Reproduction & Interoperability Architecture

## Principle

Interoperability is a projection boundary, not a finding-classification boundary.

```text
FindingCandidate + AuthorizationReport
             |
             v
ReproductionPackageBuilder
             |
             v
canonical ReproductionPackage
             |
             +--> JSON
             +--> SARIF        (Phase 2)
             +--> Burp Issue   (later adapter phase)
```

## Phase 1 invariants

1. Core reproduction state is independent of output format.
2. Exporting cannot promote CANDIDATE to confirmed vulnerability.
3. Every package is review-only.
4. REJECTED and INCONCLUSIVE are not issue-eligible.
5. Severity and confidence remain separate values.
6. Raw principal identity is replaced by SHA-256 fingerprint.
7. Candidate rationale and contradictory narrative are excluded structurally.
8. Assessment/evidence/policy provenance remains explicit.
9. Cross-project or mismatched report/candidate lineage fails closed.
10. Core remains independent of Burp/Montoya.

Phase 1 verification: run `36122252879` — SUCCESS at
`46a43350d5357f78f0756b6ce20329ef750e0cfb`.

## Phase 2 boundary

SARIF projection consumes only the canonical reproduction package. It must emit SARIF 2.1.0-compatible top-level
version/schema/run/tool/result structure and keep ACRA-specific review state in explicit properties. It must not
invent source-code line locations for API endpoints.
