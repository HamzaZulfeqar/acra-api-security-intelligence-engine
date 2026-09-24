# Sprint 10 — Authentication, Session & Token-Context Intelligence

Status: SOFTWARE COMPLETE  
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

## Phase 5 — refresh/rotation and verified context-drift assessment

Implemented:

- `SessionSecurityAssessmentState`;
- `SessionSecurityAssessment`;
- `S10SessionSecurityAssessmentEvaluator`;
- baseline, stable, safe-rotation, candidate and inconclusive assessment states;
- provenance required before any candidate promotion;
- verified principal/role/tenant/scope drift preserved as explicit dimensions;
- safe context-preserving token rotation remains `SAFE_ROTATION`;
- unverified identity differences remain `INCONCLUSIVE`;
- invalid or missing provenance remains `INCONCLUSIVE`;
- candidate rationale explicitly preserves analyst review and non-confirmation boundary.

### Phase 5 verification

GitHub Actions run `36032923048`: **SUCCESS** at commit
`6ae158a46c50663e43910041abeaeaa62a433b2a`.

Verified:

- first observation → BASELINE: PASS;
- stable verified session → STABLE: PASS;
- verified context-preserving token renewal → SAFE_ROTATION: PASS;
- verified role/tenant/scope drift → CANDIDATE: PASS;
- unverified context drift → INCONCLUSIVE: PASS;
- invalid/missing provenance → INCONCLUSIVE: PASS;
- deterministic assessment identity: PASS;
- full Sprint 10 Phases 1–4 verification: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 5 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 6 owns projection into the existing review-only `FindingCandidate` model.

## Phase 6 — provenance-gated session FindingCandidate projection

Implemented:

- `S10SessionFindingRequest`;
- `S10SessionFindingCandidateEvaluator`;
- reuse of the existing `FindingCandidate` / `FindingFingerprint` contracts;
- Phase 2 session-specific evidence lineage required before promotion;
- safe verified token rotation → FindingCandidate `REJECTED`;
- verified context-drift assessment → review-only FindingCandidate `CANDIDATE`;
- cross-project provenance → `INCONCLUSIVE`;
- assessment/correlation/request attribution mismatch → `INCONCLUSIVE`;
- session dimensions preserve token rotation plus principal/role/tenant/scope drift;
- explicit SEC-003 rule reference retained;
- candidate rationale explicitly states that it is not an automatically confirmed vulnerability.

### Phase 6 verification

GitHub Actions run `36033267339`: **SUCCESS** at commit
`2521cb7fa21193fc177f2cfc08f6a44352fe3f3b`.

Verified:

- context-preserving token rotation → REJECTED: PASS;
- provenance-valid verified context drift → CANDIDATE: PASS;
- drift dimensions preserved: PASS;
- cross-project session evidence → INCONCLUSIVE: PASS;
- attribution mismatch → INCONCLUSIVE: PASS;
- deterministic candidate ID/fingerprint: PASS;
- full Sprint 10 Phases 1–5 verification: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 6 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 7 owns explicit session-context coverage accounting; absence of observation
must remain visible rather than being treated as stable or secure.

## Phase 7 — explicit session-context coverage accounting

Implemented:

- `SessionCoverageObjective`;
- `SessionCoverageDisposition`;
- `SessionCoverageTarget`;
- `SessionCoverageEntry`;
- `SessionCoverageSummary`;
- `S10SessionCoverageTracker`;
- explicit configured coverage universe instead of traffic-derived denominator;
- baseline, continuity and rotation-context-stability objectives;
- lifecycle states: UNOBSERVED, OBSERVED_UNCORRELATED, CORRELATED_UNASSESSED, REJECTED, INCONCLUSIVE, CANDIDATE;
- duplicate target registration does not inflate denominator;
- session mismatch and non-rotation correlation for rotation objectives fail closed;
- deterministic coverage ordering and gap ID discovery.

During Phase 7 verification, the existing universal redactor correctly sanitized `session:<value>` as
secret-like key/value material. Sprint 10 therefore changed FindingCandidate resource identity to
`auth-context:<sessionId>` rather than weakening global redaction.

### Phase 7 verification

GitHub Actions run `36033819097`: **SUCCESS** at commit
`538b146b1e9e57334e6d16fe85da6d81ab9ca765`.

Verified:

- explicit target denominator: PASS;
- duplicate registration does not inflate coverage: PASS;
- unobserved context preservation: PASS;
- observed-uncorrelated preservation: PASS;
- correlated-unassessed preservation: PASS;
- candidate/rejected/inconclusive assessed states: PASS;
- session mismatch rejection: PASS;
- rotation-objective/non-rotation rejection: PASS;
- deterministic coverage ordering: PASS;
- redaction-safe `auth-context:` finding resource identity: PASS;
- full Sprint 10 Phases 1–6 verification: PASS;
- retained Sprint 6/7/8/9 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 7 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 8 owns the read-only authentication/session product workspace and Burp UI.

## Phase 8 — authentication/session product workspace and Burp UI

Implemented:

- `S10SessionProductSnapshot`;
- `S10SessionWorkspace`;
- `S10SessionPanel`;
- top-level Burp `Authentication` product area;
- Overview / Sessions / Correlations / Assessments / Candidates / Coverage views;
- backward-compatible `AcraSuiteTab` constructor chain with optional injected Sprint 10 workspace;
- read-only projection of session observations, correlation state, review-only assessments/findings and coverage;
- raw token and token-fingerprint values intentionally omitted from all Sprint 10 UI table models;
- redaction-safe `auth-context:` candidate resource identity retained;
- runtime-state clear preserves explicit coverage targets as UNOBSERVED;
- no new active authentication controls introduced by the Sprint 10 UI.

