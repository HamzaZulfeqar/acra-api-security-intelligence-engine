#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE4_COMPLETE="a07cf497e6689c9b1d161ae2e22c747fdbac33f7"
GOVERNANCE_FREEZE="11afbb80be24116c9facd0d4b0791f4a12efed3a"
LABELS="lab/ground-truth/GT-S13-GOV-EVAL-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
trap 'restore; rm -rf "$TMP_DIR"' EXIT

echo "=== Phase 4 evidence lock ==="
git diff --exit-code "$PHASE4_COMPLETE" --   lab/ground-truth/POL-S13-BASERATE-001.json   lab/ground-truth/GT-S13-BASERATE-FEATURES.json   lab/ground-truth/GT-S13-BASERATE-LABELS.json   scripts/run-sprint13-baserate.py   scripts/verify-sprint13-baserate.py
echo "SPRINT13_GOV_EVAL_PHASE4_EVIDENCE_LOCK PASS"

echo "=== Governance freeze gate ==="
git diff --exit-code "$GOVERNANCE_FREEZE" --   scripts/sprint13_uncertainty_governance.py   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_GOV_EVAL_ALGORITHM_FREEZE PASS"

rm "$LABELS"
python3 scripts/run-sprint13-governance-eval.py
cp build/s13-gov-eval/predictions.json "$TMP_DIR/p1.json"
cp build/s13-gov-eval/predictions.jsonl "$TMP_DIR/p1.jsonl"
cp build/s13-gov-eval/predictions.json.sha256 "$TMP_DIR/p1.sha"
cp build/s13-gov-eval/predictions.jsonl.sha256 "$TMP_DIR/p1l.sha"
restore
python3 scripts/verify-sprint13-governance-eval.py
cp build/s13-gov-eval/evaluation.json "$TMP_DIR/e1.json"
cp build/s13-gov-eval/evaluation-cases.jsonl "$TMP_DIR/e1.jsonl"
cp build/s13-gov-eval/evaluation.json.sha256 "$TMP_DIR/e1.sha"
cp build/s13-gov-eval/evaluation-cases.jsonl.sha256 "$TMP_DIR/e1l.sha"

rm "$LABELS"
python3 scripts/run-sprint13-governance-eval.py
cmp "$TMP_DIR/p1.json" build/s13-gov-eval/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-gov-eval/predictions.jsonl
cmp "$TMP_DIR/p1.sha" build/s13-gov-eval/predictions.json.sha256
cmp "$TMP_DIR/p1l.sha" build/s13-gov-eval/predictions.jsonl.sha256
restore
python3 scripts/verify-sprint13-governance-eval.py
cmp "$TMP_DIR/e1.json" build/s13-gov-eval/evaluation.json
cmp "$TMP_DIR/e1.jsonl" build/s13-gov-eval/evaluation-cases.jsonl
cmp "$TMP_DIR/e1.sha" build/s13-gov-eval/evaluation.json.sha256
cmp "$TMP_DIR/e1l.sha" build/s13-gov-eval/evaluation-cases.jsonl.sha256

echo "SPRINT13_GOV_EVAL_REPEATABILITY PASS"
echo "SPRINT13_GOV_EVAL_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_GOV_EVAL_RESEARCH_GATE PASS"
