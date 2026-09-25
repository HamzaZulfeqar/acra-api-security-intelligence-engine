# Sprint 14 — Productization & Release Candidate Hardening

**Date:** 2026-09-26  
**Branch:** `s14-productization-release-hardening`  
**Base:** frozen Sprint 13 head `d6797222d31984a45fbc80674193d0388757b24c`  
**Candidate version:** `0.3.0-rc1`  
**State:** IN PROGRESS

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
