#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ALGORITHM_FREEZE="c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd"
LABELS="lab/ground-truth/GT-S13-POLICY-EVAL-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
trap 'restore; rm -rf "$TMP_DIR"' EXIT

echo "=== Frozen Phase 3 algorithm gate ==="
git diff --exit-code "$ALGORITHM_FREEZE" --   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_POLICY_ALGORITHM_FREEZE PASS"

rm "$LABELS"
python3 scripts/run-sprint13-policy-eval.py
cp build/s13-policy-eval/predictions.json "$TMP_DIR/p1.json"
cp build/s13-policy-eval/predictions.jsonl "$TMP_DIR/p1.jsonl"
cp build/s13-policy-eval/predictions.json.sha256 "$TMP_DIR/p1.sha"
cp build/s13-policy-eval/predictions.jsonl.sha256 "$TMP_DIR/p1l.sha"
restore
python3 scripts/verify-sprint13-policy-eval.py
cp build/s13-policy-eval/evaluation.json "$TMP_DIR/e1.json"
cp build/s13-policy-eval/evaluation-cases.jsonl "$TMP_DIR/e1.jsonl"

rm "$LABELS"
python3 scripts/run-sprint13-policy-eval.py
cmp "$TMP_DIR/p1.json" build/s13-policy-eval/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-policy-eval/predictions.jsonl
cmp "$TMP_DIR/p1.sha" build/s13-policy-eval/predictions.json.sha256
cmp "$TMP_DIR/p1l.sha" build/s13-policy-eval/predictions.jsonl.sha256
restore
python3 scripts/verify-sprint13-policy-eval.py
cmp "$TMP_DIR/e1.json" build/s13-policy-eval/evaluation.json
cmp "$TMP_DIR/e1.jsonl" build/s13-policy-eval/evaluation-cases.jsonl

echo "SPRINT13_POLICY_EVAL_REPEATABILITY PASS"
echo "SPRINT13_POLICY_EVAL_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_POLICY_EVAL_RESEARCH_GATE PASS"
