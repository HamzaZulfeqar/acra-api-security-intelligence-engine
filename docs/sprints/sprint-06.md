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
