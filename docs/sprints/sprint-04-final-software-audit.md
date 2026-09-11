# Sprint 4 Final Software Completion Audit

**Project:** API Access Control & Routing Auditor (ACRA)  
**Audit checkpoint:** `S4-FINAL-SOFTWARE-CHECKPOINT-2026-09-05`  
**Candidate:** `0.4.0-rc1`  
**Repository input:** latest verified graph checkpoint plus the in-place Phase 3 working tree  
**Execution boundary:** ACRA-Lab, synthetic fixtures and authorized loopback development targets  
**Decision:** **S4 SOFTWARE COMPLETE**

## Audit basis

This audit inspected the actual Java source, tests, existing Swing UI, configuration, ACRA-Lab, scripts, POMs, documentation, experiment records and reproducibility metadata. Class or document existence alone was not treated as completion evidence.

NEWLY EXECUTED verification is recorded in `docs/testing/artifacts/verification-s4-phase3-product-completion.txt`. All core and extension sources compile warning-clean for the Java 21 target using `--release 21 -Xlint:all -Werror`. Execution used the available Burp-bundled OpenJDK 26.0.1; an exact JDK 21 runtime and Maven remain unavailable.

The verified graph milestone was preserved. A fresh local run again traversed:

```text
SecurityTest -> TestExecutor -> LocalhostHttpTransport -> ACRA-Lab
             -> Response -> Differential -> Observation -> Evidence
             -> ObservationGraphIntegrator -> existing SecurityContextGraph
```

The secure control produced expected `DENY`, observed `DENY`, `EXPECTED_CHANGE`; the deliberately vulnerable fixture produced expected `DENY`, observed `ALLOW`, `UNEXPECTED_CHANGE`. Both remain Observations rather than vulnerability findings.

## Status summary

| Status | Count |
|---|---:|
| COMPLETE | 45 |
| PARTIAL | 0 |
| MISSING | 0 |
| WEAK | 0 |
| UNVERIFIED | 1 |
| BLOCKED | 1 |
| DEFERRED | 5 |
| NOT APPLICABLE | 0 |
| **Total** | **52** |

## Requirement audit

