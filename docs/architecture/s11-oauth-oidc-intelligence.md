# Sprint 11 OAuth/OIDC Protocol Intelligence

## Purpose

Sprint 11 adds an explicit OAuth/OIDC context layer on top of Sprint 10 authentication/session intelligence.

The Phase 1 boundary is passive and deterministic. It records protocol facts that are already observed or supplied
with provenance. It does not contact authorization servers, replay codes, manipulate grants, refresh tokens, or
redirects, and it does not treat decoded token claims as cryptographic identity proof.

## Standards basis

The scope is informed by:

- RFC 9700 / BCP 240 — OAuth 2.0 Security Best Current Practice;
- OpenID Connect Core 1.0 incorporating errata set 2;
- the existing ACRA FR-018, SEC-003, FR-033 and SEC-010 identity/session requirements.

These standards are used to define context fields and security boundaries, not to claim standards-complete
validation in Phase 1.

## Phase 1 model

```text
Observed HTTP / supplied protocol metadata
              |
              v
      OAuthContextObservation
              |
      +-------+--------+
      |                |
      v                v
issuer/client      redirect URI
resource/aud       SHA-256 handle
scope/flow         (raw URI omitted)
PKCE/state/nonce
      |
      v
 OAuthContextCorrelator
      |
      +--> STABLE
      +--> CONTEXT_DRIFT
      +--> INCONCLUSIVE
```

## Stored facts

Phase 1 may preserve:

- OAuth2 vs OIDC protocol classification;
- issuer URI;
- observed authorization/token endpoint URIs;
- client identifier;
- resource indicators;
- audience values;
- scopes;
- response types;
- grant type;
- SHA-256 redirect-URI fingerprint;
- observed PKCE method;
- presence/absence of state and nonce signals;
- explicit identity-confidence state;
- evidence references and observation time.

## Security invariants

1. Raw access tokens, refresh tokens, authorization codes, state values and nonce values are not stored.
2. Raw redirect URIs are not retained by this model; only a SHA-256 correlation handle is accepted.
3. Decoded claims or protocol metadata do not independently verify a principal.
4. Different client contexts are not silently merged.
5. Issuer, redirect, resource, audience, scope, response-type, grant-type, PKCE, state and nonce changes are
   descriptive drift only.
6. Context drift is not automatically a vulnerability or FindingCandidate.
7. Phase 1 performs no network dispatch and no OAuth/OIDC active testing.
8. Existing Sprint 10 session/token correlation remains the lower-level authentication-context source of truth.

## Deferred

Later Sprint 11 phases may add evidence-store binding, passive extraction from existing traffic, controlled local
OAuth/OIDC fixtures, policy/metadata assessment, coverage, product UI, reporting and hardening.

Active grant manipulation, authorization-code replay, external IdP probing and production credential testing remain
out of Phase 1.
