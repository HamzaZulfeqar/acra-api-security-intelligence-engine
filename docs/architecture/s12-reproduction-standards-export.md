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

## Phase 3 publication boundary

```text
projection + explicit approval
        |
        v
S12MontoyaAuditIssueAdapter
        |
        v
S12MontoyaAuditIssueFactory
        |
        v
S12AuditIssueSink
        |
        v
review-publication receipt
```

The production sink is `S12MontoyaSiteMapAuditIssueSink`, which wraps Montoya `SiteMap.add(AuditIssue)`.
The publisher itself is not registered in the extension bootstrap.

Phase 3 invariants:

1. Denied approval causes no issue creation or sink invocation.
2. Candidate mismatch causes no issue creation or sink invocation.
3. Approved publication produces exactly one issue and one sink invocation.
4. Publication receipt is deterministic.
5. Receipt state is `IMPORTED_REVIEW_CANDIDATE`, not confirmed vulnerability.
6. Publication cannot mutate FindingCandidate state.
7. Core projection remains `publishable=false`.
8. ACRAExtension has no publisher/site-map registration.
9. Real desktop publication remains unverified.

Phase 3 verification: run `36069442716` — SUCCESS.

## Phase 4 product/UI boundary

```text
S12ReproductionWorkspace
        |
        +--> package table
        +--> JSON preview
        +--> SARIF preview
        +--> Burp review projection
        |
        +--> extension receipt ledger
                read-only display
```

Phase 4 invariants:

1. Core workspace stores only reproduction packages and deterministic export/projection state.
2. Montoya publication receipts remain extension-layer state.
3. Core Burp projections remain non-publishable.
4. Receipt display cannot mutate candidate/package state.
5. The panel exposes no publish/import/add-issue action control.
6. Earlier AcraSuiteTab constructor callers remain source-compatible.
7. Real desktop UI/site-map interaction is not inferred from headless Swing verification.

Phase 4 verification: run `36070071498` — SUCCESS.

## Phase 5 dependency

Security hardening and reproducibility must pressure-test the export/publication boundary before Sprint 12 final
closure.
