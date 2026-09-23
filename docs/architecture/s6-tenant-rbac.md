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