| S4 Area | Requirement | Implementation | Software Verification | Runtime Evidence | Deferred Validation | Final Status | Remaining Gap | Evidence/Reference |
|---|---|---|---|---|---|---|---|---|
| Contract | SecurityTest | Immutable typed test, baseline/controls, one mutation, contexts/resources, expected decision/evidence, safety, reason, dependencies and reproducibility. | Core and product contract tests pass. | Used by secure/vulnerable localhost runs. | None. | COMPLETE | None. | `active/model/SecurityTest.java`; `Sprint4CoreVerificationTestSuite` |
| Profiles | Five product profiles | Authorization Audit, Routing Audit, Context Audit, Research Differential and Expert Custom define real contracts, mutations, comparison, budgets, evidence and safety. | Profile configuration and planner-effect tests pass. | Configuration-only. | Research weighting. | COMPLETE | None in software contract. | `TestProfileDefinition.java`; product suite |
| Selection | ALL/AUTOMATIC/USER_SELECTED/HYBRID | Planner applies the four modes to candidate flags. | Exact admitted test sets pass for all modes. | Workspace planning exercised locally. | None. | COMPLETE | None. | `TestPlanner.java`; product suite |
| User modes | Beginner/Professional/Researcher/Expert | Modes map to profile, selection, depth, mutation capability, limits, evidence and controls. | Mapping, control enforcement and plan-effect tests pass. | UI bindings exercised headlessly. | Real Burp UX. | COMPLETE | None in backend mapping. | `UserModeDefinition.java`; `ActiveEngineWorkspace.java`; product/UI suites |
| Planning | TestPlanner | Reuses S3 inventory/context/route/OpenAPI/matrix/risk inputs; filters, orders, deduplicates and budgets deterministically. | Existing 54 core plus product metrics tests pass. | Local product plan exercised. | Large campaigns. | COMPLETE | None. | `active/planning/TestPlanner.java` |
| Planning | Dry run | Planner has no transport and reports the bounded request plan only. | Zero-dispatch assertions pass. | UI empty-plan view states dispatch zero. | None. | COMPLETE | None. | `TestPlan.java`; S3/S4 suites |
| Explainability | Why/change/constants/expected/evidence/cost | SecurityTest fields are projected by Test Detail. | UI suite verifies installed active views and backend binding. | Headless Swing only. | Real Burp UX. | COMPLETE | None in source projection. | `ActiveTestingPanel.java` |
| Mutation | Single controlled difference | Typed ID/type/location/original/replacement/reason/effect/safety; no-op and ambiguous mutation rejected. | Core/security/dispatch tests pass. | One resource mutation reaches ACRA-Lab. | Broader family research. | COMPLETE | None for supported locations. | `Mutation.java`; `RequestBuilder.java` |
| Requests | RequestBuilder | Immutable Baseline, Allow Control, Deny Control and Experiment requests preserve unrelated fields. | Immutability, exact-location and live echo tests pass. | Method/header/body preserved live. | None. | COMPLETE | None. | `active/execution/RequestBuilder.java` |
| Requests | RequestEquivalenceGuard | Independently derives and compares the declared mutation and authority/protocol invariants. | Valid, invalid and contaminated executor proofs pass. | Valid mutation dispatch=1; rejected mutations=0. | None. | COMPLETE | None. | `RequestEquivalenceGuard.java`; security suite |
| Safety | Pre-dispatch safety | Complete request set passes construction and guards before any transport. | Focused counting transport proof passes. | Local runtime retains same invariant. | None. | COMPLETE | None. | `MutationValidator.java`; `TestExecutor.java` |
| Safety | Scope guard | Project, target, path prefix, method and traversal constraints cover all variants. | Scope/cross-project/traversal tests pass. | External target rejected by localhost transport. | Authorized external targets. | COMPLETE | None in S4 boundary. | `HardScopeGuard.java` |
| Safety | Environment guard | LAB requires enabled, authorized loopback target. | Remote LAB/non-LAB rejection passes. | Local transport construction-bound to loopback LAB. | External environments. | COMPLETE | None locally. | `EnvironmentGuard.java`; `LocalhostHttpTransport.java` |
| Safety | Consent/local development | Explicit consent remains a guard; local policy pre-acknowledges only authorized non-destructive loopback LAB tests. | Authorized, external, unauthorized and destructive cases pass. | Workspace uses policy for local execution. | Burp UX. | COMPLETE | None in local policy. | `LocalDevelopmentExecutionPolicy.java` |
| Safety | Kill switch | Engaged by default, authorized reset, engage, STOP ALL and audit. | Security/product tests pass. | Local harness opens it explicitly. | None. | COMPLETE | None. | `KillSwitch.java`; `ActiveEngineWorkspace.java` |
| Budgets | Request budgets | Hierarchical global/project/target/endpoint/test/context reserve/commit accounting. | Exhaustion and no-underflow tests pass. | Four-request live executions stay bounded. | Load study. | COMPLETE | None. | `HierarchicalBudgetManager.java` |
| Budgets | Mutation budgets | Planning bounds mutation count; executor records generated/executed/failed mutation state. | Limit and accounting paths pass. | One declared mutation per live test. | Campaign metrics. | COMPLETE | None in core behavior. | `MutationBudgetTracker.java`; planner/executor |
| Operations | Concurrency | Hierarchical leases and exact release. | Saturation/release tests pass. | Local limit one. | Load study. | COMPLETE | None. | `HierarchicalConcurrencyController.java` |
| Operations | Rate limiting | Scoped configured token buckets gate dispatch. | Permit/rejection tests pass. | Configured for live execution. | Throughput study. | COMPLETE | None. | `ScopedRateLimiter.java` |
| Operations | Backoff | 429/503/Retry-After and connection failures use bounded attempts/delay. | Product tests verify bounds and retry count. | Timeout remains operational/no Observation. | Load study. | COMPLETE | None. | `BackoffPolicy.java`; product/local suites |
| Queue | Lifecycle/operations | All eight states plus start/pause/resume/stop/retry/skip/rerun/STOP ALL. | Focused lifecycle tests pass. | Workspace uses actual queue. | Real Burp interaction. | COMPLETE | None. | `ExecutionQueue.java`; product suite |
| Queue | Priority/dedup/dependencies | Stable priority/sequence, canonical signatures, successful/failed/missing prerequisite handling. | Ordering, duplicate, dependency and missing-dependency tests pass. | No duplicate live dispatch. | Cycle diagnostics. | COMPLETE | Cyclic user-authored dependencies are not given a specialized diagnostic. | `ExecutionQueue.java`; product/core suites |
| Execution | TestExecutor | Orchestrates validation, budgets, rate/concurrency, variants, transport, analysis, evidence, graph and terminal errors. | Core/security/local/graph regressions pass. | Full localhost pipeline passes. | Burp/external adapters. | COMPLETE | None locally. | `TestExecutor.java` |
| Execution | HttpTransport | Narrow transport interface consumes safe snapshots and returns response/timing data. | Test doubles and concrete source compile/run. | Live localhost transport used. | Other adapters. | COMPLETE | None for interface. | `HttpTransport.java` |
| Execution | LocalhostHttpTransport | Fixed authorized LAB loopback authority, redirects disabled, origin-form validation. | Boundary, echo, timeout and external rejection pass. | Secure/vulnerable ACRA-Lab runs pass. | External targets out of scope. | COMPLETE | None. | `LocalhostHttpTransport.java`; local suite |
| Evidence | Response capture | Immutable response, cookies, timing, semantic fingerprint and timestamp. | Redaction and semantic capture assertions pass. | Eleven-stage live evidence chains. | Persistent storage. | COMPLETE | In-memory only. | `ResponseSnapshot.java` |
| Analysis | Semantic normalization | ALLOW, DENY, AUTHENTICATION_REQUIRED, NOT_FOUND, PARTIAL, ERROR, UNKNOWN. | Status and HTTP-200 soft-denial tests pass. | Soft denial passes live. | Broader corpus. | COMPLETE | None in deterministic model. | `AuthorizationOutcomeNormalizer.java` |
| Analysis | Multi-way differential | Baseline/allow/deny/experiment comparison with five classifications. | Core/product/local tests pass. | Secure expected and vulnerable unexpected changes observed. | Accuracy campaign. | COMPLETE | None in engine. | `MultiWayDifferentialAnalyzer.java` |
| Analysis | Expected decision resolver | Source precedence separates ground truth, policy, test, matrix, validated metadata and inference. | Precedence/conflict/unknown tests pass. | Ground truth drives live controls. | Broader policies. | COMPLETE | None. | `ExpectedDecisionResolver.java` |
| Operations | Operational error separation | Timeout/connection/invalid target/retry exhaustion do not become security observations. | Product/local tests pass. | Live timeout created no Observation. | Target-instability campaigns. | COMPLETE | None. | `ExecutionFailure.java`; executor |
| Evidence | Observation | Complete expected/observed/differential/context/evidence/fingerprint record; not a finding. | Construction and mismatch tests pass. | Secure/vulnerable live Observations retained. | Finding logic is Sprint 5+. | COMPLETE | None. | `Observation.java` |
| Evidence | Evidence chain | Append-only stage chain links test, requests, responses, differential and observation. | Fresh identity and redaction tests pass. | Live 11-entry chains. | Persistent store. | COMPLETE | In-memory only. | `ExecutionEvidenceStore.java` |
| Graph | Existing SecurityContextGraph hydration | Minimal adapter validates provenance/project/context and atomically merges observed/conflict relations. | 87 graph assertions pass. | Fresh normal-path hydration passes. | Persistent graph store. | COMPLETE | None for in-memory integration. | `ObservationGraphIntegrator.java`; graph suite |
| Replay | Reproduction/rerun | Fingerprint verification, fresh execution/observation/evidence IDs and retained history. | Replay/duplicate tests pass. | Graph milestone includes replay acceptance. | Cross-restart persistence. | COMPLETE | In-memory history only. | `ReplayService.java`; graph suite |
| Determinism | Ordering/configuration/seed | Plan/test signatures, queue order, configuration fingerprint, research seed and order are recorded. | Determinism tests pass. | Execution IDs recorded. | Cross-platform campaign. | COMPLETE | None in records. | `TestSignature.java`; `ExperimentRunMetadata.java` |
| Coverage | Active test coverage | Eligible/tested endpoints, contexts, resources, mutation categories and terminal test counts. | Exact counter/idempotence tests pass. | One execution counted in product suite. | Campaign coverage. | COMPLETE | Vulnerability coverage intentionally separate. | `ActiveCoverageTracker.java` |
| Efficiency | Request-efficiency instrumentation | Candidate, deduplicated, scope-filtered, budget-filtered and executed counts. | Exact counters pass. | Small deterministic fixture only. | 100/1k/10k study. | COMPLETE | No optimization percentage claimed. | `PlanningMetrics.java`; `RequestEfficiencySnapshot.java` |
| UI | Eight active views and controls | Existing suite tab contains Plan, Queue, Detail, Execution, Differential, Evidence, Safety and Experiment; controls bind to workspace. | Headless UI suite passes 26. | No real Burp UI run. | Burp validation. | COMPLETE | Source projection is complete. | `ActiveTestingPanel.java`; UI suite |
| UI | Burp-session plan/executor provisioning | Constructor injection provisions the existing executor for explicit local configurations; the default extension remains disabled until configuration/consent. Passive-to-active import is a Burp adapter workflow, not part of the local S4 engine boundary. | Configured workspace plan/queue/execution and safe-disabled default tests pass. | Local executor pipeline passes; real Burp UI not run. | Explicitly deferred to Burp integration validation. | DEFERRED | Validate/provision through real Burp only when that separate phase is scheduled. | `ActiveEngineWorkspace.java`; product/UI suites; reconciliation S4 UI rule |
| Lab | Secure control | Independent ground truth, expected ALLOW and DENY and one secure mutation. | Local suite passes. | Secure execution `S4-EXEC-00000002`. | None locally. | COMPLETE | None. | `GT-EXEC-S4.json`; `EXP-EXEC-001.md` |
| Lab | Vulnerable control | Deliberate mismatch preserves valid deny control and no finding promotion. | Local suite passes. | Vulnerable execution `S4-EXEC-00000003`. | None locally. | COMPLETE | None. | ACRA-Lab; local suite |
| Research software | Labelled cases/metrics/metadata | Ground-truth case, prediction, execution, TP/TN/FP/FN, safe metric and run metadata models plus fixture registry. | Product suite verifies all labels and `N/A` denominator handling. | No broad campaign. | Research validation. | COMPLETE | None in software infrastructure. | `active/research`; `GT-S4-RESEARCH-FIXTURES.json` |
| Research validation | EXP-DIFF-001 broad campaign | Protocol and labelled corpus exist. | Not executed. | NOT MEASURED. | Explicitly deferred. | DEFERRED | Execute baseline vs treatment campaign before claiming precision/recall/F1. | `research/EXP-DIFF-001.md` |
| Performance | 100/1k/10k workloads | No S4 workload harness was present or added in this closure. | Not executed. | NOT MEASURED. | Explicitly deferred. | DEFERRED | Implement and run planning/queue/execution/differential/memory measurements. | experiment registry |
| Build | Java 21 target compile | Core and full extension compile with warnings fatal against retained stubs. | PASS. | Executed on OpenJDK 26.0.1. | None. | COMPLETE | None for target bytecode/source contract. | Phase 3 verification artifact |
| Build | Exact JDK 21 runtime | No exact JDK 21 executable is installed. | Not run. | UNVERIFIED. | Environment acquisition. | UNVERIFIED | Repeat suites on exact JDK 21. | Phase 3 verification artifact |
| Build | Maven/Montoya package | POMs parse and declare Java 21; Maven is absent. | BLOCKED. | No official dependency/package run. | Environment acquisition. | BLOCKED | Install/use Maven with official Montoya dependency availability. | root/module POMs |
| Packaging | Cumulative source checkpoint | Source, tests, UI, lab, configuration, scripts, documents and research records are packaged without disposable build output. | ZIP integrity, unsafe-path, clean-unpack and exact file/hash comparison pass. | Not a runtime claim. | Maven artifact build remains separate. | COMPLETE | None for source checkpoint. | `acra-sprint-04-final-software-checkpoint-verification.txt` |
| Reproducibility | Configuration/dataset/lab/seed/IDs | Immutable test metadata, configuration fingerprints, dataset/lab records and fresh IDs. | Core/product/replay tests pass. | Live IDs recorded. | Cross-restart persistence. | COMPLETE | Persistence remains later architecture. | model/research/replay source |
| Documentation | State, architecture, security, tests and research | Project state, test matrix, issues, registry, roadmap, changelog and dedicated active docs synchronized. | Source-to-doc audit performed. | Phase 3 artifact retained. | Burp/research updates when run. | COMPLETE | None for current facts. | `docs/architecture/*`; canonical state docs |
| Validation phase | Burp runtime | Historical S2/S3 Level 3/4 debt preserved; S4 UI not run in Burp. | Not performed. | BLOCKED / UNVERIFIED historically. | Explicitly deferred. | DEFERRED | Deliberately schedule real Burp load/UI/active-local validation. | `PROJECT_STATE.md`; `KNOWN_ISSUES.md` |
| Validation phase | Authorized external targets | S4 software deliberately implements only loopback concrete transport. | Not applicable to local closure execution. | None. | Explicitly deferred by boundary. | DEFERRED | Separate authorization, adapter and safety validation required. | safety architecture |

## Final decision

**S4 SOFTWARE COMPLETE**

The core engine, graph path, profiles, selection/user modes, queue, local consent, backoff/error behavior, resolver, coverage, efficiency, UI source projection and research software records are implemented and locally verified. The configured workspace now has a direct plan → queue → existing executor proof. The default Burp construction remains intentionally disabled until explicit configuration/consent, exactly as required by the reconciliation; passive-to-active provisioning in a real Burp session is therefore deferred validation/adaptation rather than a missing S4 local-engine requirement.

Broad labelled research, performance, Burp runtime and authorized external validation remain separate deferred phases. Maven remains an environment-blocked official artifact-build lane, while warning-clean core/extension compilation and the cumulative source checkpoint are verified. These exclusions do not erase the local software evidence, and no accuracy, optimization or real-world safety claim is made.

## Next exact action

Stop Sprint 4 implementation. Preserve this checkpoint. Real Burp provisioning/runtime, exact JDK 21, broad research/performance and authorized external validation may be scheduled as separate validation work. Sprint 5 is eligible only after an explicit new user directive; it was not started here.
