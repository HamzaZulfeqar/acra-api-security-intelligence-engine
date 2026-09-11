#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
for n in 100 1000 10000; do
  java -ea -cp build/classes:build/s3/stubs:build/s3/extension:build/s3/test io.acra.burp.tests.sprint3.Sprint3PerformanceBaseline "$n"
done
