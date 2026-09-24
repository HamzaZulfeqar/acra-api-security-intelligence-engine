# ACRA Project State

## Current state — 2026-09-24

**Current sprint:** Sprint 8 — Routing Normalization & Authorization-Path Intelligence.  
**Decision:** S8 IN PROGRESS — Phase 3 VERIFIED.  
**Working branch:** `s8-routing-normalization`.  
**Immutable Sprint 7 base:** `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`.  
**Sprint 7:** SOFTWARE COMPLETE and frozen separately.  
**Sprint 7 canonical final verification:** GitHub Actions run `35961314354`.  
**Sprint 7 canonical checkpoint SHA-256:** `5e77654d6467cc45c60825cb44bd7c3aa1d4f33c8705d41216d7f258a8187189`.

### Sprint 8 verified progress

The existing Sprint 3 route/template/equivalence layer is reused. New source adds an evidence-backed processing
trace across `RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION`.

Current candidate capabilities:
- explicit processing-stage observations with provenance evidence;
- deterministic stage ordering and missing-stage accounting;
- representation change vs route-family variation vs canonical divergence;
- stage-gap attribution marked INCONCLUSIVE;
- path-only observation boundary;
- duplicate-stage and provenance-free observations rejected;
- no bypass generation, external-target probing or vulnerability promotion.

Phase 1 verification: GitHub Actions run `35961851498` — SUCCESS.

Phase 2 verification: GitHub Actions run `35965612498` — SUCCESS.

Phase 2 adds:
- evidence-backed path/method/host/API-version/authorization observations across routing stages;
- routing-only vs authorization-only vs combined boundary differential states;
- fail-closed handling for stage gaps and unknown authorization context;
- host/path input hardening;
- retained Sprint 3 route/core and Sprint 7 foundation regressions.

No bypass generation, external-target probing or vulnerability promotion is claimed by Phase 2.

Phase 3 verification: GitHub Actions run `35966112820` — SUCCESS.

Phase 3 adds:
- controlled routing ground truth for canonical and duplicate-separator route representations;
- safe read-only `EQUIVALENT_ROUTE_REPRESENTATION` execution through the existing active engine;
- explicit scope authorization for both route forms;
- secure DENY / `NO_CHANGE` control;
- deliberately vulnerable ALLOW / `UNEXPECTED_CHANGE` differential;
- retained Sprint 3 core and Sprint 7 regression PASS.

Phase 3 remains localhost-only and does not establish real-world routing/proxy behavior.

### Sprint 7 frozen closure

- canonical final workflow: GitHub Actions run `35961314354` — SUCCESS
- canonical final checkpoint: `acra-sprint-07-final.zip`
- canonical SHA-256: `5e77654d6467cc45c60825cb44bd7c3aa1d4f33c8705d41216d7f258a8187189`
- entries: 791
- unsafe paths: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- real Burp desktop runtime: UNVERIFIED / DEFERRED

### Implemented S6 capabilities

- authorization scope taxonomy
- tenant membership and multi-role assignment
- role hierarchy with cycle-safe inheritance
- permission and role-permission models
- explicit ALLOW / DENY rules
- explicit default decision and evidence-backed precedence
- global, shared and delegated authorization
- effective role and permission resolution
- tenant relationship classification
- tenant-isolation and RBAC assessments
- explicit privileged-action role-escalation assessment
- policy-conflict assessment
- policy coverage and effective authorization matrix
- S6 composition into the existing S5 FindingCandidate and severity/risk architecture
- policy hydration into the existing SecurityContextGraph with atomic evidence preflight
- deterministic root-cause grouping foundation
- policy-import plugin boundary and manual policy builder
- controlled S6 ACRA-Lab ground truth and secure/vulnerable tenant/RBAC fixtures
- policy-aware planning recommendations
- live controlled tenant/RBAC experiment and measurement pipeline
- policy-aware S6 planning bridge into the existing S4 planner/queue/executor
- deterministic safe read-only CROSS_TENANT test generation
- live generated-test execution against secure localhost ACRA-Lab
- S6 authorization product workspace and Burp UI projection
- Authorization Overview / Policy / Tenant Map / Roles / Hierarchy / Permissions / Effective Permissions / Conflicts / Coverage views
- deterministic S6 authorization report model
- canonical secret-safe JSON report export + SHA-256
- deterministic Markdown review report
- Reporter plugin adapter and UI Report / JSON Export projections
- credential-safe authenticated-context substitution implemented for safe read-only ROLE_COMPARISON
- ROLE_COMPARISON now generates and executes through explicit contextRef controls without raw credentials in Mutation

### Newly verified planner/execution evidence

GitHub Actions run `35882729813` completed successfully on exact Temurin JDK 21.0.12.1.
`Sprint6PlannerExecutionIntegrationTestSuite` passed 15 assertions and proved a generated CROSS_TENANT test can
flow through the existing S4 planner, queue, safety validation and executor into the secure localhost ACRA-Lab.
Generated test `S6-AUTO-TENANT-a915a78d9e3fd8c06e49c4e9` completed with expected DENY,
`EXPECTED_CHANGE`, and queue state `COMPLETED`.

### Newly measured S6 evidence

GitHub Actions run `35878508170` completed successfully on exact Temurin JDK 21.0.12.1.

`EXP-S6-TENANT-RBAC-001` executed ten labelled localhost cases.

- naive baseline: TP=2, TN=1, FP=7, FN=0, precision=0.222222, recall=1.000000, F1=0.363636
- ACRA S6: TP=2, TN=8, FP=0, FN=0, precision=1.000000, recall=1.000000, F1=1.000000

