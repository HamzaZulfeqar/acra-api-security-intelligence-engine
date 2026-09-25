# Sprint 14 — Productization & Release Candidate Hardening

**Date:** 2026-09-26  
**Branch:** `s14-productization-release-hardening`  
**Base:** frozen Sprint 13 head `d6797222d31984a45fbc80674193d0388757b24c`  
**Candidate version:** `0.3.0-rc1`  
**State:** TECHNICAL RELEASE CANDIDATE HARDENED; PUBLIC RELEASE BLOCKED BY LICENSE

## Objective

Turn the frozen research repository into a reproducible, reviewable release-candidate package without rewriting Sprint 13
evidence or promoting unsupported claims.

Sprint 14 is an engineering/productization sprint, not a continuation of Sprint 13 research metrics.

## Hardening gates

### S14-01 — Frozen evidence protection

The following Sprint 13 evidence surfaces must remain unchanged from the frozen base:

- `docs/research/`;
- `lab/ground-truth/`;
- Sprint 13 run/verification scripts;
- Sprint 13 workflows.

Later product code may evolve, but historical research evidence remains anchored to its measured commits.

### S14-02 — Version contract

`VERSION`, root Maven version and both module parent versions must agree exactly.

Current expected candidate: `0.3.0-rc1`.

No automatic promotion to `0.3.0` is allowed.

### S14-03 — Build/test gate

Release candidate must pass:

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

Java compiler warnings are already configured as errors in product modules.

### S14-04 — Distribution package

Create a deterministic bundle for a fixed built JAR containing:

- shaded Burp extension JAR;
- project README;
- security policy;
- current unresolved LICENSE notice;
- changelog;
- release-candidate notes;
- final Sprint 13 claim boundary;
- final evidence manifest;
- final reproducibility guide;
- release manifest.

Generate SHA-256 checksums outside the bundle.

### S14-05 — Installability/readiness documentation

The repository root must explain:

- what ACRA is;
- how to build it;
- where the extension JAR is produced;
- the verified Burp/runtime boundary;
- safety/claim boundaries;
- unresolved license status.

### S14-06 — Public-release blocker policy

A GitHub Release/tag is **not** created by Sprint 14 until:

1. a software license is explicitly selected;
2. the candidate packaging gate is green;
3. release notes are reviewed;
4. the version-promotion decision is explicit.

## Definition of Done

Sprint 14 may be marked complete when:

- frozen Sprint 13 integrity gate passes;
- version contract passes;
- Maven clean verify passes;
- package/manifest/checksum generation passes twice deterministically for the same build input;
- packaged JAR identity matches the built shaded extension JAR;
- release manifest/ZIP verification passes;
- documentation is product-facing and claim-safe;
- unresolved public-release blockers are explicitly recorded.

No public-release status is implied by Sprint 14 completion.


## Canonical Sprint 14 hardening evidence

Release-hardening workflow:
- run: `36185472934`;
- source head: `78d6fb0474af81a3b93908e26b38b03806e97bd4`;
- result: SUCCESS.

Security workflow:
- run: `36185473086`;
- CodeQL analysis: SUCCESS;
- Gitleaks secret scanning: SUCCESS;
- dependency review: skipped because this was a branch push rather than a pull request.

Release-hardening evidence:
- retained Sprint 11 final executable regression chain: PASS;
- Maven clean verify: BUILD SUCCESS;
- version contract: PASS, `0.3.0-rc1`;
- frozen Sprint 13 research lock: PASS;
- packaged JAR identity: PASS;
- release manifest: PASS;
- deterministic fixed-input ZIP packaging: PASS;
- SHA-256 verification: PASS;
- dependency inventory generated;
- CI release artifact uploaded.

Measured build artifact from the canonical hardening run:
- shaded JAR SHA-256: `e3553702737dcd18b82c3459f5e5b46353a4263803260c5d80efcc59ebf7b34b`;
- release bundle SHA-256: `88352a4430c248f1ff46a25e1515f7f04c7670105050ad224ab4325059968988`.

These hashes identify the artifacts from that exact CI run. Maven JAR bit-reproducibility across independent clean builds
is not claimed; Sprint 14 proves deterministic packaging for a fixed built JAR input.

## Sprint 14 decision

The internal/research release candidate is technically hardened.

**Public release eligibility remains FALSE** because the repository has no selected software license.

No Git tag, GitHub Release, or version promotion to `0.3.0` is authorized by this sprint.
