#!/usr/bin/env python3
"""Sprint 13 held-out prediction pass.

The predictor reads only GT-S13-HOLDOUT-FEATURES. Ground-truth labels are intentionally
stored in a separate file and are not referenced by this module.
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
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-HOLDOUT-FEATURES.json"
SERVER = ROOT / "lab" / "heldout-api" / "server.py"
SPRINT12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
OUT_DIR = ROOT / "build" / "s13-heldout"
RESULT = OUT_DIR / "predictions.json"
ROWS = OUT_DIR / "predictions.jsonl"

def load_sprint12():
    spec = importlib.util.spec_from_file_location("acra_s12_locked", SPRINT12_RUNNER)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load_sprint12()

def sha256(path: Path) -> str:
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

def start_server(port):
    env = dict(os.environ)
    env["ACRA_HOLDOUT_MODE"] = "vulnerable"
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

def main():
    dataset = json.loads(FEATURES.read_text(encoding="utf-8"))
    if dataset.get("datasetId") != "GT-S13-HOLDOUT-FEATURES":
        raise AssertionError("unexpected Sprint 13 feature dataset")
    if dataset.get("labelsSeparated") is not True:
        raise AssertionError("held-out labels must remain separated")
    if dataset.get("independentOfAcraOutput") is not True:
        raise AssertionError("held-out features must be independent of ACRA output")
    if dataset.get("dimensionDiscoveryMeasured") is not False:
        raise AssertionError("Sprint 13 Phase 1 does not measure dimension discovery")

    cases = dataset.get("cases")
    if not isinstance(cases, list) or len(cases) != 16:
        raise AssertionError("Sprint 13 Phase 1 requires 16 frozen held-out cases")

    forbidden = {"groundTruth", "secureExpected", "vulnerableExpected", "expectedCandidate"}
    for case in cases:
        leaked = forbidden.intersection(case)
        if leaked:
            raise AssertionError(f"feature corpus leaked label fields: {sorted(leaked)}")

    views = {case["caseId"]: S12.case_view(case) for case in cases}
    if len(views) != 16:
        raise AssertionError("duplicate Sprint 13 case ID")

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

    rows = []
    variants = []
    for variant, experiment_id, capabilities in S12.VARIANTS:
        config = {
            "variant": variant,
            "experimentId": experiment_id,
            "capabilities": capabilities,
            "lockedRuleSource": "scripts/run-sprint12-ablation.py",
        }
        fingerprint = hashlib.sha256(canonical_bytes(config)).hexdigest()
        variants.append({
            "variant": variant,
            "experimentId": experiment_id,
            "capabilities": capabilities,
            "configFingerprint": fingerprint,
        })
        for case_id in sorted(views):
            prediction = S12.predict(views[case_id], observations[case_id], set(capabilities))
            rows.append({
                "variant": variant,
                "experimentId": experiment_id,
                "caseId": case_id,
                "dimension": views[case_id]["dimension"],
                "prediction": prediction["prediction"],
                "observedDecision": prediction["observedDecision"],
                "status": prediction["status"],
                "appliedCapabilities": prediction["appliedCapabilities"],
                "reasons": prediction["reasons"],
                "configFingerprint": fingerprint,
            })

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {
        "schemaVersion": "s13-heldout-predictions-v1",
        "featureDatasetId": dataset["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "sourceCommit": git_head(),
        "lockedSprint12RunnerSha256": sha256(SPRINT12_RUNNER),
        "caseCount": len(cases),
        "variantCount": len(variants),
        "predictionRowCount": len(rows),
        "variants": variants,
        "predictionBoundary": {
            "labelFileRequired": False,
            "groundTruthAvailableToPredictor": False,
            "dimensionDiscoveryMeasured": False,
            "vulnerableHeldoutFixtureOnly": True,
        },
        "scope": "localhost held-out synthetic authorization evaluation only",
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
    print(
        "SPRINT13_HELDOUT_PREDICTION PASS "
        f"cases={len(cases)} variants={len(variants)} rows={len(rows)} "
        f"features_sha256={sha256(FEATURES)}"
    )

if __name__ == "__main__":
    main()
