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
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12ReproductionStandardsFoundationTestSuite

if grep -R "burp.api.montoya" core/src/main/java/io/acra/core/reporting/s12 >/dev/null 2>&1; then
  echo "Sprint 12 core must remain Montoya-independent" >&2
  exit 1
fi

if grep -E "siteMap\\(|addToSiteMap|void publish\\(" extension/burp-extension/src/main/java/io/acra/burp/scanner/S12MontoyaAuditIssueAdapter.java >/dev/null 2>&1; then
  echo "Sprint 12 Phase 2 must not publish Burp issues" >&2
  exit 1
fi

mvn --batch-mode --no-transfer-progress -pl extension/burp-extension -am test-compile
MONTOYA_JAR="$HOME/.m2/repository/net/portswigger/burp/extensions/montoya-api/2026.7/montoya-api-2026.7.jar"
if [ ! -f "$MONTOYA_JAR" ]; then
  echo "Montoya 2026.7 dependency missing: $MONTOYA_JAR" >&2
  exit 1
fi
EXT_CP="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/test-classes:core/target/classes:$MONTOYA_JAR"
java -ea -cp "$EXT_CP" io.acra.burp.tests.sprint12.Sprint12MontoyaIssueAdapterTestSuite
java -ea -cp "$EXT_CP" io.acra.burp.tests.sprint12.Sprint12BurpIssuePublicationBoundaryTestSuite

if grep -E "S12BurpIssuePublisher|S12MontoyaSiteMapAuditIssueSink|siteMap\\(" extension/burp-extension/src/main/java/io/acra/burp/ACRAExtension.java >/dev/null 2>&1; then
  echo "Sprint 12 publisher must remain unregistered from ACRAExtension bootstrap" >&2
  exit 1
fi

bash ./scripts/verify-sprint11-foundation.sh

echo "SPRINT12_REPRODUCTION_STANDARDS_FOUNDATION_VERIFICATION PASS"
