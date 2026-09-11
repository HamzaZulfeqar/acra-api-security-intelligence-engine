#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
SECURE_LOG="build/s4-secure-lab.log"
VULN_LOG="build/s4-vulnerable-lab.log"
mkdir -p build
ACRA_LAB_MODE=secure PORT=18082 python3 lab/secure-api/basic-api/server.py >"$SECURE_LOG" 2>&1 &
SECURE_PID=$!
ACRA_LAB_MODE=vulnerable PORT=18081 python3 lab/vulnerable-api/basic-api/server.py >"$VULN_LOG" 2>&1 &
VULN_PID=$!
cleanup() {
  kill "$SECURE_PID" "$VULN_PID" 2>/dev/null || true
  wait "$SECURE_PID" "$VULN_PID" 2>/dev/null || true
}
trap cleanup EXIT
python3 - <<'PY'
import time, urllib.request
for url in ('http://127.0.0.1:18082/health','http://127.0.0.1:18081/health'):
    last=None
    for _ in range(50):
        try:
            with urllib.request.urlopen(url, timeout=.2) as r:
                if r.status == 200:
                    break
        except Exception as e:
            last=e
            time.sleep(.05)
    else:
        raise SystemExit(f'lab did not start: {url}: {last}')
PY
bash scripts/build-core.sh
java -ea -cp build/classes:build/test-classes io.acra.core.tests.sprint4.Sprint4LocalhostIntegrationTestSuite
