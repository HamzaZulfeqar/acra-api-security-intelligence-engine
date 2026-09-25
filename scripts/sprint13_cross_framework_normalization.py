#!/usr/bin/env python3
"""Framework-neutral observable API normalization for Sprint 13 Phase 6A.

This module normalizes common transport/serialization aliases before the frozen
dimension, policy, and governance layers. It does not consume framework labels,
registered dimensions, or vulnerability labels.
"""
from __future__ import annotations
from typing import Any

KEY_ALIASES = {
    "ownerId": "owner_id",
    "tenantId": "tenant_id",
    "requiredRole": "required_role",
    "fromState": "from_state",
    "toState": "to_state",
    "appliedProperties": "applied_properties",
    "resolvedResourceId": "resolved_resource_id",
    "resourceId": "resource_id",
    "resourceIds": "resource_ids",
    "recordId": "record_id",
    "recordIds": "record_ids",
    "userId": "sub",
    "principalId": "sub",
    "delegatedTenants": "delegated_tenants",
    "routeForm": "route_form",
}

FORBIDDEN = {
    "dimension",
    "groundTruth",
    "expectedDisposition",
    "secureExpected",
    "vulnerableExpected",
    "expectedCandidate",
}

def normalize_value(value: Any) -> Any:
    if isinstance(value, list):
        return [normalize_value(item) for item in value]
    if not isinstance(value, dict):
        return value

    out: dict[str, Any] = {}
    for key, item in value.items():
        canonical = KEY_ALIASES.get(str(key), str(key))
        if canonical in out:
            raise AssertionError(f"normalization key collision: {canonical}")
        out[canonical] = normalize_value(item)
    return out

def normalize_case(case: dict[str, Any]) -> dict[str, Any]:
    leaked = FORBIDDEN.intersection(case)
    if leaked:
        raise AssertionError(f"cross-framework input contains forbidden keys: {sorted(leaked)}")
    result = normalize_value(case)
    if not isinstance(result, dict):
        raise AssertionError("normalized case must remain an object")

    # JSON-list oracle metadata contains field names as string values rather
    # than dictionary keys. Normalize those references to the same canonical
    # names used by normalized response items.
    oracle = result.get("oracle")
    if isinstance(oracle, dict) and oracle.get("type") == "JSON_LIST_DECISION":
        for field_ref in ("listField", "matchField", "decisionField"):
            value = oracle.get(field_ref)
            if isinstance(value, str):
                oracle[field_ref] = KEY_ALIASES.get(value, value)

    return result

def normalize_observation(observation: dict[str, Any]) -> dict[str, Any]:
    result = normalize_value(observation)
    if not isinstance(result, dict):
        raise AssertionError("normalized observation must remain an object")
    return result
