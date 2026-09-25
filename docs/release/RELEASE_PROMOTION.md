# ACRA Release Promotion

**Final state:** PUBLISHED  
**Version:** `0.3.0`  
**License:** Apache-2.0  
**Tag:** `v0.3.0`  
**Canonical stable source:** `1bc21b6b28bc1c71f23fb9d22cd9ac663a179c71`

## Published release

GitHub Release:

https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0

The release is stable, non-draft and non-prerelease.

Published assets:
- `acra-burp-extension-0.3.0.jar`;
- `acra-0.3.0.zip`;
- `SHA256SUMS`;
- `release-manifest.json`.

Canonical digests are recorded in `release/promotion-decision.json`.

## Publication integrity

The `v0.3.0` tag points to the stable source merge commit `1bc21b6b28bc1c71f23fb9d22cd9ac663a179c71`.

A later merge added only the one-time publication workflow. The publication workflow is therefore intentionally
idempotent against the canonical stable source tag target instead of requiring the tag to point at a workflow-only
commit.

## Dependency review

GitHub native Dependency Review is unavailable while the repository Dependency Graph is disabled. This is treated as a
platform-feature limitation, not as a vulnerability result.

Blocking supply-chain checks retained by Sprint 15:
- direct dependency allowlist;
- Maven dependency inventory;
- no custom Maven repositories;
- no system-scoped dependencies;
- no SNAPSHOT dependencies;
- CodeQL;
- Gitleaks.

## Research/claim boundary

The public software release does not expand Sprint 13 research claims. External-target validation remained NOT PERFORMED,
and production accuracy/safety claims remain unsupported.
