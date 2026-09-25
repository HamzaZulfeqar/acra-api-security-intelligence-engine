# Changelog

All notable ACRA changes are recorded here. Research metrics remain governed by the canonical experiment registry and
final claim boundary rather than being generalized from changelog summaries.

## [0.3.0] — 2026-09-26

### Stable release promotion

- Promoted the hardened `0.3.0-rc1` candidate to stable `0.3.0`.
- Selected the Apache License, Version 2.0 (`Apache-2.0`).
- Added full `LICENSE` and project `NOTICE` distribution files.
- Added Maven license metadata.
- Added stable release packaging, manifest, checksums and promotion verification.
- Revalidated the retained executable regression chain and Maven build under the stable version.
- Preserved the Sprint 13 frozen research boundary without adding external-target or production-accuracy claims.

### Security/release boundary

- Active execution remains disabled by default.
- Candidate findings remain distinct from confirmed vulnerabilities.
- Automatic Burp issue publication remains unwired.
- External-target validation remained NOT PERFORMED before the research freeze.
- GitHub-native Dependency Review remains unavailable while the repository Dependency Graph is disabled; the Sprint 15
  direct-dependency contract and Maven dependency inventory remain part of promotion validation.

## [0.3.0-rc1] — 2026-09-26

### Release-candidate hardening

- Began Sprint 14 productization from frozen Sprint 13 head `d6797222d31984a45fbc80674193d0388757b24c`.
- Added a project-facing repository README.
- Added deterministic release-candidate packaging, checksums and manifest generation.
- Added version-consistency and frozen-research integrity gates.
- Added dedicated Sprint 14 release-hardening CI and release documentation.
- Preserved version `0.3.0-rc1` during Sprint 14.

### Research/runtime evidence inherited from frozen Sprint 13

- Blind held-out authorization evaluation preserved its original false-positive result.
- Automatic authorization-dimension discovery completed under the recorded synthetic boundary.
- Configured-policy generalization and negative-heavy/base-rate stress were measured separately.
- Uncertainty governance introduced review-required states rather than promoting uncertain cases.
- Cross-framework snapshot normalization and real localhost framework runtime validation completed.
- Real Burp Suite Community Edition 2026.7.3 / Montoya runtime validation completed on controlled localhost traffic.
- Sprint 13 final research/reproducibility freeze completed.
- External-target validation was not performed before freeze.

See `docs/research/FINAL_EVIDENCE_MANIFEST.json` for canonical run IDs and measured commits.

## 2026-09-25 — Sprint 12 controlled A0-A7 research execution

- Added the controlled A0-A7 cumulative ablation runner over the frozen 16-case authorization dataset.
- Enforced prediction/label separation.
- Added deterministic JSON, CSV and JSONL research artifacts with fingerprints and SHA-256 sidecars.
- Measured A0/A1 through A7 under the fixed synthetic localhost boundary.
- A6/A7 reached 8 TP / 8 TN / 0 FP / 0 FN only within that calibration fixture.
- Production/generalization claims were explicitly excluded.

## 2026-09-25 — Sprint 11 software completion

- Added evidence-backed finding lifecycle and immutable audit history.
- Added minimized deterministic reproduction packages.
- Added canonical JSON + SHA-256 and SARIF 2.1.0 reproduction exports.
- Added Burp Issue draft materialization without automatic SiteMap publication.
- Added read-only Findings & Reproduction UI.
- Added `GT-S11-AUTHORIZATION-RESEARCH`.
- Added security hardening, bounded engineering observations and deterministic closure packaging.

## 2026-09-23 — Sprint 5 software completion

- Added authoritative authorization-context normalization and completeness assessment.
- Bound analysis to authenticated evidence, project, observation, execution, test, endpoint and supplied policy references.
- Hardened BOLA/BFLA identifiers and endpoint binding.
- Added tenant, workflow and property policy assessments.
- Added conservative correlation metadata, FindingCandidate evaluation and final authorization orchestration.
- Added secret-safe authorization reporting and exact-JDK-21 verification.

## [Unreleased]

Future post-freeze work must preserve frozen Sprint 13 evidence and use new experiment/version identifiers for any new
research claims.
