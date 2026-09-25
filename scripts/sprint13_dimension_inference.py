#!/usr/bin/env python3
"""Observable-evidence authorization-dimension inference for Sprint 13 Phase 2.

The classifier must not consume the registered/ground-truth dimension. It uses only
endpoint shape, HTTP method, actor context, request structure and observed response
structure. Scores are deterministic and intentionally interpretable.
"""
from __future__ import annotations

import re
from typing import Any

DIMENSIONS = (
    "OBJECT_AUTHORIZATION",
    "TENANT_AUTHORIZATION",
    "RBAC_AUTHORIZATION",
    "WORKFLOW_AUTHORIZATION",
    "ROUTING_AUTHORIZATION",
    "PROPERTY_AUTHORIZATION",
    "BATCH_AUTHORIZATION",
    "INDIRECT_REFERENCE_AUTHORIZATION",
)

FORBIDDEN_KEYS = {
    "dimension",
    "groundTruth",
    "secureExpected",
    "vulnerableExpected",
    "expectedCandidate",
}

ADMIN_TOKENS = {"admin", "audit", "privileged", "management", "manage"}
TENANT_TOKENS = {"tenant", "tenants", "organization", "organizations", "org", "workspace", "workspaces"}
WORKFLOW_TOKENS = {"workflow", "workflows", "transition", "transitions", "approve", "approval"}
INDIRECT_TOKENS = {"share", "shares", "alias", "link", "links", "reference", "references"}
PROPERTY_TOKENS = {"profile", "profiles", "settings", "preferences", "attributes"}
BATCH_TOKENS = {"batch", "bulk", "multi"}

def _segments(path: str) -> list[str]:
    return [segment.lower() for segment in path.split("/") if segment]

def _body_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}

def _response_dict(observation: dict[str, Any]) -> dict[str, Any]:
    value = observation.get("body")
    return value if isinstance(value, dict) else {}

def _add(scores, evidence, dimension, points, reason):
    scores[dimension] += points
    evidence[dimension].append({"points": points, "reason": reason})

def observable_case(case: dict[str, Any]) -> dict[str, Any]:
    leaked = FORBIDDEN_KEYS.intersection(case)
    if leaked:
        raise AssertionError(f"dimension-inference input contains forbidden keys: {sorted(leaked)}")
    allowed = {"caseId", "family", "method", "path", "actor", "requestBody", "oracle"}
    return {key: case[key] for key in allowed if key in case}

