# Sprint 4 Checkpoint Recovery and Resumption

**Project:** API Access Control & Routing Auditor (ACRA)  
**Candidate:** `0.4.0-rc1`  
**Recovery date:** 2026-08-31  
**Execution boundary:** ACRA-Lab, synthetic fixtures, and self-contained localhost services only  
**Recovery result:** CHECKPOINT RECOVERED; CONTINUATION AUTHORIZED WITHOUT RESTART

## 1. Previous checkpoint

The Sprint 4 working copy is `work/acra-sprint-04-v0.4.0-rc1/acra`. It was compared with the immutable extracted Sprint 3 baseline at `work/sprint-03-baseline/acra`; no baseline re-extraction, reset, replacement, or deletion was performed.

The repository-first gate in `docs/sprints/sprint-04-reconciliation.md` is complete. It records the verified source archive SHA-256, complete baseline inspection, 147 reproduced Sprint 1–3 assertions, Sprint 3 lab and fixture measurements, the mandatory Sprint 3 reuse map, Sprint 4 requirement classifications, technical debt, and preserved S2/S3 Burp blockers.

After that gate, the interrupted execution added the controlled active-engine core. The exact last completed implementation slice was the integrated compilation of the model, planner, request construction, safety, budget, concurrency, rate, queue, executor orchestration, expected-decision resolution, semantic differential, immutable evidence, observation, and replay packages.

## 2. Recovered working state

The working copy contains the following changes relative to the immutable Sprint 3 baseline:

- new source under `core/src/main/java/io/acra/core/active/analysis`, `evidence`, `execution`, `model`, `planning`, `replay`, and `safety`;
- the existing `core/src/main/java/io/acra/core/domain/testing/TestState.java` extended with `BLOCKED`;
- `PROJECT_STATE.md` changed at the pre-implementation gate;
- `docs/sprints/sprint-04-reconciliation.md` added.

No Sprint 4 test source, localhost transport adapter, active UI integration, Sprint 4 lab ground truth, Sprint 4 experiment artifact, or final release documentation existed at recovery time.

The state marker at the top of `PROJECT_STATE.md` still says `IMPLEMENTATION NOT STARTED`. That text is a preserved pre-implementation gate marker and is now stale relative to the recovered source. It must be updated only after the recovered implementation has direct test evidence; it must not be reset to a new reconciliation state.

## 3. Current implementation audit

Statuses below are based on source inspection and the current compile, not on intended behavior or prior prose.

| Component | Current state | Existing class/file | Remaining work |
|---|---|---|---|
| SecurityTest | COMPLETE | `active/model/SecurityTest.java`, `TestContract.java`, `TestSignature.java` | Add direct contract, immutability, serialization, and fingerprint tests. |
| Mutation | COMPLETE | `active/model/Mutation.java`, `MutationType.java`, `MutationLocation.java` | Add exact-location and single-difference negative tests. |
| TestPlanner | PARTIAL | `active/planning/TestPlanner.java`, `PlanningInput.java`, `TestSeed.java` | Verify deterministic ordering, reuse of S3 inputs, filtering, deduplication, budgets, explanations, and zero-dispatch behavior. |
| Baseline | PARTIAL | `RequestDefinition.java`, `RequestBuilder.java`, `RequestSnapshot.java`, `ResponseSnapshot.java` | Verify immutability and semantic fingerprint capture during a real localhost execution. |
| Controls | PARTIAL | `RequestSet.java`, `RequestBuilder.java`, `RequestVariantKind.java` | Prove expected-allow and expected-deny control behavior from lab ground truth. |
| RequestBuilder | PARTIAL | `active/execution/RequestBuilder.java`, `UriMutationAdapter.java` | Add tests for every declared mutation location and unchanged-field invariants. |
| Safety guards | PARTIAL | `HardScopeGuard.java`, `EnvironmentGuard.java`, `ConsentGuard.java`, `KillSwitch.java`, `MutationValidator.java` | Add security tests for scope escape, ambiguous paths, environment, consent, kill, redaction, and cross-project isolation. |
| Queue | PARTIAL | `ExecutionQueue.java`, `QueueEntrySnapshot.java` | Verify all required transitions, deterministic order, deduplication, cancellation, retry, skip, rerun, and STOP ALL. |
| Executor | PARTIAL | `TestExecutor.java`, `HttpTransport.java`, `BackoffPolicy.java` | Core orchestration compiles; add tests and a localhost-only concrete transport, then verify cancellation, backoff, instability, and error separation. |
| Outcome normalization | PARTIAL | `AuthorizationOutcomeNormalizer.java`, `AuthorizationOutcome.java` | Add status and soft-denial semantic cases, including same-status and HTTP-200 denial controls. |
| Differential engine | PARTIAL | `MultiWayDifferentialAnalyzer.java`, `MultiWayDifferential.java`, `PairwiseDifference.java` | Verify all classifications using S3 semantic comparators and labelled controls. |
| Observation | PARTIAL | `Observation.java`, `TestExecutionResult.java` | Prove creation from a complete four-way execution and verify it is not promoted to a finding. |
| Evidence chain | PARTIAL | `ExecutionEvidenceStore.java`, `EvidenceChainEntry.java`, `RequestSnapshot.java`, `ResponseSnapshot.java` | Verify append-only behavior, complete stage linkage, immutable IDs, and credential/cookie redaction. |
| Replay | PARTIAL | `ReplayService.java`, `ReplayDescriptor.java`, `ReplayResult.java` | Verify fingerprint checks and new execution/evidence IDs on rerun. |
| Security Context Graph integration | MISSING | Existing S3 `SecurityContextGraph` is preserved | Add an adapter that attaches completed observation/evidence references to the existing graph; do not add a new graph. |
| ACRA-Lab integration | MISSING | Existing `lab/` secure and vulnerable services are available | Add Sprint 4 labelled ground truth, localhost transport, secure/vulnerable controls, and deterministic execution records. |
| UI | MISSING | Existing passive `AcraSuiteTab.java` remains unchanged | Extend it with plan, queue, test detail, execution, differential, evidence, safety, experiment, replay, and STOP ALL views. Keep active execution disabled by default. |
| Experiments | MISSING | Existing Sprint 1–3 experiment records are preserved | Create and execute `research/EXP-DIFF-001.md`, FP/FN fixtures, request-efficiency measurements, and performance workloads. |

