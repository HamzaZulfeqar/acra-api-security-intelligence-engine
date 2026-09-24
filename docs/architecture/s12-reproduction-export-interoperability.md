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
      +--> JSON      (IMPLEMENTED)
      +--> SARIF     (IMPLEMENTED)
      +--> BurpIssue (IMPLEMENTED_RUNTIME_UNVERIFIED)
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

## Phase 2 JSON boundary

The generic JSON exporter serializes only the sanitized reproduction package, emits stable content and SHA-256,
and exposes a versioned Reporter adapter. Verification: `36072071561`; promotion: `36072225976`.

## Phase 3 SARIF boundary

SARIF export emits a standard-shaped 2.1.0 log with ACRA tool/rule metadata and a `review` result. ACRA-specific
candidate/evidence metadata lives in the SARIF result property bag. Verification: `36072500775`; promotion:
`36072674240`.

## Phase 4 Burp Issue boundary

The core Burp issue projection is Montoya-independent. The extension adapter consumes that projection and calls
the official Montoya issue factory / SiteMap add path. Only CANDIDATE packages may project, CERTAIN confidence is
never emitted, and the adapter is not automatically invoked by extension initialization. Verification:
`36073018537`; promotion: `36073182973`.

Real Burp desktop runtime remains a separate **UNVERIFIED / DEFERRED** gate.

## Phase 5 dependency

Cross-format hardening must verify that JSON, SARIF and Burp projections preserve the same package/candidate/evidence
identity, remain deterministic and secret-safe, and fail closed on invalid state or origin data before final closure.
