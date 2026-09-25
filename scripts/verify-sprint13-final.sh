#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "ACRA SPRINT 13 FINAL VERIFICATION"
echo "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
java -version
git rev-parse HEAD
git status --short

bash ./scripts/verify-sprint13-foundation.sh
bash ./scripts/verify-sprint12-foundation.sh
bash ./scripts/verify-sprint11-foundation.sh
bash ./scripts/verify-sprint10-foundation.sh
bash ./scripts/verify-sprint9-foundation.sh
bash ./scripts/verify-sprint8-foundation.sh
bash ./scripts/verify-sprint7-foundation.sh
bash ./scripts/verify-sprint6-foundation.sh

mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

bash ./scripts/build-sprint2.sh
bash ./scripts/test-sprint2.sh

bash ./scripts/build-sprint3.sh
bash ./scripts/test-sprint3.sh

mvn --batch-mode --no-transfer-progress -pl extension/burp-extension -am test-compile
MONTOYA_JAR="$HOME/.m2/repository/net/portswigger/burp/extensions/montoya-api/2026.7/montoya-api-2026.7.jar"
if [ ! -f "$MONTOYA_JAR" ]; then
  echo "Montoya 2026.7 dependency missing: $MONTOYA_JAR" >&2
  exit 1
fi
CP="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/classes:$MONTOYA_JAR"
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint4.Sprint4UiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint6.Sprint6AuthorizationUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint7.Sprint7WorkflowUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint8.Sprint8RoutingUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint9.Sprint9PropertyUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint10.Sprint10BatchIndirectUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint12.Sprint12ReproductionUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint13.Sprint13FindingGovernanceUiTestSuite

bash ./scripts/package-sprint13.sh

echo "SPRINT13_FINAL_VERIFICATION PASS"
