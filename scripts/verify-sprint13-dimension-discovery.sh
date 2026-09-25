#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE1_BASE="30acba767e4ce969909f1d018b23535e9c12b582"
S12_BASE="bd944e83a6edefafba56caebaa35e89fc107c282"
LABELS="lab/ground-truth/GT-S13-DIMENSION-LABELS.json"
TMP_DIR="$(mktemp -d)"
LABEL_BACKUP="$TMP_DIR/dimension-labels.json"

restore_labels() {
  if [[ -f "$LABEL_BACKUP" && ! -f "$LABELS" ]]; then
    cp "$LABEL_BACKUP" "$LABELS"
  fi
}
trap 'restore_labels; rm -rf "$TMP_DIR"' EXIT

echo "=== Sprint 13 Phase 2 immutable-history gates ==="
git diff --exit-code "$S12_BASE" -- scripts/run-sprint12-ablation.py scripts/verify-sprint12-research.py
git diff --exit-code "$PHASE1_BASE" --   lab/heldout-api/server.py   lab/ground-truth/GT-S13-HOLDOUT-FEATURES.json   lab/ground-truth/GT-S13-HOLDOUT-LABELS.json   scripts/run-sprint13-heldout.py   scripts/verify-sprint13-heldout.py   scripts/verify-sprint13-heldout.sh

echo "SPRINT13_DIMENSION_HISTORY_LOCK PASS"

cp "$LABELS" "$LABEL_BACKUP"
rm "$LABELS"

echo "=== Blind dimension inference pass #1 (labels absent) ==="
python3 scripts/run-sprint13-dimension-discovery.py
cp build/s13-dimension-discovery/predictions.json "$TMP_DIR/predictions-1.json"
cp build/s13-dimension-discovery/predictions.jsonl "$TMP_DIR/predictions-1.jsonl"
cp build/s13-dimension-discovery/predictions.json.sha256 "$TMP_DIR/predictions-1.json.sha256"
cp build/s13-dimension-discovery/predictions.jsonl.sha256 "$TMP_DIR/predictions-1.jsonl.sha256"

restore_labels

echo "=== Sealed dimension-label join + scoring #1 ==="
python3 scripts/verify-sprint13-dimension-discovery.py
cp build/s13-dimension-discovery/evaluation.json "$TMP_DIR/evaluation-1.json"
cp build/s13-dimension-discovery/evaluation-cases.jsonl "$TMP_DIR/evaluation-1.jsonl"
cp build/s13-dimension-discovery/evaluation.json.sha256 "$TMP_DIR/evaluation-1.json.sha256"
cp build/s13-dimension-discovery/evaluation-cases.jsonl.sha256 "$TMP_DIR/evaluation-1.jsonl.sha256"

rm "$LABELS"

echo "=== Blind dimension inference pass #2 (labels absent) ==="
python3 scripts/run-sprint13-dimension-discovery.py
cmp "$TMP_DIR/predictions-1.json" build/s13-dimension-discovery/predictions.json
cmp "$TMP_DIR/predictions-1.jsonl" build/s13-dimension-discovery/predictions.jsonl
cmp "$TMP_DIR/predictions-1.json.sha256" build/s13-dimension-discovery/predictions.json.sha256
cmp "$TMP_DIR/predictions-1.jsonl.sha256" build/s13-dimension-discovery/predictions.jsonl.sha256

restore_labels

echo "=== Sealed dimension-label join + scoring #2 ==="
python3 scripts/verify-sprint13-dimension-discovery.py
cmp "$TMP_DIR/evaluation-1.json" build/s13-dimension-discovery/evaluation.json
cmp "$TMP_DIR/evaluation-1.jsonl" build/s13-dimension-discovery/evaluation-cases.jsonl
cmp "$TMP_DIR/evaluation-1.json.sha256" build/s13-dimension-discovery/evaluation.json.sha256
cmp "$TMP_DIR/evaluation-1.jsonl.sha256" build/s13-dimension-discovery/evaluation-cases.jsonl.sha256

echo "SPRINT13_DIMENSION_REPEATABILITY PASS"
echo "SPRINT13_DIMENSION_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_DIMENSION_RESEARCH_GATE PASS"
