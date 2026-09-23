#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build/s6-foundation-ci"
MAIN="$BUILD/main"
TEST="$BUILD/test"
rm -rf "$BUILD"
mkdir -p "$MAIN" "$TEST"

find "$ROOT/core/src/main/java" -name '*.java' -print | sort > "$BUILD/main-sources.txt"
find "$ROOT/core/src/test/java" -name '*.java' -print | sort > "$BUILD/test-sources.txt"

javac --release 21 -Xlint:all -Werror -d "$MAIN" @"$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$MAIN" -d "$TEST" @"$BUILD/test-sources.txt"

CP="$MAIN:$TEST"
suites=(
  io.acra.core.tests.TestSuite
  io.acra.core.tests.sprint3.Sprint3CoreTestSuite
  io.acra.core.tests.sprint4.Sprint4CoreVerificationTestSuite
  io.acra.core.tests.sprint5.Sprint5FinalClosureTestSuite
  io.acra.core.tests.sprint6.Sprint6PolicyFoundationTestSuite
  io.acra.core.tests.sprint6.Sprint6PolicyResolutionTestSuite
  io.acra.core.tests.sprint6.Sprint6OrchestrationTestSuite
  io.acra.core.tests.sprint6.Sprint6GraphAndGroupingTestSuite
  io.acra.core.tests.sprint6.Sprint6LabAndPlanningTestSuite
  io.acra.core.tests.sprint6.Sprint6LiveLabExperimentTestSuite
)

python3 -m py_compile "$ROOT/lab/common/basic_api.py" "$ROOT/lab/secure-api/basic-api/server.py" "$ROOT/lab/vulnerable-api/basic-api/server.py"
python3 - <<'PY'
import json
from pathlib import Path
data=json.loads(Path("lab/ground-truth/GT-S6-TENANT-RBAC.json").read_text(encoding="utf-8"))
assert data["id"] == "GT-S6-TENANT-RBAC"
assert len(data["cases"]) >= 10
assert any(c["label"] == "GLOBAL_ADMIN_FP_CONTROL" for c in data["cases"])
assert any(c["label"] == "DELEGATION_FP_CONTROL" for c in data["cases"])
print("SPRINT6_LAB_CONTRACT PASS cases="+str(len(data["cases"])))
PY

LAB_LOG="$BUILD/lab"
mkdir -p "$LAB_LOG"
ACRA_LAB_MODE=secure PORT=18082 python3 "$ROOT/lab/common/basic_api.py" >"$LAB_LOG/secure.log" 2>&1 &
SECURE_PID=$!
ACRA_LAB_MODE=vulnerable PORT=18081 python3 "$ROOT/lab/common/basic_api.py" >"$LAB_LOG/vulnerable.log" 2>&1 &
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
print("SPRINT6_LIVE_LAB_READY PASS")
PY

export ACRA_S6_EXPERIMENT_OUTPUT="$BUILD/EXP-S6-TENANT-RBAC-001.json"

for suite in "${suites[@]}"; do
  java -ea -cp "$CP" "$suite"
done

cleanup_lab
trap - EXIT
echo "SPRINT6_FOUNDATION_VERIFICATION PASS"
