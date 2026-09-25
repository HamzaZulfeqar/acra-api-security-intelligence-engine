#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "=== Sprint 14 Maven clean verify ==="
mvn --batch-mode --no-transfer-progress clean verify

echo "=== Sprint 14 package pass 1 ==="
python3 scripts/package-sprint14-release.py
python3 scripts/verify-sprint14-release.py

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp build/release/acra-*-release-candidate.zip "$TMP/bundle.zip"
cp build/release/release-manifest.json "$TMP/release-manifest.json"
cp build/release/SHA256SUMS "$TMP/SHA256SUMS"

echo "=== Sprint 14 package pass 2 ==="
python3 scripts/package-sprint14-release.py
python3 scripts/verify-sprint14-release.py

cmp "$TMP/bundle.zip" build/release/acra-*-release-candidate.zip
cmp "$TMP/release-manifest.json" build/release/release-manifest.json
cmp "$TMP/SHA256SUMS" build/release/SHA256SUMS

echo "SPRINT14_PACKAGE_REPEATABILITY PASS"

git diff --check
echo "SPRINT14_DIFF_CHECK PASS"
echo "SPRINT14_RELEASE_HARDENING_GATE PASS"
