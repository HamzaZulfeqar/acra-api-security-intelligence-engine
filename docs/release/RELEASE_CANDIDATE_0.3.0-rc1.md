# ACRA v0.3.0-rc1 — Release Candidate Notes

**Status:** internal/research release candidate  
**Java:** 21  
**Montoya API:** 2026.7  
**Validated Burp runtime:** Community Edition 2026.7.3, controlled localhost only.

## Purpose

This candidate packages the current ACRA Burp extension after the Sprint 13 research/reproducibility freeze.

It is intended for controlled evaluation, code review and authorized research—not as a production scanner guarantee.

## Included capability areas

- passive HTTP observation through the Burp Montoya adapter;
- security-context reconstruction;
- endpoint/resource/identity/tenant correlation;
- multiple authorization-dimension reasoning surfaces;
- configurable policy semantics;
- uncertainty governance and review-required states;
- finding/reproduction review workspace;
- controlled export/materialization support.

## Safety defaults

- active execution remains disabled by default;
- unverified candidates are not automatically published as Burp issues;
- uncertain policy/context states are routed to review;
- runtime research probes are opt-in;
- external testing requires explicit authorization.

## Research boundary

Sprint 13 is frozen. The final evidence package records controlled synthetic, localhost framework-runtime and real
Burp/Montoya runtime experiments.

External-target validation was **NOT PERFORMED** before the freeze.

Do not interpret this release candidate as evidence of:
- production accuracy;
- arbitrary API/framework compatibility;
- arbitrary Burp-version compatibility;
- production safety;
- external-target effectiveness;
- independent third-party validation.

## Build

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

Expected shaded extension:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0-rc1.jar
```

## Package verification

```bash
python3 scripts/package-sprint14-release.py
python3 scripts/verify-sprint14-release.py
```

## Public release blocker

The repository currently has **no selected software license**. The `LICENSE` file intentionally records that unresolved
decision.

Therefore this candidate must not be promoted as a public distributable release until the license decision is explicit.

## Canonical research references

- `docs/research/FINAL_RESEARCH_FREEZE.md`
- `docs/research/FINAL_CLAIM_BOUNDARY.md`
- `docs/research/FINAL_EVIDENCE_MANIFEST.json`
- `docs/research/FINAL_REPRODUCIBILITY.md`
