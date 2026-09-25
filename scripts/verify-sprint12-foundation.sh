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
java -ea -cp "$CP" io.acra.core.tests.sprint12.Sprint12ReproductionExportFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11ResearchReportExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10BatchIndirectReportingExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyReportingExportTestSuite

echo "SPRINT12_REPRODUCTION_EXPORT_FOUNDATION_VERIFICATION PASS"
