#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE5_COMPLETE="8f57460aabd62d7c009838d8207dd702aad651a1"
NORM_FREEZE="4011b9c05b99b14da66733aea47de43256060990"
LABELS="lab/ground-truth/GT-S13-XFRAME-EVAL-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
trap 'restore; rm -rf "$TMP_DIR"' EXIT

git diff --exit-code "$PHASE5_COMPLETE" --   lab/ground-truth/POL-S13-GOV-EVAL-001.json   lab/ground-truth/GT-S13-GOV-EVAL-FEATURES.json   lab/ground-truth/GT-S13-GOV-EVAL-LABELS.json   scripts/sprint13_uncertainty_governance.py   scripts/run-sprint13-governance-eval.py   scripts/verify-sprint13-governance-eval.py
echo "SPRINT13_XFRAME_EVAL_PHASE5_EVIDENCE_LOCK PASS"

git diff --exit-code "$NORM_FREEZE" --   scripts/sprint13_cross_framework_normalization.py   scripts/sprint13_uncertainty_governance.py   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_XFRAME_EVAL_NORMALIZATION_FREEZE PASS"

rm "$LABELS"
python3 scripts/run-sprint13-cross-framework-eval.py
cp build/s13-xframe-eval/predictions.json "$TMP_DIR/p1.json"
cp build/s13-xframe-eval/predictions.jsonl "$TMP_DIR/p1.jsonl"
restore
python3 scripts/verify-sprint13-cross-framework-eval.py
cp build/s13-xframe-eval/evaluation.json "$TMP_DIR/e1.json"

rm "$LABELS"
python3 scripts/run-sprint13-cross-framework-eval.py
cmp "$TMP_DIR/p1.json" build/s13-xframe-eval/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-xframe-eval/predictions.jsonl
restore
python3 scripts/verify-sprint13-cross-framework-eval.py
cmp "$TMP_DIR/e1.json" build/s13-xframe-eval/evaluation.json

echo "SPRINT13_XFRAME_EVAL_REPEATABILITY PASS"
echo "SPRINT13_XFRAME_EVAL_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_XFRAME_EVAL_RESEARCH_GATE PASS"
