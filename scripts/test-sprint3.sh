#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
java -ea -cp build/classes:build/test-classes io.acra.core.tests.sprint3.Sprint3CoreTestSuite
java -ea -cp build/classes:build/s3/stubs:build/s3/extension:build/s3/test io.acra.burp.tests.sprint3.Sprint3AdapterTestSuite
