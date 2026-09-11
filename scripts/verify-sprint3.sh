#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p docs/testing/artifacts
{
  echo 'ACRA Sprint 3 verification'
  date -u '+utc=%Y-%m-%dT%H:%M:%SZ'
  java -version 2>&1 | head -1
  echo "version=$(cat VERSION)"
  ./scripts/build-sprint2.sh
  ./scripts/build-sprint3.sh
  ./scripts/test-core.sh
  ./scripts/test-sprint2.sh
  ./scripts/test-sprint3.sh
  ./scripts/security-sprint3.sh
  ./scripts/architecture-sprint3.sh
  echo 'OFFICIAL MONTOYA MAVEN BUILD: UNVERIFIED / BLOCKED when mvn or artifact network unavailable'
  echo 'BURP LIVE VALIDATION: UNVERIFIED / BLOCKED (Burp Suite unavailable in execution environment)'
} | tee docs/testing/artifacts/verification-s3.txt
./scripts/live-lab-sprint3.sh | tee docs/testing/artifacts/exp-recon-001-local-lab.txt
java -ea -cp build/classes:build/test-classes io.acra.core.tests.sprint3.Sprint3MetricsExperiment | tee docs/testing/artifacts/metrics-s3.txt
if [[ "${RUN_S3_PERF:-0}" == "1" ]]; then
  ./scripts/perf-sprint3.sh | tee docs/testing/artifacts/performance-baseline-s3.txt
else
  echo 'PERFORMANCE BASELINE: PRESERVED; set RUN_S3_PERF=1 to rerun'
fi
cat > docs/testing/artifacts/environment-blocker-s3.txt <<TXT
ACRA Sprint 3 environment gate
utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
java=$(java -version 2>&1 | head -1)
maven=$(command -v mvn >/dev/null 2>&1 && mvn -version | head -1 || echo NOT_INSTALLED)
burp=NOT_FOUND_IN_EXECUTION_ENVIRONMENT
external_dns=$(python3 - <<'PY'
import socket
try:
 print(socket.gethostbyname('repo1.maven.org'))
except Exception as e:
 print('BLOCKED:'+type(e).__name__)
PY
)
result=REAL_BURP_AND_OFFICIAL_DEPENDENCY_RUNTIME_GATES_UNVERIFIED
TXT
echo 'SPRINT 3 LOCAL VERIFICATION PASS; LIVE BURP GATE REMAINS BLOCKED'
