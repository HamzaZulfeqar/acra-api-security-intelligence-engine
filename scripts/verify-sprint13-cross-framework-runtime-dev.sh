#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE6A_COMPLETE="84fff082a63c62d847d8b9df4bb17421d7d4bdb2"
NORM_FREEZE="4011b9c05b99b14da66733aea47de43256060990"
LABELS="lab/ground-truth/GT-S13-XRUNTIME-DEV-LABELS.json"
TMP_DIR="$(mktemp -d)"
cp "$LABELS" "$TMP_DIR/labels.json"

PIDS=()
restore_labels(){
  if [[ -f "$TMP_DIR/labels.json" && ! -f "$LABELS" ]]; then cp "$TMP_DIR/labels.json" "$LABELS"; fi
}
cleanup(){
  restore_labels
  for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

echo "=== Phase 6A evidence lock ==="
git diff --exit-code "$PHASE6A_COMPLETE" --   lab/ground-truth/POL-S13-XFRAME-EVAL-001.json   lab/ground-truth/GT-S13-XFRAME-EVAL-FEATURES.json   lab/ground-truth/GT-S13-XFRAME-EVAL-LABELS.json   scripts/run-sprint13-cross-framework-eval.py   scripts/verify-sprint13-cross-framework-eval.py
echo "SPRINT13_XRUNTIME_PHASE6A_EVIDENCE_LOCK PASS"

echo "=== Frozen normalizer/reasoning gate ==="
git diff --exit-code "$NORM_FREEZE" --   scripts/sprint13_cross_framework_normalization.py   scripts/sprint13_uncertainty_governance.py   scripts/sprint13_policy_semantics.py   scripts/sprint13_dimension_inference.py   scripts/run-sprint12-ablation.py
echo "SPRINT13_XRUNTIME_FROZEN_STACK_LOCK PASS"

echo "=== Build Spring Boot runtime ==="
mvn --batch-mode --no-transfer-progress -q -f lab/framework-runtime/spring/pom.xml -DskipTests package

echo "=== Start native framework runtimes ==="
python -m uvicorn server:app --app-dir lab/framework-runtime/fastapi --host 127.0.0.1 --port 18201 >"$TMP_DIR/fastapi.log" 2>&1 &
PIDS+=("$!")

PORT=18202 python lab/framework-runtime/flask/server.py >"$TMP_DIR/flask.log" 2>&1 &
PIDS+=("$!")

PORT=18203 node lab/framework-runtime/express/server.js >"$TMP_DIR/express.log" 2>&1 &
PIDS+=("$!")

java -jar lab/framework-runtime/spring/target/phase6b-spring-runtime-1.0.0.jar --server.address=127.0.0.1 --server.port=18204 >"$TMP_DIR/spring.log" 2>&1 &
PIDS+=("$!")

wait_health(){
  local name="$1"; local url="$2"
  for _ in $(seq 1 60); do
    if curl --fail --silent --show-error "$url" >/dev/null 2>&1; then
      echo "SPRINT13_XRUNTIME_HEALTH PASS $name $url"
      return 0
    fi
    sleep 1
  done
  echo "=== $name runtime log ==="
  cat "$TMP_DIR/$name.log" || true
  return 1
}

wait_health fastapi http://127.0.0.1:18201/health
wait_health flask http://127.0.0.1:18202/health
wait_health express http://127.0.0.1:18203/health
wait_health spring http://127.0.0.1:18204/health

rm "$LABELS"
echo "=== Blind live-runtime prediction #1 ==="
python3 scripts/run-sprint13-cross-framework-runtime-dev.py
cp build/s13-xruntime-dev/predictions.json "$TMP_DIR/p1.json"
cp build/s13-xruntime-dev/predictions.jsonl "$TMP_DIR/p1.jsonl"
cp build/s13-xruntime-dev/predictions.json.sha256 "$TMP_DIR/p1.sha"
cp build/s13-xruntime-dev/predictions.jsonl.sha256 "$TMP_DIR/p1l.sha"

restore_labels
echo "=== Sealed-label runtime scoring #1 ==="
python3 scripts/verify-sprint13-cross-framework-runtime-dev.py
cp build/s13-xruntime-dev/evaluation.json "$TMP_DIR/e1.json"

rm "$LABELS"
echo "=== Blind live-runtime prediction #2 ==="
python3 scripts/run-sprint13-cross-framework-runtime-dev.py
cmp "$TMP_DIR/p1.json" build/s13-xruntime-dev/predictions.json
cmp "$TMP_DIR/p1.jsonl" build/s13-xruntime-dev/predictions.jsonl
cmp "$TMP_DIR/p1.sha" build/s13-xruntime-dev/predictions.json.sha256
cmp "$TMP_DIR/p1l.sha" build/s13-xruntime-dev/predictions.jsonl.sha256

restore_labels
echo "=== Sealed-label runtime scoring #2 ==="
python3 scripts/verify-sprint13-cross-framework-runtime-dev.py
cmp "$TMP_DIR/e1.json" build/s13-xruntime-dev/evaluation.json

echo "SPRINT13_XRUNTIME_DEV_REPEATABILITY PASS"
echo "SPRINT13_XRUNTIME_DEV_LABEL_ABSENCE_GATE PASS"
echo "SPRINT13_XRUNTIME_DEV_RESEARCH_GATE PASS"