`COMPLETE` in this audit means the recovered data contract itself is present and structurally complete. It does not mean the full Sprint 4 verification gate has passed. Components marked `PARTIAL` compile but still lack direct Sprint 4 executable evidence.

## 4. Completed Sprint 4 components

- complete immutable `SecurityTest` contract and exact category taxonomy;
- complete single-mutation value contract and location/type taxonomy;
- explicit target, environment, safety, profile, configuration, confirmation, and reproducibility models;
- deterministic plan and queue data structures with canonical signatures and deduplication;
- baseline, allow-control, deny-control, and mutation request construction;
- scope, environment, consent, kill-switch, request/mutation budget, concurrency, rate, and audit controls;
- executor orchestration boundary, bounded backoff policy, cancellation, and normalized operational failures;
- expected-decision source precedence and conflict representation;
- normalized semantic authorization outcomes;
- four-way semantic differential structures reusing Sprint 3 comparison primitives;
- request/response snapshots, execution fingerprints, observations, append-only evidence store, and safety audit;
- replay descriptors and replay service with fingerprint verification.

## 5. Partially completed components

- planner behavior is implemented but not covered by Sprint 4 tests;
- request construction and safety behavior are implemented but not covered by negative security tests;
- the executor has an abstract transport boundary but no recovered localhost implementation;
- differential, observation, evidence, and replay source compiles but has not executed against ACRA-Lab;
- performance, request-efficiency, FP/FN, and ablation measurements have not been run;
- the existing Sprint 3 graph has not yet been connected to completed active observations.

## 6. Unfinished components

- Sprint 4 core and security test suites;
- localhost-only HTTP transport and ACRA-Lab execution harness;
- labelled Sprint 4 secure/vulnerable/FP/FN ground truth;
- observation-to-existing-graph integration;
- Sprint 4 UI source integration;
- research and performance execution records;
- Sprint 4 architecture/security/ADR documentation;
- state, changelog, test matrix, known issues, experiment registry, roadmap, version, manifest, package, and SHA-256 updates.

## 7. Failed or interrupted operations

| Operation | Result | Interpretation |
|---|---|---|
| Baseline repository Bash verifier on Windows | BLOCKED | The script uses Unix `:` classpaths. Equivalent Windows-classpath commands passed; this is verifier portability debt. |
| First recovered all-source compile command | FAILED before compilation | Windows command-line length was exceeded by absolute source paths. Retried from the source root with relative paths. |
| Recovered Java 21 core compilation | PASS | 266 class files generated using `--release 21 -Xlint:all -Werror`; no compiler output or warning. |
| Sprint 4 executable tests | UNVERIFIED | No Sprint 4 test source existed at checkpoint recovery. |
| Sprint 4 local lab experiments | UNVERIFIED | No Sprint 4 execution or metric artifact existed at checkpoint recovery. |

