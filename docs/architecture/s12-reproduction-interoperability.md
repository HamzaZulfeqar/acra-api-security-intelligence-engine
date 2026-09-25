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

## Phase 2 SARIF boundary

```text
ReproductionPackage
        |
        v
ReproductionSarifExporter
        |
        +--> OASIS SARIF 2.1.0 log
        +--> ACRA rule descriptor
        +--> review/pass/open result
        +--> stable fingerprints
        +--> minimized ACRA properties
```

Phase 2 invariants:

1. SARIF version is 2.1.0 and identifies the OASIS Errata 01 schema.
2. CANDIDATE is encoded as `kind=review`, not `fail`.
3. REJECTED is `pass`; INCONCLUSIVE is `open`.
4. All three non-fail kinds use `level=none`.
5. ACRA severity/confidence remain separate properties.
6. Endpoint context is not misrepresented as a source-code physical location.
7. Finding and reproduction fingerprints remain stable.
8. Raw principal, secrets, rationale and contradictory narrative are excluded.
9. The SARIF Reporter plugin uses the canonical exporter.
10. SARIF projection does not change finding truth.

Phase 2 verification: run `36122759287` — SUCCESS at
`3e5c63962c007865a85dfdd23edac2165f3075d7`.

## Phase 3 Burp AuditIssue boundary

```text
ReproductionPackage (core)
        +
real HttpRequestResponse (Montoya)
        |
        v
ReproductionAuditIssueAdapter
        |
        v
review-only AuditIssue
```

Phase 3 invariants:

1. Montoya types do not enter `acra-core`.
2. Only issue-eligible CANDIDATE packages can be adapted.
3. Request/response evidence and a usable request URL are mandatory.
4. The issue keeps the original request/response object.
5. Dynamic content is HTML-escaped.
6. Severity mapping is conservative and deterministic.
7. HIGH ACRA confidence maps to FIRM, never CERTAIN.
8. Issue text states that human validation is required and exploitation is not confirmed.
9. REJECTED/INCONCLUSIVE packages fail closed.
10. Constructing an AuditIssue does not imply it was accepted/displayed by a real Burp desktop.

Phase 3 verification: run `36123760660` — SUCCESS at
`be10c1c406bb475e12446ad9cff23056c3cc352a`.

## Phase 4 boundary

A Site Map publisher may accept the verified AuditIssue and call Montoya `SiteMap.add` only through an explicit
publication action. Duplicate reproduction fingerprints must be suppressed before registration. Real desktop
runtime acceptance remains separately unverified.
