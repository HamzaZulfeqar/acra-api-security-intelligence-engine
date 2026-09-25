#!/usr/bin/env python3
"""Sprint 13 Phase 5 policy reliability and uncertainty governance.

This layer does not change frozen A7, dimension inference, or Phase 3 policy semantics.
It governs whether their evidence is strong enough to become an actionable finding.
"""
from __future__ import annotations

import re
from typing import Any

CANDIDATE = "VULNERABILITY_CANDIDATE"
AUTHORIZED = "AUTHORIZED_CONTROL"
ENFORCED = "CONTROL_ENFORCED"
POLICY_GAP = "POLICY_GAP"
AMBIGUOUS = "AMBIGUOUS_POLICY"
STALE = "STALE_POLICY"
INCOMPLETE = "INCOMPLETE_CONTEXT"
INCONCLUSIVE = "INCONCLUSIVE"

REVIEW_DISPOSITIONS = {
    POLICY_GAP,
    AMBIGUOUS,
    STALE,
    INCOMPLETE,
    INCONCLUSIVE,
}

MISSING_REASON_MARKERS = {
    "OWNER_UNAVAILABLE",
    "TARGET_TENANT_UNAVAILABLE",
    "TRANSITION_UNAVAILABLE",
    "NO_REQUEST_FIELDS",
    "TARGET_ITEM_UNAVAILABLE",
    "ITEM_OWNER_UNAVAILABLE",
    "RESOLVED_RESOURCE_UNAVAILABLE",
    "RESOLVED_OWNER_UNAVAILABLE",
}

HEALTH_SCORES = {
    "CURRENT": 1.0,
    "STALE": 0.25,
    "UNKNOWN": 0.0,
}

def _response_body(observation: dict[str, Any]) -> dict[str, Any]:
    body = observation.get("body")
    return body if isinstance(body, dict) else {}

def _request_body(case: dict[str, Any]) -> dict[str, Any]:
    body = case.get("requestBody")
    return body if isinstance(body, dict) else {}

def _target_item(case: dict[str, Any], observation: dict[str, Any]) -> dict[str, Any] | None:
    body = _response_body(observation)
    oracle = case.get("oracle")
    if not isinstance(oracle, dict) or oracle.get("type") != "JSON_LIST_DECISION":
        return None
    items = body.get(oracle.get("listField"))
    if not isinstance(items, list):
        return None
    for item in items:
        if isinstance(item, dict) and item.get(oracle.get("matchField")) == oracle.get("matchValue"):
            return item
    return None

def _policy_by_id(registry: dict[str, Any], policy_id: str | None) -> dict[str, Any] | None:
    if not policy_id:
        return None
    for policy in registry.get("policies", []):
        if isinstance(policy, dict) and policy.get("policyId") == policy_id:
            return policy
    return None

def _policy_health(policy: dict[str, Any] | None) -> dict[str, Any]:
    if policy is None:
        return {
            "status": "UNKNOWN",
            "score": 0.0,
            "policyVersion": None,
            "sourceRevision": None,
            "lastValidatedAt": None,
        }
    health = policy.get("health")
    health = health if isinstance(health, dict) else {}
    status = str(health.get("status", "UNKNOWN")).upper()
    return {
        "status": status,
        "score": HEALTH_SCORES.get(status, 0.0),
        "policyVersion": health.get("policyVersion"),
        "sourceRevision": health.get("sourceRevision"),
        "lastValidatedAt": health.get("lastValidatedAt"),
    }

def _missing_context(
    dimension: str,
    case: dict[str, Any],
    observation: dict[str, Any],
    policy: dict[str, Any] | None,
) -> list[str]:
    actor = case.get("actor")
    actor = actor if isinstance(actor, dict) else {}
    request = _request_body(case)
    body = _response_body(observation)
    rules = {}
    if isinstance(policy, dict) and isinstance(policy.get("rules"), dict):
        rules = policy["rules"]

    missing: list[str] = []

    if not actor.get("sub"):
        missing.append("principal")

    if dimension == "OBJECT_AUTHORIZATION":
        owner_field = rules.get("ownerField", "owner_id")
        if body.get(owner_field) is None:
            missing.append(owner_field)

    elif dimension == "TENANT_AUTHORIZATION":
        target = body.get(rules.get("tenantField", "tenant_id"))
        path_group = rules.get("targetTenantPathGroup")
        if target is None and path_group and isinstance(policy, dict):
            match = re.fullmatch(str(policy.get("pathRegex", "")), str(case.get("path", "")))
            if match is not None:
                target = match.groupdict().get(path_group)
        if target is None:
            missing.append("target_tenant")
        if actor.get("tenant_id") is None:
            missing.append("subject_tenant")

    elif dimension == "RBAC_AUTHORIZATION":
        if not actor.get("role"):
            missing.append("role")

    elif dimension == "WORKFLOW_AUTHORIZATION":
        for key in ("action", "from_state", "to_state"):
            if request.get(key) is None:
                missing.append(key)
        if not actor.get("role"):
            missing.append("role")

    elif dimension == "PROPERTY_AUTHORIZATION":
        if not request:
            missing.append("request_fields")
        if not actor.get("role"):
            missing.append("role")

    elif dimension == "ROUTING_AUTHORIZATION":
        if not actor.get("role"):
            missing.append("role")

    elif dimension == "BATCH_AUTHORIZATION":
        item = _target_item(case, observation)
        if item is None:
            missing.append("target_item")
        else:
            owner_field = rules.get("ownerField", "owner_id")
            if item.get(owner_field) is None:
                missing.append(owner_field)

    elif dimension == "INDIRECT_REFERENCE_AUTHORIZATION":
        resource_field = rules.get("resourceField", "resource")
        resource = body.get(resource_field)
        if not isinstance(resource, dict):
            missing.append(resource_field)
        else:
            owner_field = rules.get("ownerField", "owner_id")
            if resource.get(owner_field) is None:
                missing.append(owner_field)

    return sorted(set(missing))

