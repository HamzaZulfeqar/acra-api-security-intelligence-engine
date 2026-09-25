#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DEV_FREEZE="4bb7b952fb5cd0692efc6153d51b6885a7d38f9d"
BURP_SHA256="c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069"
LABELS="lab/ground-truth/GT-S13-BURP-EVAL-LABELS.json"
WORK="$(mktemp -d)"
PIDS=()

restore_labels(){ [[ -f "$WORK/labels.json" && ! -f "$LABELS" ]] && cp "$WORK/labels.json" "$LABELS" || true; }
cleanup(){
  restore_labels
  for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done
  rm -rf "$WORK"
}
trap cleanup EXIT

cp "$LABELS" "$WORK/labels.json"

echo "=== Phase 7 development freeze ==="
git diff --exit-code "$DEV_FREEZE" --   extension/burp-extension/pom.xml   extension/burp-extension/src/main/java/io/acra/burp/ACRAExtension.java   extension/burp-extension/src/main/java/io/acra/burp/runtime/BurpRuntimeProbe.java   extension/burp-extension/src/main/java/io/acra/burp/traffic/AcraHttpHandler.java   scripts/verify-sprint13-burp-runtime-dev.py   scripts/verify-sprint13-burp-runtime-dev.sh
echo "SPRINT13_BURP_EVAL_DEV_FREEZE PASS"

mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package
EXT_JAR="$ROOT/extension/burp-extension/target/acra-burp-extension-0.3.0-rc1.jar"
test -f "$EXT_JAR"
echo "SPRINT13_BURP_EVAL_EXTENSION_BUILD PASS"

BURP_JAR="$WORK/burpsuite_desktop_v2026.7.3.jar"
curl --fail --location --retry 3   "https://portswigger.net/burp/releases/startdownload?product=desktop&type=jar&version=2026.7.3"   --output "$BURP_JAR"
echo "$BURP_SHA256  $BURP_JAR" | sha256sum -c -
echo "SPRINT13_BURP_EVAL_BINARY_INTEGRITY PASS"

python3 lab/burp-runtime-eval/server.py >"$WORK/eval-lab.log" 2>&1 &
LAB_PID="$!"; PIDS+=("$LAB_PID")
for _ in $(seq 1 40); do curl -fsS http://127.0.0.1:18502/health >/dev/null 2>&1 && break; sleep 0.25; done
curl -fsS http://127.0.0.1:18502/health >/dev/null
echo "SPRINT13_BURP_EVAL_LAB_HEALTH PASS"

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

mapfile -t CASES < <(python3 - <<'PY'
import json
d=json.load(open("lab/ground-truth/GT-S13-BURP-EVAL-FEATURES.json"))
for c in d["cases"]:
    q=c["claims"]
    print("\t".join([c["caseId"],q["sub"],q["tenant_id"],q["role"],c["path"]]))
PY
)

run_pass(){
  local pass="$1"
  local probe="$WORK/probe-$pass.jsonl"
  local blog="$WORK/burp-$pass.log"

  # The repository owner explicitly authorized acceptance of the PortSwigger
  # Burp Suite Community Edition EULA for this controlled localhost Phase 7 CI validation.
  printf 'y\n' | java -Djava.awt.headless=true -Dacra.phase7.probeFile="$probe" -Xmx2g     -jar "$BURP_JAR" --user-config-file="$USERCFG" >"$blog" 2>&1 &
  local burp_pid="$!"
  PIDS+=("$burp_pid")

  for _ in $(seq 1 120); do
    if [[ -f "$probe" ]] && grep -q '"event":"INITIALIZED"' "$probe"; then break; fi
    if ! kill -0 "$burp_pid" 2>/dev/null; then cat "$blog"; return 1; fi
    sleep 1
  done
  grep -q '"event":"INITIALIZED"' "$probe"

  for line in "${CASES[@]}"; do
    IFS=$'\t' read -r case_id sub tenant role path <<<"$line"
    token="$(python3 - "$sub" "$tenant" "$role" <<'PY'
import base64,json,sys
sub,tenant,role=sys.argv[1:]
enc=lambda x:base64.urlsafe_b64encode(json.dumps(x,separators=(",",":")).encode()).decode().rstrip("=")
print(enc({"alg":"none","typ":"JWT"})+"."+enc({"sub":sub,"tenant_id":tenant,"role":role})+".")
PY
)"
    curl --fail --silent --show-error --noproxy ""       --proxy http://127.0.0.1:8080       -H "Authorization: Bearer $token"       "http://127.0.0.1:18502$path" >"$WORK/$pass-$case_id.json"
  done

  for _ in $(seq 1 60); do
    [[ -f "$probe" ]] && [[ "$(grep -c '"event":"PROCESSED"' "$probe" || true)" -ge 2 ]] && break
    sleep 0.25
  done
  [[ "$(grep -c '"event":"PROCESSED"' "$probe" || true)" -ge 2 ]]

  kill "$burp_pid" 2>/dev/null || true
  wait "$burp_pid" 2>/dev/null || true
  sleep 1
  printf '%s\n' "$probe"
}

rm "$LABELS"
echo "SPRINT13_BURP_EVAL_LABEL_ABSENCE PASS"

PROBE1="$(run_pass pass1)"
PROBE2="$(run_pass pass2)"

restore_labels

mkdir -p build/s13-burp-runtime-eval
python3 scripts/verify-sprint13-burp-runtime-eval.py "$PROBE1" build/s13-burp-runtime-eval/evaluation-pass1.json
python3 scripts/verify-sprint13-burp-runtime-eval.py "$PROBE2" build/s13-burp-runtime-eval/evaluation-pass2.json
cmp build/s13-burp-runtime-eval/evaluation-pass1.json build/s13-burp-runtime-eval/evaluation-pass2.json

cp "$PROBE1" build/s13-burp-runtime-eval/probe-pass1.jsonl
cp "$PROBE2" build/s13-burp-runtime-eval/probe-pass2.jsonl
sha256sum build/s13-burp-runtime-eval/probe-pass1.jsonl > build/s13-burp-runtime-eval/probe-pass1.jsonl.sha256
sha256sum build/s13-burp-runtime-eval/probe-pass2.jsonl > build/s13-burp-runtime-eval/probe-pass2.jsonl.sha256
sha256sum build/s13-burp-runtime-eval/evaluation-pass1.json > build/s13-burp-runtime-eval/evaluation-pass1.json.sha256
sha256sum build/s13-burp-runtime-eval/evaluation-pass2.json > build/s13-burp-runtime-eval/evaluation-pass2.json.sha256

echo "SPRINT13_BURP_EVAL_SEMANTIC_REPEATABILITY PASS"
echo "SPRINT13_BURP_EVAL_RESEARCH_GATE PASS"
