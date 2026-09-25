# Release Promotion Checklist

## Source candidate
- [x] Sprint 13 research freeze complete.
- [x] Sprint 14 technical RC hardening complete.
- [x] Release-hardening workflow passed.
- [x] CodeQL passed.
- [x] Gitleaks passed.
- [x] RC package/checksums generated.

## Owner decisions
- [x] Select Apache-2.0.
- [x] Replace unresolved license notice with Apache-2.0 LICENSE.
- [x] Add NOTICE.
- [x] Promote `0.3.0-rc1` to `0.3.0`.
- [x] Approve stable release notes.

## Promotion verification
- [x] Promotion decision READY/PUBLISHED.
- [x] Full retained executable regression chain.
- [x] Maven clean verify.
- [x] Version contract.
- [x] Frozen Sprint 13 research lock.
- [x] Stable bundle/checksums.
- [x] CodeQL.
- [x] Gitleaks.
- [x] Claim boundary unchanged.
- [x] Sprint 5 redaction regression fixed and revalidated.

## Publication
- [x] Stable source merged to main.
- [x] Tag `v0.3.0` created.
- [x] GitHub Release created.
- [x] Release is non-draft.
- [x] Release is non-prerelease.
- [x] Extension JAR attached.
- [x] Stable ZIP attached.
- [x] `SHA256SUMS` attached.
- [x] Release manifest attached.
- [x] GitHub SHA-256 asset digests recorded.

## Final state
**PUBLISHED**
