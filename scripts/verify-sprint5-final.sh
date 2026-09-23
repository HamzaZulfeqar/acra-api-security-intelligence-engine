#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build/s5-final-ci"
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
  io.acra.core.tests.sprint4.Sprint4EngineSecurityTestSuite
  io.acra.core.tests.sprint4.Sprint4GraphIntegrationTestSuite
  io.acra.core.tests.sprint4.Sprint4ProductCompletionTestSuite
  io.acra.core.tests.sprint5.Sprint5AssessmentGuardTestSuite
  io.acra.core.tests.sprint5.Sprint5CorrelationSafetyTestSuite
  io.acra.core.tests.sprint5.Sprint5EvidenceIntegrityTestSuite
  io.acra.core.tests.sprint5.Sprint5PolicyValidationTestSuite
  io.acra.core.tests.sprint5.Sprint5SerializationSecurityTestSuite
  io.acra.core.tests.sprint5.Sprint5FinalClosureTestSuite
)

for suite in "${suites[@]}"; do
  java -ea -cp "$CP" "$suite"
done

echo "SPRINT5_FINAL_VERIFICATION PASS"
