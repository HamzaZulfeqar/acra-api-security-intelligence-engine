# ACRA — API Access Control & Routing Auditor

**Current release:** `v0.3.0`  
**License:** Apache-2.0  
**Runtime:** Java 21  
**Burp integration:** Montoya API 2026.7  
**Research state:** Sprint 13 frozen; stable release published through Sprint 15.

ACRA is a security-research and engineering project for analyzing API authorization behavior from correlated HTTP,
identity, tenant, ownership, role, workflow, routing, property, batch and indirect-reference evidence.

The project is implemented as a Java core plus a Burp Suite extension adapter.

## Quick Start

### 1. Clone the official stable release

Recommended for reproducible use:

```bash
git clone --branch v0.3.0 --depth 1 https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git
cd acra-api-security-intelligence-engine
```

For contributors who intentionally want the latest development state:

```bash
git clone https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git
cd acra-api-security-intelligence-engine
```

### 2. Verify prerequisites

```bash
java -version
mvn -version
git --version
```

Required:
- Java 21;
- Maven;
- Git.

### 3. Build ACRA

```bash
mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am
```

Expected extension JAR:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

### 4. Load ACRA in Burp Suite

In Burp:

1. Open **Extensions > Installed**.
2. Click **Add**.
3. Select **Java** as the extension type.
4. Select `acra-burp-extension-0.3.0.jar`.
5. Click **Next**.
6. Review the **Output** and **Errors** tabs.
7. Close the dialog after the extension loads.

The extension should appear as **ACRA**, and an **ACRA** suite tab should be visible.

### 5. Configure authorized scope before expecting traffic

ACRA defaults to Burp's suite scope. Add only systems you are explicitly authorized to test to **Target > Scope**
(or right-click an authorized target in the Site map and choose **Add to scope**).

ACRA's active request execution remains disabled by default.

### 6. Verify the first run

Expected extension output:

```text
ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled.
```

In the ACRA tab, verify:
- **Overview**, **Traffic**, **Contexts**, **Endpoints**, and **Configuration** are visible;
- Configuration reports `Scope mode: IN_SCOPE_ONLY`;
- Configuration reports `Active execution: DISABLED by default`;
- authorized in-scope Proxy traffic begins appearing in the passive observation views.

Full onboarding:
- [Quick Start](docs/getting-started/QUICK_START.md)
- [Burp Installation](docs/getting-started/BURP_INSTALLATION.md)
- [First-Run Verification](docs/getting-started/FIRST_RUN_VERIFICATION.md)
- [Troubleshooting](docs/getting-started/TROUBLESHOOTING.md)

## Download instead of building

The stable GitHub Release contains the prebuilt extension JAR, release ZIP, manifest, and SHA-256 checksums:

https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0

Users should verify downloaded assets against the published `SHA256SUMS`.

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
docs/                      Architecture, testing, research and onboarding
.github/workflows/         CI, security and research gates
```

## Public repository verification

Sprint 16 adds a public-onboarding smoke gate that verifies:

- public cloneability;
- Java 21 / Maven buildability;
- stable version/license consistency;
- extension JAR creation;
- required Burp entry class packaging;
- stable release asset availability/checksum;
- onboarding documentation integrity.

Run locally:

```bash
bash scripts/verify-public-onboarding.sh
```

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

## Repository hardening

The file-based public onboarding controls are versioned in this repository.

Two GitHub administration settings are tracked separately because they cannot be changed by the connected repository
automation used for this sprint:
- protect `main` with pull-request/status-check requirements;
- enable GitHub Dependency Graph so native Dependency Review becomes meaningful.

See `docs/operations/GITHUB_REPOSITORY_HARDENING.md`.

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
