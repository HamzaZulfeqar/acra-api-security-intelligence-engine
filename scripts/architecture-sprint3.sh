#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/architecture-core.sh
if grep -RniE '\b(burp\.api\.montoya|com\.portswigger|MontoyaApi)\b' core/src/main/java; then
  echo 'ARCHITECTURE FAIL: Montoya dependency found in core'; exit 1
fi
for cls in OpenApiImporter RouteTemplateEngine RouteEquivalenceEngine ApiReconnaissanceEngine ReconTestPlanner ResponseSemanticAnalyzer; do
  if ! grep -Rql "class $cls\|record $cls" core/src/main/java; then echo "ARCHITECTURE FAIL: missing $cls"; exit 1; fi
done
if ! grep -Rql 'NO_NETWORK_REQUESTS' core/src/main/java/io/acra/core/planning; then echo 'ARCHITECTURE FAIL: dry-run network boundary missing'; exit 1; fi
if grep -Rni 'Finding' extension/burp-extension/src/main/java/io/acra/burp/traffic | grep -v 'Findings: not assessed'; then
  echo 'ARCHITECTURE FAIL: finding logic in reconnaissance traffic pipeline'; exit 1
fi
echo 'SPRINT 3 ARCHITECTURE PASS'
