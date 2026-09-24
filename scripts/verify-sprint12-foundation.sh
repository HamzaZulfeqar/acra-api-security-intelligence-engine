#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s12-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12ReproductionPackageFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12ReproductionJsonExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12ReproductionSarifExportTestSuite
python3 - <<'PY'
import json
from pathlib import Path

path=Path("build/s12-foundation/reproduction-sample.sarif")
doc=json.loads(path.read_text(encoding="utf-8"))
assert doc["version"] == "2.1.0"
assert doc["$schema"].endswith("/sarif-schema-2.1.0.json")
assert isinstance(doc["runs"], list) and len(doc["runs"]) == 1
run=doc["runs"][0]
assert run["tool"]["driver"]["name"] == "ACRA"
assert isinstance(run["tool"]["driver"]["rules"], list) and len(run["tool"]["driver"]["rules"]) == 1
assert isinstance(run["results"], list) and len(run["results"]) == 1
result=run["results"][0]
assert result["ruleId"] == "ACRA-AUTHORIZATION-REVIEW"
assert result["kind"] == "review"
assert result["level"] == "error"
assert result["properties"]["acraReviewOnly"] is True
assert result["properties"]["acraCandidateState"] == "CANDIDATE"
assert result["properties"]["acraExpectedDecision"] == "DENY"
assert result["properties"]["acraObservedDecision"] == "ALLOW"
print("SPRINT12_SARIF_JSON_SHAPE PASS version=2.1.0 runs=1 results=1 kind=review")
PY

bash ./scripts/verify-sprint11-foundation.sh

echo "SPRINT12_REPRODUCTION_EXPORT_FOUNDATION_VERIFICATION PASS"
