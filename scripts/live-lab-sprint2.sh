#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
LOG="${TMPDIR:-/tmp}/acra-lab-secure.log"
ACRA_LAB_MODE=secure PORT=18082 python3 lab/secure-api/basic-api/server.py >"$LOG" 2>&1 &
PID=$!
cleanup(){ kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; }
trap cleanup EXIT
python3 - <<'PY'
import time, urllib.request
for _ in range(30):
    try:
        urllib.request.urlopen('http://127.0.0.1:18082/health', timeout=.2).read(); break
    except Exception: time.sleep(.1)
else: raise SystemExit('lab failed to start')
PY
java -ea -cp build/classes:build/s2/stubs:build/s2/extension:build/s2/test io.acra.burp.tests.LiveLabExperiment
