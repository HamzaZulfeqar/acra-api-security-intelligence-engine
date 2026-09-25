#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BASE_COMMIT="bd944e83a6edefafba56caebaa35e89fc107c282"
LABELS="lab/ground-truth/GT-S13-HOLDOUT-LABELS.json"
TMP_DIR="$(mktemp -d)"
LABEL_BACKUP="$TMP_DIR/labels.json"

restore_labels() {
  if [[ -f "$LABEL_BACKUP" && ! -f "$LABELS" ]]; then
    cp "$LABEL_BACKUP" "$LABELS"
  fi
}
trap 'restore_labels; rm -rf "$TMP_DIR"' EXIT

echo "=== Sprint 13 locked-rule gate ==="
git diff --exit-code "$BASE_COMMIT" -- scripts/run-sprint12-ablation.py
git diff --exit-code "$BASE_COMMIT" -- scripts/verify-sprint12-research.py
echo "SPRINT13_LOCKED_S12_RULE_GATE PASS"

cp "$LABELS" "$LABEL_BACKUP"
rm "$LABELS"

echo "=== Sprint 13 blind prediction pass #1 (labels absent) ==="
python3 scripts/run-sprint13-heldout.py
cp build/s13-heldout/predictions.json "$TMP_DIR/predictions-1.json"
cp build/s13-heldout/predictions.jsonl "$TMP_DIR/predictions-1.jsonl"
cp build/s13-heldout/predictions.json.sha256 "$TMP_DIR/predictions-1.json.sha256"
cp build/s13-heldout/predictions.jsonl.sha256 "$TMP_DIR/predictions-1.jsonl.sha256"

restore_labels

echo "=== Sprint 13 label join + oracle verification #1 ==="
python3 scripts/verify-sprint13-heldout.py
cp build/s13-heldout/evaluation.json "$TMP_DIR/evaluation-1.json"
cp build/s13-heldout/evaluation-cases.jsonl "$TMP_DIR/evaluation-1.jsonl"

rm "$LABELS"

echo "=== Sprint 13 blind prediction pass #2 (labels absent) ==="
python3 scripts/run-sprint13-heldout.py
cmp "$TMP_DIR/predictions-1.json" build/s13-heldout/predictions.json
cmp "$TMP_DIR/predictions-1.jsonl" build/s13-heldout/predictions.jsonl
cmp "$TMP_DIR/predictions-1.json.sha256" build/s13-heldout/predictions.json.sha256
cmp "$TMP_DIR/predictions-1.jsonl.sha256" build/s13-heldout/predictions.jsonl.sha256

restore_labels

echo "=== Sprint 13 label join + oracle verification #2 ==="
python3 scripts/verify-sprint13-heldout.py
cmp "$TMP_DIR/evaluation-1.json" build/s13-heldout/evaluation.json
cmp "$TMP_DIR/evaluation-1.jsonl" build/s13-heldout/evaluation-cases.jsonl

echo "SPRINT13_HELDOUT_REPEATABILITY PASS"
echo "SPRINT13_HELDOUT_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_HELDOUT_RESEARCH_GATE PASS"
