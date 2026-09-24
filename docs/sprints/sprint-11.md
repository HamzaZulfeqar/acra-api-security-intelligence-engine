# Sprint 11 — OAuth/OIDC Protocol & Authorization-Server Context Intelligence

Status: IN PROGRESS — Phases 1–2 VERIFIED COMPLETE  
Branch: `s11-oauth-oidc-intelligence`  
Immutable Sprint 10 promoted base: `7ada67e42c20fcdac21e3af96a9ed80dda422192`

## Dependency decision

Sprint 10 is SOFTWARE COMPLETE.

No historical file assigns a fixed Sprint 11 title. The next scope is derived from explicit unresolved repository
architecture:

- ADR-0012 remained open for JWT / Session / OAuth / OIDC scope;
- ADR-0013 deliberately kept OAuth/OIDC-specific analysis outside the early REST baseline;
- Sprint 10 established provenance-aware session/token-context intelligence as the prerequisite layer;
- the original product architecture retained OAuth/OIDC as a future extensibility target.

Sprint 11 therefore begins with passive OAuth/OIDC protocol-context intelligence. It does not begin with grant
manipulation or external authorization-server testing.

## Planned dependency order

1. passive OAuth/OIDC context model and deterministic drift correlation;
2. evidence-store/project/request provenance binding;
3. passive extraction using existing traffic/parser boundaries;
4. controlled local OAuth/OIDC ground truth;
5. metadata/policy assessment with explicit inconclusive states;
6. coverage accounting;
7. read-only product workspace and Burp UI;
8. deterministic reporting/export;
9. security hardening and bounded observations;
10. final regression and reproducible closure.

Each later phase remains subject to successful verification of the preceding dependency.

## Phase 1 — passive protocol-context foundation

Candidate implementation:

- `OAuthProtocol`;
- `PkceMethod`;
- `OAuthContextDimension`;
- `OAuthContextCorrelationState`;
- `OAuthContextObservation`;
- `OAuthContextCorrelation`;
- `OAuthContextCorrelator`;
- SHA-256 redirect-URI correlation handle instead of raw redirect URI;
- deterministic context and correlation IDs;
- issuer/client context separation;
- explicit issuer/redirect/resource/audience/scope/response-type/grant-type/PKCE/state/nonce drift dimensions;
- inferred identity remains unverified;
- no finding/vulnerability promotion;
- no active OAuth/OIDC request generation.

## Phase 1 acceptance boundary

Phase 1 must prove:

1. inferred OAuth/OIDC context does not verify identity;
2. equivalent context correlates as STABLE;
3. issuer/redirect/resource/audience/scope/response-type/PKCE/state/nonce changes are explicit drift dimensions;
4. different client contexts remain INCONCLUSIVE rather than merged;
5. raw redirect URI input is rejected where a SHA-256 handle is required;
6. secret/query material in issuer metadata fails closed;
7. provenance evidence is mandatory;
8. context/correlation identities are deterministic;
9. retained Sprint 10 session foundation remains green;
10. retained Sprint 9 and Sprint 8 foundations remain green;
11. exact Java 21 warnings-as-errors and Maven core test compilation pass.

## Explicit Phase 1 exclusions

Phase 1 does not:

- validate JWT/JWS signatures without cryptographic key evidence;
- fetch discovery metadata or JWKS from external providers;
- manipulate authorization codes or grants;
- replay refresh/access tokens;
- mutate redirect URIs;
- probe external authorization servers or identity providers;
- infer a vulnerability from missing PKCE/state/nonce alone;
- auto-confirm findings;
- claim OAuth/OIDC standards-complete validation.

## Phase 1 verification

GitHub Actions run `36042624760`: **SUCCESS**.

- `Sprint11OAuthOidcFoundationTestSuite`: PASS, 23 assertions;
- exact Java 21 compilation with `-Xlint:all -Werror`: PASS;
- retained Sprint 10 / Sprint 9 / Sprint 8 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

## Phase 2 — immutable OAuth evidence binding

Implemented:

- `OAuthEvidenceBinding`;
- `S11OAuthEvidenceValidator`;
- reuse of the existing `ExecutionEvidenceStore`;
- project ownership validation;
- execution and test ownership validation;
- passive request-ID lineage validation;
- OAuth observation-object type validation;
- exact observation/evidence-set consistency;
- duplicate and unknown evidence rejection;
- cross-project evidence rejection;
- no changes to the generic HTTP or Sprint 10 session evidence validators.

### Phase 2 verification

GitHub Actions run `36042861693`: **SUCCESS**.

- Sprint 11 Phase 1 foundation: PASS;
- `Sprint11OAuthEvidenceBindingTestSuite`: PASS, 20 assertions;
- project/request/test/execution lineage: PASS;
- cross-project rejection: PASS;
- unknown and duplicate evidence rejection: PASS;
- wrong observation type rejection: PASS;
- contradictory evidence-set rejection: PASS;
- retained Sprint 10 / 9 / 8 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 2 is **VERIFIED COMPLETE**.

Sprint 11 remains IN PROGRESS. Phase 3 owns passive OAuth/OIDC extraction through existing HTTP traffic/parser
boundaries; it must not create a second traffic collector or fetch external provider metadata.
