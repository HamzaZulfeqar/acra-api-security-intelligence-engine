#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT=18765
TARGET_PORT=18766
TMP="$(mktemp -d)"
APP_PID=""
FIXTURE_PID=""

cleanup() {
  if [ -n "$APP_PID" ]; then kill "$APP_PID" >/dev/null 2>&1 || true; fi
  if [ -n "$FIXTURE_PID" ]; then kill "$FIXTURE_PID" >/dev/null 2>&1 || true; fi
  rm -rf "$TMP"
}
trap cleanup EXIT

mvn --batch-mode --no-transfer-progress clean package -pl app/standalone -am

JAR="app/standalone/target/acra-standalone-0.3.0.jar"
test -f "$JAR"
jar tf "$JAR" | grep -qx 'io/acra/standalone/AcraStandaloneApplication.class'
jar tf "$JAR" | grep -qx 'io/acra/core/engine/SecurityContextEngine.class'
echo "SPRINT17_STANDALONE_JAR PASS"

mkdir -p "$TMP/users"
printf '{"user":{"id":"42","tenant":"alpha"}}\n' > "$TMP/users/42"
python3 -m http.server "$TARGET_PORT" --bind 127.0.0.1 --directory "$TMP" >"$TMP/fixture.log" 2>&1 &
FIXTURE_PID=$!

java -jar "$JAR" --no-browser --port="$PORT" >"$TMP/acra.log" 2>&1 &
APP_PID=$!

for _ in $(seq 1 50); do
  if curl --silent --fail "http://127.0.0.1:$PORT/api/status" >"$TMP/status.json"; then break; fi
  sleep 0.2
done

grep -q '"status":"ready"' "$TMP/status.json"
grep -q '"burpRequired":false' "$TMP/status.json"
grep -q '"mode":"standalone-localhost"' "$TMP/status.json"
echo "SPRINT17_LOCALHOST_GUI_STATUS PASS"

curl --silent --fail   -H 'Content-Type: application/x-www-form-urlencoded'   --data-urlencode "url=http://127.0.0.1:$TARGET_PORT/users/42?tenant=alpha"   --data-urlencode "authorized=true"   "http://127.0.0.1:$PORT/api/analyze" > "$TMP/result.json"

grep -q '"responseStatus":200' "$TMP/result.json"
grep -q '"method":"GET"' "$TMP/result.json"
grep -q '"action":"READ"' "$TMP/result.json"
grep -q '"contextStatus"' "$TMP/result.json"
grep -q '"evidenceCount":' "$TMP/result.json"
echo "SPRINT17_STANDALONE_CORE_ANALYSIS PASS"

curl --silent --fail "http://127.0.0.1:$PORT/api/analyses" > "$TMP/history.json"
grep -q '"id":"standalone-1"' "$TMP/history.json"
echo "SPRINT17_ANALYSIS_HISTORY PASS"

UNAUTH_CODE="$(curl --silent --output "$TMP/unauth.json" --write-out '%{http_code}'   -H 'Content-Type: application/x-www-form-urlencoded'   --data-urlencode "url=http://127.0.0.1:$TARGET_PORT/"   "http://127.0.0.1:$PORT/api/analyze")"
test "$UNAUTH_CODE" = "403"
echo "SPRINT17_AUTHORIZATION_ACK_GATE PASS"

grep -q 'Burp is not required for standalone analysis.' "$TMP/acra.log"
echo "SPRINT17_BURP_INDEPENDENCE PASS"

git diff --check
echo "SPRINT17_DIFF_CHECK PASS"
echo "SPRINT17_STANDALONE_LOCALHOST_GATE PASS"