No failed operation modified or discarded source. Build directories are disposable compilation output and are excluded from final packaging.

## 8. Current build and test state

| Gate | Recovered state |
|---|---|
| Sprint 4 core Java 21 compile with all warnings fatal | PASS |
| Sprint 4 test compile | UNVERIFIED |
| Sprint 4 unit tests | UNVERIFIED |
| Sprint 4 engine security tests | UNVERIFIED |
| Sprint 4 adapter compile | UNVERIFIED |
| Sprint 4 ACRA-Lab execution | UNVERIFIED |
| Sprint 4 FP/FN experiment | NOT MEASURED |
| Sprint 4 performance workloads | NOT MEASURED |
| Real Burp runtime | UNVERIFIED; not attempted at this checkpoint |

The previously reproduced Sprint 1–3 result remains 147 passing assertions. It is not counted as Sprint 4 test evidence.

## 9. Exact next implementation step

The next unfinished requirement is the Sprint 4 core verification slice. Add a focused test suite that proves the recovered contracts and safety invariants before adding any dispatch adapter. The first slice must cover:

1. `SecurityTest` and single-mutation invariants;
2. immutable baseline/control construction and unchanged dimensions;
3. deterministic planner and zero-dispatch dry run;
4. scope, environment, consent, kill, budget, concurrency, rate, and redaction controls;
5. queue order, transitions, deduplication, cancellation, and STOP ALL;
6. outcome normalization, four-way differential, append-only evidence, and replay identity behavior.

Compile and run that suite. Only after it passes may continuation add the localhost-only transport and controlled ACRA-Lab integration. UI, research measurement, release documentation, and packaging follow in that order.

## 10. Preserved historical status

Historical evidence is unchanged:

- S2 Burp Level 3: BLOCKED / UNVERIFIED;
- S2 Burp Level 4: BLOCKED / UNVERIFIED;
- S3 Burp Level 3: BLOCKED / UNVERIFIED;
- S3 Burp Level 4: BLOCKED / UNVERIFIED.

The current host contains Burp Suite and a bundled Java toolchain. That fact may support a separate current validation later; it does not rewrite historical Sprint 2 or Sprint 3 records.

## 11. Continuation rule

Continue from the verified working copy only. Do not implement Sprint 5, add vulnerability-specific analyzers, discover arbitrary external targets, or turn an observation into a vulnerability finding. Active dispatch remains limited to explicitly configured loopback ACRA-Lab and synthetic local fixtures, with all recovered safety controls enforced.


## 12. Core verification slice completed

**Result:** PASS  
**Checkpoint:** `S4-RESUME-2026-08-31-CORE-INTEGRATED`  
**Evidence:** `docs/testing/artifacts/verification-s4-core.txt`

The focused verification slice specified in section 9 was executed without adding a concrete network transport, active UI, research experiment, vulnerability analyzer, or Sprint 5 functionality.

### Tests added and executed

- `Sprint4CoreVerificationTestSuite`: PASS, 54 assertions.
- `Sprint4EngineSecurityTestSuite`: PASS, 41 assertions.
- Focused Sprint 4 total: PASS, 95 assertions.
- Existing core `TestSuite`: PASS, 37 assertions.
- Existing `Sprint3CoreTestSuite`: PASS, 47 assertions.

The Java 21 main/test compile passes with `--release 21 -Xlint:all -Werror`. The first test compile exposed one test-only lint failure because an acquired `AutoCloseable` concurrency lease was not referenced. The test was corrected to assert the lease, then the build and both Sprint 4 suites were rerun and passed. No Sprint 4 engine main-source fix was required.

### Verification conclusions

Direct executable evidence now exists for the recovered `SecurityTest` and mutation contracts, request immutability/single-difference construction, deterministic planner/deduplication, zero-dispatch S3 dry-run invariant, queue ordering/transitions/deduplication/STOP ALL, scope/environment/consent/kill controls, request budgets, mutation-safety rejection, concurrency/rate limits, configuration tamper rejection, credential/audit redaction, semantic outcome normalization, four-way differential analysis, append-only evidence, in-memory executor orchestration with a non-network transport double, observation creation, and replay identity/fresh execution IDs.

This does not promote those tests into ACRA-Lab or real-network evidence. Concrete localhost transport, active ACRA-Lab execution, labelled secure/vulnerable controls, FP/FN measurement, graph integration, active UI, research experiments, performance workloads, Maven/Montoya packaging and current Burp runtime remain UNVERIFIED or NOT MEASURED. Historical S2/S3 Burp status remains unchanged.

## 13. Next exact implementation step

