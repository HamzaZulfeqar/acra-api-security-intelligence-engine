#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p docs/testing/artifacts
{
  echo 'ACRA Sprint 2 verification'
  date -u '+utc=%Y-%m-%dT%H:%M:%SZ'
  java -version 2>&1 | head -1
  ./scripts/build-sprint2.sh
  ./scripts/test-core.sh
  ./scripts/test-sprint2.sh
  ./scripts/security-core.sh
  ./scripts/security-sprint2.sh
  ./scripts/architecture-core.sh
  ./scripts/architecture-sprint2.sh
  echo 'BURP LIVE VALIDATION: UNVERIFIED / BLOCKED (Burp Suite unavailable in execution environment)'
} | tee docs/testing/artifacts/verification-s2.txt
./scripts/live-lab-sprint2.sh | tee docs/testing/artifacts/exp-integration-001-local-lab.txt
./scripts/perf-sprint2.sh | tee docs/testing/artifacts/performance-baseline-s2-rerun.txt
echo 'SPRINT 2 LOCAL VERIFICATION PASS; BURP LIVE GATE REMAINS BLOCKED'
