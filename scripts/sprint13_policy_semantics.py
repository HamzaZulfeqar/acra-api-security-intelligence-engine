#!/usr/bin/env python3
"""Configurable authorization-policy semantics for Sprint 13 Phase 3."""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

VALID_DIMENSIONS = {
    "OBJECT_AUTHORIZATION",
    "TENANT_AUTHORIZATION",
    "RBAC_AUTHORIZATION",
    "WORKFLOW_AUTHORIZATION",
    "ROUTING_AUTHORIZATION",
    "PROPERTY_AUTHORIZATION",
    "BATCH_AUTHORIZATION",
    "INDIRECT_REFERENCE_AUTHORIZATION",
}

def load_policy_registry(path: str | Path) -> dict[str, Any]:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    if value.get("schemaVersion") != "s13-policy-registry-v1":
        raise AssertionError("unexpected policy registry schema")
    policies = value.get("policies")
    if not isinstance(policies, list) or not policies:
        raise AssertionError("policy registry must contain policies")
    ids = set()
    for policy in policies:
        policy_id = policy.get("policyId")
        if not isinstance(policy_id, str) or not policy_id:
            raise AssertionError("policyId required")
        if policy_id in ids:
            raise AssertionError("duplicate policyId")
        ids.add(policy_id)
        if policy.get("dimension") not in VALID_DIMENSIONS:
            raise AssertionError(f"invalid policy dimension: {policy.get('dimension')}")
        if not isinstance(policy.get("pathRegex"), str):
            raise AssertionError("pathRegex required")
        re.compile(policy["pathRegex"])
    return value

def _match_policy(registry, dimension, case):
    method = str(case.get("method", "")).upper()
    path = str(case.get("path", ""))
    matches = []
    for policy in registry["policies"]:
        if policy["dimension"] != dimension:
            continue
        methods = {str(item).upper() for item in policy.get("methods", ["GET", "POST", "PUT", "PATCH", "DELETE"])}
        if method not in methods:
            continue
        if re.fullmatch(policy["pathRegex"], path):
            matches.append(policy)
    if len(matches) > 1:
        priorities = sorted(matches, key=lambda item: (-int(item.get("priority", 0)), item["policyId"]))
        if len(priorities) > 1 and int(priorities[0].get("priority", 0)) == int(priorities[1].get("priority", 0)):
            return None, "AMBIGUOUS_POLICY"
        return priorities[0], "MATCHED"
    if not matches:
        return None, "NO_POLICY"
    return matches[0], "MATCHED"

def _target_item(case, observation):
    body = observation.get("body")
    oracle = case.get("oracle") or {}
    if oracle.get("type") != "JSON_LIST_DECISION" or not isinstance(body, dict):
        return None
    items = body.get(oracle.get("listField"))
    if not isinstance(items, list):
        return None
    for item in items:
        if isinstance(item, dict) and item.get(oracle.get("matchField")) == oracle.get("matchValue"):
            return item
    return None

def _decision(decision, policy=None, reasons=None, status="MATCHED"):
    return {
        "decision": decision,
        "policyId": None if policy is None else policy["policyId"],
        "status": status,
        "reasons": list(reasons or []),
    }