Add the concrete **localhost-only `HttpTransport` implementation and controlled ACRA-Lab integration harness**. Reuse the verified executor and safety layers. The first integration execution must establish an expected-ALLOW control and an expected-DENY control from independent synthetic lab ground truth before any FP/FN or research measurement begins.

## 14. Localhost transport + ACRA-Lab integration slice completed

**Result:** PASS  
**Evidence:** `docs/testing/artifacts/verification-s4-localhost.txt`, `docs/testing/artifacts/exp-exec-001-localhost.txt`  
**Ground truth:** `lab/ground-truth/GT-EXEC-S4.json`  
**Experiment:** `research/EXP-EXEC-001.md`

The continuation added the smallest concrete transport behind the existing `HttpTransport` interface. `LocalhostHttpTransport` is bound to one authorized `LAB` target on loopback, disables redirects, preserves the existing request model, captures the existing response model and timing, and is not a general external-target transport.

The existing ACRA-Lab was extended rather than replaced. The secure fixture established `ALLOW` for `User-A → Document-A` and `DENY` for `User-A → Document-B`. A single resource mutation changed only `Document-A → Document-B`; the secure live result was `DENY` and the four-way differential was `EXPECTED_CHANGE`. The deliberately vulnerable fixture kept the independent negative control at `DENY` while returning `ALLOW` for the single controlled resource mutation. The pipeline retained the mismatch as an observation and classified the comparison `UNEXPECTED_CHANGE`. No vulnerability finding was created.

The minimum FP-preparation cases also passed: HTTP-200 application denial normalized to `DENY`, dynamic timestamp/request-ID fields were treated as volatile, and reordered/formatted public responses remained semantically equivalent. The live timeout mapped to `TIMEOUT` and produced no security observation. A live connection-error case was not executed in this slice.

### Verified failures and fixes

1. The first live localhost run preserved the request method and body, but the custom-header assertion failed because the existing echo fixture reflects `X-Echo` while the test initially sent `X-S4-Trace`. The test was corrected to exercise the existing echo contract; no transport change was required.
2. The second live run returned `DENY` for the valid baseline because the new Sprint 4 lab fixture used display labels (`User-A`, `Tenant-A`) as backend identity values while the existing synthetic JWT decoder resolves canonical IDs (`user-a`, `tenant-a`). The lab fixture was corrected to use the established canonical backend IDs while independent ground truth retains the human-readable labels.
3. A cross-executor execution-ID collision had been reproduced during the interrupted continuation. The existing repair was preserved: `TestExecutor` uses a process-wide sequence, and the localhost suite verifies that separate executor instances receive distinct execution IDs.
4. The legacy passive-core architecture gate was preserved while a separate Sprint 4 architecture gate verifies that the only active network client is the loopback-only `LocalhostHttpTransport`.

### Final verification for this slice

- `Sprint4CoreVerificationTestSuite`: PASS, 54 assertions.
- `Sprint4EngineSecurityTestSuite`: PASS, 41 assertions.
- `Sprint4LocalhostIntegrationTestSuite`: PASS, 51 assertions.
- Core `TestSuite`: PASS, 37 assertions.
- `Sprint3CoreTestSuite`: PASS, 47 assertions.
- `Sprint3AdapterTestSuite`: PASS, 11 assertions.
- Sprint 3 security and architecture gates: PASS.
- Sprint 4 architecture gate: PASS.
- Maven/Montoya package: BLOCKED / UNVERIFIED because Maven/dependency resolution remain unavailable.

This slice does not run the large FP/FN campaign, request-efficiency study, performance workloads, active UI, current Burp validation, or Sprint 5 analyzers.

## 15. Next exact implementation step

Add the **active Observation/Evidence → existing `SecurityContextGraph` integration**. Reuse the Sprint 1–3 graph; do not create another graph implementation. Verify provenance, context-completeness blocking, and graph evidence linkage locally before moving into the larger research/FP/FN campaign.

## 16. S4-PHASE2-01 graph-integration closure

**Result:** PASS  
**Checkpoint:** `S4-PHASE2-01-GRAPH-INTEGRATION-VERIFIED-2026-09-04`  
**Evidence:** `docs/testing/artifacts/verification-s4-graph-integration.txt`  
**Architecture:** `docs/architecture/s4-graph-integration.md`

The interrupted checkpoint contained the existing-graph adapter and a historically reported 71-assertion focused result. Recovery inspection also found an unverified delta/preflight atomicity change and collision fixture. This continuation did not recreate the adapter. It completed the named credential-exclusion and whole-graph atomicity proofs, rebuilt all core main/test sources for the Java 21 target with warnings as errors, and reran the complete relevant regression batch.

