#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "ACRA SPRINT 12 RESEARCH FINAL VERIFICATION"
echo "utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
java -version
git rev-parse HEAD
git status --short

mvn --batch-mode --no-transfer-progress clean package

CP_CORE="core/target/test-classes:core/target/classes"
CP_STANDALONE="standalone/target/test-classes:standalone/target/classes:core/target/classes"
CP_EXTENSION="extension/burp-extension/target/test-classes:extension/burp-extension/target/classes:core/target/classes"

# Retain the current Sprint 10 standalone product boundary.
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

# Sprint 11 finding lifecycle, reproduction and product surface.
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingLifecycleFoundationTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingReviewWorkspaceTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingReproductionPackageTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingReproductionJsonExportTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingReproductionSarifExportTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingBurpIssueDraftTestSuite
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingSecurityHardeningTestSuite

mkdir -p build/s11-final
java -ea -cp "$CP_CORE" io.acra.core.tests.sprint11.Sprint11FindingPerformanceObservationTestSuite \
  | tee build/s11-final/performance-observation.txt

java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneFindingLifecycleTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneFindingLifecycleApiTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneFindingReproductionTestSuite
java -ea -cp "$CP_STANDALONE" io.acra.standalone.tests.StandaloneFindingProductSurfaceTestSuite

# Independent controlled research-oracle readiness only; A0-A7 remain NOT_RUN.
python3 -m json.tool lab/ground-truth/GT-S11-AUTHORIZATION-RESEARCH.json >/dev/null
python3 scripts/verify-sprint11-ground-truth.py
bash ./scripts/verify-sprint12-research.sh
node --check standalone/src/main/resources/web/app.js

# Retained early local-contract regressions.
bash ./scripts/build-sprint2.sh
bash ./scripts/test-sprint2.sh
bash ./scripts/build-sprint3.sh
bash ./scripts/test-sprint3.sh

# Build a Sprint 11-labelled current binary distribution.
bash ./scripts/build-sprint12-distribution.sh

EXTRACT="$(mktemp -d)"
PID=""
cleanup() {
  if [[ -n "$PID" ]]; then kill "$PID" 2>/dev/null || true; fi
  rm -rf "$EXTRACT"
}
trap cleanup EXIT

unzip -q dist/acra-sprint12-bundle.zip -d "$EXTRACT"
(
  cd "$EXTRACT/acra-sprint12"
  sha256sum -c SHA256SUMS.txt
  bash -n acra-standalone.sh
)

HOME_DIR="$EXTRACT/home"
java -jar "$EXTRACT/acra-sprint12/acra-standalone.jar" \
  --port=18822 \
  --home="$HOME_DIR" \
  --no-browser > "$EXTRACT/standalone.log" 2>&1 &
PID=$!
for i in $(seq 1 60); do
  if curl --fail --silent http://127.0.0.1:18822/api/health > "$EXTRACT/health.json"; then
    break
  fi
  sleep 0.2
done
grep -q '"status":"UP"' "$EXTRACT/health.json"
grep -q '"burpRequired":false' "$EXTRACT/health.json"
curl --fail --silent http://127.0.0.1:18822/ | grep -q 'Standalone Security Workbench'
kill "$PID"
wait "$PID" 2>/dev/null || true
PID=""

bash ./scripts/package-sprint12-research.sh

echo "SPRINT12_RESEARCH_FINAL_VERIFICATION PASS"
