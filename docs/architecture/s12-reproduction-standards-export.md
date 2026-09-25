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

## Phase 5 security/reproducibility boundary

Phase 5 hardens both sides of the reproduction boundary rather than relying on exporter redaction alone.

```text
FindingCandidate
      |
      v
secret-safe / query-free reproduction identity
      |
      +--> deterministic workspace identity
      |
      +--> JSON / SARIF / Burp-review projection
      |
      v
explicit publication approval
      |
      +--> absolute HTTP(S), host required
      +--> no userinfo
      +--> no query
      +--> no fragment
      +--> secret-safe approval reference
      |
      v
review-only publication receipt
      |
      +--> deterministic ID/fingerprint
      +--> IMPORTED_REVIEW_CANDIDATE only
```

Phase 5 invariants:

1. Secret-bearing material is rejected before package/approval/receipt identity is computed.
2. Reproduction endpoint identity excludes query and fragment material.
3. One source candidate cannot silently drift to multiple package identities inside one workspace.
4. Exact duplicate package recording is idempotent.
5. Publication URLs cannot carry credentials, query or fragment material.
6. Only HTTP(S) publication URLs with a concrete host are accepted.
7. Receipt validation does not merely trust earlier approval validation.
8. Receipt state cannot claim CONFIRMED or another unsupported publication state.
9. Real Montoya compilation and legacy stub-contract regressions both remain green.
10. Real Burp desktop SiteMap publication remains a separate runtime validation lane.

Phase 5 verification: run `36075554684` — SUCCESS at
`01aa13abd0385d6976eae15f583fd956afe69f5e`.
Retained Core/Sprint2/Sprint3: `36075554589` / `36075554624` / `36075554697` — SUCCESS.

## Final closure dependency

Sprint 12 now requires requirements traceability, complete retained regressions, deterministic source packaging,
archive-integrity verification and final audit before SOFTWARE COMPLETE can be claimed.
