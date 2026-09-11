# Changelog

## Unreleased — S5 Batch 1 evidence integrity

- Added evidence-reference validation on the existing `ExecutionEvidenceStore`, including explicit project scope, exact execution/test chain ownership, observation lineage, replay separation, and fail-closed reason codes.
- Added store-backed BOLA/BFLA and validator-backed correlation entry points; invalid references cannot raise supported assessment confidence through those paths.
- Added `Sprint5EvidenceIntegrityTestSuite` for invalid references, replay separation, immutability, correlation blocking, and serializer leakage. The requested Java 21 compile/test execution is blocked because only OpenJDK 17.0.8 was found, `javac` is absent from PATH, and Maven is unavailable; no new test pass count is claimed.
- Newly verified `acra-sprint-05-batch1-evidence-integrity-checkpoint.zip`: 604 files, safe paths, no build/cache artifacts, clean extraction equality, and a matching external SHA-256 sidecar.

## Unreleased — S5 Batch 2 offline policy validation

- Added explicit, offline policy review for tenant, workflow, and property authorization using the existing authorization context and Batch 1 evidence validator.
- Added deterministic conflict handling that preserves ALLOW/DENY contradictions without latest-wins, majority-wins, or confidence-wins precedence.
- Added secret-safe immutable `PolicyValidationResult` output and focused Batch 2 policy tests. Java 21 compilation and test execution remain blocked because only OpenJDK 17.0.8 is available and Maven is unavailable; no new test pass count is claimed.
- Newly verified Batch 2 checkpoint packaging: 609 entries, safe paths, no build/cache artifacts, clean extraction equality, and a matching external SHA-256 sidecar.

## Unreleased — S5 Batch 3 final integration, 2026-09-11

- Completed source-level defensive flow review from evidence validation through authorization context, BOLA/BFLA assessment, tenant/workflow/property review, correlation, and immutable result serialization.
- Closed the policy observation-reference boundary: policy reviews now fail closed when the observation is missing, unknown, wrong type, replay-mismatched, cross-project, or otherwise unverifiable in the existing evidence store.
- Added focused source assertions for unknown and forged observation references without creating duplicate Evidence, Observation, AuthorizationContext, Correlation, Provenance, or Replay models.
- Security review found no new raw password, API key, bearer token, refresh token, session secret, cookie, or Authorization-header storage path in Batch 3 changes.
- Java 21 compilation is BLOCKED because the discovered compiler is OpenJDK 17.0.8 and rejects `--release 21`; Maven is unavailable; no new test pass count is claimed. S5 remains SOFTWARE PARTIAL and S6 is not started.

## Unreleased — S5 defensive continuation, 2026-09-09

- Preserve the verified S5-04 source and interrupted working changes; no release version promotion.
- Reject incomplete/redacted context, evidence and provenance references in the existing assessment evaluators; remove fabricated BFLA endpoint content.
- Harden existing correlation against false corroboration, conflict suppression and order-dependent output.
- Sanitize recognized credential patterns in existing S5 records and sensitive serializer fields; retain immutable copies.
- Add three focused defensive test suites, Java 21 verification automation and cumulative ZIP verification/packaging.
- Correct current documentation to S5 SOFTWARE PARTIAL with source-backed gaps. S4 historical evidence is preserved; S6 is not started.

## v0.4.0-rc1 - Sprint 4 continuation candidate

### Phase 3 active-engine product completion