def evaluate_policy(registry, dimension, case, observation):
    """Return ALLOW / DENY / UNKNOWN based only on explicit configured policy."""
    policy, match_status = _match_policy(registry, dimension, case)
    if policy is None:
        return _decision("UNKNOWN", status=match_status, reasons=[match_status])

    actor = case.get("actor") if isinstance(case.get("actor"), dict) else {}
    role = str(actor.get("role", ""))
    principal = actor.get("sub")
    subject_tenant = actor.get("tenant_id")
    request = case.get("requestBody") if isinstance(case.get("requestBody"), dict) else {}
    body = observation.get("body") if isinstance(observation.get("body"), dict) else {}
    rules = policy.get("rules") if isinstance(policy.get("rules"), dict) else {}
    reasons = []

    if dimension == "OBJECT_AUTHORIZATION":
        owner_field = rules.get("ownerField", "owner_id")
        owner = body.get(owner_field)
        if owner is None:
            return _decision("UNKNOWN", policy, ["OWNER_UNAVAILABLE"])
        if principal == owner:
            return _decision("ALLOW", policy, ["OWNER_MATCH"])
        if role in set(rules.get("privilegedRoles", [])):
            return _decision("ALLOW", policy, ["PRIVILEGED_ROLE"])
        return _decision("DENY", policy, ["OWNER_MISMATCH"])

    if dimension == "TENANT_AUTHORIZATION":
        tenant_field = rules.get("tenantField", "tenant_id")
        target_tenant = body.get(tenant_field)
        if target_tenant is None:
            path_group = rules.get("targetTenantPathGroup")
            if path_group:
                match = re.fullmatch(policy["pathRegex"], str(case.get("path", "")))
                if match is not None:
                    target_tenant = match.groupdict().get(path_group)
        if target_tenant is None:
            return _decision("UNKNOWN", policy, ["TARGET_TENANT_UNAVAILABLE"])
        if target_tenant == subject_tenant:
            return _decision("ALLOW", policy, ["SAME_TENANT"])
        if role in set(rules.get("crossTenantRoles", [])):
            return _decision("ALLOW", policy, ["CROSS_TENANT_ROLE"])
        delegated = actor.get("delegated_tenants")
        if isinstance(delegated, list) and role in set(rules.get("delegatedRoles", [])) and target_tenant in delegated:
            return _decision("ALLOW", policy, ["DELEGATED_TENANT"])
        return _decision("DENY", policy, ["TENANT_MISMATCH"])

    if dimension == "RBAC_AUTHORIZATION":
        allowed = set(rules.get("allowedRoles", []))
        denied = set(rules.get("deniedRoles", []))
        if role in denied:
            return _decision("DENY", policy, ["EXPLICIT_ROLE_DENY"])
        if role in allowed:
            return _decision("ALLOW", policy, ["ROLE_ALLOWED"])
        if rules.get("default") == "ALLOW":
            return _decision("ALLOW", policy, ["DEFAULT_ALLOW"])
        return _decision("DENY", policy, ["ROLE_NOT_ALLOWED"])

    if dimension == "WORKFLOW_AUTHORIZATION":
        action = request.get("action")
        from_state = request.get("from_state")
        to_state = request.get("to_state")
        if action is None or from_state is None or to_state is None:
            return _decision("UNKNOWN", policy, ["TRANSITION_UNAVAILABLE"])
        transitions = rules.get("transitions", [])
        matched_transition = None
        for transition in transitions:
            if (
                transition.get("action") == action
                and transition.get("from") == from_state
                and transition.get("to") == to_state
            ):
                matched_transition = transition
                break
        if matched_transition is None:
            return _decision("DENY", policy, ["TRANSITION_NOT_REGISTERED"])
        if role not in set(matched_transition.get("roles", [])):
            return _decision("DENY", policy, ["TRANSITION_ROLE_NOT_ALLOWED"])
        for required_flag in matched_transition.get("requiredTrueFields", []):
            if request.get(required_flag) is not True:
                return _decision("DENY", policy, [f"REQUIRED_FLAG_FALSE:{required_flag}"])
        return _decision("ALLOW", policy, ["TRANSITION_ALLOWED"])

    if dimension == "PROPERTY_AUTHORIZATION":
        role_fields = rules.get("allowedFieldsByRole", {})
        allowed = set(role_fields.get(role, role_fields.get("*", [])))
        requested = set(request.keys())
        if not requested:
            return _decision("UNKNOWN", policy, ["NO_REQUEST_FIELDS"])
        forbidden = sorted(requested - allowed)
        if forbidden:
            return _decision("DENY", policy, ["FORBIDDEN_FIELDS:" + ",".join(forbidden)])
        return _decision("ALLOW", policy, ["FIELDS_ALLOWED"])

    if dimension == "ROUTING_AUTHORIZATION":
        allowed_roles = set(rules.get("allowedRoles", []))
        canonical_roles = set(rules.get("canonicalRoles", rules.get("allowedRoles", [])))
        path = str(case.get("path", ""))
        normalized = re.sub(r"/{2,}", "/", path)
        route_equivalent = normalized != path
        if route_equivalent:
            if role in allowed_roles:
                return _decision("ALLOW", policy, ["EQUIVALENT_ROUTE_ROLE_ALLOWED"])
            return _decision("DENY", policy, ["EQUIVALENT_ROUTE_ROLE_DENIED"])
        if role in canonical_roles:
            return _decision("ALLOW", policy, ["CANONICAL_ROUTE_ROLE_ALLOWED"])
        return _decision("DENY", policy, ["CANONICAL_ROUTE_ROLE_DENIED"])

    if dimension == "BATCH_AUTHORIZATION":
        item = _target_item(case, observation)
        if not isinstance(item, dict):
            return _decision("UNKNOWN", policy, ["TARGET_ITEM_UNAVAILABLE"])
        owner = item.get(rules.get("ownerField", "owner_id"))
        if owner is None:
            return _decision("UNKNOWN", policy, ["ITEM_OWNER_UNAVAILABLE"])
        if owner == principal:
            return _decision("ALLOW", policy, ["ITEM_OWNER_MATCH"])
        if role in set(rules.get("privilegedRoles", [])):
            return _decision("ALLOW", policy, ["BATCH_PRIVILEGED_ROLE"])
        return _decision("DENY", policy, ["ITEM_OWNER_MISMATCH"])

    if dimension == "INDIRECT_REFERENCE_AUTHORIZATION":
        resource = body.get(rules.get("resourceField", "resource"))
        if not isinstance(resource, dict):
            return _decision("UNKNOWN", policy, ["RESOLVED_RESOURCE_UNAVAILABLE"])
        owner = resource.get(rules.get("ownerField", "owner_id"))
        if owner is None:
            return _decision("UNKNOWN", policy, ["RESOLVED_OWNER_UNAVAILABLE"])
        if owner == principal:
            return _decision("ALLOW", policy, ["RESOLVED_OWNER_MATCH"])
        if role in set(rules.get("privilegedRoles", [])):
            return _decision("ALLOW", policy, ["INDIRECT_PRIVILEGED_ROLE"])
        return _decision("DENY", policy, ["RESOLVED_OWNER_MISMATCH"])

    return _decision("UNKNOWN", policy, ["UNSUPPORTED_DIMENSION"])

