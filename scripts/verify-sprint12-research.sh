#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "ACRA SPRINT 12 CONTROLLED RESEARCH VERIFICATION"
echo "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "head=$(git rev-parse HEAD)"

rm -rf build/s12-research
python3 ./scripts/verify-sprint11-ground-truth.py
python3 ./scripts/run-sprint12-ablation.py
python3 ./scripts/verify-sprint12-research.py

FIRST_JSON="$(sha256sum build/s12-research/EXP-A0-A7.json | awk '{print $1}')"
FIRST_CSV="$(sha256sum build/s12-research/EXP-A0-A7.csv | awk '{print $1}')"
FIRST_CASES="$(sha256sum build/s12-research/EXP-A0-A7-cases.jsonl | awk '{print $1}')"

python3 ./scripts/run-sprint12-ablation.py >/tmp/s12-repeat.log
python3 ./scripts/verify-sprint12-research.py

SECOND_JSON="$(sha256sum build/s12-research/EXP-A0-A7.json | awk '{print $1}')"
SECOND_CSV="$(sha256sum build/s12-research/EXP-A0-A7.csv | awk '{print $1}')"
SECOND_CASES="$(sha256sum build/s12-research/EXP-A0-A7-cases.jsonl | awk '{print $1}')"

test "$FIRST_JSON" = "$SECOND_JSON"
test "$FIRST_CSV" = "$SECOND_CSV"
test "$FIRST_CASES" = "$SECOND_CASES"

if grep -R -E 'Bearer |synthetic-cookie-secret|"Authorization"[[:space:]]*:' build/s12-research; then
  echo "Secret-like execution material found in Sprint 12 research artifacts" >&2
  exit 1
fi

cat /tmp/s12-repeat.log
echo "SPRINT12_REPEATABILITY PASS json=$SECOND_JSON csv=$SECOND_CSV cases=$SECOND_CASES"
echo "SPRINT12_RESEARCH_VERIFICATION PASS"
