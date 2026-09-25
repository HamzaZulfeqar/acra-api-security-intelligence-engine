#!/usr/bin/env python3
import base64
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
COMMON = ROOT / "lab" / "common" / "basic_api.py"
SECURE_SERVER = ROOT / "lab" / "secure-api" / "basic-api" / "server.py"
VULNERABLE_SERVER = ROOT / "lab" / "vulnerable-api" / "basic-api" / "server.py"
OUT = ROOT / "build" / "s11-ground-truth" / "verification.json"


def free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def b64url(value):
    raw = json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def synthetic_token(actor):
    return b64url({"alg": "none", "typ": "JWT"}) + "." + b64url(actor) + "."


def wait_ready(port):
    url = f"http://127.0.0.1:{port}/health"
    deadline = time.monotonic() + 10.0
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=0.5) as response:
                if response.status == 200:
                    return
        except Exception:
            time.sleep(0.05)
    raise RuntimeError(f"ACRA-Lab server on port {port} did not become ready")


def start_server(path, mode, port):
    env = dict(os.environ)
    env["ACRA_LAB_MODE"] = mode
    env["PORT"] = str(port)
    process = subprocess.Popen(
        [sys.executable, str(path)],
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
    data = None if body is None else json.dumps(body, separators=(",", ":")).encode("utf-8")
    headers = {
        "Authorization": "Bearer " + synthetic_token(case["actor"]),
        "Accept": "application/json",
    }
    if data is not None:
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=case["method"],
    )
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
    return status, parsed


def decision(status, body, oracle):
    oracle_type = oracle["type"]
    if oracle_type == "HTTP_STATUS_AUTHZ":
        if 200 <= status < 300:
            return "ALLOW"
        if status in (401, 403):
            return "DENY"
        raise AssertionError(f"unexpected HTTP status for authorization oracle: {status}")

    if oracle_type == "JSON_LIST_DECISION":
        if not (200 <= status < 300):
            raise AssertionError(f"batch oracle requires 2xx response, got {status}")
        if not isinstance(body, dict):
            raise AssertionError("batch oracle response must be a JSON object")
        items = body.get(oracle["listField"])
        if not isinstance(items, list):
            raise AssertionError("batch oracle list field missing")
        for item in items:
            if (
                isinstance(item, dict)
                and item.get(oracle["matchField"]) == oracle["matchValue"]
            ):
                value = item.get(oracle["decisionField"])
                if value not in ("ALLOW", "DENY"):
                    raise AssertionError("batch oracle decision must be ALLOW or DENY")
                return value
        raise AssertionError("batch oracle target item not found")

    raise AssertionError(f"unsupported oracle type: {oracle_type}")


def validate_registry(dataset):
    if dataset.get("schemaVersion") != "s11-authorization-research-v1":
        raise AssertionError("unexpected ground-truth schema version")
    if dataset.get("groundTruthId") != "GT-S11-AUTHORIZATION-RESEARCH":
        raise AssertionError("unexpected ground-truth identity")
    if dataset.get("independentOfAcraOutput") is not True:
        raise AssertionError("ground truth must be independent of ACRA output")
    if dataset.get("researchExecutionState") != "A0_A7_NOT_RUN":
        raise AssertionError("dataset must not claim A0-A7 execution")

    cases = dataset.get("cases")
    if not isinstance(cases, list) or not cases:
        raise AssertionError("ground-truth cases required")

    ids = [case.get("caseId") for case in cases]
    if len(ids) != len(set(ids)) or any(not value for value in ids):
        raise AssertionError("ground-truth case IDs must be unique and non-blank")

    positives = [case for case in cases if case.get("groundTruth") == "POSITIVE"]
    negatives = [case for case in cases if case.get("groundTruth") == "NEGATIVE"]
    if len(positives) != len(negatives):
        raise AssertionError("Sprint 11 registry must retain balanced positive/negative controls")
    if any(case.get("expectedCandidate") is not True for case in positives):
        raise AssertionError("positive ground truth must expect a candidate")
    if any(case.get("expectedCandidate") is not False for case in negatives):
        raise AssertionError("negative ground truth must not expect a candidate")
    if any(
        case.get("secureExpected") != "DENY"
        or case.get("vulnerableExpected") != "ALLOW"
        for case in positives
    ):
        raise AssertionError("positive controls must be secure DENY / vulnerable ALLOW")
    if any(
        case.get("secureExpected") != case.get("vulnerableExpected")
        for case in negatives
    ):
        raise AssertionError("negative controls must retain equivalent secure/vulnerable decisions")

    dimensions = {}
    for case in cases:
        dimensions.setdefault(case["dimension"], {"POSITIVE": 0, "NEGATIVE": 0})
        dimensions[case["dimension"]][case["groundTruth"]] += 1
    if len(dimensions) < 8:
        raise AssertionError("Sprint 11 registry must cover at least eight authorization dimensions")
    for dimension, counts in dimensions.items():
        if counts != {"POSITIVE": 1, "NEGATIVE": 1}:
            raise AssertionError(
                f"dimension {dimension} must contain exactly one positive and one negative control"
            )

    metrics = dataset.get("metrics", {})
    if any(value != "NOT_MEASURED" for value in metrics.values()):
        raise AssertionError("A0-A7 metrics must remain NOT_MEASURED before research execution")
    return cases, dimensions


