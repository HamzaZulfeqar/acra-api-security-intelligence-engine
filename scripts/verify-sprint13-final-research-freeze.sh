#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

python3 scripts/verify-sprint13-final-research-freeze.py
cp build/s13-final-freeze/evidence-hashes.json "$TMP/evidence-hashes-1.json"
cp build/s13-final-freeze/freeze-summary.json "$TMP/freeze-summary-1.json"

rm -rf build/s13-final-freeze
python3 scripts/verify-sprint13-final-research-freeze.py
cmp "$TMP/evidence-hashes-1.json" build/s13-final-freeze/evidence-hashes.json
cmp "$TMP/freeze-summary-1.json" build/s13-final-freeze/freeze-summary.json
echo "SPRINT13_FINAL_FREEZE_REPEATABILITY PASS"

mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package
echo "SPRINT13_FINAL_FREEZE_MAVEN PASS"

git diff --check
echo "SPRINT13_FINAL_FREEZE_DIFF_CHECK PASS"

echo "SPRINT13_FINAL_RESEARCH_FREEZE_GATE PASS"