### Phase 8 verification

GitHub Actions run `36034257857`: **SUCCESS** at commit
`93718c2b0ac7510c2a265608901d92db4406fe3b`.

Verified:

- full Sprint 10 core suite: PASS;
- Maven core test compilation: PASS;
- real extension Maven test compilation: PASS;
- retained Sprint 4 UI: PASS;
- retained Sprint 6 Authorization UI: PASS;
- retained Sprint 7 Workflow UI: PASS;
- retained Sprint 8 Routing UI: PASS;
- retained Sprint 9 Property UI: PASS;
- Sprint 10 Authentication UI: PASS;
- explicit coverage denominator/gaps rendered: PASS;
- review-only candidate / rejected-control states rendered: PASS;
- raw synthetic token not rendered: PASS;
- SHA-256 token fingerprint not rendered: PASS;
- redaction-safe auth-context resource identity rendered without weakening secret redaction: PASS.

Phase 8 is **VERIFIED COMPLETE**.

Sprint 10 remains IN PROGRESS. Phase 9 owns deterministic secret-safe JSON/SHA-256 and Markdown report/export
generated from the same `S10SessionWorkspace` source of truth.


## Phase 9 — deterministic authentication/session report and export

Implemented:

- versioned `S10SessionReport` and `S10SessionReportSummary`;
- deterministic report identity independent of presentation timestamp;
- secret-minimized observation/correlation/assessment/candidate/coverage projections;
- raw session identifiers replaced by deterministic `authctx-<hash>` references;
- raw bearer material and token fingerprints excluded from the report schema;
- canonical JSON export + SHA-256;
- deterministic Markdown review export;
- `S10SessionJsonReporter` adapter;
- Report / JSON Export projections in the existing Authentication product area;
- `confirmedFindingCount = 0` enforced by the report contract.

An intermediate test failure exposed a fixture substring collision: the synthetic raw session ID `session-r`
was a substring of the original report-version literal. The report version was renamed to
`s10-auth-context-report-v1` rather than weakening the session secrecy assertions or universal redaction.

### Phase 9 verification

GitHub Actions run `36040768002`: **SUCCESS** at commit
`2ce4895c017a022fff81d16346f10c7155c92ef5`.

Verified:

- complete Sprint 10 core/reporting verification: PASS;
- deterministic JSON/SHA-256 export: PASS;
- deterministic Markdown export: PASS;
- raw bearer exclusion: PASS;
- token-fingerprint exclusion: PASS;
- raw session-identifier exclusion: PASS;
- Maven core test compilation: PASS;
- extension test compilation: PASS;
- retained Sprint 4 / 6 / 7 / 8 / 9 UI regressions: PASS;
- Sprint 10 Authentication UI including Report / JSON Export: PASS.

Phase 9 is **VERIFIED COMPLETE**.

## Phase 10 — security hardening and bounded performance observations

Implemented candidate verification:

- reject non-SHA-256 authentication-token fingerprints;
- reject secret-bearing session metadata;
- preserve INFERRED identity as unverified;
- preserve USER_CONFIRMED identity as verified;
- reject cross-session coverage observation binding;
- reject non-session FindingCandidate projections from the session workspace;
- verify JSON/report exclusion of raw session IDs and token fingerprints;
- enforce report `confirmedFindingCount = 0`;
- bounded 100 / 1,000 / 10,000 explicit session-coverage workspace/report observations;
- persist a CI performance-observation CSV artifact.

These timing/memory values are engineering observations only and are not benchmarks, SLOs, release thresholds,
scanner-accuracy evidence or real-world capacity claims.

Phase 10 verification is pending the current GitHub Actions gate.


## Final closure — S10 SOFTWARE COMPLETE

GitHub Actions run `36041511898` passed the dedicated Sprint 10 final closure workflow at source commit
`2a7e753a3866ffb869df17abb53a9d2cc93dd876`.

Final verification included:

- exact Temurin Java 21 Sprint 10 verification;
- all authentication/session foundation, evidence, controlled-lab, hydration, assessment, finding, coverage,
  reporting, security-hardening and performance-observation suites;
- retained Sprint 9 / Sprint 8 / Sprint 7 / Sprint 6 foundations;
- official Maven package;
- retained Sprint 2 and Sprint 3 local-contract regressions;
- Sprint 4 / 6 / 7 / 8 / 9 / 10 headless UI regressions;
- deterministic source checkpoint generation;
- archive safe-path and duplicate-entry validation;
- clean-extraction equality;
- per-file SHA-256 equality.

Closure-candidate checkpoint: `acra-sprint-10-final.zip`  
Closure-candidate SHA-256: `3e1352ca886096ff2df01d6e8fdbc3f281eb29794651ff6bba23205b7a70d26b`  
Entries: **924**  
Unsafe paths: **0**  
Duplicate entries: **0**  
Clean extraction: **PASS**  
Per-file SHA-256 equality: **PASS**

Sprint 11 is **NOT STARTED** by this closure.

Real Burp desktop runtime/load/handler/UI validation remains a separate **UNVERIFIED / DEFERRED** validation
lane. Controlled localhost and headless UI evidence do not establish production OAuth/session behavior,
external-target safety, real-world scanner accuracy/capacity, or automatic confirmed vulnerabilities.