def apply_policy_to_prediction(locked_prediction, policy_result):
    """Explicit policy supersedes locked A7 only when policy is decisive."""
    observed = locked_prediction.get("observedDecision")
    original = locked_prediction.get("prediction")
    decision = policy_result.get("decision")
    reasons = list(locked_prediction.get("reasons", []))

    if decision == "ALLOW":
        reasons.append("EXPLICIT_POLICY_ALLOW")
        return {
            **locked_prediction,
            "prediction": "NEGATIVE",
            "reasons": reasons,
            "policyDecision": "ALLOW",
            "policyId": policy_result.get("policyId"),
            "policyReasons": policy_result.get("reasons", []),
        }

    if decision == "DENY" and observed == "ALLOW":
        reasons.append("EXPLICIT_POLICY_DENY_OBSERVED_ALLOW")
        return {
            **locked_prediction,
            "prediction": "POSITIVE",
            "reasons": reasons,
            "policyDecision": "DENY",
            "policyId": policy_result.get("policyId"),
            "policyReasons": policy_result.get("reasons", []),
        }

    reasons.append("EXPLICIT_POLICY_UNKNOWN_FALLBACK_A7")
    return {
        **locked_prediction,
        "prediction": original,
        "reasons": reasons,
        "policyDecision": "UNKNOWN",
        "policyId": policy_result.get("policyId"),
        "policyReasons": policy_result.get("reasons", []),
    }
