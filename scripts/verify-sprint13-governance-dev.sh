#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE4_COMPLETE="a07cf497e6689c9b1d161ae2e22c747fdbac33f7"
ALGORITHM_FREEZE="c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd"
LABELS="lab/ground-truth/GT-S13-GOV-DEV-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
trap 'restore; rm -rf "$TMP_DIR"' EXIT

git diff --exit-code "$PHASE4_COMPLETE" --   lab/ground-truth/POL-S13-BASERATE-001.json   lab/ground-truth/GT-S13-BASERATE-FEATURES.json   lab/ground-truth/GT-S13-BASERATE-LABELS.json   scripts/run-sprint13-baserate.py   scripts/verify-sprint13-baserate.py
echo "SPRINT13_GOV_PHASE4_EVIDENCE_LOCK PASS"

git diff --exit-code "$ALGORITHM_FREEZE" --   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_GOV_FROZEN_ENGINE_LOCK PASS"

rm "$LABELS"
python3 scripts/run-sprint13-governance-dev.py
cp build/s13-gov-dev/predictions.json "$TMP_DIR/p1.json"
cp build/s13-gov-dev/predictions.jsonl "$TMP_DIR/p1.jsonl"
restore
python3 scripts/verify-sprint13-governance-dev.py
cp build/s13-gov-dev/evaluation.json "$TMP_DIR/e1.json"
cp build/s13-gov-dev/evaluation-cases.jsonl "$TMP_DIR/e1.jsonl"

rm "$LABELS"
python3 scripts/run-sprint13-governance-dev.py
cmp "$TMP_DIR/p1.json" build/s13-gov-dev/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-gov-dev/predictions.jsonl
restore
python3 scripts/verify-sprint13-governance-dev.py
cmp "$TMP_DIR/e1.json" build/s13-gov-dev/evaluation.json
cmp "$TMP_DIR/e1.jsonl" build/s13-gov-dev/evaluation-cases.jsonl

echo "SPRINT13_GOV_DEV_REPEATABILITY PASS"
echo "SPRINT13_GOV_DEV_LABEL_ABSENCE_GATE PASS"
