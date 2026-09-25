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
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12SarifExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11ResearchReportExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10BatchIndirectReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint7.Sprint7WorkflowReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint6.Sprint6ReportingExportTestSuite

python3 - <<'PY'
import json
from pathlib import Path

path=Path("build/s12-foundation/reporting/ACRA-REPRODUCTION.sarif")
doc=json.loads(path.read_text(encoding="utf-8"))

assert doc["version"] == "2.1.0"
assert doc["$schema"] == "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json"
assert isinstance(doc["runs"], list) and len(doc["runs"]) == 1

run=doc["runs"][0]
driver=run["tool"]["driver"]
assert driver["name"] == "ACRA"
assert driver["semanticVersion"] == "0.3.0-rc1"
assert len(driver["rules"]) == 1
assert driver["rules"][0]["id"] == "ACRA.AUTHORIZATION.BOLA_TENANT"

assert len(run["results"]) == 1
result=run["results"][0]
assert result["ruleId"] == driver["rules"][0]["id"]
assert result["kind"] == "review"
assert result["level"] == "none"
assert isinstance(result["message"]["text"], str) and "Human review is required" in result["message"]["text"]
assert "locations" not in result
assert result["properties"]["acraReviewOnly"] is True
assert result["properties"]["acraIssueEligible"] is True
assert result["properties"]["acraSeverity"] == "CRITICAL"
assert result["properties"]["acraConfidence"] == "HIGH"
assert set(result["fingerprints"]) == {"acraFinding/v1","acraReproduction/v1"}

text=path.read_text(encoding="utf-8")
for forbidden in ("super-secret-token","DoNotExport","Authorization: Bearer"):
    assert forbidden not in text

print("SPRINT12_SARIF_STRUCTURE PASS version=2.1.0 kind=review level=none locations=absent")
PY

mvn --batch-mode --no-transfer-progress -pl extension/burp-extension -am test-compile
MONTOYA_JAR="$HOME/.m2/repository/net/portswigger/burp/extensions/montoya-api/2026.7/montoya-api-2026.7.jar"
test -f "$MONTOYA_JAR"
BURP_CP="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/classes:$MONTOYA_JAR"
java -ea -cp "$BURP_CP" io.acra.burp.tests.sprint12.Sprint12BurpIssueAdapterTestSuite

echo "SPRINT12_REPRODUCTION_EXPORT_FOUNDATION_VERIFICATION PASS"