def _result(
    disposition: str,
    *,
    actionable: bool,
    review: bool,
    policy_result: dict[str, Any],
    health: dict[str, Any],
    missing: list[str],
    reason: str,
    observed_decision: str | None,
    locked_prediction: str | None,
    dimension_confidence: str | None,
) -> dict[str, Any]:
    return {
        "disposition": disposition,
        "actionableFinding": actionable,
        "reviewRequired": review,
        "reason": reason,
        "policyDecision": policy_result.get("decision"),
        "policyStatus": policy_result.get("status"),
        "policyId": policy_result.get("policyId"),
        "policyReasons": list(policy_result.get("reasons", [])),
        "policyHealth": health,
        "missingContext": missing,
        "observedDecision": observed_decision,
        "lockedA7Prediction": locked_prediction,
        "dimensionConfidence": dimension_confidence,
    }

def govern(
    *,
    registry: dict[str, Any],
    dimension: str,
    dimension_confidence: str,
    case: dict[str, Any],
    observation: dict[str, Any],
    locked_prediction: dict[str, Any],
    policy_result: dict[str, Any],
) -> dict[str, Any]:
    """Convert detection/policy evidence into a governed disposition.

    Ordering is intentionally conservative:
    missing/ambiguous policy -> review state;
    stale policy -> review state;
    incomplete evidence -> review state;
    low-confidence dimension -> inconclusive;
    only a CURRENT, decisive policy may produce an actionable vulnerability candidate.
    """
    observed = locked_prediction.get("observedDecision")
    locked_label = locked_prediction.get("prediction")
    status = str(policy_result.get("status", ""))
    decision = str(policy_result.get("decision", "UNKNOWN"))
    policy = _policy_by_id(registry, policy_result.get("policyId"))
    health = _policy_health(policy)

    if status == "NO_POLICY":
        return _result(
            POLICY_GAP,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="NO_MATCHING_POLICY",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if status == "AMBIGUOUS_POLICY":
        return _result(
            AMBIGUOUS,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="MULTIPLE_EQUAL_PRIORITY_POLICIES",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if health["status"] != "CURRENT":
        return _result(
            STALE if health["status"] == "STALE" else INCONCLUSIVE,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="POLICY_NOT_CURRENT",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    missing = _missing_context(dimension, case, observation, policy)
    if missing:
        return _result(
            INCOMPLETE,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=missing,
            reason="REQUIRED_AUTHORIZATION_CONTEXT_MISSING",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if decision == "UNKNOWN" or any(
        marker in set(policy_result.get("reasons", []))
        for marker in MISSING_REASON_MARKERS
    ):
        return _result(
            INCONCLUSIVE,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=missing,
            reason="POLICY_DECISION_UNCERTAIN",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if str(dimension_confidence).upper() == "LOW":
        return _result(
            INCONCLUSIVE,
            actionable=False,
            review=True,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="LOW_DIMENSION_CONFIDENCE",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if decision == "ALLOW":
        return _result(
            AUTHORIZED,
            actionable=False,
            review=False,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="CURRENT_POLICY_EXPLICIT_ALLOW",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if decision == "DENY" and observed == "ALLOW":
        return _result(
            CANDIDATE,
            actionable=True,
            review=False,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="CURRENT_POLICY_DENY_BUT_OPERATION_ALLOWED",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    if decision == "DENY" and observed == "DENY":
        return _result(
            ENFORCED,
            actionable=False,
            review=False,
            policy_result=policy_result,
            health=health,
            missing=[],
            reason="CURRENT_POLICY_DENY_ENFORCED",
            observed_decision=observed,
            locked_prediction=locked_label,
            dimension_confidence=dimension_confidence,
        )

    return _result(
        INCONCLUSIVE,
        actionable=False,
        review=True,
        policy_result=policy_result,
        health=health,
        missing=[],
        reason="UNRESOLVED_AUTHORIZATION_STATE",
        observed_decision=observed,
        locked_prediction=locked_label,
        dimension_confidence=dimension_confidence,
    )
