#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

git config core.fileMode false
chmod +x ./scripts/*.sh

python3 scripts/verify-sprint15-release-promotion.py
python3 scripts/verify-sprint15-dependency-contract.py
mvn --batch-mode --no-transfer-progress dependency:tree
echo "SPRINT15_DEPENDENCY_INVENTORY PASS"

echo "=== Retained executable regression chain ==="
bash scripts/verify-sprint11-final.sh
echo "SPRINT15_EXECUTABLE_REGRESSION_CHAIN PASS"

echo "=== Stable Maven clean verify ==="
mvn --batch-mode --no-transfer-progress clean verify
echo "SPRINT15_STABLE_MAVEN_VERIFY PASS"

python3 scripts/package-sprint15-stable-release.py
python3 scripts/verify-sprint15-stable-release.py

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp build/release/acra-0.3.0.zip "$TMP/acra-0.3.0.zip"
cp build/release/release-manifest.json "$TMP/release-manifest.json"
cp build/release/SHA256SUMS "$TMP/SHA256SUMS"

python3 scripts/package-sprint15-stable-release.py
python3 scripts/verify-sprint15-stable-release.py

cmp "$TMP/acra-0.3.0.zip" build/release/acra-0.3.0.zip
cmp "$TMP/release-manifest.json" build/release/release-manifest.json
cmp "$TMP/SHA256SUMS" build/release/SHA256SUMS
echo "SPRINT15_STABLE_PACKAGE_REPEATABILITY PASS"

git diff --check
echo "SPRINT15_DIFF_CHECK PASS"
echo "SPRINT15_READY_FOR_PROMOTION_GATE PASS"
