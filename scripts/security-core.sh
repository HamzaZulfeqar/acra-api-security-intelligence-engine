#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
# Security assertions are executable and fail the test suite on secret leakage,
# malformed JWT handling, control-character serialization, invalid evidence edges,
# invalid confidence, and oversized-header handling.
java -ea -cp build/classes:build/test-classes io.acra.core.tests.TestSuite >/tmp/acra-core-security-tests.log
if grep -Eq 'FAIL|AssertionError' /tmp/acra-core-security-tests.log; then
  cat /tmp/acra-core-security-tests.log
  exit 1
fi
printf 'SECURITY TEST PASS\n'
