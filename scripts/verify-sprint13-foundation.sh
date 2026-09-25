#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s13-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint13.Sprint13FindingLifecycleFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint13.Sprint13FindingGovernanceWorkspaceTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint13.Sprint13GovernanceReportingExportTestSuite

bash ./scripts/verify-sprint12-foundation.sh

MONTOYA_JAR="$HOME/.m2/repository/net/portswigger/burp/extensions/montoya-api/2026.7/montoya-api-2026.7.jar"
if [ ! -f "$MONTOYA_JAR" ]; then
  echo "Montoya 2026.7 dependency missing: $MONTOYA_JAR" >&2
  exit 1
fi
EXT_CP="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/test-classes:core/target/classes:$MONTOYA_JAR"
java -ea -Djava.awt.headless=true -cp "$EXT_CP" io.acra.burp.tests.sprint13.Sprint13FindingGovernanceUiTestSuite

echo "SPRINT13_FINDING_LIFECYCLE_FOUNDATION_VERIFICATION PASS"
