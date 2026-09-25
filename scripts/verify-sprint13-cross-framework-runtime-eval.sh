#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE6B_DEV_FREEZE="683933836d2c2fa5ec155bc8e224be87e6958e95"
NORM_FREEZE="4011b9c05b99b14da66733aea47de43256060990"
LABELS="lab/ground-truth/GT-S13-XRUNTIME-EVAL-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"
PIDS=()
restore(){ [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]] && cp "$TMP_DIR/labels.json" "$LABELS" || true; }
cleanup(){ restore; for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done; rm -rf "$TMP_DIR"; }
trap cleanup EXIT

echo "=== Phase 6B development evidence lock ==="
git diff --exit-code "$PHASE6B_DEV_FREEZE" --   lab/framework-runtime   lab/ground-truth/POL-S13-XRUNTIME-DEV-001.json   lab/ground-truth/GT-S13-XRUNTIME-DEV-FEATURES.json   lab/ground-truth/GT-S13-XRUNTIME-DEV-LABELS.json   scripts/run-sprint13-cross-framework-runtime-dev.py   scripts/verify-sprint13-cross-framework-runtime-dev.py
echo "SPRINT13_XRUNTIME_EVAL_DEV_FREEZE PASS"

echo "=== Frozen normalizer/reasoning gate ==="
git diff --exit-code "$NORM_FREEZE" --   scripts/sprint13_cross_framework_normalization.py   scripts/sprint13_uncertainty_governance.py   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_XRUNTIME_EVAL_FROZEN_STACK PASS"

mvn --batch-mode --no-transfer-progress -q -f lab/framework-runtime-eval/spring/pom.xml -DskipTests package

python -m uvicorn server:app --app-dir lab/framework-runtime-eval/fastapi --host 127.0.0.1 --port 18301 >"$TMP_DIR/fastapi.log" 2>&1 & PIDS+=("$!")
PORT=18302 python lab/framework-runtime-eval/flask/server.py >"$TMP_DIR/flask.log" 2>&1 & PIDS+=("$!")
PORT=18303 node lab/framework-runtime-eval/express/server.js >"$TMP_DIR/express.log" 2>&1 & PIDS+=("$!")
java -jar lab/framework-runtime-eval/spring/target/phase6b-spring-eval-1.0.0.jar --server.address=127.0.0.1 --server.port=18304 >"$TMP_DIR/spring.log" 2>&1 & PIDS+=("$!")

wait_health(){
 local name="$1"; local url="$2"
 for _ in $(seq 1 60); do
  if curl --fail --silent "$url" >/dev/null 2>&1; then echo "SPRINT13_XRUNTIME_EVAL_HEALTH PASS $name"; return 0; fi
  sleep 1
 done
 cat "$TMP_DIR/$name.log" || true
 return 1
}
wait_health fastapi http://127.0.0.1:18301/health
wait_health flask http://127.0.0.1:18302/health
wait_health express http://127.0.0.1:18303/health
wait_health spring http://127.0.0.1:18304/health

rm "$LABELS"
python3 scripts/run-sprint13-cross-framework-runtime-eval.py
cp build/s13-xruntime-eval/predictions.json "$TMP_DIR/p1.json"
cp build/s13-xruntime-eval/predictions.jsonl "$TMP_DIR/p1.jsonl"
restore
python3 scripts/verify-sprint13-cross-framework-runtime-eval.py
cp build/s13-xruntime-eval/evaluation.json "$TMP_DIR/e1.json"
cp build/s13-xruntime-eval/evaluation-cases.jsonl "$TMP_DIR/e1.jsonl"

rm "$LABELS"
python3 scripts/run-sprint13-cross-framework-runtime-eval.py
cmp "$TMP_DIR/p1.json" build/s13-xruntime-eval/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-xruntime-eval/predictions.jsonl
restore
python3 scripts/verify-sprint13-cross-framework-runtime-eval.py
cmp "$TMP_DIR/e1.json" build/s13-xruntime-eval/evaluation.json
cmp "$TMP_DIR/e1.jsonl" build/s13-xruntime-eval/evaluation-cases.jsonl

echo "SPRINT13_XRUNTIME_EVAL_REPEATABILITY PASS"
echo "SPRINT13_XRUNTIME_EVAL_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_XRUNTIME_EVAL_RESEARCH_GATE PASS"
