#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
java -ea -cp build/classes:build/s2/stubs:build/s2/extension:build/s2/test io.acra.burp.tests.Sprint2TestSuite
