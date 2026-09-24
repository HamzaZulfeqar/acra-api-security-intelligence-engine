#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s10-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

python3 -m py_compile lab/common/basic_api.py lab/secure-api/basic-api/server.py lab/vulnerable-api/basic-api/server.py
python3 - <<'PY'
import json
from pathlib import Path
data=json.loads(Path("lab/ground-truth/GT-S10-AUTH-SESSION-CONTEXT.json").read_text(encoding="utf-8"))
assert data["id"] == "GT-S10-AUTH-SESSION-CONTEXT"
assert len(data["cases"]) == 4
assert any(c["label"] == "STABLE_CONTEXT_TOKEN_ROTATION" for c in data["cases"])
assert any(c["label"] == "TOKEN_ROTATION_CONTEXT_DRIFT" for c in data["cases"])
print("SPRINT10_SESSION_LAB_CONTRACT PASS cases="+str(len(data["cases"])))
PY

ACRA_LAB_MODE=secure PORT=18082 python3 "$ROOT/lab/common/basic_api.py" >"$BUILD/secure-lab.log" 2>&1 &
LAB_PID=$!
cleanup_lab() {
  kill "$LAB_PID" 2>/dev/null || true
  wait "$LAB_PID" 2>/dev/null || true
}
trap cleanup_lab EXIT

python3 - <<'PY'
import time, urllib.request
url="http://127.0.0.1:18082/health"
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
print("SPRINT10_SESSION_LAB_READY PASS")
PY

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10SessionContextFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10SessionEvidenceBindingTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10ControlledSessionContextTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10PassiveSessionHydrationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingNormalizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint7.Sprint7WorkflowAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint6.Sprint6PolicyFoundationTestSuite

cleanup_lab
trap - EXIT
echo "SPRINT10_AUTH_SESSION_FOUNDATION_VERIFICATION PASS"
