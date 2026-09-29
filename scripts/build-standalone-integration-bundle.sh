#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STAGE="$ROOT/dist/acra-standalone-integration-candidate"
ZIP="$ROOT/dist/acra-standalone-integration-candidate.zip"
rm -rf "$STAGE" "$ZIP" "$ZIP.sha256"
mkdir -p "$STAGE"

test -f standalone/target/acra-standalone.jar
EXT_JAR="$(find extension/burp-extension/target -maxdepth 1 -type f -name 'acra-burp-extension-*.jar' ! -name 'original-*' | sort | head -n 1)"
test -n "$EXT_JAR"

cp standalone/target/acra-standalone.jar "$STAGE/acra-standalone.jar"
cp "$EXT_JAR" "$STAGE/acra-burp-extension.jar"
cp packaging/acra-standalone.sh "$STAGE/acra-standalone.sh"
cp packaging/acra-standalone.bat "$STAGE/acra-standalone.bat"
cp docs/release/STANDALONE_INTEGRATION.md "$STAGE/README.md"
chmod +x "$STAGE/acra-standalone.sh"

(cd "$STAGE" && sha256sum acra-standalone.jar acra-burp-extension.jar > SHA256SUMS.txt)
(cd "$ROOT/dist" && zip -q -r "$(basename "$ZIP")" "$(basename "$STAGE")")
sha256sum "$ZIP" > "$ZIP.sha256"

EXTRACT="$(mktemp -d)"
trap 'rm -rf "$EXTRACT"' EXIT
unzip -q "$ZIP" -d "$EXTRACT"
(cd "$EXTRACT/$(basename "$STAGE")" && sha256sum -c SHA256SUMS.txt && bash -n acra-standalone.sh)

echo "Candidate bundle: $ZIP"
cat "$ZIP.sha256"
