#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
java -ea -cp build/classes:build/test-classes io.acra.core.tests.TestSuite
