#!/usr/bin/env python3
"""Sprint 13 Phase 2 automatic authorization-dimension discovery evaluation."""
from __future__ import annotations

import base64
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
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
S12_DATASET = ROOT / "lab" / "ground-truth" / "GT-S11-AUTHORIZATION-RESEARCH.json"
S13_FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-HOLDOUT-FEATURES.json"
S13_LABELS = ROOT / "lab" / "ground-truth" / "GT-S13-HOLDOUT-LABELS.json"
S12_SERVER = ROOT / "lab" / "vulnerable-api" / "basic-api" / "server.py"
S13_SERVER = ROOT / "lab" / "heldout-api" / "server.py"
S12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
INFERENCE_MODULE = ROOT / "scripts" / "sprint13_dimension_inference.py"
OUT_DIR = ROOT / "build" / "s13-dimension-discovery"
RESULT = OUT_DIR / "evaluation.json"
ROWS = OUT_DIR / "cases.jsonl"

def load_module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load_module(S12_RUNNER, "acra_s12_phase2")
DIM = load_module(INFERENCE_MODULE, "acra_s13_dimension")

def canonical_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

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
    raise RuntimeError("dimension-discovery fixture did not become ready")

def start_server(server, env_name, mode, port):
    env = dict(os.environ)
    env[env_name] = mode
    env["PORT"] = str(port)
    process = subprocess.Popen(
        [sys.executable, str(server)],
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

def execute_dataset(cases, server, env_name):
    port = free_port()
    process = start_server(server, env_name, "vulnerable", port)
    observations = {}
    try:
        for case in sorted(cases, key=lambda item: item["caseId"]):
            sanitized = DIM.observable_case({
                key: value
                for key, value in case.items()
                if key not in DIM.FORBIDDEN_KEYS
            })
            observations[case["caseId"]] = request_case(port, sanitized)
    finally:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
    return observations

def dimension_metrics(rows):
    labels = list(DIM.DIMENSIONS)
    confusion = {
        actual: {predicted: 0 for predicted in labels}
        for actual in labels
    }
    for row in rows:
        confusion[row["registeredDimension"]][row["inferredDimension"]] += 1

    total = len(rows)
    correct = sum(1 for row in rows if row["registeredDimension"] == row["inferredDimension"])
    per_class = {}
    f1_values = []
    for label in labels:
        tp = confusion[label][label]
        fp = sum(confusion[actual][label] for actual in labels if actual != label)
        fn = sum(confusion[label][predicted] for predicted in labels if predicted != label)
        precision = None if tp + fp == 0 else tp / (tp + fp)
        recall = None if tp + fn == 0 else tp / (tp + fn)
        f1 = None
        if precision is not None and recall is not None and precision + recall > 0:
            f1 = 2 * precision * recall / (precision + recall)
        per_class[label] = {
            "support": sum(confusion[label].values()),
            "tp": tp,
            "fp": fp,
            "fn": fn,
            "precision": precision,
            "recall": recall,
            "f1": f1,
        }
        f1_values.append(0.0 if f1 is None else f1)

    return {
        "count": total,
        "correct": correct,
        "accuracy": None if total == 0 else correct / total,
        "macroF1": None if not f1_values else sum(f1_values) / len(f1_values),
        "confusion": confusion,
        "perClass": per_class,
    }

def binary_metrics(rows):
    return S12.confusion(rows)

def evaluate_dataset(dataset_id, cases, observations, truth_by_case):
    inference_rows = []
    downstream_rows = []

    for case in sorted(cases, key=lambda item: item["caseId"]):
        case_id = case["caseId"]

        # Construct an observable-only view. The inference function itself rejects
        # dimension and label fields as a second boundary.
        raw_observable = {
            key: value
            for key, value in case.items()
            if key not in DIM.FORBIDDEN_KEYS
        }
        observable = DIM.observable_case(raw_observable)
        inference = DIM.infer_dimension(observable, observations[case_id])

        registered_dimension = case["dimension"]
        inference_rows.append({
            "datasetId": dataset_id,
            "caseId": case_id,
            "registeredDimension": registered_dimension,
            "inferredDimension": inference["dimension"],
            "correct": inference["dimension"] == registered_dimension,
            "confidence": inference["confidence"],
            "topScore": inference["topScore"],
            "margin": inference["margin"],
            "scores": inference["scores"],
            "evidence": inference["evidence"],
        })

        # Downstream ACRA gets only the inferred dimension.
        prediction_case = dict(observable)
        prediction_case["dimension"] = inference["dimension"]
        for variant, experiment_id, capabilities in S12.VARIANTS:
            prediction = S12.predict(
                prediction_case,
                observations[case_id],
                set(capabilities),
            )
            downstream_rows.append({
                "datasetId": dataset_id,
                "variant": variant,
                "experimentId": experiment_id,
                "caseId": case_id,
                "registeredDimension": registered_dimension,
                "inferredDimension": inference["dimension"],
                "groundTruth": truth_by_case[case_id],
                "prediction": prediction["prediction"],
                "observedDecision": prediction["observedDecision"],
                "status": prediction["status"],
                "appliedCapabilities": prediction["appliedCapabilities"],
                "reasons": prediction["reasons"],
            })

    by_variant = {}
    for variant, _, _ in S12.VARIANTS:
        subset = [row for row in downstream_rows if row["variant"] == variant]
        by_variant[variant] = binary_metrics(subset)

    return {
        "dimensionRows": inference_rows,
        "downstreamRows": downstream_rows,
        "dimensionMetrics": dimension_metrics(inference_rows),
        "downstreamMetrics": by_variant,
    }

def main():
    s12 = json.loads(S12_DATASET.read_text(encoding="utf-8"))
    s13 = json.loads(S13_FEATURES.read_text(encoding="utf-8"))

    s12_cases = s12["cases"]
    s13_cases = s13["cases"]

    if len(s12_cases) != 16 or len(s13_cases) != 16:
        raise AssertionError("Phase 2 requires the frozen 16-case S12 and 16-case S13 corpora")

    # Observe both datasets before loading the separately sealed S13 vulnerability labels.
    s12_observations = execute_dataset(
        s12_cases,
        S12_SERVER,
        "ACRA_LAB_MODE",
    )
    s13_observations = execute_dataset(
        s13_cases,
        S13_SERVER,
        "ACRA_HOLDOUT_MODE",
    )

    # Registered dimensions are used only as post-inference evaluation labels.
    s12_truth = {case["caseId"]: case["groundTruth"] for case in s12_cases}

    # Load S13 vulnerability labels only after all endpoint observations have been collected.
    s13_labels = json.loads(S13_LABELS.read_text(encoding="utf-8"))
    s13_truth = {item["caseId"]: item["groundTruth"] for item in s13_labels["cases"]}

    s12_eval = evaluate_dataset(
        "GT-S11-AUTHORIZATION-RESEARCH",
        s12_cases,
        s12_observations,
        s12_truth,
    )
    s13_eval = evaluate_dataset(
        "GT-S13-HOLDOUT-FEATURES",
        s13_cases,
        s13_observations,
        s13_truth,
    )

    combined_dimension_rows = s12_eval["dimensionRows"] + s13_eval["dimensionRows"]
    combined_metrics = dimension_metrics(combined_dimension_rows)

    artifact = {
        "schemaVersion": "s13-dimension-discovery-v1",
        "sourceCommit": git_head(),
        "inferenceEngineSha256": sha256(INFERENCE_MODULE),
        "lockedSprint12RunnerSha256": sha256(S12_RUNNER),
        "datasets": {
            "s12Calibration": {
                "datasetId": s12["groundTruthId"],
                "datasetSha256": sha256(S12_DATASET),
                "dimensionMetrics": s12_eval["dimensionMetrics"],
                "downstreamMetrics": s12_eval["downstreamMetrics"],
            },
            "s13Holdout": {
                "datasetId": s13["datasetId"],
                "datasetSha256": sha256(S13_FEATURES),
                "labelSha256": sha256(S13_LABELS),
                "dimensionMetrics": s13_eval["dimensionMetrics"],
                "downstreamMetrics": s13_eval["downstreamMetrics"],
            },
        },
        "combinedDimensionMetrics": combined_metrics,
        "boundary": {
            "registeredDimensionAvailableToInference": False,
            "vulnerabilityLabelsAvailableToInference": False,
            "endpointMethodUsed": True,
            "endpointPathUsed": True,
            "identityContextUsed": True,
            "requestStructureUsed": True,
            "responseStructureUsed": True,
            "responseValuesUsedOnlyForStructuralOrPolicySignals": True,
            "dimensionInjectedIntoDownstreamFromInferenceOnly": True,
        },
        "claimBoundary": [
            "dimension accuracy is measured only on the frozen S12 and S13 synthetic localhost corpora",
            "the deterministic evidence rules are authored within the project and are not independently trained",
            "path semantics are observable evidence but may not generalize across naming conventions",
            "no external-target or production accuracy claim is established",
        ],
    }

    all_rows = []
    for row in s12_eval["dimensionRows"] + s13_eval["dimensionRows"]:
        all_rows.append({"rowType": "DIMENSION", **row})
    for row in s12_eval["downstreamRows"] + s13_eval["downstreamRows"]:
        all_rows.append({"rowType": "DOWNSTREAM", **row})

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    RESULT.write_bytes(canonical_bytes(artifact) + b"\n")
    ROWS.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in all_rows),
        encoding="utf-8",
    )
    for path in (RESULT, ROWS):
        path.with_suffix(path.suffix + ".sha256").write_text(
            sha256(path) + "  " + path.name + "\n",
            encoding="utf-8",
        )

    for name, evaluation in (("S12", s12_eval), ("S13", s13_eval)):
        dm = evaluation["dimensionMetrics"]
        print(
            "SPRINT13_DIMENSION_RESULT "
            f"{name} correct={dm['correct']}/{dm['count']} "
            f"accuracy={dm['accuracy']:.6f} macro_f1={dm['macroF1']:.6f}"
        )
        for variant in ("A0", "A2", "A7"):
            m = evaluation["downstreamMetrics"][variant]
            print(
                "SPRINT13_DIMENSION_DOWNSTREAM "
                f"{name} {variant} TP={m['tp']} TN={m['tn']} FP={m['fp']} FN={m['fn']} "
                f"precision={m['precision'] if m['precision'] is not None else 'N/A'} "
                f"recall={m['recall'] if m['recall'] is not None else 'N/A'} "
                f"f1={m['f1'] if m['f1'] is not None else 'N/A'}"
            )

    print(
        "SPRINT13_DIMENSION_COMBINED "
        f"correct={combined_metrics['correct']}/{combined_metrics['count']} "
        f"accuracy={combined_metrics['accuracy']:.6f} macro_f1={combined_metrics['macroF1']:.6f}"
    )

if __name__ == "__main__":
    main()
