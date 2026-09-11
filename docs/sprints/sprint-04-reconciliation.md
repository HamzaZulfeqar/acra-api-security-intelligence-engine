# Sprint 4 Repository-Driven Reconciliation

**Project:** API Access Control & Routing Auditor (ACRA)  
**Baseline package:** `acra-sprint-03-v0.3.0-rc1.zip`  
**Baseline version:** `0.3.0-rc1`  
**Sprint 4 candidate:** `0.4.0-rc1`  
**Reconciliation date:** 2026-08-31  
**Gate result:** COMPLETE — implementation may proceed only for the gaps recorded here

## 1. Intake and integrity evidence

The supplied archive was treated as the canonical implementation baseline and was not edited.

| Check | Result |
|---|---|
| Source package | `C:\Users\hamzi\Downloads\API PATH AND BOLA SECURITY SCANNER\Completed Sprints\acra-sprint-03-v0.3.0-rc1.zip` |
| Size | 280,331 bytes |
| SHA-256 | `EF9EC889ABA9C7C6BFBCE4FB3078140901143BAD56F31852169AD486DF143C4F` |
| ZIP entries | 414 |
| Top-level root | `acra/` |
| Unsafe absolute or traversal entries | 0 |
| Extracted immutable inspection copy | `work/sprint-03-baseline/acra` |
| Sprint 4 working copy | `work/acra-sprint-04-v0.4.0-rc1/acra` |

## 2. Classification rules

| State | Meaning in this report |
|---|---|
| `EXISTS` | Working implementation with directly relevant executable tests or reproduced evidence. |
| `PARTIAL` | Relevant implementation exists but does not satisfy the complete Sprint 4 contract. |
| `WEAK` | Implementation exists, but its current safety or architectural properties are insufficient for active use. |
| `MISSING` | No meaningful implementation exists in the baseline. |
| `UNVERIFIED` | Code or a claim exists without sufficient executable evidence in this environment. |
| `BLOCKED` | Validation cannot currently run because a required runtime or dependency gate is unavailable. |

`UNVERIFIED` and `BLOCKED` are not treated as PASS.

## 3. Repository inspection performed

The reconciliation inspected the complete 414-entry tree and read the implementation and records relevant to continuation:

- root state and governance: `PROJECT_STATE.md`, `sprint-status.md`, `CHANGELOG.md`, `DECISIONS.md`, `KNOWN_ISSUES.md`, `TEST_MATRIX.md`, `EXPERIMENT_REGISTRY.md`, `ROADMAP.md`, `VERSION`, and `REPOSITORY_MANIFEST.txt`;
- Sprint records: `docs/sprints/SPRINT-000.md` through `SPRINT-003.md`, the roadmap, and the continuation protocol;
- verification evidence: Sprint 1–3 verification, performance, metrics, lab, dependency, environment, and Burp-status artifacts under `docs/testing/artifacts/`;
- research records: methodology, ground-truth compatibility, registry, novelty ledger, and all experiment records under `experiments/`;
- architecture and security: all files under `docs/architecture/`, `docs/security/`, and all ADRs through ADR-0035 plus the ADR index;
- build and configuration: all Maven POMs, workflow definitions, scripts, configuration placeholders, schemas, and lab Compose configuration;
- implementation: all core, Burp adapter, UI, test, lab, schema, and example source files;
- tests: all core, Sprint 2, Sprint 3, adapter, lab, performance, negative-security, and serialization-security sources;
- ACRA-Lab: secure and vulnerable services, shared fixture implementation, OpenAPI fixture, test contexts, and ground truth.

No repository-level `AGENTS.md` instruction file is present.

## 4. Reproduced baseline evidence

The canonical source was copied to an isolated validation directory before execution. The repository Bash verifier was also attempted and failed on Windows because it uses Unix `:` classpaths; that is a verifier-portability issue, not a Java source failure. Equivalent commands were then run with Windows classpaths and the installed OpenJDK compiler using `--release 21 -Xlint:all -Werror`.

