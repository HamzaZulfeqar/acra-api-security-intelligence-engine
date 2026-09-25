#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "ACRA SPRINT 10 FINAL VERIFICATION"
echo "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
java -version
git rev-parse HEAD
git status --short

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
CP="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/classes"
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint4.Sprint4UiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint6.Sprint6AuthorizationUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint7.Sprint7WorkflowUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint8.Sprint8RoutingUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint9.Sprint9PropertyUiTestSuite
java -ea -Djava.awt.headless=true -cp "$CP" io.acra.burp.tests.sprint10.Sprint10BatchIndirectUiTestSuite

bash ./scripts/package-sprint10.sh

echo "SPRINT10_FINAL_VERIFICATION PASS"
