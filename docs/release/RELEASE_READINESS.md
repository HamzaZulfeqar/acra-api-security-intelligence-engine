# ACRA v0.3.0-rc1 — Release Readiness

**Sprint 14 branch:** `s14-productization-release-hardening`  
**Candidate:** `0.3.0-rc1`

## Technical release-candidate gates

| Gate | Required |
|---|---|
| Frozen Sprint 13 research unchanged | Yes |
| VERSION/POM version consistency | Yes |
| Retained executable regression chain | Yes |
| Maven clean verify | Yes |
| Shaded extension JAR present | Yes |
| Packaged JAR hash equals built JAR | Yes |
| Release manifest | Yes |
| SHA-256 checksum file | Yes |
| Deterministic packaging for fixed build input | Yes |
| Safe ZIP paths/fixed timestamps | Yes |
| Release artifact uploaded by CI | Yes |
| Dependency inventory uploaded by CI | Yes |

## Public release blockers

### Hard blocker — software license

The repository currently has no selected software license.

`LICENSE` explicitly records that no license has been selected. Sprint 14 does not choose one automatically.

**Public release eligibility: BLOCKED until an explicit license decision is made.**

### Version promotion

The project remains `0.3.0-rc1`.

Promotion to `0.3.0` requires an explicit decision after the release-candidate gate is green. Sprint 14 does not silently
promote the version.

### Git tag / GitHub Release

No public tag or GitHub Release is created by the hardening workflow. That action should follow the explicit license and
version-promotion decisions.

## Evidence boundary

Release hardening does not expand the frozen Sprint 13 research claims.

In particular:
- external-target validation remains NOT PERFORMED;
- production accuracy is not established;
- production safety is not established;
- active ACRA execution remains outside the validated Phase 7 runtime claim.

## Test-harness note

Many retained project test classes are executable assertion suites rather than standard JUnit/Surefire tests.

Therefore Sprint 14 runs `scripts/verify-sprint11-final.sh` in addition to Maven `clean verify`. A Maven log showing
zero Surefire tests for legacy classes is not treated as sufficient regression evidence on its own.
