#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
mvn --batch-mode --no-transfer-progress -pl app/standalone -am package
exec java -jar app/standalone/target/acra-standalone-0.3.0.jar "$@"
