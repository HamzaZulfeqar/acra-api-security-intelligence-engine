# Sprint 10 — Authentication, Session & Token-Context Intelligence

Status: IN PROGRESS — Phase 1 VERIFIED COMPLETE  
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

## Phase 1 verification

GitHub Actions run `36031651777`: **SUCCESS** at commit
`e520a631e667fb5885230b094aa9045e6dfd7bf7`.

Verified gates:

- exact Temurin Java 21 setup: PASS;
- core source/test compilation with `-Xlint:all -Werror`: PASS;
- `Sprint10SessionContextFoundationTestSuite`: PASS;
- stable verified session correlation: PASS;
- safe token rotation with stable context: PASS;
- verified principal/role/tenant/scope drift detection: PASS;
- unverified token identity fails closed: PASS;
- different-session boundary: PASS;
- raw authentication material rejection: PASS;
- retained Sprint 9 property foundation: PASS;
- retained Sprint 8 routing foundation: PASS;
- retained Sprint 7 workflow foundation: PASS;
- retained Sprint 6 policy foundation: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 2 owns project/test/execution/evidence provenance binding for passive
authentication/session observations.

## Phase 2 — session evidence provenance binding

Implemented:

- `SessionEvidenceBinding`;
- `S10SessionEvidenceValidator`;
- project ownership validation;
- execution/test ownership validation;
- session observation object/type/stage validation;
- evidence-object lineage validation;
- duplicate and unknown evidence rejection;
- observation/evidence-set contradiction rejection;
- cross-project fail-closed behavior;
- existing generic S4 `EvidenceReferenceValidator` left unchanged.

### Phase 2 verification

GitHub Actions run `36031909153`: **SUCCESS** at commit
`4646bbf434422a6b90e3a8b2947cd519c869812a`.

Verified:

- Sprint 10 Phase 1 foundation: PASS;
- session evidence binding suite: PASS;
- valid project/test/execution lineage: PASS;
- cross-project rejection: PASS;
- execution/test mismatch rejection: PASS;
- unknown and duplicate evidence rejection: PASS;
- wrong observation type rejection: PASS;
- contradictory evidence-set rejection: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 2 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 3 owns controlled synthetic ACRA-Lab token/session rotation ground truth.

## Phase 3 — controlled synthetic session-rotation ground truth

Implemented:

- `GT-S10-AUTH-SESSION-CONTEXT.json` with four independently declared cases;
- read-only `/api/v1/s10/session-context` ACRA-Lab endpoint;
- synthetic LAB_CONFIRMED session contexts only;
- raw bearer token never returned by the lab endpoint;
- live baseline observation;
- stable-context token rotation;
- role/tenant/scope drift during token rotation;
- different-session boundary control;
- each live observation stored in `ExecutionEvidenceStore`;
- Phase 2 provenance validation before Phase 1 correlation;
- `Sprint10ControlledSessionContextTestSuite`.

### Phase 3 verification

GitHub Actions run `36032231484`: **SUCCESS** at commit
`1fd2d12604bbfdc475b01460683a95150571f290`.

Verified:

- Python lab syntax: PASS;
- four-case Sprint 10 session ground-truth contract: PASS;
- localhost lab readiness: PASS;
- live synthetic session context capture: PASS;
- raw token non-disclosure: PASS;
- immutable evidence binding for every session observation: PASS;
- stable token rotation → TOKEN_ROTATED: PASS;
- verified role/tenant/scope drift → CONTEXT_DRIFT: PASS;
- different session IDs → INCONCLUSIVE: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 3 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 4 owns passive traffic/session hydration using the existing collection and
reconnaissance model; it must not create a parallel traffic collector.

## Phase 4 — passive traffic/session hydration

Implemented:

- `PassiveSessionHydrationResult`;
- `S10PassiveSessionHydrator`;
- reuse of existing `SecurityContextEngine`, `IdentityExtraction` and `IdentityConfirmationRegistry`;
- no second traffic collector or token parser;
- session ID sourced from collector metadata when available;
- collector-provided scope metadata normalized deterministically;
- JWT principal/role/tenant claims remain INFERRED until separately confirmed;
- matching USER_CONFIRMED / LAB_CONFIRMED mappings upgrade the hydrated identity state;
- confirmation/claim mismatch is downgraded to SUSPECTED;
- missing session ID remains request-scoped unresolved context;
- no credential evidence produces no session observation;
- raw bearer values excluded from the hydrated observation.

### Phase 4 verification

GitHub Actions run `36032676285`: **SUCCESS** at commit
`f2026b4866f5117d758631b620c829c20dc230ff`.

Verified:

- passive JWT claims without confirmation → INFERRED: PASS;
- matching confirmation → USER_CONFIRMED: PASS;
- confirmation/claim mismatch → SUSPECTED: PASS;
- missing session ID fail-closed behavior: PASS;
- no credential → no session observation: PASS;
- raw bearer exclusion / SHA-256 correlation handle retention: PASS;
- full Sprint 10 Phases 1–3 verification: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 4 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 5 owns explicit refresh/rotation and verified context-drift assessment.
