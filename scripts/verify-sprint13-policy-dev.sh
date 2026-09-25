#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE2_BASE="2d030735401e336292ec5d18d7e01702c1e70039"
LABELS="lab/ground-truth/GT-S13-POLICY-DEV-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
trap 'restore; rm -rf "$TMP_DIR"' EXIT

echo "=== Phase 2 immutable-history gate ==="
git diff --exit-code "$PHASE2_BASE" --   scripts/sprint13_dimension_inference.py   scripts/run-sprint13-dimension-discovery.py   scripts/verify-sprint13-dimension-discovery.py   lab/ground-truth/GT-S13-DIMENSION-FEATURES.json   lab/ground-truth/GT-S13-DIMENSION-LABELS.json
echo "SPRINT13_POLICY_PHASE2_LOCK PASS"

rm "$LABELS"
python3 scripts/run-sprint13-policy-dev.py
cp build/s13-policy-dev/predictions.json "$TMP_DIR/p1.json"
cp build/s13-policy-dev/predictions.jsonl "$TMP_DIR/p1.jsonl"
restore
python3 scripts/verify-sprint13-policy-dev.py
cp build/s13-policy-dev/evaluation.json "$TMP_DIR/e1.json"
cp build/s13-policy-dev/evaluation-cases.jsonl "$TMP_DIR/e1.jsonl"

rm "$LABELS"
python3 scripts/run-sprint13-policy-dev.py
cmp "$TMP_DIR/p1.json" build/s13-policy-dev/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-policy-dev/predictions.jsonl
restore
python3 scripts/verify-sprint13-policy-dev.py
cmp "$TMP_DIR/e1.json" build/s13-policy-dev/evaluation.json
cmp "$TMP_DIR/e1.jsonl" build/s13-policy-dev/evaluation-cases.jsonl

echo "SPRINT13_POLICY_DEV_REPEATABILITY PASS"
echo "SPRINT13_POLICY_DEV_LABEL_ABSENCE_GATE PASS"
