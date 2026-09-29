#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
if ! command -v java >/dev/null 2>&1; then
  echo "ACRA requires Java 21 or newer." >&2
  exit 1
fi
exec java -jar "$ROOT/acra-standalone.jar" "$@"
