# Sprint 12 Reproduction & Standards Export Architecture

## Phase 1 flow

```text
FindingCandidate
      |
      v
S12ReproductionPackage
      |
      +--> JSON + SHA-256
      |
      +--> SARIF 2.1.0
      |       review/informational
      |       level=none
      |
      +--> Burp Issue-neutral projection
              INFORMATION / TENTATIVE
              publishable=false
```

## Invariants

1. Candidate does not mean confirmed vulnerability.
2. Review candidates require supporting evidence.
3. Raw principal identifiers and candidate rationale are structurally excluded.
4. SARIF review candidates are not emitted as `kind=fail`.
5. Non-fail SARIF results use `level=none`.
6. Burp projection is not publishable in core.
7. Core has no Montoya dependency.
8. Export does not perform active replay.

Phase 1 verification: run `36068186039` — SUCCESS.

## Phase 2 Montoya conversion boundary

```text
core S12BurpIssueProjection (publishable=false)
        |
        + explicit approval:
        |   candidateId match
        |   approved=true
        |   absolute HTTP(S) base URL
        v
S12MontoyaAuditIssueSpec
        |
        v
AuditIssue.auditIssue(...)
        |
        X  no SiteMap.add in Phase 2
```

Phase 2 invariants:

1. Montoya types stay outside core.
2. Explicit approval is separate from candidate state.
3. Approval is candidate-bound.
4. Publication URL is explicit and absolute.
5. INFORMATION/TENTATIVE review semantics are retained.
6. Phase 2 adapter has no publication method.
7. ACRAExtension bootstrap does not register or invoke the adapter.
8. Real Burp creation/publication remains unverified.

Phase 2 verification: run `36068996138` — SUCCESS.

## Phase 3 dependency

An injected publication service may wrap `SiteMap.add(AuditIssue)` after approval validation. It must remain
unregistered from automatic application flows and preserve a deterministic receipt/audit trail.
