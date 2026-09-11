#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/test-sprint2.sh >/tmp/acra-s2-tests.log
if grep -RniE 'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.' core/src/main/java extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: JWT-like literal in production source'; exit 1
fi
if grep -RniE '(client_secret|private[_-]?key|password)[[:space:]]*=[[:space:]]*[^<"[:space:]]+' core/src/main/java extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: possible secret literal in production source'; exit 1
fi
if grep -Rni 'registerActiveScanCheck\|sendRequest(' extension/burp-extension/src/main/java; then
  echo 'SECURITY FAIL: active Montoya execution/scan registration in Sprint 2 production source'; exit 1
fi
echo 'SPRINT 2 SECURITY PASS'
