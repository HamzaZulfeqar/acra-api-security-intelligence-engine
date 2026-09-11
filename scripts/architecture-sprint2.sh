#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
if grep -RniE '\b(burp\.api\.montoya|com\.portswigger|MontoyaApi)\b' core/src/main/java; then
  echo 'ARCHITECTURE FAIL: Montoya dependency found in core'; exit 1
fi
if ! grep -q '<version>2026.7</version>' extension/burp-extension/pom.xml; then
  # version may be inherited as property from parent
  grep -q '<montoya.version>2026.7</montoya.version>' pom.xml || { echo 'ARCHITECTURE FAIL: Montoya 2026.7 not pinned'; exit 1; }
fi
if ! grep -q '<artifactId>montoya-api</artifactId>' extension/burp-extension/pom.xml; then echo 'ARCHITECTURE FAIL: Montoya dependency absent'; exit 1; fi
if grep -Rni 'Finding\|BolaAnalyzer\|BflaAnalyzer' extension/burp-extension/src/main/java/io/acra/burp/traffic; then
  echo 'ARCHITECTURE FAIL: vulnerability-finding logic in passive traffic pipeline'; exit 1
fi
echo 'SPRINT 2 ARCHITECTURE PASS'
