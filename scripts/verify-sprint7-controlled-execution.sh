#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BUILD="$ROOT/build/s7-foundation"
mkdir -p "$BUILD/lab"

test -d "$BUILD/main" || { echo "Sprint 7 compiled main classes missing"; exit 1; }
test -d "$BUILD/test" || { echo "Sprint 7 compiled test classes missing"; exit 1; }

ACRA_LAB_MODE=secure PORT=18082 python3 "$ROOT/lab/common/basic_api.py" >"$BUILD/lab/secure.log" 2>&1 &
SECURE_PID=$!
ACRA_LAB_MODE=vulnerable PORT=18081 python3 "$ROOT/lab/common/basic_api.py" >"$BUILD/lab/vulnerable.log" 2>&1 &
VULNERABLE_PID=$!
cleanup_lab() {
  kill "$SECURE_PID" "$VULNERABLE_PID" 2>/dev/null || true
  wait "$SECURE_PID" "$VULNERABLE_PID" 2>/dev/null || true
}
trap cleanup_lab EXIT

python3 - <<'PY'
import time, urllib.request
for url in ("http://127.0.0.1:18082/health","http://127.0.0.1:18081/health"):
    last=None
    for _ in range(50):
        try:
            with urllib.request.urlopen(url, timeout=0.5) as r:
                if r.status == 200:
                    break
        except Exception as exc:
            last=exc
            time.sleep(0.1)
    else:
        raise SystemExit(f"lab not ready: {url}: {last}")
print("SPRINT7_LIVE_LAB_READY PASS")
PY

java -ea -cp "$BUILD/main:$BUILD/test" io.acra.core.tests.sprint7.Sprint7WorkflowPlannerExecutionIntegrationTestSuite

cleanup_lab
trap - EXIT
echo "SPRINT7_CONTROLLED_EXECUTION PASS"