| Gate rerun on 2026-08-31 | Result |
|---|---|
| Core compile and test compile | PASS |
| Montoya stub, adapter, and adapter-test compile | PASS |
| Sprint 1/core regression assertions | PASS, 37 |
| Sprint 2 adapter assertions | PASS, 52 |
| Sprint 3 core assertions | PASS, 47 |
| Sprint 3 adapter assertions | PASS, 11 |
| Total baseline executable assertions | PASS, 147 |
| Sprint 3 secure ACRA-Lab reconnaissance | PASS, 10/10 operations observed and documented |
| Sprint 3 dry-run dispatch | PASS, 0 requests |
| Sprint 3 controlled metrics | Reproduced exactly within fixture scope |
| Repository Bash verifier on Windows | BLOCKED by non-portable classpath syntax |
| Official Maven dependency build | BLOCKED / UNVERIFIED; Maven is not installed |
| Real Burp runtime validation | UNVERIFIED in this reconciliation; not executed |

The current host contains Burp Suite and a bundled OpenJDK 26 toolchain, unlike the historical Sprint 2/3 execution environment. That discovery does not retroactively change stored S2/S3 evidence.

## 5. Sprint 3 components that must be reused

| Required abstraction | Existing repository component | Baseline state | Sprint 4 rule |
|---|---|---|---|
| URI | `UriModel`, `DefaultUriExtractor`, `IdentifierDetector` | EXISTS | Reuse raw/decoded/normalized representations. |
| Endpoint | `Endpoint`, `ApiEndpointRecord` | EXISTS | Extend by reference; do not create a competing endpoint model. |
| Security Context | `AuthorizationContext`, `SecurityContextSnapshot`, `SecurityContextEngine` | EXISTS | Use as the source and target context substrate. |
| Security Context Graph | `SecurityContextGraph`, graph nodes/edges/query | EXISTS | Query or reference it; do not duplicate the graph. |
| Evidence | `Evidence`, evidence timeline, graph evidence registry | EXISTS | Extend with execution-chain references and append-only active evidence. |
| Tenant | `Tenant`, tenant extractor and recon intelligence | EXISTS | Preserve evidence and confidence. |
| Resource / owner | `Resource`, resource extractor, relationships and semantic matcher | EXISTS | Preserve type, ID, tenant, owner, source and confidence. |
| Identifier | `IdentifierCandidate`, `SemanticIdentifier` and classifiers | EXISTS | Use for controlled substitutions. |
| Authorization matrix | `AuthorizationMatrix`, `AuthorizationMatrixEntry` | EXISTS | Use as one expected-decision source, not the highest-precedence source. |
| Security-context fingerprint | `SecurityContextFingerprint` | EXISTS | Embed in test and execution fingerprints. |
| Response fingerprint | `ResponseSemanticFingerprint`, `ResponseSemanticAnalyzer` | EXISTS | Reuse; no second semantic response implementation. |
| Route / URI equivalence | `RouteTemplateModel`, `RouteTemplateEngine`, `RouteEquivalenceEngine` | EXISTS | Adapt for bounded representation mutations only. |
| OpenAPI | `OpenApiDocument`, importer, correlator and drift analyzer | EXISTS | Treat specification data as evidence. |
| Dry run | `DryRunPlan`, `PlannedTest`, `ReconTestPlanner` | EXISTS | Convert selected recommendations into reviewed active tests; preserve zero-dispatch behavior of S3 APIs. |
| Risk priority | `EndpointRiskAssessment`, `EndpointRiskPrioritizer` | EXISTS | Use only for deterministic ordering, never as vulnerability severity. |
| API inventory | `EndpointInventory`, `EndpointAggregate` | EXISTS | Planner input. |
| Semantic response | semantic analyzer and resource matcher | EXISTS | Differential input. |
| Context coverage | `ContextCoverage` | EXISTS | Prioritization and test sufficiency input. |

## 6. Sprint 4 requirement-to-code map

### S4-00 through S4-13 — test definition, planning and request construction

