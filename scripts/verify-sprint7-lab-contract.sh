#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 -m py_compile lab/common/basic_api.py lab/secure-api/basic-api/server.py lab/vulnerable-api/basic-api/server.py
python3 - <<'PY'
import json
from pathlib import Path
data=json.loads(Path("lab/ground-truth/GT-S7-WORKFLOW-AUTHORIZATION.json").read_text(encoding="utf-8"))
assert data["id"] == "GT-S7-WORKFLOW-AUTHORIZATION"
assert len(data["cases"]) == 10
assert any(c["label"] == "VALID_SUBMIT_CONTROL" for c in data["cases"])
assert any(c["label"] == "UNPROVEN_PRECEDENCE" for c in data["cases"])
print("SPRINT7_LAB_CONTRACT PASS cases="+str(len(data["cases"])))
PY