These are controlled-fixture measurements only. They do not establish real-world scanner accuracy.

The campaign also exposed and closed a SHARED-scope isolation defect before the successful run.

### S6 authorization UI verification

GitHub Actions run `35885676715` completed successfully at commit
`7458b37706bb5ebe24839f3bb7464ef88ed41f4b`.

- Sprint 6 core/live verification: PASS
- Maven extension test compilation: PASS
- retained `Sprint4UiTestSuite`: PASS, 26 checks
- `Sprint6AuthorizationUiTestSuite`: PASS, 22 assertions
- Authorization product area now renders policy, tenant map, role assignments, hierarchy, permissions,
  effective authorization matrix, conflict assessments and coverage without inventing missing policy state.

### S6 reporting/export verification

GitHub Actions run `35887862337` completed successfully at commit `b051f08612730a455375e75312ca70d6e5fc9f6f`.

- `Sprint6ReportingExportTestSuite`: PASS, 19 assertions
- canonical report JSON / digest / Markdown artifact generation: PASS
- Maven extension compilation: PASS
- retained `Sprint4UiTestSuite`: PASS, 26 checks
- `Sprint6AuthorizationUiTestSuite`: PASS, 27 assertions
- report UI and JSON export UI are backed by the same S6 authorization product workspace
- report boundary preserves FindingCandidate as review-only and records confirmedFindingCount=0

### S6 performance/security closure verified

GitHub Actions run `35890873379` completed successfully at commit `76389e13b996a02c9f7011b262364b5b03a9231d`.

- `Sprint6PerformanceObservationTestSuite`: PASS, 12 assertions
- `Sprint6SecurityHardeningTestSuite`: PASS, 13 assertions
- 100 / 1,000 / 10,000 policy-resolution workloads completed
- policy-aware recommendation and report-generation workloads completed
- missing / ambiguous / spoofed authenticated-context references rejected
- path / non-authentication header / resource drift rejected
- unchanged authentication material rejected
- raw viewer/admin credentials excluded from serialized test state
- conflicting policy produces no executable S6 active seed

Observed CI timings are engineering observations only:
- 100: resolve 34 ms, planning 1 ms, report 9 ms, approx memory delta 2,948,768 bytes
- 1,000: resolve 57 ms, planning 2 ms, report 6 ms, approx memory delta 19,825,512 bytes
- 10,000: resolve 169 ms, planning 4 ms, report 29 ms, approx memory delta 10,003,728 bytes

Memory deltas are JVM-process observations affected by allocation/GC and are not monotonic or release thresholds.

### Legacy Sprint 2 / Sprint 3 CI closure

The retained Sprint 2 and Sprint 3 local-contract test/lab sources were restored from repository commit
`07a3f60bc2bd2218d0749e497b4f0b9dc57a9c2c`; they were not reconstructed from chat.

Current CI:
- Sprint 2 run `35890873262`: SUCCESS; Maven package PASS, Sprint2 tests=52, EXP-INTEGRATION-001 local-lab PASS
- Sprint 3 run `35890873276`: SUCCESS; Maven package PASS, Sprint2 tests=52, Sprint3 core=47, Sprint3 adapter=11, EXP-RECON-001 local-lab PASS
- official Maven test compilation excludes the legacy local-Montoya-stub suites because those suites target the preserved stub contract rather than the current official Montoya test interface
- real Burp desktop runtime remains a separate UNVERIFIED/BLOCKED gate

### Sprint 7 verified progress

Phases 1–6 and the final Sprint 7 closure are complete on `s7-workflow-token-binding`.

Latest verified gate: GitHub Actions run `35960826621`.

Phase 3 now adds:
- controlled secure/vulnerable workflow transition fixtures
- automatic policy-aware `WORKFLOW_TRANSITION` planning
- one-variable target-state mutation with request-equivalence invariants
- state-changing safety classification without destructive auto-execution
- existing Sprint 4 planner/queue/safety/executor reuse
- secure DENY / `EXPECTED_CHANGE` control
- deliberately vulnerable ALLOW / `UNEXPECTED_CHANGE` control
- secret-safe generated-test serialization
- fail-closed conflict handling
- retained Sprint 6 regression PASS

Phase 4 now adds:
- deterministic workflow transition coverage identities and lifecycle
- policy/resolution → plan → execution → observation evidence tracking
- explicit unresolved/planning/observation coverage gaps and ratios
- fail-closed policy-resolution drift handling
- S7 workflow product workspace
- Burp Workflow Overview / Workflow Map / Transition Matrix / Policy Conflicts / Coverage views
- Maven extension compilation plus retained S4/S6 UI regressions

These are controlled localhost validation results only and do not establish real-world scanner accuracy.

### Sprint 7 final closure

- final workflow: GitHub Actions run `35960826621` — SUCCESS
- closure candidate checkpoint: `acra-sprint-07-final.zip`
- closure candidate SHA-256: `0f69ce2e770dc5010f17fe00474455d18f34de11e02460889d428a87a7af9b4d`
- package entries: 791
- unsafe paths: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- official Maven package: PASS
- retained Sprint 2 / Sprint 3 local-contract regressions: PASS
- retained Sprint 4 / Sprint 6 / Sprint 7 UI regressions: PASS
- Sprint 8: NOT STARTED

### Residual validation debt

Real Burp desktop runtime/load/handler/UI validation remains UNVERIFIED / DEFERRED. Controlled localhost fixtures
do not establish real-world scanner accuracy, production authentication behavior or external-target safety.

Canonical final-status checkpoint SHA-256: external sidecar generated by the final-status verification run.