- added executable definitions for Authorization Audit, Routing Audit, Context Audit, Research Differential and Expert Custom profiles.
- verified `ALL`, `AUTOMATIC`, `USER_SELECTED` and `HYBRID` planner behavior.
- added Beginner, Professional, Researcher and Expert mode mappings that change real profile, selection, depth, limits, evidence and controls.
- completed and verified queue lifecycle/operations and prerequisite handling, including a minimal fix that blocks a missing dependency instead of leaving it queued indefinitely.
- added narrowly automatic consent for authorized, non-destructive ACRA-Lab/loopback execution while preserving every scope, environment, kill, budget, concurrency, rate and redaction guard.
- verified bounded 429, 503, Retry-After and connection-failure behavior; operational failures create no Observation.
- changed unknown expected policy interpretation to `INCONCLUSIVE` while retaining the separately observed semantic outcome.
- added active coverage and request-efficiency accounting without claiming an optimization percentage.
- extended the existing Swing suite tab with eight active views and backend-bound profile/mode/selection/limit/STOP ALL controls.
- added labelled FP/FN ground-truth definitions and research software records; broad metrics remain NOT MEASURED.
- added active-engine, planner, executor, differential and active-safety architecture documentation.
- newly executed 558 represented assertions across S4 graph/core/security/product/localhost/UI and retained core/S3/S2 regressions; the product suite includes configured workspace plan/queue/execution.
- final Phase 3 decision is **S4 SOFTWARE COMPLETE** for the local/synthetic software boundary. Burp provisioning/runtime, exact JDK 21 and research/performance/external validation remain separate; official Maven/Montoya execution is environment-blocked.

### Final software audit checkpoint

- verified recovery archive SHA-256 `db303eeac4b6ffe44b6756b667b748414797069d235cc541ec0e3326e55266bf` and audited 50 S4 requirements from extracted source bytes.
- added `RequestEquivalenceGuard` to pre-dispatch validation without redesigning `TestExecutor`.
- added executor-level proof: valid mutation dispatches once; invalid and type/location-contaminated mutations dispatch zero times.
- freshly compiled all core main/test sources with Java 21 and `-Xlint:all -Werror`.
- current S4 engine-security suite is 52 PASS; S4 core remains 54 PASS and live localhost remains 51 PASS.
- final audit decision: **S4 SOFTWARE PARTIAL**. Active graph hydration, active S4 UI, Beginner/Professional modes, verification/instrumentation breadth, official packaging and missing architecture/security documentation remain.

### Added / verified in localhost-integration slice

- isolated `LocalhostHttpTransport` behind the existing Sprint 4 transport interface; authorized LAB loopback only, redirects disabled.
- existing ACRA-Lab extended with `Document-A` / `Document-B`, independent `GT-EXEC-S4`, semantic HTTP-200 denial, dynamic/request-ID, formatting/order, echo and timeout fixtures.
- live expected-ALLOW, expected-DENY and single resource-mutation execution through the existing `TestExecutor`.
- live semantic differential, Observation and append-only evidence-chain verification with credential/cookie redaction.
- deliberately vulnerable local mutation retained as an observation and `UNEXPECTED_CHANGE`, not promoted to a finding.
- explicit localhost timeout operational mapping test.
- process-wide execution sequence repair after a live cross-executor ID collision was reproduced.
- Sprint 4 architecture gate isolating active networking from passive core.
- `EXP-EXEC-001` controlled local execution record.

### Verified locally

- Sprint 4 core: 54 PASS.
- Sprint 4 engine security: 41 PASS.
- Sprint 4 localhost integration: 51 PASS.
- Core regression: 37 PASS.
- Sprint 3 core: 47 PASS.
- Sprint 3 adapter: 11 PASS.
- Sprint 3 security/architecture: PASS.
- Sprint 4 architecture: PASS.

### Still blocked / unverified

- active Observation/Evidence → Security Context Graph hydration.
- large FP/FN, request-efficiency and performance campaigns.
- active UI verification.
- official Maven/Montoya packaging.
- current Burp runtime validation; historical S2/S3 Level 3/4 evidence remains unchanged.

The repository `VERSION` remains governed by the existing Sprint 3 Burp-promotion hold; `0.4.0-rc1` is the Sprint 4 continuation candidate, not a final release promotion.

## v0.3.0-rc1 - Sprint 3 release candidate

### Added

