# Sprint 15 — Release Promotion

**Date:** 2026-09-26  
**Source RC:** `0.3.0-rc1`  
**Stable version:** `0.3.0`  
**License:** Apache-2.0  
**State:** PUBLISHED

## Objective

Promote the technically hardened ACRA release candidate into a stable public software release without altering the
frozen Sprint 13 research evidence or introducing unsupported production/external-target claims.

## Owner-approved decisions

- license: Apache License, Version 2.0 (`Apache-2.0`);
- target: `v0.3.0`;
- release type: stable.

## Promotion validation

The stable promotion path verified:
- Sprint 14 hardened source ancestry;
- Sprint 13 frozen-research integrity;
- version consistency at `0.3.0`;
- Apache-2.0 `LICENSE` and `NOTICE`;
- direct dependency contract and Maven dependency inventory;
- retained executable regression chain;
- Maven clean verify;
- stable package/JAR identity;
- deterministic fixed-input ZIP packaging;
- CodeQL;
- Gitleaks;
- complete PR regression matrix, including Sprint 5 serialization-security regression.

During promotion, a legacy embedded credential-header redaction edge case was exposed and fixed. The final release head
passed Sprint 5 Final Verification and all other current PR checks.

## Published release

- stable source merge: `1bc21b6b28bc1c71f23fb9d22cd9ac663a179c71`;
- tag: `v0.3.0`;
- tag target: `1bc21b6b28bc1c71f23fb9d22cd9ac663a179c71`;
- GitHub Release: https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0;
- release ID: `396973117`;
- published: `2026-09-25T23:01:26Z`;
- draft: false;
- prerelease: false.

Published assets:
- `acra-burp-extension-0.3.0.jar` — SHA-256 `95e3271328a77fe76e8c66728b4045a5f83ed782cc797382173ce87cc842f22e`;
- `acra-0.3.0.zip` — SHA-256 `d0f407e3fa824d160836d992d952bf780cd856dd9b5fee415c2bfd145b76b4ff`;
- `release-manifest.json` — SHA-256 `e50bf5bfbcc288e15677ecfe1d7c777ddf553458653820eefb09aca2ed608159`;
- `SHA256SUMS` — SHA-256 `402ef1c6ceff690ad2abb1ea71544aa30ef3a5a44ff612a4c81b4e38b46eeb77`.

## Claim boundary

Publication is a software distribution milestone, not a new research-validation result.

The release does not establish:
- production scanner accuracy;
- production safety;
- external-target effectiveness;
- arbitrary framework/runtime compatibility;
- arbitrary Burp-version compatibility;
- independent third-party validation.

Phase 8 external-target validation remained **NOT PERFORMED** before the Sprint 13 research freeze.

## Final Sprint 15 decision

`PREPARED_BLOCKED → READY_FOR_PROMOTION → PUBLISHED`

Sprint 15 is complete.
