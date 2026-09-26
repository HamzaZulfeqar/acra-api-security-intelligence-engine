#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "ACRA SPRINT 10 FINAL VERIFICATION"
echo "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
java -version
git rev-parse HEAD
git status --short

# Retain the complete previous release boundary.
bash ./scripts/verify-sprint9-final.sh

# Build the complete current reactor including standalone + official Montoya adapter.
mvn --batch-mode --no-transfer-progress clean package

CP_CORE="core/target/test-classes:core/target/classes"
CP_STANDALONE="standalone/target/test-classes:standalone/target/classes:core/target/classes"
CP_EXTENSION="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/classes"

java -ea -cp "$CP_CORE" io.acra.core.tests.sprint10.Sprint10ImportFoundationTestSuite

java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneFoundationTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneImportInventoryTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneSecurityContextTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneSecurityContextApiTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneCoreProjectionTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneCoreProjectionApiTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneEvidenceDifferentialTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneEvidenceApiTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneReviewReportingTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneReviewReportingApiTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneControlledExecutionTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneControlledExecutionApiTestSuite

java -ea -Djava.awt.headless=true -cp "$CP_EXTENSION" io.acra.burp.tests.sprint10.Sprint10StandaloneBridgeTestSuite
node --check standalone/src/main/resources/web/app.js

bash ./scripts/build-sprint10-distribution.sh

EXTRACT="$(mktemp -d)"
trap 'rm -rf "$EXTRACT"; if [[ -n "${PID:-}" ]]; then kill "$PID" 2>/dev/null || true; fi' EXIT
unzip -q dist/acra-sprint10-bundle.zip -d "$EXTRACT"
(
  cd "$EXTRACT/acra-sprint10"
  sha256sum -c SHA256SUMS.txt
  bash -n acra-standalone.sh
)

HOME_DIR="$EXTRACT/home"
java -jar "$EXTRACT/acra-sprint10/acra-standalone.jar"   --port=18790   --home="$HOME_DIR"   --no-browser > "$EXTRACT/standalone.log" 2>&1 &
PID=$!
for i in $(seq 1 60); do
  if curl --fail --silent http://127.0.0.1:18790/api/health > "$EXTRACT/health.json"; then
    break
  fi
  sleep 0.2
done
grep -q '"status":"UP"' "$EXTRACT/health.json"
grep -q '"burpRequired":false' "$EXTRACT/health.json"
kill "$PID"
wait "$PID" 2>/dev/null || true
PID=""

bash ./scripts/package-sprint10.sh

echo "SPRINT10_FINAL_VERIFICATION PASS"
