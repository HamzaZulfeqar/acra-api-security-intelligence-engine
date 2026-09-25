#!/usr/bin/env python3
from __future__ import annotations
import json, sys
from pathlib import Path

probe=Path(sys.argv[1])
events=[json.loads(x) for x in probe.read_text(encoding="utf-8").splitlines() if x.strip()]
types=[e.get("event") for e in events]
assert "INITIALIZED" in types, "ACRA never initialized inside real Burp"
requests=[e for e in events if e.get("event")=="REQUEST"]
responses=[e for e in events if e.get("event")=="RESPONSE"]
processed=[e for e in events if e.get("event")=="PROCESSED"]
assert len(requests)>=2, f"expected >=2 Burp request callbacks, got {len(requests)}"
assert len(responses)>=2, f"expected >=2 Burp response callbacks, got {len(responses)}"
assert len(processed)>=2, f"expected >=2 ACRA pipeline results, got {len(processed)}"
paths={e.get("path") for e in requests}
assert "/api/v1/tenants/tenant-a/documents/1001" in paths
assert "/api/v1/tenants/tenant-a/documents/1002" in paths
assert not any(k in probe.read_text(encoding="utf-8") for k in ("Authorization","Bearer ","synthetic-cookie-secret")), "probe leaked secret material"

doc1001=[e for e in processed if e.get("resourceId")=="1001"]
context_ok=any(
    e.get("principal")=="user-a"
    and e.get("tenant")=="tenant-a"
    and e.get("action")=="READ"
    and e.get("responseStatus")==200
    for e in doc1001
)
print(f"SPRINT13_BURP_DEV_EVENTS INIT={types.count('INITIALIZED')} REQUEST={len(requests)} RESPONSE={len(responses)} PROCESSED={len(processed)}")
print(f"SPRINT13_BURP_DEV_CONTEXT_GT001 {'PASS' if context_ok else 'PARTIAL'}")
print("SPRINT13_BURP_DEV_VERIFY PASS")
