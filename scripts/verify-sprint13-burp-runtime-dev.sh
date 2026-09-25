#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PHASE6B_COMPLETE="d35e2a54891357e680253e2cfc766c3e4bb6f88d"
BURP_SHA256="c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069"
WORK="$(mktemp -d)"
PIDS=()
cleanup(){ for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done; rm -rf "$WORK"; }
trap cleanup EXIT

git diff --exit-code "$PHASE6B_COMPLETE" --   lab/framework-runtime-eval   lab/ground-truth/GT-S13-XRUNTIME-EVAL-FEATURES.json   lab/ground-truth/GT-S13-XRUNTIME-EVAL-LABELS.json   lab/ground-truth/POL-S13-XRUNTIME-EVAL-001.json   scripts/run-sprint13-cross-framework-runtime-eval.py   scripts/verify-sprint13-cross-framework-runtime-eval.py
echo "SPRINT13_BURP_PHASE6B_EVIDENCE_LOCK PASS"

mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package
EXT_JAR="$ROOT/extension/burp-extension/target/acra-burp-extension-0.3.0-rc1.jar"
test -f "$EXT_JAR"
echo "SPRINT13_BURP_EXTENSION_BUILD PASS"

BURP_JAR="$WORK/burpsuite_desktop_v2026.7.3.jar"
curl --fail --location --retry 3   "https://portswigger.net/burp/releases/startdownload?product=desktop&type=jar&version=2026.7.3"   --output "$BURP_JAR"
echo "$BURP_SHA256  $BURP_JAR" | sha256sum -c -
echo "SPRINT13_BURP_BINARY_INTEGRITY PASS"

ACRA_LAB_MODE=secure PORT=18501 python3 lab/secure-api/basic-api/server.py >"$WORK/lab.log" 2>&1 &
PIDS+=("$!")
for _ in $(seq 1 40); do curl -fsS http://127.0.0.1:18501/health >/dev/null 2>&1 && break; sleep 0.25; done
curl -fsS http://127.0.0.1:18501/health >/dev/null
echo "SPRINT13_BURP_LAB_HEALTH PASS"

PROBE="$WORK/acra-burp-probe.jsonl"
USERCFG="$WORK/burp-user.json"
python3 - "$EXT_JAR" "$USERCFG" <<'PY'
import json,sys
jar,cfg=sys.argv[1:]
value={"user_options":{"extender":{"extensions":[{
  "errors":"ui","extension_file":jar,"extension_type":"java","loaded":True,
  "name":"ACRA","output":"ui","use_ai":False
}],"settings":{"automatically_reload_extensions_on_startup":True,"suppress_extension_loaded_popup":True}},
"misc":{"enable_proxy_interception_at_startup":"never"}}}
open(cfg,"w",encoding="utf-8").write(json.dumps(value))
PY

# User explicitly authorized acceptance of the PortSwigger Burp Suite Community Edition EULA for this controlled localhost Phase 7 CI run.
printf 'y\n' | java -Djava.awt.headless=true -Dacra.phase7.probeFile="$PROBE" -Xmx2g   -jar "$BURP_JAR" --user-config-file="$USERCFG" >"$WORK/burp.log" 2>&1 &
BURP_PID="$!"; PIDS+=("$BURP_PID")

for _ in $(seq 1 120); do
  if [[ -f "$PROBE" ]] && grep -q '"event":"INITIALIZED"' "$PROBE"; then break; fi
  if ! kill -0 "$BURP_PID" 2>/dev/null; then cat "$WORK/burp.log"; exit 1; fi
  sleep 1
done
grep -q '"event":"INITIALIZED"' "$PROBE"
echo "SPRINT13_BURP_MONTOYA_INITIALIZATION PASS"

TOKEN="$(python3 - <<'PY'
import base64,json
enc=lambda x:base64.urlsafe_b64encode(json.dumps(x,separators=(",",":")).encode()).decode().rstrip("=")
print(enc({"alg":"none","typ":"JWT"})+"."+enc({"sub":"user-a","tenant_id":"tenant-a","role":"viewer"})+".")
PY
)"

for DOC in 1001 1002; do
  curl --fail --silent --show-error --noproxy ""     --proxy http://127.0.0.1:8080     -H "Authorization: Bearer $TOKEN"     "http://127.0.0.1:18501/api/v1/tenants/tenant-a/documents/$DOC" >"$WORK/doc-$DOC.json"
done

for _ in $(seq 1 40); do
  [[ -f "$PROBE" ]] && [[ "$(grep -c '"event":"PROCESSED"' "$PROBE" || true)" -ge 2 ]] && break
  sleep 0.25
done

mkdir -p build
python3 scripts/verify-sprint13-burp-runtime-dev.py "$PROBE"
cp "$PROBE" build/s13-burp-runtime-dev.jsonl
sha256sum build/s13-burp-runtime-dev.jsonl > build/s13-burp-runtime-dev.jsonl.sha256
echo "SPRINT13_BURP_DEV_RESEARCH_GATE PASS"
