#!/usr/bin/env python3
"""Join sealed Sprint 13 labels only after prediction and verify the held-out oracle."""
from __future__ import annotations

import hashlib
import importlib.util
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
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-HOLDOUT-FEATURES.json"
LABELS = ROOT / "lab" / "ground-truth" / "GT-S13-HOLDOUT-LABELS.json"
SERVER = ROOT / "lab" / "heldout-api" / "server.py"
SPRINT12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
SPRINT13_RUNNER = ROOT / "scripts" / "run-sprint13-heldout.py"
OUT_DIR = ROOT / "build" / "s13-heldout"
PREDICTION_RESULT = OUT_DIR / "predictions.json"
PREDICTION_ROWS = OUT_DIR / "predictions.jsonl"
EVALUATION = OUT_DIR / "evaluation.json"
EVALUATION_ROWS = OUT_DIR / "evaluation-cases.jsonl"

def load_module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load_module(SPRINT12_RUNNER, "acra_s12_verify")

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def canonical_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")

def git_head():
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()

def free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]

def wait_ready(port):
    deadline = time.monotonic() + 10
    url = f"http://127.0.0.1:{port}/health"
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=0.5) as response:
                if response.status == 200:
                    return
        except Exception:
            time.sleep(0.05)
    raise RuntimeError("Sprint 13 held-out fixture did not become ready")

def start_server(mode, port):
    env = dict(os.environ)
    env["ACRA_HOLDOUT_MODE"] = mode
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

