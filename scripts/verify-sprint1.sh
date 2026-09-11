#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/build-core.sh
./scripts/test-core.sh
./scripts/security-core.sh
./scripts/architecture-core.sh
./scripts/perf-baseline.sh
printf 'SPRINT 1 LOCAL VERIFICATION PASS\n'
