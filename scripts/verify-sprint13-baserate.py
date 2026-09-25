#!/usr/bin/env python3
"""Sprint 13 Phase 4 sealed-label verifier with base-rate-sensitive metrics."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import math
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURES = ROOT / "lab" / "ground-truth" / "GT-S13-BASERATE-FEATURES.json"
LABELS = ROOT / "lab" / "ground-truth" / "GT-S13-BASERATE-LABELS.json"
REGISTRY = ROOT / "lab" / "ground-truth" / "POL-S13-BASERATE-001.json"
PREDICTOR = ROOT / "scripts" / "run-sprint13-baserate.py"
DIM_MODULE = ROOT / "scripts" / "sprint13_dimension_inference.py"
POLICY_MODULE = ROOT / "scripts" / "sprint13_policy_semantics.py"
S12_RUNNER = ROOT / "scripts" / "run-sprint12-ablation.py"
OUT_DIR = ROOT / "build" / "s13-baserate"
RESULT = OUT_DIR / "predictions.json"
ROWS = OUT_DIR / "predictions.jsonl"
EVALUATION = OUT_DIR / "evaluation.json"
EVALUATION_ROWS = OUT_DIR / "evaluation-cases.jsonl"

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")

def ratio(numerator, denominator):
    return None if denominator == 0 else numerator / denominator

def wilson(successes, total, z=1.959963984540054):
    if total == 0:
        return None
    p = successes / total
    denom = 1.0 + (z * z) / total
    center = (p + (z * z) / (2.0 * total)) / denom
    half = (
        z
        * math.sqrt((p * (1.0 - p) / total) + (z * z) / (4.0 * total * total))
        / denom
    )
    return {"lower": max(0.0, center - half), "upper": min(1.0, center + half)}

def confusion(rows, prediction_key):
    tp = tn = fp = fn = 0
    for row in rows:
        truth = row["groundTruth"]
        predicted = row[prediction_key]
        if truth == "POSITIVE" and predicted == "POSITIVE":
            tp += 1
        elif truth == "NEGATIVE" and predicted == "NEGATIVE":
            tn += 1
        elif truth == "NEGATIVE" and predicted == "POSITIVE":
            fp += 1
        elif truth == "POSITIVE" and predicted == "NEGATIVE":
            fn += 1
        else:
            raise AssertionError("invalid binary truth/prediction")

    sensitivity = ratio(tp, tp + fn)
    specificity = ratio(tn, tn + fp)
    precision = ratio(tp, tp + fp)
    npv = ratio(tn, tn + fn)
    fpr = ratio(fp, fp + tn)
    fnr = ratio(fn, fn + tp)
    accuracy = ratio(tp + tn, tp + tn + fp + fn)
    balanced_accuracy = None
    if sensitivity is not None and specificity is not None:
        balanced_accuracy = (sensitivity + specificity) / 2.0
    f1 = None
    if precision is not None and sensitivity is not None and precision + sensitivity > 0:
        f1 = 2.0 * precision * sensitivity / (precision + sensitivity)

    mcc_den = math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
    mcc = None if mcc_den == 0 else ((tp * tn) - (fp * fn)) / mcc_den

    return {
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
        "sensitivity": sensitivity,
        "recall": sensitivity,
        "specificity": specificity,
        "fpr": fpr,
        "fnr": fnr,
        "precision": precision,
        "npv": npv,
        "accuracy": accuracy,
        "balancedAccuracy": balanced_accuracy,
        "f1": f1,
        "mcc": mcc,
        "sensitivityWilson95": wilson(tp, tp + fn),
        "specificityWilson95": wilson(tn, tn + fp),
        "precisionWilson95": wilson(tp, tp + fp),
        "npvWilson95": wilson(tn, tn + fn),
    }

def prevalence_projection(metrics, prevalence):
    sensitivity = metrics["sensitivity"]
    specificity = metrics["specificity"]
    if sensitivity is None or specificity is None:
        return {"prevalence": prevalence, "ppv": None, "npv": None, "alertsPer1000": None}
    fpr = 1.0 - specificity
    ppv_den = sensitivity * prevalence + fpr * (1.0 - prevalence)
    ppv = None if ppv_den == 0 else (sensitivity * prevalence) / ppv_den
    npv_den = specificity * (1.0 - prevalence) + (1.0 - sensitivity) * prevalence
    npv = None if npv_den == 0 else (specificity * (1.0 - prevalence)) / npv_den
    alerts_per_1000 = 1000.0 * (sensitivity * prevalence + fpr * (1.0 - prevalence))
    true_alerts_per_1000 = 1000.0 * sensitivity * prevalence
    false_alerts_per_1000 = 1000.0 * fpr * (1.0 - prevalence)
    return {
        "prevalence": prevalence,
        "ppv": ppv,
        "npv": npv,
        "alertsPer1000": alerts_per_1000,
        "trueAlertsPer1000": true_alerts_per_1000,
        "falseAlertsPer1000": false_alerts_per_1000,
    }

def subset_breakdown(rows, field, prediction_key):
    values = sorted({row[field] for row in rows})
    result = {}
    for value in values:
        subset = [row for row in rows if row[field] == value]
        result[value] = {
            "count": len(subset),
            "positive": sum(1 for row in subset if row["groundTruth"] == "POSITIVE"),
            "negative": sum(1 for row in subset if row["groundTruth"] == "NEGATIVE"),
            "metrics": confusion(subset, prediction_key),
            "unknownPolicyCount": sum(1 for row in subset if row["policyDecision"] == "UNKNOWN"),
        }
    return result

def main():
    features = json.loads(FEATURES.read_text(encoding="utf-8"))
    labels = json.loads(LABELS.read_text(encoding="utf-8"))
    prediction = json.loads(RESULT.read_text(encoding="utf-8"))
    rows = [
        json.loads(line)
        for line in ROWS.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]

    if labels.get("sealedForPrediction") is not True:
        raise AssertionError("Phase 4 labels must be sealed")
    if labels.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("Phase 4 feature/label identity mismatch")
    if prediction.get("featureDatasetId") != features.get("datasetId"):
        raise AssertionError("Phase 4 prediction dataset mismatch")
    if prediction.get("featureDatasetSha256") != sha256(FEATURES):
        raise AssertionError("Phase 4 feature digest drift")
    if prediction.get("policyRegistrySha256") != sha256(REGISTRY):
        raise AssertionError("Phase 4 registry digest drift")
    if prediction.get("lockedDimensionEngineSha256") != sha256(DIM_MODULE):
        raise AssertionError("dimension engine digest drift")
    if prediction.get("lockedPolicyEngineSha256") != sha256(POLICY_MODULE):
        raise AssertionError("policy engine digest drift")
    if prediction.get("lockedSprint12RunnerSha256") != sha256(S12_RUNNER):
        raise AssertionError("locked A7 digest drift")
    if len(rows) != 96 or prediction.get("caseCount") != 96:
        raise AssertionError("Phase 4 requires 96 prediction rows")

    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if prediction.get("sourceCommit") != head:
        raise AssertionError("Phase 4 prediction source commit drift")

    predictor_text = PREDICTOR.read_text(encoding="utf-8")
    if "GT-S13-BASERATE-LABELS" in predictor_text:
        raise AssertionError("blind Phase 4 predictor references sealed label file")

    feature_ids = {case["caseId"] for case in features["cases"]}
    by_label = {case["caseId"]: case for case in labels["cases"]}
    if len(feature_ids) != 96 or len(by_label) != 96 or set(by_label) != feature_ids:
        raise AssertionError("Phase 4 case identity mismatch")

    joined = []
    for row in rows:
        label = by_label.get(row["caseId"])
        if label is None:
            raise AssertionError("prediction references unknown Phase 4 case")
        expected_policy = label["expectedAuthorization"]
        policy_correct = row["policyDecision"] == expected_policy
        joined.append({
            **row,
            **label,
            "policyDecisionCorrect": policy_correct,
        })

    positive_count = sum(1 for row in joined if row["groundTruth"] == "POSITIVE")
    negative_count = sum(1 for row in joined if row["groundTruth"] == "NEGATIVE")
    if (positive_count, negative_count) != (8, 88):
        raise AssertionError("Phase 4 prevalence drift")

    dimension_correct = sum(
        1 for row in joined if row["inferredDimension"] == row["registeredDimension"]
    )
    unknown_policy = sum(1 for row in joined if row["policyDecision"] == "UNKNOWN")
    decisive_policy = len(joined) - unknown_policy
    policy_correct_all = sum(1 for row in joined if row["policyDecisionCorrect"])
    policy_correct_decisive = sum(
        1 for row in joined
        if row["policyDecision"] != "UNKNOWN" and row["policyDecisionCorrect"]
    )

    a7 = confusion(joined, "lockedA7Prediction")
    g1 = confusion(joined, "generalizedG1Prediction")

    prevalence_points = [0.01, 0.05, positive_count / len(joined), 0.10]
    projections = {
        "lockedA7": [prevalence_projection(a7, p) for p in prevalence_points],
        "generalizedG1": [prevalence_projection(g1, p) for p in prevalence_points],
    }

    condition_a7 = subset_breakdown(joined, "policyCondition", "lockedA7Prediction")
    condition_g1 = subset_breakdown(joined, "policyCondition", "generalizedG1Prediction")
    dimension_a7 = subset_breakdown(joined, "registeredDimension", "lockedA7Prediction")
    dimension_g1 = subset_breakdown(joined, "registeredDimension", "generalizedG1Prediction")

    policy_status_counts = {}
    policy_decision_counts = {}
    for row in joined:
        policy_status_counts[row["policyStatus"]] = policy_status_counts.get(row["policyStatus"], 0) + 1
        policy_decision_counts[row["policyDecision"]] = policy_decision_counts.get(row["policyDecision"], 0) + 1

    evaluation = {
        "schemaVersion": "s13-baserate-evaluation-v1",
        "sourceCommit": head,
        "featureDatasetId": features["datasetId"],
        "featureDatasetSha256": sha256(FEATURES),
        "labelSetId": labels["labelSetId"],
        "labelSetSha256": sha256(LABELS),
        "policyRegistryId": prediction["policyRegistryId"],
        "policyRegistrySha256": sha256(REGISTRY),
        "caseCount": len(joined),
        "positiveCount": positive_count,
        "negativeCount": negative_count,
        "prevalence": positive_count / len(joined),
        "dimensionInference": {
            "correct": dimension_correct,
            "count": len(joined),
            "accuracy": dimension_correct / len(joined),
        },
        "policyCoverage": {
            "decisiveCount": decisive_policy,
            "unknownCount": unknown_policy,
            "decisiveRate": decisive_policy / len(joined),
            "allCaseExpectedDecisionAccuracy": policy_correct_all / len(joined),
            "decisiveExpectedDecisionAccuracy": (
                None if decisive_policy == 0 else policy_correct_decisive / decisive_policy
            ),
            "statusCounts": policy_status_counts,
            "decisionCounts": policy_decision_counts,
        },
        "lockedA7": a7,
        "generalizedG1": g1,
        "prevalenceProjections": projections,
        "byPolicyCondition": {
            "lockedA7": condition_a7,
            "generalizedG1": condition_g1,
        },
        "byDimension": {
            "lockedA7": dimension_a7,
            "generalizedG1": dimension_g1,
        },
        "claimBoundary": [
            "96-case synthetic internal stress corpus with 8.33% measured prevalence",
            "configured policy quality is intentionally degraded in missing, ambiguous, stale and incomplete-context conditions",
            "prevalence projections are mathematical projections from measured sensitivity/specificity, not additional observed datasets",
            "no production accuracy, external validity or automatic policy extraction claim is established",
        ],
    }

    EVALUATION.write_bytes(canonical(evaluation) + b"\n")
    EVALUATION_ROWS.write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in joined),
        encoding="utf-8",
    )
    for artifact in (RESULT, ROWS, EVALUATION, EVALUATION_ROWS):
        if artifact in (EVALUATION, EVALUATION_ROWS):
            artifact.with_suffix(artifact.suffix + ".sha256").write_text(
                sha256(artifact) + "  " + artifact.name + "\n",
                encoding="utf-8",
            )
        sidecar = artifact.with_suffix(artifact.suffix + ".sha256")
        if sha256(artifact) != sidecar.read_text(encoding="utf-8").split()[0]:
            raise AssertionError(f"SHA-256 mismatch: {artifact.name}")

    blob = (
        RESULT.read_text(encoding="utf-8")
        + ROWS.read_text(encoding="utf-8")
        + EVALUATION.read_text(encoding="utf-8")
        + EVALUATION_ROWS.read_text(encoding="utf-8")
    )
    for forbidden in ("Bearer ", "\"Authorization\":", "synthetic-cookie-secret"):
        if forbidden in blob:
            raise AssertionError("secret-like material leaked into Phase 4 artifacts")

    def fmt(value):
        return "N/A" if value is None else f"{value:.6f}"

    print(
        "SPRINT13_BASERATE_RESULT "
        f"DIM={dimension_correct}/96 "
        f"POLICY_DECISIVE={decisive_policy}/96 UNKNOWN={unknown_policy} "
        f"POLICY_ALL_ACC={policy_correct_all}/96"
    )
    print(
        "SPRINT13_BASERATE_A7 "
        f"TP={a7['tp']} TN={a7['tn']} FP={a7['fp']} FN={a7['fn']} "
        f"P={fmt(a7['precision'])} R={fmt(a7['recall'])} SPEC={fmt(a7['specificity'])} "
        f"FPR={fmt(a7['fpr'])} NPV={fmt(a7['npv'])} BACC={fmt(a7['balancedAccuracy'])} "
        f"F1={fmt(a7['f1'])} MCC={fmt(a7['mcc'])}"
    )
    print(
        "SPRINT13_BASERATE_G1 "
        f"TP={g1['tp']} TN={g1['tn']} FP={g1['fp']} FN={g1['fn']} "
        f"P={fmt(g1['precision'])} R={fmt(g1['recall'])} SPEC={fmt(g1['specificity'])} "
        f"FPR={fmt(g1['fpr'])} NPV={fmt(g1['npv'])} BACC={fmt(g1['balancedAccuracy'])} "
        f"F1={fmt(g1['f1'])} MCC={fmt(g1['mcc'])}"
    )
    for condition in sorted(condition_g1):
        m = condition_g1[condition]["metrics"]
        print(
            "SPRINT13_BASERATE_CONDITION "
            f"{condition} count={condition_g1[condition]['count']} "
            f"FP={m['fp']} FN={m['fn']} unknown={condition_g1[condition]['unknownPolicyCount']}"
        )
    for projection in projections["generalizedG1"]:
        print(
            "SPRINT13_BASERATE_PROJECTED_G1 "
            f"prevalence={projection['prevalence']:.6f} "
            f"ppv={fmt(projection['ppv'])} npv={fmt(projection['npv'])} "
            f"alerts_per_1000={fmt(projection['alertsPer1000'])} "
            f"false_alerts_per_1000={fmt(projection['falseAlertsPer1000'])}"
        )
    print("SPRINT13_BASERATE_VERIFY PASS rows=96")

if __name__ == "__main__":
    main()
