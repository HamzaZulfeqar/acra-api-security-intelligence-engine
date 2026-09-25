# Sprint 13 Phase 6A — Cross-Framework-Shaped Normalization Protocol

**Protocol ID:** ACRA-S13-XFRAME-v1  
**Branch:** `s13-cross-framework-generalization`  
**Phase 5 completion base:** `8f57460aabd62d7c009838d8207dd702aad651a1`  
**Normalization freeze:** `4011b9c05b99b14da66733aea47de43256060990`  
**Development workflow:** `36173526384` — SUCCESS.

## Scope boundary

Phase 6A tests framework-shaped request/response representations, not actual framework runtime execution.

The corpus models common serialization styles associated with:
- FastAPI-style Python JSON;
- Flask-style Python JSON;
- Express-style camelCase JSON;
- Spring-style camelCase JSON.

No claim is made that FastAPI, Flask, Express or Spring applications were actually launched in Phase 6A.

Actual framework runtime execution is a separate Phase 6B gate.

## Research question

Can a framework-neutral normalization boundary preserve the frozen Sprint 13 authorization reasoning and governance
semantics across snake_case and camelCase transport representations without teaching the classifier framework names?

## Frozen upstream stack

Unchanged:
- Sprint 12 A7;
- Phase 2 dimension inference;
- Phase 3 policy semantics;
- Phase 5 uncertainty governance.

Phase 6A adds only a pre-reasoning normalization boundary.

## Normalization contract

The normalizer canonicalizes common transport aliases including:
- `ownerId -> owner_id`;
- `tenantId -> tenant_id`;
- `requiredRole -> required_role`;
- `fromState -> from_state`;
- `toState -> to_state`;
- `appliedProperties -> applied_properties`;
- `resolvedResourceId -> resolved_resource_id`;
- `resourceId -> resource_id`;
- `resourceIds -> resource_ids`;
- `userId/principalId -> sub`;
- `delegatedTenants -> delegated_tenants`;
- `routeForm -> route_form`.

JSON-list oracle field references are normalized alongside response-item keys.

The normalizer does not consume framework labels, registered dimensions or vulnerability labels.

## Development corpus

64 cases:
- four framework styles;
- eight authorization dimensions;
- one positive + one legitimate configured-policy control per dimension/style.

Development result:
- raw dimension: 60/64;
- raw disposition: 32/64;
- normalized dimension: 64/64;
- normalized disposition: 64/64.

By framework:
- FastAPI: raw 16/16 dimension, 16/16 disposition; normalized 16/16, 16/16;
- Flask: raw 16/16 dimension, 16/16 disposition; normalized 16/16, 16/16;
- Express: raw 14/16 dimension, 0/16 disposition; normalized 16/16, 16/16;
- Spring: raw 14/16 dimension, 0/16 disposition; normalized 16/16, 16/16.

Repeatability and blind-label gates passed; Maven package succeeded.

## Untouched evaluation rule

After normalization freeze, create a new 64-case evaluation corpus using different:
- endpoint/resource vocabulary;
- identity names;
- role names;
- tenant/workspace names;
- workflow/resource names;
- property names;
- policy IDs/versions.

The normalizer and frozen upstream stack may not change after evaluation is observed.

## Metrics

Report separately:
- raw dimension accuracy;
- normalized dimension accuracy;
- raw governed-disposition accuracy;
- normalized governed-disposition accuracy;
- per-framework raw/normalized results.

## Claim boundary

Even a perfect Phase 6A result establishes only robustness to the tested serialization representations.
It does not establish:
- real FastAPI/Flask/Express/Spring runtime behavior;
- middleware/router behavior;
- framework dependency/version compatibility;
- Burp runtime behavior;
- external-target generalization.

Phase 6B must execute actual framework runtimes before a runtime cross-framework claim is made.
