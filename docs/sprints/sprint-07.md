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
