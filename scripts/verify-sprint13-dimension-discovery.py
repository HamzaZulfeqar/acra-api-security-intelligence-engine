#!/usr/bin/env python3
"""Sprint 13 Phase 2 sealed-label join and metric verification."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-DIMENSION-FEATURES.json"
LABELS = ROOT / "lab" / "ground-truth" / "GT-S13-DIMENSION-LABELS.json"
PREDICTION_RESULT = ROOT / "build" / "s13-dimension-discovery" / "predictions.json"
PREDICTION_ROWS = ROOT / "build" / "s13-dimension-discovery" / "predictions.jsonl"
EVALUATION = ROOT / "build" / "s13-dimension-discovery" / "evaluation.json"
EVALUATION_ROWS = ROOT / "build" / "s13-dimension-discovery" / "evaluation-cases.jsonl"
S12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
INFERENCE_MODULE = ROOT / "scripts" / "sprint13_dimension_inference.py"
PREDICTOR = ROOT / "scripts" / "run-sprint13-dimension-discovery.py"

def load_module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module

S12 = load_module(S12_RUNNER, "acra_s12_phase2_verify")
DIM = load_module(INFERENCE_MODULE, "acra_s13_dimension_verify")

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def canonical_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")

def close(a, b):
    if a is None or b is None:
        return a is b
    return abs(a - b) < 1e-12

def dimension_metrics(rows):
    labels = list(DIM.DIMENSIONS)
    confusion = {
        actual: {predicted: 0 for predicted in labels}
        for actual in labels
    }
    for row in rows:
        actual = row["registeredDimension"]
        predicted = row["inferredDimension"]
        if actual not in confusion or predicted not in confusion[actual]:
            raise AssertionError("invalid dimension label")
        confusion[actual][predicted] += 1

    total = len(rows)
    correct = sum(confusion[label][label] for label in labels)
    per_class = {}
    f1_values = []
    for label in labels:
        tp = confusion[label][label]
        fp = sum(confusion[actual][label] for actual in labels if actual != label)
        fn = sum(confusion[label][predicted] for predicted in labels if predicted != label)
        support = sum(confusion[label].values())
        precision = None if tp + fp == 0 else tp / (tp + fp)
        recall = None if tp + fn == 0 else tp / (tp + fn)
        f1 = None
        if precision is not None and recall is not None and precision + recall > 0:
            f1 = 2 * precision * recall / (precision + recall)
        per_class[label] = {
            "support": support,
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

def main():
    features = json.loads(FEATURES.read_text(encoding="utf-8"))
    labels = json.loads(LABELS.read_text(encoding="utf-8"))
    prediction = json.loads(PREDICTION_RESULT.read_text(encoding="utf-8"))
    rows = [
        json.loads(line)
        for line in PREDICTION_ROWS.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]

    if labels.get("sealedForInference") is not True:
        raise AssertionError("dimension labels must remain sealed for inference")
    if labels.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("dimension feature/label identity mismatch")
    if prediction.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("prediction feature dataset mismatch")
    if prediction.get("featureDatasetSha256") != sha256(FEATURES):
        raise AssertionError("dimension feature digest drift")
    if prediction.get("inferenceEngineSha256") != sha256(INFERENCE_MODULE):
        raise AssertionError("inference engine digest drift")
    if prediction.get("lockedSprint12RunnerSha256") != sha256(S12_RUNNER):
        raise AssertionError("locked Sprint 12 runner digest drift")

    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if prediction.get("sourceCommit") != head:
        raise AssertionError("prediction artifact must record exact executing commit")

    boundary = prediction.get("boundary", {})
    if boundary.get("registeredDimensionAvailableToInference") is not False:
        raise AssertionError("registered dimension must be unavailable during inference")
    if boundary.get("vulnerabilityLabelsAvailableToInference") is not False:
        raise AssertionError("vulnerability labels must be unavailable during inference")
    if boundary.get("labelFileRequired") is not False:
        raise AssertionError("prediction stage must run without the sealed label file")
    if boundary.get("dimensionInjectedIntoDownstreamFromInferenceOnly") is not True:
        raise AssertionError("downstream ACRA must use inferred dimension only")

    predictor_text = PREDICTOR.read_text(encoding="utf-8")
    if "GT-S13-DIMENSION-LABELS" in predictor_text:
        raise AssertionError("blind predictor directly references sealed dimension labels")

    if prediction.get("caseCount") != 32:
        raise AssertionError("expected 32 Phase 2 cases")
    if prediction.get("variantCount") != 8:
        raise AssertionError("expected eight A0-A7 variants")
    if prediction.get("predictionRowCount") != 256 or len(rows) != 256:
        raise AssertionError("expected 32 cases x 8 downstream predictions")

    feature_cases = features.get("cases", [])
    label_cases = labels.get("cases", [])
    if len(feature_cases) != 32 or len(label_cases) != 32:
        raise AssertionError("Phase 2 feature/label cardinality drift")

    feature_ids = {case["caseId"] for case in feature_cases}
    by_label = {case["caseId"]: case for case in label_cases}
    if len(feature_ids) != 32 or set(by_label) != feature_ids:
        raise AssertionError("Phase 2 feature/label case identity mismatch")

    for case in feature_cases:
        if DIM.FORBIDDEN_KEYS.intersection(case):
            raise AssertionError("forbidden dimension/label field leaked into feature corpus")

    case_predictions = prediction.get("cases", [])
    if len(case_predictions) != 32:
        raise AssertionError("missing dimension inference case summaries")
    by_case_prediction = {case["caseId"]: case for case in case_predictions}
    if len(by_case_prediction) != 32 or set(by_case_prediction) != feature_ids:
        raise AssertionError("dimension inference summary identity mismatch")

    dimension_rows = []
    for case_id in sorted(feature_ids):
        inferred = by_case_prediction[case_id]
        label = by_label[case_id]
        dimension_rows.append({
            "rowType": "DIMENSION",
            "caseId": case_id,
            "sourceDataset": label["sourceDataset"],
            "registeredDimension": label["registeredDimension"],
            "inferredDimension": inferred["inferredDimension"],
            "correct": inferred["inferredDimension"] == label["registeredDimension"],
            "confidence": inferred["confidence"],
            "topScore": inferred["topScore"],
            "margin": inferred["margin"],
            "scores": inferred["scores"],
            "evidence": inferred["evidence"],
        })

    sources = ("S12_CALIBRATION", "S13_HOLDOUT")
    dimension_by_source = {}
    for source in sources:
        subset = [row for row in dimension_rows if row["sourceDataset"] == source]
        if len(subset) != 16:
            raise AssertionError(f"{source} must contain 16 dimension labels")
        dimension_by_source[source] = dimension_metrics(subset)

    combined_dimension = dimension_metrics(dimension_rows)

    seen = set()
    downstream_rows = []
    for row in rows:
        key = (row["caseId"], row["variant"])
        if key in seen:
            raise AssertionError("duplicate Phase 2 downstream prediction row")
        seen.add(key)
        label = by_label.get(row["caseId"])
        inferred = by_case_prediction.get(row["caseId"])
        if label is None or inferred is None:
            raise AssertionError("downstream prediction references unknown case")
        if row["inferredDimension"] != inferred["inferredDimension"]:
            raise AssertionError("downstream inferred dimension drift")
        downstream_rows.append({
            "rowType": "DOWNSTREAM",
            **row,
            "registeredDimension": label["registeredDimension"],
            "groundTruth": label["groundTruth"],
        })

    downstream_by_source = {}
    for source in sources:
        downstream_by_source[source] = {}
        for variant, _, _ in S12.VARIANTS:
            subset = [
                row for row in downstream_rows
                if row["sourceDataset"] == source and row["variant"] == variant
            ]
            if len(subset) != 16:
                raise AssertionError(f"{source}/{variant} must contain 16 downstream rows")
            downstream_by_source[source][variant] = S12.confusion(subset)

    OUT_DIR = EVALUATION.parent
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    evaluation = {
        "schemaVersion": "s13-dimension-evaluation-v1",
        "featureDatasetId": features["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "labelSetId": labels["labelSetId"],
        "labelSetSha256": sha256(LABELS),
        "sourceCommit": head,
        "inferenceEngineSha256": sha256(INFERENCE_MODULE),
        "dimensionMetrics": {
            **dimension_by_source,
            "COMBINED": combined_dimension,
        },
        "downstreamMetrics": downstream_by_source,
        "boundary": boundary,
        "claimBoundary": [
            "dimension accuracy is measured only on the frozen 32-case synthetic localhost corpus",
            "registered dimension and vulnerability labels were absent from the blind prediction process",
            "the evidence rules are deterministic project-authored heuristics rather than an independently trained model",
            "path semantics may not generalize to unseen naming conventions or frameworks",
            "no production or external-target accuracy claim is established",
        ],
    }

    EVALUATION.write_bytes(canonical_bytes(evaluation) + b"\n")
    EVALUATION_ROWS.write_text(
        "".join(
            json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n"
            for row in dimension_rows + downstream_rows
        ),
        encoding="utf-8",
    )

    for artifact in (PREDICTION_RESULT, PREDICTION_ROWS, EVALUATION, EVALUATION_ROWS):
        if artifact in (EVALUATION, EVALUATION_ROWS):
            artifact.with_suffix(artifact.suffix + ".sha256").write_text(
                sha256(artifact) + "  " + artifact.name + "\n",
                encoding="utf-8",
            )
        sidecar = artifact.with_suffix(artifact.suffix + ".sha256")
        expected = sidecar.read_text(encoding="utf-8").split()[0]
        if sha256(artifact) != expected:
            raise AssertionError(f"SHA-256 sidecar mismatch: {artifact.name}")

    text_blob = (
        PREDICTION_RESULT.read_text(encoding="utf-8")
        + PREDICTION_ROWS.read_text(encoding="utf-8")
        + EVALUATION.read_text(encoding="utf-8")
        + EVALUATION_ROWS.read_text(encoding="utf-8")
    )
    for forbidden in ("Bearer ", "\"Authorization\":", "synthetic-cookie-secret"):
        if forbidden in text_blob:
            raise AssertionError("secret-like material leaked into Phase 2 artifacts")

    for source in sources:
        dm = dimension_by_source[source]
        print(
            "SPRINT13_DIMENSION_RESULT "
            f"{source} correct={dm['correct']}/{dm['count']} "
            f"accuracy={dm['accuracy']:.6f} macro_f1={dm['macroF1']:.6f}"
        )
        for variant in ("A0", "A2", "A7"):
            m = downstream_by_source[source][variant]
            print(
                "SPRINT13_DIMENSION_DOWNSTREAM "
                f"{source} {variant} TP={m['tp']} TN={m['tn']} FP={m['fp']} FN={m['fn']} "
                f"precision={m['precision'] if m['precision'] is not None else 'N/A'} "
                f"recall={m['recall'] if m['recall'] is not None else 'N/A'} "
                f"f1={m['f1'] if m['f1'] is not None else 'N/A'}"
            )

    print(
        "SPRINT13_DIMENSION_COMBINED "
        f"correct={combined_dimension['correct']}/{combined_dimension['count']} "
        f"accuracy={combined_dimension['accuracy']:.6f} "
        f"macro_f1={combined_dimension['macroF1']:.6f}"
    )
    print("SPRINT13_DIMENSION_VERIFY PASS rows=288")

if __name__ == "__main__":
    main()
