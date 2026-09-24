#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s9-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint5.Sprint5FinalClosureTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint6.Sprint6PolicyFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint7.Sprint7WorkflowAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingNormalizationFoundationTestSuite

echo "SPRINT9_PROPERTY_AUTHORIZATION_FOUNDATION_VERIFICATION PASS"
