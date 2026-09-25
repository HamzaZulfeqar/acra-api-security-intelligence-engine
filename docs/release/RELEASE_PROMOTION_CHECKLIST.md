# Release Promotion Checklist

## Source candidate

- [x] Sprint 13 research freeze complete.
- [x] Sprint 14 technical RC hardening complete.
- [x] Release-hardening workflow passed.
- [x] CodeQL passed.
- [x] Gitleaks passed.
- [x] RC package and checksums generated.
- [x] No release tag exists from the completed Sprint 14 work.

## Explicit owner decisions

- [ ] Select software license.
- [ ] Replace unresolved `LICENSE` notice with selected license text.
- [ ] Select publication path:
  - [ ] publish `v0.3.0-rc1` as pre-release; or
  - [ ] promote to `v0.3.0`.
- [ ] Approve release notes.

## Promotion verification

- [ ] Update `release/promotion-decision.json`.
- [ ] Run full retained executable regression chain.
- [ ] Run Maven clean verify.
- [ ] Verify version contract.
- [ ] Verify frozen Sprint 13 research lock.
- [ ] Generate final release bundle + SHA-256 checksums.
- [ ] Run CodeQL.
- [ ] Run Gitleaks.
- [ ] Confirm claim boundary unchanged.

## Publication

- [ ] Create immutable Git tag.
- [ ] Create GitHub Release.
- [ ] Mark prerelease/stable according to selected version path.
- [ ] Attach extension JAR.
- [ ] Attach release ZIP.
- [ ] Attach `SHA256SUMS`.
- [ ] Attach release manifest.
- [ ] Record final tag/release identifiers in promotion decision.
- [ ] Verify release assets after publication.

## Post-release

- [ ] Update project state to PUBLISHED.
- [ ] Record published artifact hashes.
- [ ] Preserve Sprint 13/Sprint 14 historical evidence.
