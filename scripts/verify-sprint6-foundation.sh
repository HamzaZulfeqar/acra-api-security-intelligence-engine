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

for suite in "${suites[@]}"; do
  java -ea -cp "$CP" "$suite"
done

echo "SPRINT6_FOUNDATION_VERIFICATION PASS"
