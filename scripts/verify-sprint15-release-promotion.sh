#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

git config core.fileMode false

python3 scripts/verify-sprint15-release-promotion.py

# Preserve the exact technical release-candidate gate as a promotion prerequisite.
chmod +x ./scripts/*.sh
bash scripts/verify-sprint14-release.sh

echo "SPRINT15_RC_REVALIDATION PASS"

git diff --check
echo "SPRINT15_DIFF_CHECK PASS"
echo "SPRINT15_RELEASE_PROMOTION_PREFLIGHT PASS"
