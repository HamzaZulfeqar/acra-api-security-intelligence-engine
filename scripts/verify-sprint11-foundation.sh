#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BUILD="$ROOT/build/s11-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11OAuthOidcFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11OAuthEvidenceBindingTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11PassiveOAuthExtractionTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10SessionContextFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingNormalizationFoundationTestSuite

echo "SPRINT11_OAUTH_OIDC_FOUNDATION_VERIFICATION PASS"