| ID | Capability | Existing evidence | State | Verified gap / action |
|---|---|---|---|---|
| S4-00 | Repository reconciliation | Continuation protocol exists; this report did not | MISSING at intake | This report satisfies the pre-implementation gate. |
| S4-00A | Requirement classification | No Sprint 4 classification record | MISSING at intake | Use this table as the canonical classification. |
| S4-00B | Reuse Sprint 3 | All named S3 abstractions are present | EXISTS | Extend the components listed in section 5. |
| S4-00C | Preserve S2/S3 debt | State, sprint, known-issue and experiment records retain it | EXISTS | Do not rewrite historical evidence. |
| S4-01 | Serializable `SecurityTest` | `TestCase` and `DifferentialTest` provide small foundations | PARTIAL | Add one immutable, reproducible active-test contract with all specified metadata. |
| S4-02 | Category contract | `TestFamily` has future analyzer families | PARTIAL | Add Sprint 4 category/subcategory taxonomy without claiming every family is implemented. |
| S4-03 | Ordered `TestPlanner` | `ReconTestPlanner` emits zero-dispatch recommendations | PARTIAL | Add an active-plan adapter that consumes S3 intelligence, explains selection, and obeys budgets/safety. |
| S4-04 | Selection modes | No automatic/user/hybrid/all model | MISSING | Add deterministic selection modes; safe hybrid is default. |
| S4-05 | Test profiles | `TargetProfile` exists, but no test-profile contract | MISSING | Add named profiles with safe bounded settings. |
| S4-06 | Baseline acquisition | Immutable `HttpRequest`/`HttpResponse` and semantic fingerprint exist | PARTIAL | Add immutable baseline request/response snapshots and acquisition stage. |
| S4-07 | Positive control | `DifferentialTest.controlRef` is not an explicit positive control | PARTIAL | Model and execute a known-valid control independently. |
| S4-08 | Negative control | No explicit negative-control model | MISSING | Model and execute a known-denied control independently. |
| S4-09 | Mutation contract | Mutation maps and `MutationProvider` are generic placeholders | PARTIAL | Add typed, single-dimension, deduplicated mutation metadata. |
| S4-10 | Context substitution | Context intelligence exists; substitution does not | MISSING | Add explicit identity/role/tenant/resource source-to-target substitutions. |
| S4-11 | Resource substitution | Resource/identifier intelligence exists | PARTIAL | Add mutation adapter while preserving provenance and confidence. |
| S4-12 | URI mutation adapter | Route and URI equivalence engines exist | PARTIAL | Add bounded representation-only variants; defer Sprint 8 routing research. |
| S4-13 | Request builder | No active request builder | MISSING | Build baseline/positive/negative/mutation requests from immutable S3 HTTP models. |

### S4-14 through S4-28 — safety, budgets, queue and execution

| ID | Capability | Existing evidence | State | Verified gap / action |
|---|---|---|---|---|
| S4-14 | Mutation validation pipeline | Disabled executor checks scope superficially | WEAK | Add ordered target, environment, safety, scope, budget, rate and confirmation validation. |
| S4-15 | Hard scope guard | `ScopeController`/`ScopeRule` support passive filtering | WEAK | Add non-bypassable project/environment/scheme/host/port/path/method active policy. Do not use `ALL_TRAFFIC` for active execution. |
| S4-16 | Environment guard | `TargetEnvironment` lacks authorized dev/QA distinctions and enforcement | PARTIAL | Add LAB, AUTHORIZED_DEV/QA/STAGING, UNKNOWN and OUT_OF_SCOPE rules; no production mode. |
| S4-17 | Active consent | `activeExecutionEnabled` and `userConfirmed` exist as booleans | PARTIAL | Require active enabled, target authorization, test approval, and destructive confirmation. |
| S4-18 | Global kill switch | A flag exists in disabled safety controls | PARTIAL | Add stateful STOP ALL with audit, queue cancellation and dispatch rejection. |
| S4-19 | Multi-level request budgets | Only one zero-valued maximum exists | MISSING | Add global/project/target/endpoint/test/context reservation and execution accounting. |
| S4-20 | Mutation budget | Only a maximum field exists | PARTIAL | Track generated, deduplicated, skipped, executed and failed. |
| S4-21 | Concurrency control | One configuration limit exists; no controller | PARTIAL | Add global, host, target and endpoint reservation controls with safe defaults. |
| S4-22 | Rate limits | Requests/second is configuration only | PARTIAL | Add scoped rate-limit enforcement for target/host/endpoint/test. |
| S4-23 | Backoff | No 429/503/Retry-After or transient-failure policy | MISSING | Add bounded operational backoff kept separate from security conclusions. |
| S4-24 | Execution queue | `TestState` and `ExecutionHandle` define some states; no queue | PARTIAL | Add deterministic queue states and lifecycle operations, including BLOCKED. |
| S4-25 | Priority queue | Endpoint priority exists | PARTIAL | Reuse priority/context/resource/dependency signals with stable tie-breaking. |
| S4-26 | Test deduplication | Finding fingerprint exists; no test signature | PARTIAL | Add canonical test signature and preserved skip reason. |
| S4-27 | Test executor | Interface and intentionally disabled implementation exist | PARTIAL | Add validated lifecycle orchestration behind a transport boundary; retain disabled default. |
| S4-28 | Execution errors | No normalized active error taxonomy | MISSING | Add operational error types that cannot directly become findings. |

