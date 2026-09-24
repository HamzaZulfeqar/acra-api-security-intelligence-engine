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

## Phase 5 interoperability boundary

```text
FindingCandidate
      |
      v
ReproductionPackage
      |
      v
ReproductionInteroperabilityService
      |
      +--> deterministic JSON artifact
      +--> deterministic SARIF artifact
      +--> deterministic Burp Issue projection
      |
      v
ReproductionInteroperabilityBundle
```

Phase 5 invariants:

1. All three outputs retain the same package/candidate/evidence lineage.
2. All three remain review-only.
3. Bundle/artifact/projection identities are deterministic.
4. Secret material remains absent across JSON, SARIF and Burp surfaces.
5. Capability states are exact and independently meaningful.
6. REJECTED/INCONCLUSIVE candidates cannot enter unified Burp interoperability.
7. Unsafe origins and mismatched artifacts fail closed.
8. Real Burp desktop insertion is not inferred from source/compile verification.

Verification: `36073674429` — SUCCESS, 39 interoperability assertions.

## Final closure dependency

Final closure must re-run the complete software stack, retained regressions, official Maven packaging and
deterministic archive-integrity verification before Sprint 12 may be promoted to SOFTWARE COMPLETE.
