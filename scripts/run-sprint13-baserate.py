#!/usr/bin/env python3
"""Sprint 13 Phase 4 blind adversarial/base-rate prediction."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-BASERATE-FEATURES.json"
REGISTRY = ROOT / "lab" / "ground-truth" / "POL-S13-BASERATE-001.json"
S12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
DIM_MODULE = ROOT / "scripts" / "sprint13_dimension_inference.py"
POLICY_MODULE = ROOT / "scripts" / "sprint13_policy_semantics.py"
OUT_DIR = ROOT / "build" / "s13-baserate"
RESULT = OUT_DIR / "predictions.json"
ROWS = OUT_DIR / "predictions.jsonl"

def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load(S12_RUNNER, "s12_baserate")
DIM = load(DIM_MODULE, "dim_baserate")
POL = load(POLICY_MODULE, "policy_baserate")

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")

def git_head():
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()

def main():
    data = json.loads(FEATURES.read_text(encoding="utf-8"))
    registry = POL.load_policy_registry(REGISTRY)

    if data.get("datasetId") != "GT-S13-BASERATE-FEATURES":
        raise AssertionError("unexpected Phase 4 dataset")
    if data.get("algorithmFreeze") != "c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd":
        raise AssertionError("algorithm-freeze provenance drift")
    if data.get("labelsSeparated") is not True:
        raise AssertionError("Phase 4 labels must be separated")
    if data.get("policyRegistryId") != registry.get("registryId"):
        raise AssertionError("Phase 4 policy registry identity mismatch")

    cases = data.get("cases")
    if not isinstance(cases, list) or len(cases) != 96:
        raise AssertionError("Phase 4 requires 96 frozen cases")

    rows = []
    case_summaries = []
    a7_caps = set(S12.VARIANTS[-1][2])

    for case in sorted(cases, key=lambda item: item["caseId"]):
        forbidden = DIM.FORBIDDEN_KEYS.intersection(case)
        if forbidden:
            raise AssertionError(f"Phase 4 feature leaked forbidden fields: {sorted(forbidden)}")
        if "policyCondition" in case or "expectedAuthorization" in case:
            raise AssertionError("Phase 4 feature leaked policy labels")

        observation = case.get("observation")
        if not isinstance(observation, dict):
            raise AssertionError("Phase 4 snapshot observation required")

        input_case = {key: value for key, value in case.items() if key != "observation"}
        observable = DIM.observable_case(input_case)
        inference = DIM.infer_dimension(observable, observation)

        downstream = dict(observable)
        downstream["dimension"] = inference["dimension"]
        locked = S12.predict(downstream, observation, a7_caps)
        policy = POL.evaluate_policy(registry, inference["dimension"], observable, observation)
        generalized = POL.apply_policy_to_prediction(locked, policy)

        case_summaries.append({
            "caseId": case["caseId"],
            "inferredDimension": inference["dimension"],
            "dimensionConfidence": inference["confidence"],
            "dimensionTopScore": inference["topScore"],
            "dimensionMargin": inference["margin"],
            "policyStatus": policy["status"],
            "policyDecision": policy["decision"],
            "policyId": policy["policyId"],
            "policyReasons": policy["reasons"],
        })

        rows.append({
            "caseId": case["caseId"],
            "inferredDimension": inference["dimension"],
            "dimensionConfidence": inference["confidence"],
            "lockedA7Prediction": locked["prediction"],
            "generalizedG1Prediction": generalized["prediction"],
            "observedDecision": locked["observedDecision"],
            "policyStatus": policy["status"],
            "policyDecision": generalized["policyDecision"],
            "policyId": generalized["policyId"],
            "policyReasons": generalized["policyReasons"],
            "lockedReasons": locked["reasons"],
            "generalizedReasons": generalized["reasons"],
        })

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {
        "schemaVersion": "s13-baserate-predictions-v1",
        "sourceCommit": git_head(),
        "algorithmFreeze": data["algorithmFreeze"],
        "phase3EvaluationHead": data["phase3EvaluationHead"],
        "featureDatasetId": data["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "policyRegistryId": registry["registryId"],
        "policyRegistrySha256": sha256(REGISTRY),
        "lockedSprint12RunnerSha256": sha256(S12_RUNNER),
        "lockedDimensionEngineSha256": sha256(DIM_MODULE),
        "lockedPolicyEngineSha256": sha256(POLICY_MODULE),
        "caseCount": len(rows),
        "declaredPositiveCount": data["positiveCount"],
        "declaredNegativeCount": data["negativeCount"],
        "declaredPrevalence": data["prevalence"],
        "cases": case_summaries,
        "boundary": {
            "labelsAvailableToPredictor": False,
            "registeredDimensionAvailableToPredictor": False,
            "policyConditionAvailableToPredictor": False,
            "dimensionSource": "automatic-inference",
            "policySource": "explicit-configured-registry",
            "unknownPolicyFallback": "locked-A7",
            "snapshotObservationOnly": True,
        },
    }

    RESULT.write_bytes(canonical(artifact) + b"\n")
    ROWS.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in rows),
        encoding="utf-8",
    )
    for path in (RESULT, ROWS):
        path.with_suffix(path.suffix + ".sha256").write_text(
            sha256(path) + "  " + path.name + "\n",
            encoding="utf-8",
        )

    policy_status = {}
    policy_decision = {}
    confidence = {}
    for row in rows:
        policy_status[row["policyStatus"]] = policy_status.get(row["policyStatus"], 0) + 1
        policy_decision[row["policyDecision"]] = policy_decision.get(row["policyDecision"], 0) + 1
        confidence[row["dimensionConfidence"]] = confidence.get(row["dimensionConfidence"], 0) + 1

    print(
        "SPRINT13_BASERATE_PREDICTION PASS "
        f"cases={len(rows)} "
        f"policy_status={json.dumps(policy_status, sort_keys=True, separators=(',', ':'))} "
        f"policy_decision={json.dumps(policy_decision, sort_keys=True, separators=(',', ':'))} "
        f"dimension_confidence={json.dumps(confidence, sort_keys=True, separators=(',', ':'))}"
    )

if __name__ == "__main__":
    main()
