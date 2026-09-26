#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
mvn --batch-mode --no-transfer-progress -pl standalone -am package
exec java -jar standalone/target/acra-standalone.jar "$@"