def main():
    common = COMMON.read_bytes()
    if SECURE_SERVER.read_bytes() != common or VULNERABLE_SERVER.read_bytes() != common:
        raise AssertionError("Docker-facing ACRA-Lab server copies drift from lab/common/basic_api.py")

    dataset = json.loads(DATASET.read_text(encoding="utf-8"))
    cases, dimensions = validate_registry(dataset)

    secure_port = free_port()
    vulnerable_port = free_port()
    while vulnerable_port == secure_port:
        vulnerable_port = free_port()

    secure = start_server(SECURE_SERVER, "secure", secure_port)
    vulnerable = start_server(VULNERABLE_SERVER, "vulnerable", vulnerable_port)

    results = []
    try:
        for case in cases:
            secure_status, secure_body = request_case(secure_port, case)
            vulnerable_status, vulnerable_body = request_case(vulnerable_port, case)
            secure_decision = decision(secure_status, secure_body, case["oracle"])
            vulnerable_decision = decision(
                vulnerable_status, vulnerable_body, case["oracle"]
            )

            if secure_decision != case["secureExpected"]:
                raise AssertionError(
                    f"{case['caseId']} secure expected={case['secureExpected']} "
                    f"observed={secure_decision}"
                )
            if vulnerable_decision != case["vulnerableExpected"]:
                raise AssertionError(
                    f"{case['caseId']} vulnerable expected={case['vulnerableExpected']} "
                    f"observed={vulnerable_decision}"
                )

            results.append(
                {
                    "caseId": case["caseId"],
                    "dimension": case["dimension"],
                    "groundTruth": case["groundTruth"],
                    "secureDecision": secure_decision,
                    "vulnerableDecision": vulnerable_decision,
                    "expectedCandidate": case["expectedCandidate"],
                    "verified": True,
                }
            )
    finally:
        for process in (secure, vulnerable):
            process.terminate()
        for process in (secure, vulnerable):
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "schemaVersion": "s11-ground-truth-verification-v1",
        "groundTruthId": dataset["groundTruthId"],
        "caseCount": len(cases),
        "positiveCount": sum(1 for case in cases if case["groundTruth"] == "POSITIVE"),
        "negativeCount": sum(1 for case in cases if case["groundTruth"] == "NEGATIVE"),
        "dimensionCount": len(dimensions),
        "researchExecutionState": "A0_A7_NOT_RUN",
        "metrics": dataset["metrics"],
        "results": results,
    }
    OUT.write_text(
        json.dumps(artifact, sort_keys=True, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print(
        "SPRINT11_GROUND_TRUTH PASS "
        f"cases={artifact['caseCount']} "
        f"positive={artifact['positiveCount']} "
        f"negative={artifact['negativeCount']} "
        f"dimensions={artifact['dimensionCount']} "
        "a0_a7=NOT_RUN metrics=NOT_MEASURED"
    )


if __name__ == "__main__":
    main()
