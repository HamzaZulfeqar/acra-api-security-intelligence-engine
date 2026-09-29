#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STAGE="$ROOT/dist/acra-sprint10"
ZIP="$ROOT/dist/acra-sprint10-bundle.zip"
ZIP_HASH="$ROOT/dist/acra-sprint10-bundle.zip.sha256"

rm -rf "$STAGE" "$ZIP" "$ZIP_HASH"
mkdir -p "$STAGE"

if [[ ! -f standalone/target/acra-standalone.jar ]]; then
  echo "Missing standalone/target/acra-standalone.jar" >&2
  exit 1
fi

EXT_JAR="$(find extension/burp-extension/target -maxdepth 1 -type f -name 'acra-burp-extension-*.jar' ! -name 'original-*' | sort | head -n 1 || true)"
if [[ -z "$EXT_JAR" ]]; then
  echo "Missing shaded Burp extension JAR" >&2
  exit 1
fi

cp standalone/target/acra-standalone.jar "$STAGE/acra-standalone.jar"
cp "$EXT_JAR" "$STAGE/acra-burp-extension.jar"
cp packaging/acra-standalone.sh "$STAGE/acra-standalone.sh"
cp packaging/acra-standalone.bat "$STAGE/acra-standalone.bat"
cp packaging/README.txt "$STAGE/README.txt"
chmod +x "$STAGE/acra-standalone.sh"

(
  cd "$STAGE"
  sha256sum acra-standalone.jar acra-burp-extension.jar > SHA256SUMS.txt
)

mkdir -p "$ROOT/dist"
(
  cd "$ROOT/dist"
  zip -qr "$(basename "$ZIP")" "$(basename "$STAGE")"
)
sha256sum "$ZIP" > "$ZIP_HASH"

echo "Distribution: $ZIP"
cat "$ZIP_HASH"