The current `Sprint4GraphIntegrationTestSuite` passes 87 assertions. It verifies complete hydration, execution/test/observation/evidence provenance, five incomplete-context cases, configured-versus-observed resource/owner/tenant conflict retention, identical-observation idempotence, replay freshness and history, credential exclusion, forged provenance refusal, unrelated relationship preservation, project isolation and atomic conflict refusal. Inserted nodes are added, equivalent nodes are reused, and conflicting identities return `BLOCKED` without changing the original serialized graph.

One fresh controlled local acceptance used both existing ACRA-Lab processes and the normal executor path. The secure graph relationship is traceable through:

- test: `S4-EXEC-SECURE-001`
- execution: `S4-EXEC-00000002`
- observation: `S4-EXEC-00000002:observation`
- originating chain evidence: `S4-EVIDENCE-00000009`
- graph evidence: `ev-97dbe353e0decdbb45ee`
- graph relationship: `edge-837c5bd8d56408e3c53b`

The live suite passes 60 assertions. The secure control remains expected `DENY` / observed `DENY` / `EXPECTED_CHANGE`; the deliberately vulnerable control remains expected `DENY` / observed `ALLOW` / `UNEXPECTED_CHANGE`, stored only as an observation. The local lab processes were stopped and ports 18081/18082 were confirmed closed.

The full newly executed batch passes 348 assertions: 87 graph, 54 S4 core, 52 S4 engine security, 60 localhost, 37 core, 47 Sprint 3 core and 11 Sprint 3 adapter. Compilation uses `--release 21 -Xlint:all -Werror`; the only available compiler/runtime is OpenJDK 26.0.1, so exact JDK 21 runtime execution is UNVERIFIED. Maven remains BLOCKED because `mvn` is unavailable. Historical S2/S3 Burp Level 3/4 remains BLOCKED / UNVERIFIED.

STOP after this graph slice. Remaining S4 work is active UI integration, Beginner/Professional mode contracts, remaining profile/branch verification, the planned FP/FN and efficiency/performance research, dedicated non-graph S4 architecture/security documentation, Maven/Montoya packaging and separately scheduled Burp validation. No Sprint 5 analyzer or finding classifier was added.

## 17. S4-PHASE3 active-engine product completion

**Result:** PARTIAL closure / all dependency-ready local items PASS  
**Evidence:** `docs/testing/artifacts/verification-s4-phase3-product-completion.txt`  
**Audit:** `docs/sprints/sprint-04-final-software-audit.md`

Phase 3 continued from the verified graph checkpoint. It added no graph, Observation, Evidence, executor or vulnerability implementation. Existing abstractions were extended with executable product profiles, four user modes, planner accounting, active coverage/efficiency, a narrowly scoped local-development consent policy, research records and one product workspace composing the current planner/queue/executor.

Focused verification closes every named profile and selection branch, queue lifecycle and dependency behavior, local consent boundaries, 429/503/Retry-After/connection failure handling, expected-decision precedence/conflict/unknown policy, coverage/efficiency and labelled TP/TN/FP/FN metric semantics. An absent prerequisite was the only additional queue defect found; it now becomes a traceable `BLOCKED` entry instead of waiting forever.

The existing Swing suite was extended with Test Plan, Test Queue, Test Detail, Execution, Differential Result, Evidence, Safety and Experiment views. The controls update `ActiveEngineWorkspace`; the UI suite passes 26 assertions. The default extension intentionally has no active executor, and there is not yet an authorized UI action that imports selected passive context into `PlanningInput`, so actual Burp-session active use remains PARTIAL.

NEWLY EXECUTED verification passes 558 represented assertions: graph 87, S4 core 54, S4 security 52, S4 product 132, localhost 60, UI 26, core regression 37, Sprint 3 core 47, Sprint 3 adapter 11 and Sprint 2 52. The product suite includes explicit configured workspace plan → queue → existing executor → result/coverage. A fresh ACRA-Lab run retained the graph-hydrating milestone; both services were stopped and their ports were confirmed closed. Core/extension compilation is warning-clean for `--release 21`; exact JDK 21 and Maven remain unavailable.

`EXP-DIFF-001` and its labelled dataset are software-ready but research-deferred. TP/TN/FP/FN, precision, recall, F1, optimization and 100/1,000/10,000 performance values remain NOT MEASURED. The final 52-row audit classifies real Burp provisioning as deferred validation because the original S4 contract requires a fail-closed default until explicit configuration/consent; configured local workspace execution now passes. Historical S2/S3 Burp Level 3/4 status is unchanged. The final decision is **S4 SOFTWARE COMPLETE**; no Sprint 5 work was started.