### S4-29 through S4-37 — capture, differential decisions, evidence and reproducibility

| ID | Capability | Existing evidence | State | Verified gap / action |
|---|---|---|---|---|
| S4-29 | Response capture | `HttpResponse` is immutable and captures status/headers/body/content type | PARTIAL | Add cookies, timing and request correlation in immutable execution snapshots. |
| S4-30 | Semantic response reuse | S3 semantic fingerprint and matcher are tested | EXISTS | Call the S3 analyzer; do not implement a replacement. |
| S4-31 | Multi-way differential | Pairwise passive comparator exists | PARTIAL | Compare baseline, positive, negative and mutation and classify the specified change states. |
| S4-32 | Authorization outcome normalization | `AuthorizationDecision` exists; no HTTP+semantic resolver | PARTIAL | Add ALLOW/DENY/AUTHENTICATION_REQUIRED/NOT_FOUND/PARTIAL/ERROR/UNKNOWN normalization. |
| S4-33 | Expected-decision precedence | Ground truth and matrix exist separately | PARTIAL | Add explicit precedence and always record the selected policy source. |
| S4-34 | Active observation | `TrafficObservation` and `DifferentialTest` are passive/foundational | PARTIAL | Add an immutable active `Observation`; never equate it with a confirmed vulnerability. |
| S4-35 | Evidence chain | Evidence and timeline primitives exist | PARTIAL | Persist the complete test-to-observation chain with originating execution IDs. |
| S4-36 | Execution fingerprint | Context, response and finding fingerprints exist | PARTIAL | Add request, response, configuration and environment fingerprints. |
| S4-37 | Reproducible execution | Deterministic models exist; no replay service | PARTIAL | Add immutable configuration snapshots, replay descriptors and fresh execution IDs. |

### Remaining execution-spec delivery capabilities

The continuation request also explicitly requires the following cross-cutting capabilities. They are mapped independently so none are hidden by the numbered tables above.

