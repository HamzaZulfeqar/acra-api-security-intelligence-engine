#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s11-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingLifecycleFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingReviewWorkspaceTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingReproductionPackageTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingReproductionJsonExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingReproductionSarifExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingBurpIssueDraftTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingSecurityHardeningTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11FindingPerformanceObservationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10BatchIndirectReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10BatchIndirectSecurityHardeningTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint5.Sprint5FinalClosureTestSuite

python3 -m json.tool "$BUILD/reporting/S11-FINDING-REPRODUCTION.sarif" >/dev/null
python3 -m json.tool lab/ground-truth/GT-S11-AUTHORIZATION-RESEARCH.json >/dev/null
python3 scripts/verify-sprint11-ground-truth.py
python3 -m json.tool build/s11-ground-truth/verification.json >/dev/null

if grep -Eq 'siteMap\(\)\.add|siteMap\.add|\.siteMap\(\).*add'     extension/burp-extension/src/main/java/io/acra/burp/reporting/S11BurpIssueAdapter.java; then
  echo "ERROR: Sprint 11 Burp adapter must not publish directly to SiteMap" >&2
  exit 1
fi

echo "SPRINT11_FINDING_LIFECYCLE_FOUNDATION_VERIFICATION PASS"
