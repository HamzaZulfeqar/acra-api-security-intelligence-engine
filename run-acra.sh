#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if ! command -v java >/dev/null 2>&1; then
  echo "ACRA requires Java 21 or newer." >&2
  exit 1
fi

NO_BUILD=false
if [[ "${1:-}" == "--no-build" ]]; then
  NO_BUILD=true
  shift
fi

if [[ "$NO_BUILD" == "false" ]]; then
  if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven is required for source-clone startup. Use --no-build only after packaging." >&2
    exit 1
  fi
  mvn --batch-mode --no-transfer-progress -pl standalone -am package
elif [[ ! -f standalone/target/acra-standalone.jar ]]; then
  echo "Prebuilt standalone/target/acra-standalone.jar was not found." >&2
  exit 1
fi

exec java -jar standalone/target/acra-standalone.jar "$@"
