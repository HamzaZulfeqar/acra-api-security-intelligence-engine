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

## Phase 2 dependency

Montoya conversion belongs only in `extension/burp-extension` and must require explicit human publication
approval before constructing a publishable Burp issue object. Real Burp desktop publication remains a separate
runtime-validation lane.
