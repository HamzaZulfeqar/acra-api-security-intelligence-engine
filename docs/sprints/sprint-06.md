# Sprint 6 — Tenant Isolation, Advanced RBAC and Policy Intelligence

Status: **IN PROGRESS**  
Base checkpoint: `89ceb1eec0aca7ebb2c5db60b4bfce43251d0f84`  
Working branch: `s6-tenant-rbac`

## Completed architecture

Sprint 6 now includes explicit authorization scope, tenant membership, multi-role assignments, role hierarchy,
effective permissions, allow/deny policy, policy precedence/conflict preservation, global/shared/delegated access,
tenant relationship resolution, tenant-isolation assessment, RBAC assessment, explicit privileged-action
role-escalation assessment, policy coverage, effective authorization matrix, S5 FindingCandidate composition,
policy graph hydration and root-cause grouping foundation.

## Controlled-lab integration

The ACRA-Lab includes independent S6 ground truth and secure/vulnerable fixtures covering:
same-tenant access, secure cross-tenant denial, vulnerable cross-tenant allow, global admin, delegated admin,
shared resources, low-role privileged denial and deliberately vulnerable low-role privileged allow.

The policy-aware planning advisor maps resolved policy context into existing `CROSS_TENANT` and
`ROLE_COMPARISON` test contracts without treating global/delegated/shared cases as vulnerabilities.

## Measured experiment

`EXP-S6-TENANT-RBAC-001` completed successfully in GitHub Actions run `35878508170`.

| Treatment | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Naive baseline | 2 | 1 | 7 | 0 | 0.222222 | 1.000000 | 0.363636 |
| ACRA S6 | 2 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

The successful campaign followed correction of a shared-scope isolation defect found by the first live run.

## Remaining before S6 SOFTWARE COMPLETE

1. finish planner→controlled execution product integration
2. implement S6 UI surfaces
3. extend technical/machine-readable reporting
4. run larger performance workloads
5. expand negative-security/serialization coverage
6. finish ADRs and requirements traceability
7. run full S1–S6 final regression/package gates
8. produce final S6 audit
9. create reproducible S6 ZIP, manifest and SHA-256
10. keep Sprint 7 NOT STARTED until the S6 checkpoint is frozen


## Policy-aware planner → queue → executor integration

Implemented:
- `S6PolicyPlanningCandidate` explicit input contract
- `S6PolicyTestSeedFactory` deterministic policy-aware seed generation
- `S6PolicyPlanningBridge` that augments the existing S4 `PlanningInput` instead of replacing the planner
- automatic safe read-only `CROSS_TENANT` tenant-substitution generation when baseline and target policy decisions are resolved
- generated tests preserve the existing S4 planner, queue, safety policy, kill switch, budgets, concurrency, rate limits and executor
- localhost integration proof uses one shared resource identifier present independently in Tenant-A and Tenant-B so only the tenant path value changes
- raw credentials remain in explicit request controls and are never copied into `Mutation`
- generic `ROLE_COMPARISON` active generation deliberately fails closed until a credential-safe context substitution adapter exists; the current system does not serialize raw tokens into mutation values

The integration suite proves:
`S6 policy → generated TestSeed → S4 TestPlanner → ExecutionQueue → TestExecutor → live secure ACRA-Lab → differential observation`.


## Credential-safe automatic ROLE_COMPARISON

Implemented:
- `AUTHENTICATED_CONTEXT_SUBSTITUTION` mutation type
- `CONTEXT` mutation location
- `AuthenticatedContextSubstitutionResolver`
- automatic safe read-only ROLE_COMPARISON seed generation
- context-reference resolution from explicit SecurityTest controls
- non-authentication equivalence enforcement before dispatch
- localhost privileged read-only `/api/v1/s6/tenants/{tenant}/admin/summary` fixture
- live viewer→admin authenticated-context substitution proof
- credential-exclusion assertions covering both source and target synthetic tokens

No raw credential is copied into Mutation metadata.


## S6 authorization product UI — phase started

Implemented in the existing Burp suite-tab architecture:
- top-level `Authorization` product area
- Overview
- Policy
- Tenant Map
- Roles
- Role Hierarchy
- Permissions
- Effective Permissions
- Policy Conflicts
- Coverage

The UI consumes `S6AuthorizationWorkspace`, a core product projection that stores an explicit
`AuthorizationPolicySnapshot` plus deterministic S6 analysis results. The Swing layer does not invent
authorization facts or maintain a parallel policy engine.

UI tables expose policy evidence counts, effective roles/decisions, tenant relationships, conflict reasons and
coverage gaps. The Overview explicitly states that a FindingCandidate is not a confirmed vulnerability.


### Authorization UI verification evidence

GitHub Actions run `35885676715`:
- exact Java 21 S6 verification: PASS
- Maven extension compilation: PASS
- retained Sprint 4 UI regression: PASS (26 checks)
- Sprint 6 Authorization UI: PASS (22 assertions)

This verifies the headless Swing product projection only. It does not by itself establish a real Burp runtime/UI
session; historical Burp runtime validation remains a separate gate.


## S6 reporting and machine-readable export

Implemented:
- deterministic S6 authorization report model with content-derived report ID
- summary counts for expected ALLOW/DENY, conflicts, review candidates, risk assessments and coverage
- effective-authorization, policy-conflict and coverage projections
- FindingCandidate and risk export without promotion to confirmed vulnerability
- aggregated evidence-reference inventory
- canonical secret-safe JSON export through the existing DomainSerializer
- deterministic Markdown review report
- SHA-256 digest for JSON report artifact
- S6AuthorizationJsonReporter adapter using the existing Reporter plugin contract
- Authorization UI Report and JSON Export views backed by the same S6 product workspace
- CI artifact generation for JSON, digest and Markdown

The report explicitly records confirmedFindingCount=0. SARIF and Burp Issue export remain broader reporting-roadmap items and are not claimed complete in this slice.


### Reporting/export verification evidence

GitHub Actions run `35887862337`:
- Sprint 6 core/live verification: PASS
- `Sprint6ReportingExportTestSuite`: PASS (19 assertions)
- Maven extension compilation: PASS
- retained Sprint 4 UI regression: PASS (26 checks)
- Sprint 6 Authorization UI including Report / JSON Export: PASS (27 assertions)
- JSON, SHA-256 and Markdown report artifacts produced and uploaded by CI

This validates the deterministic S6 authorization-report slice. SARIF and Burp Issue export remain future reporting targets.


## S6 performance and security closure

Added dedicated exact-Java-21 verification for:
- 100 / 1,000 / 10,000 authorization-policy resolution workloads
- policy-aware planning recommendation workload
- S6 authorization report-generation workload
- approximate JVM memory delta observations
- credential-safe context-reference abuse cases
- missing / ambiguous / spoofed authenticated context references
- non-authentication path/header/resource drift rejection
- unchanged authentication-context rejection
- serialized raw-token exclusion
- conflicting-policy fail-closed planning

Performance values are environment-specific engineering observations only. No release latency threshold or general performance claim is introduced.
