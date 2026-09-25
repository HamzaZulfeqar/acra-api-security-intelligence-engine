#!/usr/bin/env python3
"""Sprint 13 Phase 2 blind dimension inference + downstream prediction.

This process intentionally has no dependency on the sealed dimension-label file.
It consumes only GT-S13-DIMENSION-FEATURES and produces unlabelled inference and
A0-A7 downstream prediction artifacts.
"""
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
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-DIMENSION-FEATURES.json"
S12_SERVER = ROOT / "lab" / "vulnerable-api" / "basic-api" / "server.py"
S13_SERVER = ROOT / "lab" / "heldout-api" / "server.py"
S12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
INFERENCE_MODULE = ROOT / "scripts" / "sprint13_dimension_inference.py"
OUT_DIR = ROOT / "build" / "s13-dimension-discovery"
RESULT = OUT_DIR / "predictions.json"
ROWS = OUT_DIR / "predictions.jsonl"

def load_module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load_module(S12_RUNNER, "acra_s12_phase2_predict")
DIM = load_module(INFERENCE_MODULE, "acra_s13_dimension_predict")

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
    raise RuntimeError("Sprint 13 Phase 2 fixture did not become ready")

def start_server(server, env_name, port):
    env = dict(os.environ)
    env[env_name] = "vulnerable"
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

def execute_fixture(cases, server, env_name):
    port = free_port()
    process = start_server(server, env_name, port)
    observations = {}
    try:
        for case in sorted(cases, key=lambda item: item["caseId"]):
            observations[case["caseId"]] = request_case(port, case)
    finally:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
    return observations

def main():
    dataset = json.loads(FEATURES.read_text(encoding="utf-8"))
    if dataset.get("datasetId") != "GT-S13-DIMENSION-FEATURES":
        raise AssertionError("unexpected Sprint 13 Phase 2 feature dataset")
    if dataset.get("registeredDimensionSeparated") is not True:
        raise AssertionError("registered dimensions must be separated from Phase 2 features")
    if dataset.get("vulnerabilityLabelsSeparated") is not True:
        raise AssertionError("vulnerability labels must be separated from Phase 2 features")

    cases = dataset.get("cases")
    if not isinstance(cases, list) or len(cases) != 32:
        raise AssertionError("Phase 2 requires the frozen 32-case dimension-free corpus")

    for case in cases:
        forbidden = DIM.FORBIDDEN_KEYS.intersection(case)
        if forbidden:
            raise AssertionError(f"feature dataset leaked forbidden inference fields: {sorted(forbidden)}")

    s12_cases = [case for case in cases if case.get("fixture") == "ACRA_LAB"]
    s13_cases = [case for case in cases if case.get("fixture") == "S13_HOLDOUT"]
    if len(s12_cases) != 16 or len(s13_cases) != 16:
        raise AssertionError("unexpected Phase 2 fixture partition")

    observations = {}
    observations.update(execute_fixture(s12_cases, S12_SERVER, "ACRA_LAB_MODE"))
    observations.update(execute_fixture(s13_cases, S13_SERVER, "ACRA_HOLDOUT_MODE"))

    rows = []
    case_summaries = []
    for case in sorted(cases, key=lambda item: item["caseId"]):
        observable = DIM.observable_case(case)
        observation = observations[case["caseId"]]
        inference = DIM.infer_dimension(observable, observation)

        case_summaries.append({
            "caseId": case["caseId"],
            "sourceDataset": case["sourceDataset"],
            "fixture": case["fixture"],
            "inferredDimension": inference["dimension"],
            "confidence": inference["confidence"],
            "topScore": inference["topScore"],
            "margin": inference["margin"],
            "scores": inference["scores"],
            "evidence": inference["evidence"],
        })

        prediction_case = dict(observable)
        prediction_case["dimension"] = inference["dimension"]
        for variant, experiment_id, capabilities in S12.VARIANTS:
            prediction = S12.predict(
                prediction_case,
                observation,
                set(capabilities),
            )
            rows.append({
                "caseId": case["caseId"],
                "sourceDataset": case["sourceDataset"],
                "fixture": case["fixture"],
                "variant": variant,
                "experimentId": experiment_id,
                "inferredDimension": inference["dimension"],
                "confidence": inference["confidence"],
                "prediction": prediction["prediction"],
                "observedDecision": prediction["observedDecision"],
                "status": prediction["status"],
                "appliedCapabilities": prediction["appliedCapabilities"],
                "reasons": prediction["reasons"],
            })

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {
        "schemaVersion": "s13-dimension-predictions-v1",
        "featureDatasetId": dataset["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "sourceCommit": git_head(),
        "inferenceEngineSha256": sha256(INFERENCE_MODULE),
        "lockedSprint12RunnerSha256": sha256(S12_RUNNER),
        "caseCount": len(cases),
        "variantCount": len(S12.VARIANTS),
        "predictionRowCount": len(rows),
        "cases": case_summaries,
        "boundary": {
            "registeredDimensionAvailableToInference": False,
            "vulnerabilityLabelsAvailableToInference": False,
            "labelFileRequired": False,
            "endpointMethodUsed": True,
            "endpointPathUsed": True,
            "identityContextUsed": True,
            "requestStructureUsed": True,
            "responseStructureUsed": True,
            "dimensionInjectedIntoDownstreamFromInferenceOnly": True,
        },
        "scope": "synthetic localhost S12 calibration + S13 holdout fixtures only",
    }
    RESULT.write_bytes(canonical_bytes(artifact) + b"\n")
    ROWS.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in rows),
        encoding="utf-8",
    )
    for path in (RESULT, ROWS):
        path.with_suffix(path.suffix + ".sha256").write_text(
            sha256(path) + "  " + path.name + "\n",
            encoding="utf-8",
        )

    confidence_counts = {}
    for case in case_summaries:
        confidence_counts[case["confidence"]] = confidence_counts.get(case["confidence"], 0) + 1

    print(
        "SPRINT13_DIMENSION_PREDICTION PASS "
        f"cases={len(cases)} rows={len(rows)} "
        f"confidence={json.dumps(confidence_counts, sort_keys=True, separators=(',', ':'))} "
        f"features_sha256={sha256(FEATURES)}"
    )

if __name__ == "__main__":
    main()
