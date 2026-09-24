# Sprint 7 — Workflow Authorization, Delegation and Token Binding

Status: IN PROGRESS  
Branch: `s7-workflow-token-binding`  
Immutable base: `s6-final-software-2026-09-23` @ `8d996a2b2b1bb47372490945aec197dd9e20ff36`

## Sprint boundary

Sprint 7 owns workflow-state authorization and authenticated-context binding for workflow transitions.

It does not replace the completed Sprint 6 tenant/RBAC policy engine. Sprint 7 composes with the existing
`AuthorizationPolicySnapshot`, `Delegation`, Security Context and evidence architecture.

## Planned A–Z scope

1. source reconciliation against frozen S6;
2. workflow transition policy model;
3. deterministic transition authorization resolver;
4. current-state → target-state reasoning;
5. transition action binding;
6. required-role enforcement;
7. approval-required transition controls;
8. separation-of-duties controls;
9. terminal-state transition handling;
10. tenant-scoped workflow rules;
11. S6 delegation reuse and validation;
12. expired/invalid delegation fail-closed behavior;
13. credential-safe token-context binding by SHA-256 fingerprint/reference only;
14. no raw credential storage in workflow policy or request models;
15. policy conflict and precedence handling;
16. explicit default decision;
17. workflow evidence/provenance binding;
18. WorkflowAuthorizationResolution;
19. assessment/finding/risk integration;
20. controlled ACRA-Lab workflow fixtures;
21. secure/vulnerable labelled transition cases;
22. automatic WORKFLOW_TRANSITION test planning;
23. immutable mutation/request equivalence;
24. controlled live localhost validation;
25. differential evidence correlation;
26. workflow coverage matrix;
27. UI workflow map / transition matrix / conflicts / coverage;
28. deterministic report/export integration;
29. security-hardening tests;
30. performance observations;
31. retained S1–S6 regression;
32. final traceability/audit/package freeze.

## Phase 1 — foundation

Implemented in the first S7 slice:
- `WorkflowTransitionRule`
- `WorkflowTokenBinding`
- `WorkflowPolicySnapshot`
- `WorkflowAuthorizationRequest`
- `WorkflowAuthorizationResolution`
- `WorkflowAuthorizationResolver`
- explicit default DENY/ALLOW/UNKNOWN handling
- workflow transition matching
- role, approval, separation-of-duties and terminal-source checks
- delegation validation against the existing S6 authorization policy
- SHA-256-only token-context binding
- explicit allow/deny conflict and precedence handling
- deterministic resolution IDs
- focused exact-Java-21 verification

No real Burp runtime or external-target claim is introduced by this phase.


## Phase 1 verification

GitHub Actions run `35894118859` completed successfully on Temurin Java 21.

- `Sprint7WorkflowAuthorizationFoundationTestSuite`: PASS, 19 assertions
- exact Java 21 compilation with warnings as errors: PASS
- retained Sprint 6 foundation verification: PASS
- S5 final closure regression retained: PASS
- all S6 policy, orchestration, lab, planner, reporting, performance and security suites retained: PASS

Phase 1 is therefore complete. Sprint 7 remains IN PROGRESS because assessment/finding integration,
controlled workflow ACRA-Lab execution, automatic WORKFLOW_TRANSITION planning, UI/reporting closure,
performance/security finalization and package freeze remain outstanding.


## Phase 2 — workflow assessment, finding/risk integration and ground truth

Implemented:
- `WorkflowTransitionAssessment` and explicit assessment states
- deterministic assessment IDs
- `S7WorkflowAnalysisRequest` / `S7WorkflowAnalysisResult`
- `S7WorkflowOrchestrator`
- evidence/project/test/execution/observation ownership checks before candidate promotion
- workflow FindingCandidate projection
- deterministic risk prioritization through the existing `AuthorizationSeverityEvaluator`
- dimensions for workflow, approval, separation-of-duties, token binding and delegation context
- downstream token-context data minimization
- controlled `GT-S7-WORKFLOW-AUTHORIZATION.json` with ten independently declared cases

Candidate promotion remains conservative: policy conflict, incomplete policy, evidence ownership failure or
observation provenance failure yields INCONCLUSIVE rather than a vulnerability claim.


## Phase 2 verification

GitHub Actions run `35902076454` completed successfully.