| Capability | Existing evidence | State | Verified gap / action |
|---|---|---|---|
| Replay and reproducibility | Deterministic serialization foundation only | PARTIAL | Add replay package/export/import contracts without overwriting prior evidence. |
| Immutable request/response evidence | HTTP byte arrays are defensively copied | PARTIAL | Add redacted persisted snapshots, correlation IDs and content hashes. |
| Credential redaction | `UniversalRedactor`, `TokenFingerprint`, serialization tests | EXISTS for S1–S3 | Apply the same redaction boundary to every new active audit/evidence serializer and test it. |
| Safety audit | No active safety-event ledger | MISSING | Add append-only structured audit events for allow/block/confirm/kill/budget/rate actions. |
| UI integration | Passive Swing views and read-only disabled-active labels | PARTIAL | Add plan, queue, observation, safety and replay views; keep execution disabled until explicit configuration/consent. |
| Regression tests | S1–S3 suites exist | MISSING for S4 | Run all historical suites plus new S4 suites. |
| Security tests | Existing secret and passive no-dispatch checks | PARTIAL | Add scope-bypass, kill-switch, budget, rate, mutation-isolation, replay-tamper and active-redaction tests. |
| Secure/vulnerable ACRA-Lab controls | Paired services exist | PARTIAL | Add explicit active ground truth and controlled cross-context cases. |
| FP/FN evaluation | Methodology and A0–A7 placeholders exist | MISSING | Execute only against registered controlled ground truth and report the small-fixture limitation. |
| A0–A7 research ablation | Registry entries are PLANNED | MISSING | Implement and run only if the fixture data and deterministic modes support it. |
| Performance evidence | S3 observational baseline exists | UNVERIFIED for S4 | Run a bounded S4 observational benchmark; do not make production claims. |
| Official Maven/Montoya build | POM target exists | BLOCKED / UNVERIFIED | Maven unavailable; local stub build is not official dependency validation. |
| Real Burp S2/S3 runtime evidence | Historical records are blocked | BLOCKED / UNVERIFIED historically | Preserve unchanged. Any new run must be recorded separately. |
| Release/package records | Sprint 4 records do not exist | MISSING | Update required documents, package RC1, hash it, and keep release HOLD unless all gates truly pass. |

## 7. Architecture findings and implementation constraints

1. `acra-core` is dependency-free and does not import Montoya. Sprint 4 orchestration should remain deterministic in core behind a transport interface; adapter-specific dispatch stays outside the semantic core.
2. The S3 dry-run APIs must retain zero-dispatch behavior. Sprint 4 converts reviewed output into a separate controlled active plan.
3. The baseline active executor is intentionally disabled and must remain the default/fallback path.
4. Existing `ScopeMode.ALL_TRAFFIC` is acceptable for passive collection and controlled tests but is not an active authorization policy.
5. The active target model must not carry forward the baseline `PRODUCTION` environment as an executable Sprint 4 option.
6. Expected-decision resolution and vulnerability reporting are separate concerns. Sprint 4 produces observations, not automatically confirmed findings.
7. Baseline HTTP models provide immutability in memory, but new persisted audit/evidence forms require explicit redaction and integrity fingerprints.

The first material architecture changes require new ADRs for the controlled active-execution boundary, safety-gate composition, and immutable execution evidence/replay design.

## 8. Technical debt discovered

- Repository Bash verification uses Unix classpath separators and is not portable to Windows.
- Maven is absent, so official Montoya dependency packaging cannot be reproduced here.
- Current production source uses dense one-line formatting in many records/classes, increasing review cost.
- Tests use custom `main` methods and manually counted assertions rather than a standard test runner.
- Persistence/database remains open; current stores are memory-first.
- OpenAPI/YAML ingestion is deliberately partial.
- Passive scope prefix matching is insufficient as a hard active scope boundary.
- The passive UI is source-tested only; runtime UI behavior is not established by local headless tests.
- ACRA-Lab uses mutable process-global fixture objects, so write/approve experiments must isolate or restart services.
- The synthetic lab token parser intentionally does not validate signatures and must never be presented as production authentication.
- The extension initialization log still names `v0.2.0-rc1` even though the repository baseline is `v0.3.0-rc1`.

## 9. Preserved historical blockers

The following historical statuses remain unchanged:

```text
S2 Burp Level 3: BLOCKED / UNVERIFIED
S2 Burp Level 4: BLOCKED / UNVERIFIED
S3 Burp Level 3: BLOCKED / UNVERIFIED
S3 Burp Level 4: BLOCKED / UNVERIFIED
```

Local Sprint 4 ACRA-Lab evidence cannot be substituted for those records.

## 10. Implementation decision

Proceed with Sprint 4 gap implementation under `v0.4.0-rc1`, subject to these gates:

- implement only gaps classified `PARTIAL`, `WEAK`, or `MISSING` above;
- preserve S3 public contracts and regression suites;
- default active execution to disabled and fail closed;
- execute only against loopback ACRA-Lab during automated validation;
- keep observations separate from confirmed vulnerability findings;
- record every unexecuted or environment-dependent gate as `UNVERIFIED` or `BLOCKED`;
- do not start Sprint 5.

