#!/usr/bin/env python3
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "lab" / "ground-truth" / "GT-S11-AUTHORIZATION-RESEARCH.json"
OUT_DIR = ROOT / "build" / "s12-research"
RESULT = OUT_DIR / "EXP-A0-A7.json"
ROWS = OUT_DIR / "EXP-A0-A7-cases.jsonl"
CSV = OUT_DIR / "EXP-A0-A7.csv"
EXPECTED_VARIANTS = [f"A{i}" for i in range(8)]


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def close(a, b):
    if a is None or b is None:
        return a is b
    return abs(a - b) < 1e-12


def metrics(rows):
    tp = tn = fp = fn = 0
    for row in rows:
        truth = row["groundTruth"]
        pred = row["prediction"]
        if truth == "POSITIVE" and pred == "POSITIVE":
            tp += 1
        elif truth == "NEGATIVE" and pred == "NEGATIVE":
            tn += 1
        elif truth == "NEGATIVE" and pred == "POSITIVE":
            fp += 1
        elif truth == "POSITIVE" and pred == "NEGATIVE":
            fn += 1
        else:
            raise AssertionError("invalid binary label")

    precision = None if tp + fp == 0 else tp / (tp + fp)
    recall = None if tp + fn == 0 else tp / (tp + fn)
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


def main():
    dataset = json.loads(DATASET.read_text(encoding="utf-8"))
    result = json.loads(RESULT.read_text(encoding="utf-8"))
    rows = [
        json.loads(line)
        for line in ROWS.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]

    if result["schemaVersion"] != "s12-a0-a7-research-v1":
        raise AssertionError("unexpected Sprint 12 result schema")
    if result["datasetId"] != dataset["groundTruthId"]:
        raise AssertionError("dataset identity drift")
    if result["datasetSha256"] != sha256(DATASET):
        raise AssertionError("dataset digest drift")
    if result["caseCount"] != 16 or result["variantCount"] != 8:
        raise AssertionError("unexpected campaign cardinality")
    if len(rows) != 128:
        raise AssertionError("expected 16 cases x 8 variants")

    boundary = result["predictionBoundary"]
    if boundary.get("groundTruthJoinedAfterPrediction") is not True:
        raise AssertionError("label join boundary must be explicit")
    if boundary.get("usesVulnerableFixtureOnly") is not True:
        raise AssertionError("treatment must use only the vulnerable fixture")
    if boundary.get("dimensionDiscoveryMeasured") is not False:
        raise AssertionError("dimension discovery must remain outside the measured claim")

    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if result["sourceCommit"] != head:
        raise AssertionError("result must identify the exact executing commit")

    variants = result["variants"]
    if [item["variant"] for item in variants] != EXPECTED_VARIANTS:
        raise AssertionError("A0-A7 ordering drift")

    previous_capabilities = set()
    by_variant = {}
    for item in variants:
        current = set(item["capabilities"])
        if not previous_capabilities.issubset(current):
            raise AssertionError("ablation capabilities must be cumulative")
        previous_capabilities = current
        by_variant[item["variant"]] = item

    seen = set()
    grouped = {name: [] for name in EXPECTED_VARIANTS}
    for row in rows:
        key = (row["variant"], row["caseId"])
        if key in seen:
            raise AssertionError("duplicate variant/case row")
        seen.add(key)
        grouped[row["variant"]].append(row)

        if row["groundTruth"] not in {"POSITIVE", "NEGATIVE"}:
            raise AssertionError("invalid ground truth")
        if row["prediction"] not in {"POSITIVE", "NEGATIVE"}:
            raise AssertionError("invalid prediction")
        if "Bearer " in json.dumps(row) or "Authorization" in json.dumps(row):
            raise AssertionError("credential material leaked into case artifact")

    for variant in EXPECTED_VARIANTS:
        subset = grouped[variant]
        if len(subset) != 16:
            raise AssertionError(f"{variant} missing case results")
        positives = sum(1 for row in subset if row["groundTruth"] == "POSITIVE")
        negatives = sum(1 for row in subset if row["groundTruth"] == "NEGATIVE")
        if (positives, negatives) != (8, 8):
            raise AssertionError("balanced frozen labels drifted")

        recomputed = metrics(subset)
        reported = by_variant[variant]["metrics"]
        for key in ("tp", "tn", "fp", "fn"):
            if recomputed[key] != reported[key]:
                raise AssertionError(f"{variant} {key} mismatch")
        for key in ("precision", "recall", "f1"):
            if not close(recomputed[key], reported[key]):
                raise AssertionError(f"{variant} {key} mismatch")

        fingerprints = {row["configFingerprint"] for row in subset}
        if fingerprints != {by_variant[variant]["configFingerprint"]}:
            raise AssertionError(f"{variant} configuration fingerprint mismatch")

    for artifact in (RESULT, ROWS, CSV):
        sidecar = artifact.with_suffix(artifact.suffix + ".sha256")
        expected = sidecar.read_text(encoding="utf-8").split()[0]
        if sha256(artifact) != expected:
            raise AssertionError(f"SHA-256 sidecar mismatch: {artifact.name}")

    text = RESULT.read_text(encoding="utf-8") + ROWS.read_text(encoding="utf-8")
    for forbidden in ("synthetic-cookie-secret", "\"Authorization\":", "Bearer "):
        if forbidden in text:
            raise AssertionError("secret-like execution material leaked to research artifacts")

    print(
        "SPRINT12_RESEARCH_ARTIFACT_VERIFY PASS "
        f"rows={len(rows)} variants={len(variants)} "
        f"dataset_sha256={result['datasetSha256']} result_sha256={sha256(RESULT)}"
    )


if __name__ == "__main__":
    main()
