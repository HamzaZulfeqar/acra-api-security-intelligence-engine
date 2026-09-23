# Sprint 6 — Tenant Isolation and Advanced RBAC Architecture

Sprint 6 extends ACRA's S5 authorization intelligence with explicit multi-role, tenant-membership,
scope, permission, inheritance, rule and delegation models.

Core direction:

`Principal → TenantMembership / RoleAssignment → RoleHierarchy → RolePermissionAssignment → Permission → AuthorizationRule → Effective Policy → Tenant/RBAC Assessment → S5 FindingCandidate`

The architecture remains evidence-first. Role names, tenant inequality, HTTP status and graph topology
must not independently create vulnerability findings.

## Initial implemented foundation

- `AuthorizationScope` / `AuthorizationScopeType`
- `TenantMembership` / `TenantMembershipType`
- `RoleAssignment`
- `RoleInheritance`
- `Permission`
- `RolePermissionAssignment`
- `AuthorizationRule` / `AuthorizationRuleEffect`
- `Delegation`
- `AuthorizationPolicySnapshot`
- `RoleHierarchyResolver`
- `EffectiveRoleResolution`

The policy snapshot fingerprint excludes capture time and canonicalizes collection ordering so the same
policy content produces the same fingerprint regardless of caller list order.

## Fail-closed constraints

- cyclic role inheritance → CONFLICTING
- no applicable role assignment → INCOMPLETE
- tenant-scoped role assignment must not bleed into a different tenant
- global/shared assignments are explicit rather than inferred from role names
- explicit rule precedence requires both a numeric value and a provenance/source label
- S6 policy results remain separate from confirmed real-world vulnerability findings


## Policy-aware active planning bridge

Sprint 6 does not introduce a second active-testing engine. `S6PolicyPlanningBridge` adds policy-derived seeds to
the existing S4 `PlanningInput`; `TestPlanner`, `ExecutionQueue`, `MutationValidator`, budgets, rate controls,
kill switch and `TestExecutor` remain canonical.

Current automatically generated active mutation:
- `CROSS_TENANT` → one explicit tenant-path substitution, only when baseline and target policy decisions are resolved.

Current fail-closed boundary:
- `ROLE_COMPARISON` recommendations are retained, but generic active generation is skipped until credential-safe
  context substitution can reference an explicit alternate authenticated context without copying token material into
  `Mutation.originalValue` / `Mutation.mutatedValue`.


## Credential-safe ROLE_COMPARISON

Sprint 6 now supports automatic ROLE_COMPARISON for safe read-only endpoints through
`AUTHENTICATED_CONTEXT_SUBSTITUTION`.

The generated mutation stores:
- source role label
- target role label
- source `contextRef`
- target `contextRef`

It does not store bearer tokens, cookies, passwords or API keys.

At execution, `AuthenticatedContextSubstitutionResolver` resolves the target request only from the explicit
in-memory controls already attached to that SecurityTest. `RequestEquivalenceGuard` rejects the substitution if
endpoint, method, body, resource reference or non-authentication headers change.

This preserves a clean experiment:
same tenant + same resource + same operation + different authenticated role context.


## Product UI projection

`S6AuthorizationWorkspace` is the product-facing projection boundary between S6 authorization intelligence and
the Burp UI. It holds the explicitly loaded policy snapshot and deterministic analysis results keyed by resolution
ID. `S6AuthorizationPanel` renders that state into Authorization sub-views for tenant membership/delegation,
roles, hierarchy, permission catalog, effective authorization matrix, policy conflict and policy coverage.

No authorization policy is inferred in the UI. Empty policy state renders as NOT LOADED rather than guessed data.
