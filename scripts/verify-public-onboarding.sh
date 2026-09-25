#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python3 scripts/verify-public-onboarding.py

mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am

JAR="extension/burp-extension/target/acra-burp-extension-0.3.0.jar"
test -f "$JAR"
jar tf "$JAR" | grep -qx 'io/acra/burp/ACRAExtension.class'

echo "SPRINT16_EXTENSION_JAR PASS path=$JAR"

git diff --check
echo "SPRINT16_DIFF_CHECK PASS"
echo "SPRINT16_PUBLIC_ONBOARDING_GATE PASS"
