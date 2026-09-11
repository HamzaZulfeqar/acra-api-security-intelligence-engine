#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p build
java -cp build/classes:build/test-classes io.acra.core.tests.PerformanceBaseline | tee build/performance-baseline.txt
