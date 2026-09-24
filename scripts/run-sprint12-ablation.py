#!/usr/bin/env python3
"""
Sprint 12 controlled A0-A7 authorization ablation campaign.

Research boundary:
- localhost ACRA-Lab only;
- predictions are derived from the vulnerable fixture observation plus non-label case inputs;
- ground-truth labels are joined only after prediction;
- no result establishes real-world scanner accuracy or external validity.
"""
from __future__ import annotations

import base64
import csv
import hashlib
import io
import json
import os
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "lab" / "ground-truth" / "GT-S11-AUTHORIZATION-RESEARCH.json"
SERVER = ROOT / "lab" / "vulnerable-api" / "basic-api" / "server.py"
OUT_DIR = ROOT / "build" / "s12-research"
JSON_OUT = OUT_DIR / "EXP-A0-A7.json"
CSV_OUT = OUT_DIR / "EXP-A0-A7.csv"
CASE_OUT = OUT_DIR / "EXP-A0-A7-cases.jsonl"

FORBIDDEN_PREDICTION_KEYS = {
    "groundTruth",
    "secureExpected",
    "vulnerableExpected",
    "expectedCandidate",
}
VARIANTS = [
    ("A0", "EXP-A0", []),
    ("A1", "EXP-A1", ["identity"]),
    ("A2", "EXP-A2", ["identity", "ownership"]),
    ("A3", "EXP-A3", ["identity", "ownership", "tenant"]),
    ("A4", "EXP-A4", ["identity", "ownership", "tenant", "role"]),
    ("A5", "EXP-A5", ["identity", "ownership", "tenant", "role", "workflow"]),
    ("A6", "EXP-A6", ["identity", "ownership", "tenant", "role", "workflow", "semantic_evidence"]),
    ("A7", "EXP-A7", ["identity", "ownership", "tenant", "role", "workflow", "semantic_evidence", "correlation"]),
]


def canonical_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def file_sha256(path):
    return sha256_bytes(path.read_bytes())


def git_head():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL
        ).strip()
    except Exception:
        return "UNAVAILABLE"


def free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def b64url(value):
    return base64.urlsafe_b64encode(canonical_bytes(value)).decode("ascii").rstrip("=")


def synthetic_token(actor):
    # The token is deliberately ephemeral and is never written to experiment artifacts.
    return b64url({"alg": "none", "typ": "JWT"}) + "." + b64url(actor) + "."


def wait_ready(port):
    deadline = time.monotonic() + 10.0
    url = f"http://127.0.0.1:{port}/health"
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=0.5) as response:
                if response.status == 200:
                    return
        except Exception:
            time.sleep(0.05)
    raise RuntimeError("ACRA-Lab vulnerable fixture did not become ready")


def start_server(port):
    env = dict(os.environ)
    env["ACRA_LAB_MODE"] = "vulnerable"
    env["PORT"] = str(port)
    process = subprocess.Popen(
        [sys.executable, str(SERVER)],
        cwd=str(ROOT),
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    try:
        wait_ready(port)
    except Exception:
        process.terminate()
        process.wait(timeout=5)
        raise
    return process


def case_view(case):
    """Return the only fields the predictor is permitted to consume."""
    allowed = {
        "caseId",
        "family",
        "dimension",
        "method",
        "path",
        "actor",
        "requestBody",
        "oracle",
    }
    view = {key: case[key] for key in allowed if key in case}
    leaked = FORBIDDEN_PREDICTION_KEYS.intersection(view)
    if leaked:
        raise AssertionError(f"prediction input leaked oracle labels: {sorted(leaked)}")
    return view


def request_case(port, case):
    url = f"http://127.0.0.1:{port}{case['path']}"
    body = case.get("requestBody")
    data = None if body is None else canonical_bytes(body)
    headers = {
        "Authorization": "Bearer " + synthetic_token(case["actor"]),
        "Accept": "application/json",
    }
    if data is not None:
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method=case["method"])
    try:
        with urllib.request.urlopen(request, timeout=3.0) as response:
            status = response.status
            raw = response.read()
    except urllib.error.HTTPError as failure:
        status = failure.code
        raw = failure.read()

    parsed = None
    if raw:
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            parsed = None
    return {"status": status, "body": parsed}