def infer_dimension(case: dict[str, Any], observation: dict[str, Any]) -> dict[str, Any]:
    leaked = FORBIDDEN_KEYS.intersection(case)
    if leaked:
        raise AssertionError(f"dimension-inference input contains forbidden keys: {sorted(leaked)}")

    method = str(case.get("method", "")).upper()
    path = str(case.get("path", ""))
    segments = _segments(path)
    segment_set = set(segments)
    request = _body_dict(case.get("requestBody"))
    response = _response_dict(observation)
    actor = _body_dict(case.get("actor"))

    scores = {dimension: 0 for dimension in DIMENSIONS}
    evidence = {dimension: [] for dimension in DIMENSIONS}

    # Routing anomalies must dominate resource semantics when an equivalent path
    # carries structural separator/normalization evidence.
    path_without_scheme = path.replace("://", "__SCHEME__")
    if "//" in path_without_scheme:
        _add(scores, evidence, "ROUTING_AUTHORIZATION", 20, "duplicate-path-separator")
    if any(token in {"route", "routing", "rewrite", "normalized"} for token in segment_set):
        _add(scores, evidence, "ROUTING_AUTHORIZATION", 5, "routing-semantic-path-token")
    if response.get("route_form") is not None:
        _add(scores, evidence, "ROUTING_AUTHORIZATION", 7, "response-route-form")

    # Batch: list-valued object selection plus item-level authorization decisions.
    if any(token in segment_set for token in BATCH_TOKENS):
        _add(scores, evidence, "BATCH_AUTHORIZATION", 5, "batch-semantic-path-token")
    if any(isinstance(value, list) for value in request.values()):
        _add(scores, evidence, "BATCH_AUTHORIZATION", 4, "request-list-valued-selection")
    items = response.get("items")
    if isinstance(items, list):
        if any(isinstance(item, dict) and "decision" in item for item in items):
            _add(scores, evidence, "BATCH_AUTHORIZATION", 10, "response-per-item-decision")
        elif len(items) > 1:
            _add(scores, evidence, "BATCH_AUTHORIZATION", 2, "response-collection")

    # Workflow: state transition structure is stronger than endpoint naming.
    transition_keys = {"action", "from_state", "to_state"}
    if transition_keys.issubset(request):
        _add(scores, evidence, "WORKFLOW_AUTHORIZATION", 14, "request-transition-triple")
    if any(token in segment_set for token in WORKFLOW_TOKENS):
        _add(scores, evidence, "WORKFLOW_AUTHORIZATION", 5, "workflow-semantic-path-token")
    if {"from_state", "to_state"}.issubset(response) or "state" in response:
        _add(scores, evidence, "WORKFLOW_AUTHORIZATION", 3, "response-state-transition")

    # Indirect reference: alias/reference resolves to a distinct protected resource.
    if any(token in segment_set for token in INDIRECT_TOKENS):
        _add(scores, evidence, "INDIRECT_REFERENCE_AUTHORIZATION", 7, "indirect-reference-path-token")
    if "resolved_resource_id" in response:
        _add(scores, evidence, "INDIRECT_REFERENCE_AUTHORIZATION", 11, "response-resolved-resource-id")
    if isinstance(response.get("resource"), dict):
        _add(scores, evidence, "INDIRECT_REFERENCE_AUTHORIZATION", 5, "response-nested-resolved-resource")
    if "alias" in response:
        _add(scores, evidence, "INDIRECT_REFERENCE_AUTHORIZATION", 4, "response-alias")

    # Property authorization: field-level mutation/read surface.
    if method in {"PATCH", "PUT"}:
        _add(scores, evidence, "PROPERTY_AUTHORIZATION", 4, "field-mutation-http-method")
    if any(token in segment_set for token in PROPERTY_TOKENS):
        _add(scores, evidence, "PROPERTY_AUTHORIZATION", 6, "property-semantic-path-token")
    if request and method in {"PATCH", "PUT"}:
        scalar_fields = [key for key, value in request.items() if not isinstance(value, (dict, list))]
        if scalar_fields:
            _add(scores, evidence, "PROPERTY_AUTHORIZATION", 4, "scalar-property-mutation")
    if isinstance(response.get("applied_properties"), list):
        _add(scores, evidence, "PROPERTY_AUTHORIZATION", 10, "response-applied-properties")
    if "properties" in response and isinstance(response.get("properties"), list):
        _add(scores, evidence, "PROPERTY_AUTHORIZATION", 5, "response-property-policy")

    # RBAC: privilege-bearing surface plus explicit required-role or role echo.
    if any(token in segment_set for token in ADMIN_TOKENS):
        _add(scores, evidence, "RBAC_AUTHORIZATION", 12, "privileged-surface-path-token")
    if "required_role" in response:
        _add(scores, evidence, "RBAC_AUTHORIZATION", 12, "response-required-role")
    if response.get("role") is not None:
        _add(scores, evidence, "RBAC_AUTHORIZATION", 3, "response-role-context")
    area = str(response.get("area", "")).lower()
    if "admin" in area or "audit" in area:
        _add(scores, evidence, "RBAC_AUTHORIZATION", 5, "privileged-response-area")
    if actor.get("role"):
        _add(scores, evidence, "RBAC_AUTHORIZATION", 1, "actor-role-present")

    # Tenant: explicit tenant/organization scoping is stronger than generic
    # tenant_id fields that may appear on ordinary object responses.
    if any(token in segment_set for token in TENANT_TOKENS):
        _add(scores, evidence, "TENANT_AUTHORIZATION", 10, "tenant-scope-path-token")
    if "tenant_id" in response:
        _add(scores, evidence, "TENANT_AUTHORIZATION", 3, "response-tenant-id")
    if actor.get("tenant_id"):
        _add(scores, evidence, "TENANT_AUTHORIZATION", 1, "actor-tenant-context")
    if "reports" in segment_set and any(token in segment_set for token in TENANT_TOKENS):
        _add(scores, evidence, "TENANT_AUTHORIZATION", 2, "tenant-scoped-report-resource")

    # Object authorization: direct resource identity/ownership. Kept after more
    # specialized dimensions so generic owner/id metadata cannot dominate them.
    if "owner_id" in response:
        _add(scores, evidence, "OBJECT_AUTHORIZATION", 7, "response-owner-id")
    if "id" in response:
        _add(scores, evidence, "OBJECT_AUTHORIZATION", 3, "response-resource-id")
    if method in {"GET", "DELETE", "PATCH", "PUT"} and len(segments) >= 2:
        _add(scores, evidence, "OBJECT_AUTHORIZATION", 2, "direct-resource-http-shape")
    if response.get("resource_id") is not None and "resolved_resource_id" not in response:
        _add(scores, evidence, "OBJECT_AUTHORIZATION", 2, "response-resource-id-field")

    # If an indirect response exposes nested ownership, keep it as indirect rather
    # than recursively treating the resolved target as a direct object request.
    nested = response.get("resource")
    if isinstance(nested, dict) and nested.get("owner_id") is not None:
        _add(scores, evidence, "INDIRECT_REFERENCE_AUTHORIZATION", 3, "resolved-resource-owner-evidence")

    ranked = sorted(DIMENSIONS, key=lambda d: (-scores[d], DIMENSIONS.index(d)))
    best = ranked[0]
    second = ranked[1]
    top_score = scores[best]
    margin = top_score - scores[second]

    if top_score <= 0:
        best = "OBJECT_AUTHORIZATION"
        margin = 0
        evidence[best].append({"points": 0, "reason": "conservative-object-fallback"})

    confidence = "HIGH" if top_score >= 12 and margin >= 4 else "MEDIUM" if top_score >= 7 and margin >= 2 else "LOW"

    return {
        "dimension": best,
        "confidence": confidence,
        "topScore": scores[best],
        "margin": margin,
        "scores": scores,
        "evidence": evidence[best],
    }
