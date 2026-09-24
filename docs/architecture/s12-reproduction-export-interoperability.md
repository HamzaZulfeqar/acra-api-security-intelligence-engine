# Sprint 12 Reproduction Export & Interoperability Architecture

## Boundary

```text
FindingCandidate
      |
      v
ReproductionPackageProjector
      |
      v
acra-reproduction-package-v1
      |
      +--> JSON      (contract defined)
      +--> SARIF     (contract defined)
      +--> BurpIssue (contract defined)
```

Format-specific adapters consume the sanitized core package rather than raw HTTP/session/runtime objects.

## Phase 1 invariants

- supporting evidence mandatory;
- candidate state preserved;
- package always review-only;
- no confirmed-vulnerability state invented;
- severity/confidence remain independent;
- authorization and evidence lineage retained;
- universal redaction enforced;
- deterministic package identity/fingerprint;
- core remains Montoya-independent.

Verification: `36071724705` — SUCCESS.

## Dependency order

Phase 2 JSON → Phase 3 SARIF 2.1.0 → Phase 4 Burp Issue projection → hardening/closure.

Real Burp desktop runtime remains a separate deferred gate.