def authorization_decision(observation, oracle):
    status = observation["status"]
    body = observation["body"]
    oracle_type = oracle["type"]
    if oracle_type == "HTTP_STATUS_AUTHZ":
        if 200 <= status < 300:
            return "ALLOW"
        if status in (401, 403):
            return "DENY"
        return "UNKNOWN"

    if oracle_type == "JSON_LIST_DECISION":
        if not (200 <= status < 300) or not isinstance(body, dict):
            return "UNKNOWN"
        items = body.get(oracle["listField"])
        if not isinstance(items, list):
            return "UNKNOWN"
        for item in items:
            if isinstance(item, dict) and item.get(oracle["matchField"]) == oracle["matchValue"]:
                value = item.get(oracle["decisionField"])
                return value if value in ("ALLOW", "DENY") else "UNKNOWN"
        return "UNKNOWN"

    return "UNKNOWN"


def target_item(case, body):
    oracle = case.get("oracle", {})
    if oracle.get("type") != "JSON_LIST_DECISION" or not isinstance(body, dict):
        return None
    items = body.get(oracle.get("listField"))
    if not isinstance(items, list):
        return None
    for item in items:
        if isinstance(item, dict) and item.get(oracle.get("matchField")) == oracle.get("matchValue"):
            return item
    return None


def evidence_signals(case, observation):
    body = observation["body"]
    dimension = case["dimension"]
    actor = case.get("actor") or {}
    owner_id = None
    resource_tenant = None

    if dimension == "OBJECT_AUTHORIZATION" and isinstance(body, dict):
        owner_id = body.get("owner_id")
        resource_tenant = body.get("tenant_id")
    elif dimension == "BATCH_AUTHORIZATION":
        item = target_item(case, body)
        if isinstance(item, dict):
            owner_id = item.get("owner_id")
            resource_tenant = item.get("tenant_id")
    elif dimension == "INDIRECT_REFERENCE_AUTHORIZATION" and isinstance(body, dict):
        resource = body.get("resource")
        if isinstance(resource, dict):
            owner_id = resource.get("owner_id")
            resource_tenant = resource.get("tenant_id")
    elif dimension == "TENANT_AUTHORIZATION" and isinstance(body, dict):
        resource_tenant = body.get("tenant_id")

    request_body = case.get("requestBody")
    request_body = request_body if isinstance(request_body, dict) else {}

    return {
        "identity_present": bool(actor.get("sub")),
        "principal_id": actor.get("sub"),
        "subject_tenant_id": actor.get("tenant_id"),
        "role": actor.get("role"),
        "resource_owner_id": owner_id,
        "resource_tenant_id": resource_tenant,
        "workflow_action": request_body.get("action"),
        "workflow_from_state": request_body.get("from_state"),
        "workflow_to_state": request_body.get("to_state"),
        "property_names": sorted(request_body.keys()),
        "duplicate_separator": "//" in case.get("path", "").replace("://", ""),
    }


def valid_registered_workflow(signals):
    # Controlled fixture policy: author SUBMIT from DRAFT is valid only to SUBMITTED.
    return (
        signals["role"] == "author"
        and signals["workflow_action"] == "SUBMIT"
        and signals["workflow_from_state"] == "DRAFT"
        and signals["workflow_to_state"] == "SUBMITTED"
    )


