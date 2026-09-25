# ACRA Release Promotion

**Stage:** Sprint 15 — Release Promotion  
**Branch:** `s15-release-promotion`  
**Source RC:** `0.3.0-rc1`  
**Target:** `0.3.0` stable  
**License:** Apache-2.0

## Current state

**READY_FOR_PROMOTION — owner-approved stable target and license selected.**

The project owner explicitly approved:
- Apache License, Version 2.0;
- stable promotion from `0.3.0-rc1` to `0.3.0`;
- public release promotion after all stable gates pass.

## Promotion contract

Before publication:
- Sprint 13 frozen research must remain unchanged;
- Sprint 14 hardened source must remain in ancestry;
- `VERSION` and all Maven versions must equal `0.3.0`;
- `LICENSE` must contain Apache-2.0;
- `NOTICE` must be present;
- direct dependency contract must pass;
- retained executable regressions must pass;
- Maven clean verify must pass;
- stable package/checksum verification must pass;
- CodeQL and Gitleaks must pass;
- external-target/production claim boundaries must remain unchanged.

## Dependency-review status

GitHub native Dependency Review is unavailable because the repository Dependency Graph is disabled. This is a platform
configuration limitation, not a vulnerability result.

Sprint 15 separately enforces:
- zero direct dependencies in `acra-core`;
- only `acra-core` and the provided Montoya API as direct extension dependencies;
- no custom Maven repositories;
- no system-scoped dependencies;
- no SNAPSHOT dependencies;
- Maven dependency-tree generation.

## Stable publication assets

Expected:
- `acra-burp-extension-0.3.0.jar`;
- `acra-0.3.0.zip`;
- `release-manifest.json`;
- `SHA256SUMS`.

## Claim boundary

Stable publication does not create new research evidence.

Phase 8 external-target validation remained **NOT PERFORMED** before the Sprint 13 freeze. Production accuracy, production
safety, arbitrary framework/Burp compatibility and external-target effectiveness remain unsupported claims.

## State transition

`PREPARED_BLOCKED → READY_FOR_PROMOTION`

After all current-head CI/security gates pass, publication may proceed to:

`READY_FOR_PROMOTION → PUBLISHED`
