# Sprint 10 — Authentication, Session & Token-Context Intelligence

Status: IN PROGRESS — Phase 1 candidate implemented, verification pending  
Branch: `s10-auth-session-intelligence`  
Immutable Sprint 9 base: `9bffa59c1b360b3e74e0b5e97d2ce22a06dc73f5`

## Dependency decision

Sprint 9 is SOFTWARE COMPLETE.

No authoritative historical document assigns a named Sprint 10 feature theme. The Sprint 10 scope is therefore
derived from explicit unresolved repository requirements rather than from speculative roadmap expansion.

The strongest unmet dependency is authentication/session/token-context intelligence:

- FR-018 requires UNKNOWN/provenance-aware identity and forbids treating Authorization-header possession as
  principal proof;
- SEC-003 requires token renewal not to silently change principal, role, tenant or scope;
- FR-033 requires passive authentication/session observations with provenance;
- SEC-010 requires passive token/session correlation not to equate a token fingerprint with a verified principal.

This also provides the prerequisite context layer for any later OAuth/OIDC-specific work.

## Sprint boundary

Planned dependency order:

1. passive authentication/session observation and deterministic correlation foundation;
2. evidence-store/project/test/observation provenance binding;
3. controlled ACRA-Lab token/session rotation ground truth;
4. passive traffic/session hydration from existing collection paths;
5. refresh/rotation and context-drift assessment;
6. review-only finding projection and coverage;
7. product workspace/Burp UI;
8. deterministic report/export;
9. security hardening and bounded performance observations;
10. final regression, traceability, reproducible checkpoint and software audit.

## Phase 1 — passive session/token-context correlation

Candidate implementation:

- `AuthenticationSessionObservation`;
- `SessionCorrelationState`;
- `SessionContextDimension`;
- `SessionCorrelationResult`;
- `S10SessionContextAnalyzer`;
- SHA-256-only token fingerprint boundary;
- explicit identity-confidence provenance;
- verified identity limited to USER_CONFIRMED / LAB_CONFIRMED;
- same-session correlation boundary;
- token rotation separated from principal/role/tenant/scope drift;
- unverified identity fails closed;
- deterministic correlation identity;
- no active authentication manipulation.

## Phase 1 acceptance boundary

Phase 1 must prove:

1. first observation creates a baseline, not a vulnerability;
2. stable verified session remains STABLE;
3. verified token renewal with unchanged context becomes TOKEN_ROTATED;
4. token renewal with verified role/tenant/scope drift becomes CONTEXT_DRIFT;
5. an unverified identity remains UNVERIFIED_IDENTITY even with a token fingerprint;
6. different session IDs are not silently correlated;
7. raw authentication material cannot be accepted as a token fingerprint;
8. correlation IDs are deterministic;
9. retained Sprint 9 property foundation remains green;
10. retained Sprint 8 routing foundation remains green;
11. retained Sprint 7 workflow foundation remains green;
12. retained Sprint 6 policy foundation remains green;
13. exact Java 21 compilation with warnings as errors and Maven core test compilation pass.

## Explicit Phase 1 exclusions

Phase 1 does not:

- validate OAuth authorization-server cryptography;
- execute OAuth authorization-code or refresh-token flows;
- brute-force login/authentication endpoints;
- replay real credentials;
- perform DPoP/mTLS active validation;
- manipulate production refresh tokens;
- infer principal identity from token possession;
- scan external targets;
- auto-confirm broken authentication findings.

## Verification

Pending GitHub Actions execution on `s10-auth-session-intelligence`.

Until that workflow passes, Phase 1 remains IMPLEMENTED / UNVERIFIED.
