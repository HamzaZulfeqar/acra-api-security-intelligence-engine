#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/security-core.sh
if grep -RniE 'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.' core/src/main/java extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: JWT-like literal in production source'; exit 1
fi
if grep -RniE '(client_secret|private[_-]?key|password)[[:space:]]*=[[:space:]]*[^<"[:space:]]+' core/src/main/java extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: possible secret literal in production source'; exit 1
fi
if grep -Rni 'sendRequest(\|registerActiveScanCheck\|registerScanCheck' extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: active Montoya execution/scan registration found'; exit 1
fi
if grep -RniE 'BOLA confirmed|BFLA confirmed|vulnerability confirmed' core/src/main/java extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: premature vulnerability verdict literal'; exit 1
fi
echo 'SPRINT 3 SECURITY PASS'
