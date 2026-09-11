#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes
find core/src/main/java -name '*.java' | sort > build/main-sources.txt
javac --release 21 -Xlint:all -Werror -d build/classes @build/main-sources.txt
find core/src/test/java -name '*.java' | sort > build/test-sources.txt
javac --release 21 -Xlint:all -Werror -cp build/classes -d build/test-classes @build/test-sources.txt
printf 'BUILD PASS\n'
