#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE3_COMPLETE="53e1995b22c29f1a4672cf16ffa420f5cf74fde0"
ALGORITHM_FREEZE="c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd"
LABELS="lab/ground-truth/GT-S13-BASERATE-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"

restore_labels() {
  if [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]]; then
    cp "$TMP_DIR/labels.json" "$LABELS"
  fi
}
trap 'restore_labels; rm -rf "$TMP_DIR"' EXIT

echo "=== Phase 3 immutable evidence gate ==="
git diff --exit-code "$PHASE3_COMPLETE" --   lab/ground-truth/POL-S13-DEV-001.json   lab/ground-truth/POL-S13-EVAL-001.json   lab/ground-truth/GT-S13-POLICY-DEV-FEATURES.json   lab/ground-truth/GT-S13-POLICY-DEV-LABELS.json   lab/ground-truth/GT-S13-POLICY-EVAL-FEATURES.json   lab/ground-truth/GT-S13-POLICY-EVAL-LABELS.json   scripts/run-sprint13-policy-dev.py   scripts/verify-sprint13-policy-dev.py   scripts/run-sprint13-policy-eval.py   scripts/verify-sprint13-policy-eval.py
echo "SPRINT13_BASERATE_PHASE3_EVIDENCE_LOCK PASS"

echo "=== Frozen algorithm gate ==="
git diff --exit-code "$ALGORITHM_FREEZE" --   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_BASERATE_ALGORITHM_FREEZE PASS"

echo "=== Policy registry robustness ==="
python3 scripts/verify-sprint13-policy-registry-robustness.py

rm "$LABELS"

echo "=== Blind Phase 4 prediction #1 ==="
python3 scripts/run-sprint13-baserate.py
cp build/s13-baserate/predictions.json "$TMP_DIR/predictions-1.json"
cp build/s13-baserate/predictions.jsonl "$TMP_DIR/predictions-1.jsonl"
cp build/s13-baserate/predictions.json.sha256 "$TMP_DIR/predictions-1.sha"
cp build/s13-baserate/predictions.jsonl.sha256 "$TMP_DIR/predictions-1l.sha"

restore_labels

echo "=== Sealed-label Phase 4 scoring #1 ==="
python3 scripts/verify-sprint13-baserate.py
cp build/s13-baserate/evaluation.json "$TMP_DIR/evaluation-1.json"
cp build/s13-baserate/evaluation-cases.jsonl "$TMP_DIR/evaluation-1.jsonl"
cp build/s13-baserate/evaluation.json.sha256 "$TMP_DIR/evaluation-1.sha"
cp build/s13-baserate/evaluation-cases.jsonl.sha256 "$TMP_DIR/evaluation-1l.sha"

rm "$LABELS"

echo "=== Blind Phase 4 prediction #2 ==="
python3 scripts/run-sprint13-baserate.py
cmp "$TMP_DIR/predictions-1.json" build/s13-baserate/predictions.json
cmp "$TMP_DIR/predictions-1.jsonl" build/s13-baserate/predictions.jsonl
cmp "$TMP_DIR/predictions-1.sha" build/s13-baserate/predictions.json.sha256
cmp "$TMP_DIR/predictions-1l.sha" build/s13-baserate/predictions.jsonl.sha256

restore_labels

echo "=== Sealed-label Phase 4 scoring #2 ==="
python3 scripts/verify-sprint13-baserate.py
cmp "$TMP_DIR/evaluation-1.json" build/s13-baserate/evaluation.json
cmp "$TMP_DIR/evaluation-1.jsonl" build/s13-baserate/evaluation-cases.jsonl
cmp "$TMP_DIR/evaluation-1.sha" build/s13-baserate/evaluation.json.sha256
cmp "$TMP_DIR/evaluation-1l.sha" build/s13-baserate/evaluation-cases.jsonl.sha256

echo "SPRINT13_BASERATE_REPEATABILITY PASS"
echo "SPRINT13_BASERATE_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_BASERATE_RESEARCH_GATE PASS"
