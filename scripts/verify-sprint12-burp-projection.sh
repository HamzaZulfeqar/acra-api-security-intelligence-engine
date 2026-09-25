#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

mvn --batch-mode --no-transfer-progress -DskipTests -pl extension/burp-extension -am install

mvn --batch-mode --no-transfer-progress   -pl extension/burp-extension   org.codehaus.mojo:exec-maven-plugin:3.5.0:java   -Dexec.classpathScope=test   -Dexec.mainClass=io.acra.burp.tests.sprint12.Sprint12BurpIssueAdapterRuntimeTestSuite

echo "SPRINT12_BURP_PROJECTION_VERIFICATION PASS"
