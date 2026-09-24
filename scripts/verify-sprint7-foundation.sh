#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BUILD="$ROOT/build/s7-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

java -ea -cp "$BUILD/main:$BUILD/test" io.acra.core.tests.sprint7.Sprint7WorkflowAuthorizationFoundationTestSuite
java -ea -cp "$BUILD/main:$BUILD/test" io.acra.core.tests.sprint7.Sprint7WorkflowAssessmentIntegrationTestSuite
java -ea -cp "$BUILD/main:$BUILD/test" io.acra.core.tests.sprint7.Sprint7WorkflowCoverageTestSuite

echo "SPRINT7_FOUNDATION_VERIFICATION PASS"
