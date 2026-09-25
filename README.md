# ACRA — API Access Control & Routing Auditor

**Current release:** `v0.3.0`  
**License:** Apache-2.0  
**Runtime:** Java 21  
**Burp integration:** Montoya API 2026.7  
**Research state:** Sprint 13 frozen; stable release promotion validated through Sprint 15.

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

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

The shaded Burp extension is produced at:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

## Stable release packaging

```bash
bash scripts/verify-sprint15-release-promotion.sh
```

The stable packaging lane verifies the promotion decision, dependency contract, retained executable regression chain,
Maven build, package contents, license/NOTICE presence, checksums and deterministic fixed-input ZIP packaging.

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

- active execution is disabled by default;
- candidate != confirmed vulnerability;
- no automatic Burp issue publication;
- uncertainty is routed to review;
- research/runtime evidence probes exclude secret-bearing material;
- external testing requires explicit authorization and scope.

See `SECURITY.md`.

## Research status

Sprint 13 is frozen with eight canonical completed controlled experiments.

Phase 8 external-target validation was **NOT PERFORMED** before the freeze. Therefore this release does not claim:

- production scanner accuracy;
- production safety;
- external-target effectiveness;
- independent real-world validation.

A public software release is a packaging/distribution event, not new validation evidence.

## License

ACRA is licensed under the **Apache License, Version 2.0**.

SPDX: `Apache-2.0`

See `LICENSE` and `NOTICE`.

## Maintainer

Hamza Zulfiqar  
GitHub: `HamzaZulfeqar`

## Version

```text
0.3.0
```
