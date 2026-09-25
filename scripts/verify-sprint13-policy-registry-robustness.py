#!/usr/bin/env python3
"""Phase 4 policy-registry robustness checks against the frozen Phase 3 policy engine."""
from __future__ import annotations

import importlib.util
import json
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY_MODULE = ROOT / "scripts" / "sprint13_policy_semantics.py"

def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

POL = load(POLICY_MODULE, "policy_robustness")

def write_temp(value):
    handle = tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".json", delete=False)
    with handle:
        json.dump(value, handle)
    return Path(handle.name)

def expect_rejected(name, value):
    path = write_temp(value)
    try:
        try:
            POL.load_policy_registry(path)
        except Exception:
            print(f"SPRINT13_POLICY_ROBUSTNESS_REJECT PASS {name}")
            return
        raise AssertionError(f"malformed registry unexpectedly accepted: {name}")
    finally:
        path.unlink(missing_ok=True)

def main():
    base_policy = {
        "policyId": "P1",
        "dimension": "RBAC_AUTHORIZATION",
        "methods": ["GET"],
        "pathRegex": "^/x$",
        "priority": 100,
        "rules": {"allowedRoles": ["admin"]},
    }

    expect_rejected("BAD_SCHEMA", {"schemaVersion": "wrong", "policies": [base_policy]})
    expect_rejected("EMPTY_POLICIES", {"schemaVersion": "s13-policy-registry-v1", "policies": []})
    expect_rejected("POLICIES_NOT_LIST", {"schemaVersion": "s13-policy-registry-v1", "policies": {}})
    expect_rejected(
        "DUPLICATE_POLICY_ID",
        {"schemaVersion": "s13-policy-registry-v1", "policies": [base_policy, dict(base_policy)]},
    )
    invalid_dimension = dict(base_policy)
    invalid_dimension["policyId"] = "P2"
    invalid_dimension["dimension"] = "NOT_A_DIMENSION"
    expect_rejected(
        "INVALID_DIMENSION",
        {"schemaVersion": "s13-policy-registry-v1", "policies": [invalid_dimension]},
    )
    missing_regex = dict(base_policy)
    missing_regex["policyId"] = "P3"
    missing_regex.pop("pathRegex")
    expect_rejected(
        "MISSING_REGEX",
        {"schemaVersion": "s13-policy-registry-v1", "policies": [missing_regex]},
    )
    invalid_regex = dict(base_policy)
    invalid_regex["policyId"] = "P4"
    invalid_regex["pathRegex"] = "(unclosed"
    expect_rejected(
        "INVALID_REGEX",
        {"schemaVersion": "s13-policy-registry-v1", "policies": [invalid_regex]},
    )

    ambiguous = {
        "schemaVersion": "s13-policy-registry-v1",
        "policies": [
            dict(base_policy, policyId="AMB-A"),
            dict(base_policy, policyId="AMB-B"),
        ],
    }
    case = {"method": "GET", "path": "/x", "actor": {"sub": "u", "role": "admin"}}
    observation = {"status": 200, "body": {"role": "admin"}}
    result = POL.evaluate_policy(ambiguous, "RBAC_AUTHORIZATION", case, observation)
    if result["decision"] != "UNKNOWN" or result["status"] != "AMBIGUOUS_POLICY":
        raise AssertionError("ambiguous policy must become UNKNOWN")
    print("SPRINT13_POLICY_ROBUSTNESS_AMBIGUOUS PASS")

    missing = {
        "schemaVersion": "s13-policy-registry-v1",
        "policies": [dict(base_policy, policyId="OTHER", pathRegex="^/other$")],
    }
    result = POL.evaluate_policy(missing, "RBAC_AUTHORIZATION", case, observation)
    if result["decision"] != "UNKNOWN" or result["status"] != "NO_POLICY":
        raise AssertionError("missing policy must become UNKNOWN")
    print("SPRINT13_POLICY_ROBUSTNESS_MISSING PASS")

    priority = {
        "schemaVersion": "s13-policy-registry-v1",
        "policies": [
            dict(base_policy, policyId="LOW", priority=10, rules={"allowedRoles": []}),
            dict(base_policy, policyId="HIGH", priority=20, rules={"allowedRoles": ["admin"]}),
        ],
    }
    result = POL.evaluate_policy(priority, "RBAC_AUTHORIZATION", case, observation)
    if result["decision"] != "ALLOW" or result["policyId"] != "HIGH":
        raise AssertionError("higher-priority policy must win deterministically")
    print("SPRINT13_POLICY_ROBUSTNESS_PRIORITY PASS")

    locked_positive = {
        "prediction": "POSITIVE",
        "observedDecision": "ALLOW",
        "reasons": ["LOCKED"],
    }
    fallback = POL.apply_policy_to_prediction(
        locked_positive,
        {"decision": "UNKNOWN", "policyId": None, "reasons": ["NO_POLICY"]},
    )
    if fallback["prediction"] != "POSITIVE":
        raise AssertionError("UNKNOWN policy must preserve locked A7 prediction")

    allow = POL.apply_policy_to_prediction(
        locked_positive,
        {"decision": "ALLOW", "policyId": "P", "reasons": ["ALLOW"]},
    )
    if allow["prediction"] != "NEGATIVE":
        raise AssertionError("explicit ALLOW must suppress candidate")

    locked_negative = {
        "prediction": "NEGATIVE",
        "observedDecision": "ALLOW",
        "reasons": ["LOCKED"],
    }
    deny = POL.apply_policy_to_prediction(
        locked_negative,
        {"decision": "DENY", "policyId": "P", "reasons": ["DENY"]},
    )
    if deny["prediction"] != "POSITIVE":
        raise AssertionError("explicit DENY + observed ALLOW must promote mismatch")

    print("SPRINT13_POLICY_ROBUSTNESS_FALLBACK PASS")
    print("SPRINT13_POLICY_REGISTRY_ROBUSTNESS PASS")

if __name__ == "__main__":
    main()