- semantic identifier intelligence for user, tenant, resource, organization, role and workflow identifiers.
- query/body parameter and security-header classification.
- API version discovery from path, query, headers and media-type hints.
- resource relationship intelligence and hybrid identity-confirmation/manual-context contracts.
- deterministic route grammar, canonical templates and route-family equivalence.
- dependency-free OpenAPI 3.x / Swagger 2.0 JSON importer plus conservative common YAML subset.
- declared-vs-observed OpenAPI correlation and schema-drift observation model.
- response semantic fingerprints for fields, resources, owners, tenants, volatile values and error-like responses.
- collection/pagination intelligence and semantic same-resource matching.
- AuthorizationMatrix, SecurityContextFingerprint, DifferentialTest and FindingFingerprint foundations.
- Context Coverage and endpoint test-priority model, explicitly separated from severity.
- zero-dispatch reconnaissance-to-test dry-run planner.
- per-transaction reconnaissance store integrated into Sprint 2 passive traffic pipeline.
- Burp UI source tabs for Parameters, Identities, Tenants, Resources, Routes and Context Graph.
- expanded 10-operation ACRA-Lab reconnaissance fixture, OpenAPI 3.1 document and GT-RECON-S3.
- Sprint 3 controlled experiment set and observational performance baseline.

### Verified locally

- Sprint 1 regression: 37 PASS.
- Sprint 2 regression: 52 PASS.
- Sprint 3 core: 47 PASS.
- Sprint 3 adapter: 11 PASS.
- Total executable assertions: 147 PASS.
- Security and architecture gates: PASS.
- local 10-operation reconnaissance experiment: PASS, 10/10 documented-observed.
- controlled fixture metrics executed and recorded.

### Blocked / unverified

- official Maven/Montoya dependency build because Maven and external artifact resolution are unavailable in this environment.
- real Burp extension/runtime/UI validation.
- real Burp Sprint 3 reconnaissance experiment.

The candidate is intentionally not tagged final `v0.3.0` until mandatory live Burp validation passes.

## v0.2.0-rc1 - Sprint 2 release candidate

### Added

- Burp Montoya adapter source targeting Montoya API 2026.7.
- Passive HTTP request/response observation and ACRA transaction correlation.
- Scope controller and safety/configuration primitives.
- Passive authentication, identity/session, tenant, resource, action and context reconstruction.
- Endpoint-family inventory and evidence-backed global Security Context Graph updates.
- Observation/evidence timeline distinct from vulnerability findings.
- Deterministic response normalization and passive differential primitives.
- ACRA Burp Swing tabs for Overview, Traffic, Contexts, Endpoints and Configuration.
- Disabled active-request executor and scanner integration boundary.
- Basic secure/vulnerable ACRA-Lab fixtures, Docker Compose specification and GT-INTEGRATION-001.
- EXP-INTEGRATION-001 local live-HTTP execution and 100/1k/10k observational performance baseline.
- Maven multi-module POM targeting Java 21 with Montoya API as a provided dependency.

### Verified locally

- Sprint 1 regression: 37 PASS.
- Sprint 2 suite: 52 PASS.
- Security and architecture checks: PASS.
- Direct local ACRA-Lab context reconstruction: PASS.

### Blocked / unverified

- official Maven dependency package and real Burp Level 3/4 runtime evidence.

## v0.1.0 - Sprint 1

### Added

- Standalone Java 21 `acra-core` domain engine decoupled from Burp/Montoya.
- HTTP transaction, URI representation, path segment and identifier-candidate models.
- Principal, session, role, tenant, resource, action, workflow and authorization-context models.
- Evidence provenance with deterministic IDs and confidence basis.
- Security Context Graph with directional evidence-bound edges and direct adjacency queries.
- Conservative identity, tenant, resource and action extraction foundation.
- Explicit UNKNOWN and CONFLICTING_EVIDENCE handling.
- Endpoint/API inventory records, entity resolution, TestCase/TestState and ground-truth contracts.
- Plugin interfaces for analyzers, mutation/evidence providers, API parsers, workflow detectors and reporters.
- Deterministic schema-v1 serialization with universal credential redaction and SHA-256 fingerprints.
- JDK-only build, test, security and performance verification scripts plus GitHub Actions workflow.
- 37 automated tests and 1k/10k/100k observational performance baseline.

## Sprint 0

- Initial RB-0 repository structure and governance foundation.