def predict(case, observation, capabilities):
    decision = authorization_decision(observation, case["oracle"])
    signals = evidence_signals(case, observation)
    dimension = case["dimension"]

    candidate = decision == "ALLOW"
    reasons = ["A0_OBSERVED_ALLOW" if candidate else "A0_OBSERVED_NOT_ALLOW"]
    applied = []

    if candidate and "identity" in capabilities:
        applied.append("identity")
        if not signals["identity_present"]:
            candidate = False
            reasons.append("IDENTITY_UNRESOLVED")

    if candidate and "ownership" in capabilities and dimension in {
        "OBJECT_AUTHORIZATION",
        "BATCH_AUTHORIZATION",
        "INDIRECT_REFERENCE_AUTHORIZATION",
    }:
        applied.append("ownership")
        owner = signals["resource_owner_id"]
        principal = signals["principal_id"]
        if owner:
            candidate = owner != principal
            reasons.append("OWNER_MISMATCH" if candidate else "OWNER_MATCH")
        else:
            reasons.append("OWNER_UNKNOWN")

    if candidate and "tenant" in capabilities and dimension == "TENANT_AUTHORIZATION":
        applied.append("tenant")
        target = signals["resource_tenant_id"]
        subject = signals["subject_tenant_id"]
        if target:
            candidate = target != subject
            reasons.append("TENANT_MISMATCH" if candidate else "TENANT_MATCH")
        else:
            reasons.append("TENANT_UNKNOWN")

    if candidate and "role" in capabilities and dimension == "RBAC_AUTHORIZATION":
        applied.append("role")
        candidate = str(signals["role"]).lower() not in {
            "admin",
            "tenant-admin",
            "global-admin",
            "delegated-admin",
        }
        reasons.append("ROLE_NOT_PRIVILEGED" if candidate else "ROLE_PRIVILEGED")

    if candidate and "workflow" in capabilities and dimension == "WORKFLOW_AUTHORIZATION":
        applied.append("workflow")
        candidate = not valid_registered_workflow(signals)
        reasons.append("WORKFLOW_POLICY_MISMATCH" if candidate else "WORKFLOW_POLICY_MATCH")

    if candidate and "semantic_evidence" in capabilities:
        if dimension == "PROPERTY_AUTHORIZATION":
            applied.append("semantic_property")
            forbidden = sorted(set(signals["property_names"]) - {"display_name"})
            candidate = bool(forbidden)
            reasons.append("FORBIDDEN_PROPERTY:" + ",".join(forbidden) if candidate else "PROPERTY_POLICY_MATCH")
        elif dimension == "ROUTING_AUTHORIZATION":
            applied.append("semantic_routing")
            candidate = (
                signals["duplicate_separator"]
                and str(signals["role"]).lower() not in {"admin", "global-admin"}
                and decision == "ALLOW"
            )
            reasons.append("ROUTING_AUTHZ_DIVERGENCE" if candidate else "ROUTING_CONTROL")
        elif dimension in {"BATCH_AUTHORIZATION", "INDIRECT_REFERENCE_AUTHORIZATION"}:
            applied.append("semantic_resolved_target")
            owner = signals["resource_owner_id"]
            principal = signals["principal_id"]
            if owner:
                candidate = owner != principal
                reasons.append("RESOLVED_TARGET_OWNER_MISMATCH" if candidate else "RESOLVED_TARGET_OWNER_MATCH")

    if "correlation" in capabilities and candidate:
        applied.append("correlation")
        explained = any(
            marker in "|".join(reasons)
            for marker in (
                "MISMATCH",
                "DIVERGENCE",
                "NOT_PRIVILEGED",
                "FORBIDDEN_PROPERTY",
            )
        )
        if not explained:
            reasons.append("CORRELATION_RETAINS_OBSERVED_ALLOW")

    return {
        "prediction": "POSITIVE" if candidate else "NEGATIVE",
        "observedDecision": decision,
        "status": observation["status"],
        "appliedCapabilities": applied,
        "reasons": reasons,
    }


def confusion(rows):
    tp = tn = fp = fn = 0
    for row in rows:
        truth = row["groundTruth"]
        prediction = row["prediction"]
        if truth == "POSITIVE" and prediction == "POSITIVE":
            tp += 1
        elif truth == "NEGATIVE" and prediction == "NEGATIVE":
            tn += 1
        elif truth == "NEGATIVE" and prediction == "POSITIVE":
            fp += 1
        else:
            fn += 1

    def ratio(n, d):
        return None if d == 0 else n / d

    precision = ratio(tp, tp + fp)
    recall = ratio(tp, tp + fn)
    f1 = None
    if precision is not None and recall is not None and precision + recall > 0:
        f1 = 2 * precision * recall / (precision + recall)
    return {
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
        "precision": precision,
        "recall": recall,
        "f1": f1,
    }


def metric_text(value):
    return "N/A" if value is None else f"{value:.6f}"


