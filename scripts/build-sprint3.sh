#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/build-core.sh
rm -rf build/s3
mkdir -p build/s3/stubs build/s3/extension build/s3/test build/dist
find tests/montoya-stubs/src/main/java -name '*.java' | sort > build/s3/stub-sources.txt
javac --release 21 -Xlint:all -Werror -d build/s3/stubs @build/s3/stub-sources.txt
find extension/burp-extension/src/main/java -name '*.java' | sort > build/s3/extension-sources.txt
javac --release 21 -Xlint:all -Werror -cp build/classes:build/s3/stubs -d build/s3/extension @build/s3/extension-sources.txt
find extension/burp-extension/src/test/java -name '*.java' | sort > build/s3/test-sources.txt
javac --release 21 -Xlint:all -Werror -cp build/classes:build/s3/stubs:build/s3/extension -d build/s3/test @build/s3/test-sources.txt
jar --create --file build/dist/acra-burp-extension-0.3.0-rc1-unverified.jar -C build/classes . -C build/s3/extension .
python3 - <<'PY'
import xml.etree.ElementTree as ET
for p in ['pom.xml','core/pom.xml','extension/burp-extension/pom.xml']:
    ET.parse(p)
print('POM XML PASS')
PY
if command -v mvn >/dev/null 2>&1; then
  mvn -q -Dmaven.test.skip=true package
  echo 'MAVEN PACKAGE PASS'
else
  echo 'MAVEN PACKAGE UNVERIFIED: mvn not installed in execution environment'
fi
echo 'SPRINT 3 CONTRACT BUILD PASS'
