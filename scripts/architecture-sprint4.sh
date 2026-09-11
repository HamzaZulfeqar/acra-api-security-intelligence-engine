#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
bash ./scripts/architecture-core.sh
NETWORK_MATCHES="$(grep -RliE 'java\.net\.http\.HttpClient|java\.net\.Socket|java\.nio\.channels\.SocketChannel' core/src/main/java/io/acra/core/active || true)"
EXPECTED='core/src/main/java/io/acra/core/active/execution/LocalhostHttpTransport.java'
if [[ "$NETWORK_MATCHES" != "$EXPECTED" ]]; then
  echo 'ARCHITECTURE FAIL: Sprint 4 network client is not isolated to LocalhostHttpTransport'
  printf '%s\n' "$NETWORK_MATCHES"
  exit 1
fi
if ! grep -q 'ExecutionEnvironment.LAB' "$EXPECTED"; then
  echo 'ARCHITECTURE FAIL: localhost transport lacks explicit LAB binding'; exit 1
fi
if ! grep -q 'LOOPBACK_HOSTS\|loopback host' "$EXPECTED"; then
  echo 'ARCHITECTURE FAIL: localhost transport lacks loopback enforcement'; exit 1
fi
if grep -RniE 'BolaAnalyzer|BflaAnalyzer|vulnerability confirmed|confirmed vulnerability' core/src/main/java/io/acra/core/active; then
  echo 'ARCHITECTURE FAIL: Sprint 5 finding/analyzer logic entered Sprint 4 active engine'; exit 1
fi
echo 'SPRINT 4 ARCHITECTURE PASS'