def main():
    dataset_bytes = DATASET.read_bytes()
    dataset = json.loads(dataset_bytes)
    if dataset.get("groundTruthId") != "GT-S11-AUTHORIZATION-RESEARCH":
        raise AssertionError("unexpected Sprint 12 dataset")
    if dataset.get("independentOfAcraOutput") is not True:
        raise AssertionError("ground truth must remain independent of ACRA output")
    cases = dataset.get("cases")
    if not isinstance(cases, list) or len(cases) != 16:
        raise AssertionError("Sprint 12 requires the frozen 16-case Sprint 11 dataset")

    views = {case["caseId"]: case_view(case) for case in cases}
    port = free_port()
    server = start_server(port)
    observations = {}
    try:
        for case_id in sorted(views):
            observations[case_id] = request_case(port, views[case_id])
    finally:
        server.terminate()
        try:
            server.wait(timeout=5)
        except subprocess.TimeoutExpired:
            server.kill()
            server.wait(timeout=5)

    all_rows = []
    variant_results = []
    by_id = {case["caseId"]: case for case in cases}

    for variant, experiment_id, capabilities in VARIANTS:
        rows = []
        config = {
            "variant": variant,
            "experimentId": experiment_id,
            "capabilities": capabilities,
            "fallback": "retain-prior-candidate-when-capability-not-applicable",
            "predictionLabelAccess": "FORBIDDEN",
        }
        config_fingerprint = sha256_bytes(canonical_bytes(config))

        for case_id in sorted(views):
            prediction = predict(views[case_id], observations[case_id], set(capabilities))
            labelled = by_id[case_id]
            row = {
                "variant": variant,
                "experimentId": experiment_id,
                "caseId": case_id,
                "dimension": views[case_id]["dimension"],
                "groundTruth": labelled["groundTruth"],
                "prediction": prediction["prediction"],
                "observedDecision": prediction["observedDecision"],
                "status": prediction["status"],
                "appliedCapabilities": prediction["appliedCapabilities"],
                "reasons": prediction["reasons"],
                "configFingerprint": config_fingerprint,
            }
            rows.append(row)
            all_rows.append(row)

        metrics = confusion(rows)
        variant_results.append(
            {
                "variant": variant,
                "experimentId": experiment_id,
                "capabilities": capabilities,
                "configFingerprint": config_fingerprint,
                "metrics": metrics,
            }
        )

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {
        "schemaVersion": "s12-a0-a7-research-v1",
        "datasetId": dataset["groundTruthId"],
        "datasetSha256": sha256_bytes(dataset_bytes),
        "sourceCommit": git_head(),
        "scope": "controlled localhost ACRA-Lab synthetic authorization fixtures only",
        "predictionBoundary": {
            "usesVulnerableFixtureOnly": True,
            "groundTruthJoinedAfterPrediction": True,
            "forbiddenPredictionKeys": sorted(FORBIDDEN_PREDICTION_KEYS),
            "dimensionDiscoveryMeasured": False,
        },
        "caseCount": len(cases),
        "variantCount": len(VARIANTS),
        "variants": variant_results,
        "limitations": [
            "synthetic localhost fixtures",
            "registered dimensions are supplied to the campaign; dimension discovery is not measured",
            "fixture-specific policy knowledge is used by later ablations",
            "unsigned synthetic lab tokens are execution scaffolding only",
            "results do not establish real-world scanner accuracy or external validity",
            "real Burp runtime is outside this experiment",
        ],
    }
    JSON_OUT.write_bytes(canonical_bytes(artifact) + b"\n")
    CASE_OUT.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in all_rows),
        encoding="utf-8",
    )

    buffer = io.StringIO()
    writer = csv.writer(buffer, lineterminator="\n")
    writer.writerow([
        "variant",
        "experiment_id",
        "case_id",
        "dimension",
        "ground_truth",
        "prediction",
        "observed_decision",
        "http_status",
        "config_fingerprint",
    ])
    for row in all_rows:
        writer.writerow([
            row["variant"],
            row["experimentId"],
            row["caseId"],
            row["dimension"],
            row["groundTruth"],
            row["prediction"],
            row["observedDecision"],
            row["status"],
            row["configFingerprint"],
        ])
    CSV_OUT.write_text(buffer.getvalue(), encoding="utf-8")

    for path in (JSON_OUT, CSV_OUT, CASE_OUT):
        (path.with_suffix(path.suffix + ".sha256")).write_text(
            file_sha256(path) + "  " + path.name + "\n", encoding="utf-8"
        )

    for result in variant_results:
        m = result["metrics"]
        print(
            "SPRINT12_ABLATION_RESULT "
            f"{result['variant']} "
            f"TP={m['tp']} TN={m['tn']} FP={m['fp']} FN={m['fn']} "
            f"precision={metric_text(m['precision'])} "
            f"recall={metric_text(m['recall'])} "
            f"f1={metric_text(m['f1'])}"
        )
    print(
        "SPRINT12_A0_A7_CAMPAIGN PASS "
        f"cases={len(cases)} variants={len(VARIANTS)} "
        f"dataset_sha256={artifact['datasetSha256']} "
        f"artifact_sha256={file_sha256(JSON_OUT)}"
    )


if __name__ == "__main__":
    main()
