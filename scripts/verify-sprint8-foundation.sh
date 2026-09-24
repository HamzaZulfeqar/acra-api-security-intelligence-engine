#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BUILD="$ROOT/build/s8-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test" "$BUILD/lab"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

python3 -m py_compile lab/common/basic_api.py lab/secure-api/basic-api/server.py lab/vulnerable-api/basic-api/server.py
python3 - <<'PY'
import json
from pathlib import Path
data=json.loads(Path("lab/ground-truth/GT-S8-ROUTING-NORMALIZATION.json").read_text(encoding="utf-8"))
assert data["id"] == "GT-S8-ROUTING-NORMALIZATION"
assert len(data["cases"]) == 3
assert any(c["label"] == "EQUIVALENT_ROUTE_VIEWER_DENY" for c in data["cases"])
print("SPRINT8_ROUTING_LAB_CONTRACT PASS cases="+str(len(data["cases"])))
PY

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
print("SPRINT8_ROUTING_LAB_READY PASS")
PY

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingNormalizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8AuthorizationPathDifferentialTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8ControlledRoutingDifferentialTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingSecurityHardeningTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingPerformanceObservationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint3.Sprint3CoreTestSuite

cleanup_lab
trap - EXIT
echo "SPRINT8_ROUTING_FOUNDATION_VERIFICATION PASS"
