# ACRA — API Access Control & Routing Auditor

**Current candidate:** `v0.3.0-rc1`  
**Runtime:** Java 21  
**Burp integration:** Montoya API 2026.7  
**Research state:** Sprint 13 frozen; Sprint 14 productization/release hardening in progress.

ACRA is a security-research and engineering project for analyzing API authorization behavior from correlated HTTP,
identity, tenant, ownership, role, workflow, routing, property, batch and indirect-reference evidence.

The project is implemented as a Java core plus a Burp Suite extension adapter.

## What ACRA does

ACRA passively observes API traffic and builds structured security context for authorization analysis. Its architecture
supports:

- object-level authorization analysis;
- tenant-boundary analysis;
- role/function authorization analysis;
- workflow/state authorization analysis;
- routing-equivalence analysis;
- property-level authorization analysis;
- batch authorization analysis;
- indirect-reference analysis;
- evidence correlation and governed uncertainty;
- review-oriented finding candidates and reproduction artifacts.

ACRA deliberately distinguishes a **candidate** from a **confirmed vulnerability**. Uncertain policy/context states are
routed to review rather than silently promoted.

## Repository layout

```text
core/                     Framework-neutral ACRA domain and reasoning code
extension/burp-extension/ Burp Suite / Montoya adapter and UI
lab/                      Controlled local research fixtures
scripts/                  Verification, packaging and research automation
docs/                      Architecture, testing, research and sprint records
.github/workflows/         CI, security and research gates
```

## Build

Requirements:

- JDK 21
- Maven
- Git

Build and run the project test suite:

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

The shaded Burp extension candidate is produced at:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0-rc1.jar
```

## Release-candidate packaging

Sprint 14 adds a deterministic packaging layer around a fixed Maven build input.

```bash
mvn --batch-mode --no-transfer-progress clean verify
python3 scripts/package-sprint14-release.py
python3 scripts/verify-sprint14-release.py
```

Generated files are written under `build/release/` and include:

- the shaded Burp extension JAR;
- a deterministic release ZIP;
- a machine-readable release manifest;
- SHA-256 checksums;
- release/readme/security/license/claim-boundary documentation.

The package is a **release candidate**, not a public release.

## Verified runtime boundary

Sprint 13 Phase 7 loaded the actual shaded ACRA extension inside real Burp Suite Community Edition 2026.7.3 and observed:

- real Montoya extension initialization;
- real Burp Proxy request callbacks;
- real Burp Proxy response callbacks;
- ACRA passive-pipeline processing;
- correct post-freeze context reconstruction on two unseen localhost cases in two independent runs;
- review/publication eligibility protections;
- active execution disabled.

Canonical strengthened Phase 7 workflow: `36180569483`.

This evidence is limited to the exact controlled localhost runtime boundary. It does **not** establish production
accuracy, arbitrary Burp-version compatibility, external-target effectiveness or independent real-world validation.

See:

- `docs/research/FINAL_RESEARCH_FREEZE.md`
- `docs/research/FINAL_CLAIM_BOUNDARY.md`
- `docs/research/FINAL_EVIDENCE_MANIFEST.json`
- `docs/research/FINAL_REPRODUCIBILITY.md`

## Safety model

ACRA is developed for authorized security research.

Current release-candidate rules include:

- active execution disabled by default;
- candidate != confirmed vulnerability;
- no automatic Burp issue publication;
- uncertainty routed to review;
- no secrets in research/runtime evidence probes;
- external testing only with explicit authorization and scope.

See `SECURITY.md`.

## Research status

Sprint 13 is frozen with eight canonical completed controlled experiments.

Phase 8 external-target validation was **NOT PERFORMED** before the freeze. Therefore this repository does not claim:

- production scanner accuracy;
- production safety;
- external-target effectiveness;
- independent real-world validation.

Any later external validation must be a new post-freeze experiment/version.

## License status

**No software license has been selected yet.**

The repository's `LICENSE` file intentionally records this unresolved state. Until a license is explicitly selected,
do not treat the source or packaged release candidate as granting reuse, redistribution or modification rights.

This is currently a blocker for public release promotion.

## Maintainer

Hamza Zulfiqar  
GitHub: `HamzaZulfeqar`

## Version

Source-of-truth version:

```text
0.3.0-rc1
```

See `VERSION`, the Maven POMs and `CHANGELOG.md`.
