# Sprint 10 Authentication, Session & Token-Context Intelligence

## Purpose

Sprint 10 closes explicit identity/session requirements that remained outside the Sprint 9 property-authorization
release boundary.

The existing repository already contains `SessionContext`, `IdentityEvidence`,
`IdentityConfirmationRegistry`, token fingerprints and Sprint 7 workflow token binding. Sprint 10 extends those
contracts rather than creating a second identity system.

## Standards and requirements anchors

Repository requirements:

- FR-018: identity/session/role models support UNKNOWN and provenance; an Authorization header alone must not
  identify a principal;
- SEC-003: token renewal must not silently alter principal, role, tenant or scope;
- FR-033: live traffic produces passive authentication/session observations with provenance;
- SEC-010: passive token/session correlation must not equate a token fingerprint with a verified principal.

External security context:

- OWASP API2:2023 Broken Authentication treats token authenticity and authentication-flow correctness as core API
  security concerns;
- RFC 9700 is the OAuth 2.0 Security Best Current Practice and includes current guidance on secure OAuth flows and
  token replay prevention.

## Phase 1 processing model

```text
Passive authentication/session observation
        |
        +--> SHA-256 token fingerprint only
        +--> session identifier
        +--> principal / role / tenant / scope context
        +--> explicit identity-confidence state
        +--> evidence provenance
        |
        v
S10SessionContextAnalyzer
        |
        +--> same-session boundary
        +--> verified-vs-unverified identity boundary
        +--> token rotation detection
        +--> principal/role/tenant/scope drift detection
        |
        v
SessionCorrelationResult
```

## Invariants

1. Raw bearer/session/API-key values are not accepted as token fingerprints.
2. Token fingerprints are correlation handles, not proof of principal identity.
3. Only USER_CONFIRMED or LAB_CONFIRMED identity states are treated as verified.
4. Different session identifiers are not silently merged.
5. Token rotation with stable verified context is represented separately from context drift.
6. Token renewal that changes verified principal, role, tenant or scope is explicit CONTEXT_DRIFT.
7. Unverified identity remains UNVERIFIED_IDENTITY even if a token fingerprint is present.
8. Phase 1 is passive and deterministic; it performs no OAuth grant manipulation, login brute force or external
   target testing.
9. No Phase 1 result is an automatically confirmed vulnerability.

## Later Sprint 10 slices

Subject to successful preceding gates, later phases may add evidence-store binding, passive traffic hydration,
controlled synthetic token/session ground truth, refresh/rotation policy validation, product coverage, UI,
report/export, hardening/performance and final closure.

OAuth/OIDC protocol-specific active testing is not claimed by Phase 1.