- workflow/token-binding foundation: PASS (19 assertions)
- workflow assessment/finding/risk integration: PASS (19 assertions)
- exact Java 21 compilation with `-Xlint:all -Werror`: PASS
- retained Sprint 6 foundation: PASS
- S7 ground-truth contract: ten controlled localhost cases
- raw token material: excluded from downstream analysis serialization
- token-context fingerprint: minimized out of downstream S7 result
- cross-project evidence ownership mismatch: INCONCLUSIVE, not Candidate

Phase 2 is complete. Sprint 7 remains IN PROGRESS.

## Phase 3 — controlled workflow planning and execution

Implemented:
- controlled secure and deliberately vulnerable ACRA-Lab workflow-transition fixtures
- automatic policy-aware `WORKFLOW_TRANSITION` seed generation
- `S7WorkflowPlanningCandidate`, `S7WorkflowTestSeedFactory` and `S7WorkflowPlanningBridge`
- one-variable target-state mutation: `SUBMITTED → APPROVED`
- immutable request-equivalence enforcement through the existing Sprint 4 safety path
- explicit `STATE_CHANGING` safety classification
- automatic DELETE rejection for S7 workflow seed generation
- fail-closed behavior for conflicting/incomplete workflow policy
- execution through the existing planner, queue, safety validator and `TestExecutor`
- secure/vulnerable differential evidence correlation
- raw bearer material excluded from serialized generated-test state

The controlled fixture is intentionally non-persistent. It models state-changing authorization semantics while
remaining deterministic and repeatable for localhost research validation.

## Phase 3 verification

GitHub Actions run `35958482546` completed successfully on the current Sprint 7 branch.

- Sprint 7 workflow/token-binding foundation: PASS
- Sprint 7 lab source/ground-truth contract: PASS
- `Sprint7WorkflowPlannerExecutionIntegrationTestSuite`: PASS, 17 assertions
- secure ACRA-Lab transition bypass attempt: expected DENY, observed DENY, `EXPECTED_CHANGE`
- deliberately vulnerable ACRA-Lab transition bypass attempt: expected DENY, observed ALLOW, `UNEXPECTED_CHANGE`
- conflicting workflow policy: no executable seed generated
- retained Sprint 6 foundation/regression: PASS
- controlled workflow logs uploaded by CI

Phase 3 is complete. Sprint 7 remains IN PROGRESS; workflow coverage/product UI, deterministic report/export,
security/performance closure, traceability and final package freeze remain outstanding.

## Phase 4 — workflow coverage intelligence and product UI

Implemented:
- deterministic `WorkflowTransitionCoverageEntry` identities derived from policy + workflow context
- explicit coverage lifecycle: `POLICY_ONLY → PLANNED → ATTEMPTED → OBSERVED`
- `WorkflowTransitionCoverageMatrix` with deterministic ordering
- `WorkflowCoverageSummary` with resolution/planning/attempt/observation ratios
- unresolved, missing-planning and missing-observation gap views
- `S7WorkflowCoverageTracker` binding policy resolution → generated test → execution → observation evidence
- fail-closed rejection of policy-resolution drift for an existing coverage identity
- execution cannot be credited unless the matching workflow test was previously registered as planned
- live secure/vulnerable Phase 3 executions now hydrate the same workflow coverage context
- `S7WorkflowWorkspace` and immutable product snapshot
- Burp `Workflow` product area with Overview / Workflow Map / Transition Matrix / Policy Conflicts / Coverage
- coverage remains explicitly separate from vulnerability severity and finding confirmation
- backward-compatible `AcraSuiteTab` constructors preserve prior extension integrations

### Phase 4 verification

GitHub Actions run `35959028026`:
- focused workflow coverage model: PASS
- live controlled planning/execution + coverage hydration: PASS
- retained Sprint 6 regression: PASS

GitHub Actions run `35959256852`:
- Sprint 7 foundation: PASS
- controlled lab contract/execution: PASS
- retained Sprint 6 foundation: PASS
- Maven extension `test-compile`: PASS
- retained Sprint 4 UI: PASS
- retained Sprint 6 Authorization UI: PASS
- Sprint 7 Workflow UI: PASS
- controlled workflow logs: uploaded

Phase 4 is complete. Sprint 7 remains IN PROGRESS. Deterministic workflow report/export, expanded
security/performance closure, final traceability/regression and package freeze remain outstanding.