def request_case(port, case):
    url = f"http://127.0.0.1:{port}{case['path']}"
    body = case.get("requestBody")
    data = None if body is None else canonical_bytes(body)
    headers = {
        "Authorization": "Bearer " + S12.synthetic_token(case["actor"]),
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

def execute_mode(mode, cases):
    port = free_port()
    server = start_server(mode, port)
    observations = {}
    try:
        for case in sorted(cases, key=lambda item: item["caseId"]):
            observations[case["caseId"]] = request_case(port, case)
    finally:
        server.terminate()
        try:
            server.wait(timeout=5)
        except subprocess.TimeoutExpired:
            server.kill()
            server.wait(timeout=5)
    return observations

def metric_text(value):
    return "N/A" if value is None else f"{value:.6f}"

def main():
    features = json.loads(FEATURES.read_text(encoding="utf-8"))
    labels = json.loads(LABELS.read_text(encoding="utf-8"))
    prediction_result = json.loads(PREDICTION_RESULT.read_text(encoding="utf-8"))
    prediction_rows = [
        json.loads(line)
        for line in PREDICTION_ROWS.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]

    if labels.get("sealedForPrediction") is not True:
        raise AssertionError("Sprint 13 labels must remain sealed for prediction")
    if labels.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("feature/label dataset identity mismatch")
    if prediction_result.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("prediction feature dataset mismatch")
    if prediction_result.get("featureDatasetSha256") != sha256(FEATURES):
        raise AssertionError("feature dataset digest drift")
    if prediction_result.get("sourceCommit") != git_head():
        raise AssertionError("predictions must identify exact executing commit")
    if prediction_result.get("caseCount") != 16 or prediction_result.get("variantCount") != 8:
        raise AssertionError("unexpected Sprint 13 prediction cardinality")
    if len(prediction_rows) != 128:
        raise AssertionError("expected 16 held-out cases x 8 locked variants")

    boundary = prediction_result.get("predictionBoundary", {})
    if boundary.get("labelFileRequired") is not False:
        raise AssertionError("predictor must not require labels")
    if boundary.get("groundTruthAvailableToPredictor") is not False:
        raise AssertionError("ground truth must be unavailable during prediction")
    if boundary.get("dimensionDiscoveryMeasured") is not False:
        raise AssertionError("Phase 1 must not claim dimension discovery")

    runner_text = SPRINT13_RUNNER.read_text(encoding="utf-8")
    if "GT-S13-HOLDOUT-LABELS" in runner_text:
        raise AssertionError("prediction runner directly references the sealed label file")

    for artifact in (PREDICTION_RESULT, PREDICTION_ROWS):
        sidecar = artifact.with_suffix(artifact.suffix + ".sha256")
        expected = sidecar.read_text(encoding="utf-8").split()[0]
        if sha256(artifact) != expected:
            raise AssertionError(f"prediction SHA-256 mismatch: {artifact.name}")

    cases = features.get("cases", [])
    label_cases = labels.get("cases", [])
    by_label = {item["caseId"]: item for item in label_cases}
    feature_ids = {item["caseId"] for item in cases}
    if len(cases) != 16 or len(label_cases) != 16 or set(by_label) != feature_ids:
        raise AssertionError("held-out feature/label case set mismatch")

    secure = execute_mode("secure", cases)
    vulnerable = execute_mode("vulnerable", cases)
    oracle_rows = []
    for case in sorted(cases, key=lambda item: item["caseId"]):
        case_id = case["caseId"]
        label = by_label[case_id]
        secure_decision = S12.authorization_decision(secure[case_id], case["oracle"])
        vulnerable_decision = S12.authorization_decision(vulnerable[case_id], case["oracle"])
        if secure_decision != label["secureExpected"]:
            raise AssertionError(f"{case_id} secure oracle drift: {secure_decision}")
        if vulnerable_decision != label["vulnerableExpected"]:
            raise AssertionError(f"{case_id} vulnerable oracle drift: {vulnerable_decision}")
        derived = "POSITIVE" if secure_decision == "DENY" and vulnerable_decision == "ALLOW" else "NEGATIVE"
        if derived != label["groundTruth"]:
            raise AssertionError(f"{case_id} ground-truth derivation mismatch")
        oracle_rows.append({
            "caseId": case_id,
            "groundTruth": label["groundTruth"],
            "secureDecision": secure_decision,
            "vulnerableDecision": vulnerable_decision,
        })

    variants = [f"A{i}" for i in range(8)]
    grouped = {variant: [] for variant in variants}
    seen = set()
    evaluated_rows = []
    for row in prediction_rows:
        key = (row["variant"], row["caseId"])
        if key in seen:
            raise AssertionError("duplicate held-out prediction row")
        seen.add(key)
        label = by_label.get(row["caseId"])
        if label is None:
            raise AssertionError("prediction references unknown held-out case")
        joined = {
            **row,
            "groundTruth": label["groundTruth"],
            "rationale": label["rationale"],
        }
        grouped[row["variant"]].append(joined)
        evaluated_rows.append(joined)

    summaries = []
    for variant in variants:
        rows = grouped[variant]
        if len(rows) != 16:
            raise AssertionError(f"{variant} missing held-out rows")
        positive_count = sum(1 for row in rows if row["groundTruth"] == "POSITIVE")
        negative_count = sum(1 for row in rows if row["groundTruth"] == "NEGATIVE")
        if (positive_count, negative_count) != (8, 8):
            raise AssertionError("held-out label balance drift")
        metrics = S12.confusion(rows)
        hard_negative_fp = sum(
            1 for row in rows
            if row["groundTruth"] == "NEGATIVE" and row["prediction"] == "POSITIVE"
        )
        summaries.append({
            "variant": variant,
            "metrics": metrics,
            "hardNegativeFalsePositives": hard_negative_fp,
        })

    evaluation = {
        "schemaVersion": "s13-heldout-evaluation-v1",
        "featureDatasetId": features["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "labelSetId": labels["labelSetId"],
        "labelSetSha256": sha256(LABELS),
        "sourceCommit": git_head(),
        "lockedSprint12RunnerSha256": sha256(SPRINT12_RUNNER),
        "oracleValidation": {
            "caseCount": len(oracle_rows),
            "secureAndVulnerableExpectationsVerified": True,
        },
        "predictionBoundary": prediction_result["predictionBoundary"],
        "variants": summaries,
        "claimBoundary": [
            "results apply only to the separately frozen localhost held-out fixture",
            "labels were unavailable to the prediction process",
            "registered authorization dimensions remain supplied; dimension discovery is not measured",
            "the held-out fixture is separately authored inside the same project and is not independent third-party replication",
            "no real-world scanner accuracy or external-target safety claim is established",
        ],
    }

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    EVALUATION.write_bytes(canonical_bytes(evaluation) + b"\n")
    EVALUATION_ROWS.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in evaluated_rows),
        encoding="utf-8",
    )
    for artifact in (EVALUATION, EVALUATION_ROWS):
        artifact.with_suffix(artifact.suffix + ".sha256").write_text(
            sha256(artifact) + "  " + artifact.name + "\n",
            encoding="utf-8",
        )

    combined = EVALUATION.read_text(encoding="utf-8") + EVALUATION_ROWS.read_text(encoding="utf-8")
    for forbidden in ("synthetic-cookie-secret", "\"Authorization\":", "Bearer "):
        if forbidden in combined:
            raise AssertionError("secret-like material leaked into Sprint 13 artifacts")

    for summary in summaries:
        m = summary["metrics"]
        print(
            "SPRINT13_HELDOUT_RESULT "
            f"{summary['variant']} "
            f"TP={m['tp']} TN={m['tn']} FP={m['fp']} FN={m['fn']} "
            f"precision={metric_text(m['precision'])} "
            f"recall={metric_text(m['recall'])} "
            f"f1={metric_text(m['f1'])} "
            f"hard_negative_fp={summary['hardNegativeFalsePositives']}"
        )
    print(
        "SPRINT13_HELDOUT_VERIFY PASS "
        f"oracle_cases={len(oracle_rows)} prediction_rows={len(prediction_rows)}"
    )

if __name__ == "__main__":
    main()
