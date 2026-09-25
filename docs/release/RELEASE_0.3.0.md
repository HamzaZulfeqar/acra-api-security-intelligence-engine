# ACRA v0.3.0 — Stable Release

**Release type:** stable  
**License:** Apache-2.0  
**Java:** 21  
**Montoya API:** 2026.7  
**Validated Burp runtime boundary:** Community Edition 2026.7.3, controlled localhost only.

## Summary

ACRA v0.3.0 is the stable promotion of the hardened v0.3.0-rc1 candidate.

The release packages the Java core and Burp Suite extension after completion of the Sprint 13 research freeze, Sprint 14
release-candidate hardening, and Sprint 15 promotion/security gates.

## Included capability areas

- passive Burp/Montoya HTTP observation;
- identity/tenant/resource/context reconstruction;
- multiple API authorization-dimension analysis surfaces;
- configurable policy semantics;
- uncertainty governance and review-required states;
- finding/reproduction review workspace;
- deterministic evidence/reporting support.

## Safety defaults

- active execution is disabled by default;
- candidate findings are not treated as confirmed vulnerabilities;
- automatic Burp issue publication is not wired;
- uncertain policy/context states are routed to review;
- external testing requires explicit authorization.

## Research claim boundary

The stable software version does not expand the frozen Sprint 13 research evidence.

External-target validation was **NOT PERFORMED** before the freeze.

This release therefore does not claim:
- production scanner accuracy;
- production safety;
- arbitrary API/framework compatibility;
- arbitrary Burp-version compatibility;
- external-target effectiveness;
- independent third-party validation.

## License

Apache License, Version 2.0.

SPDX: `Apache-2.0`

The release bundle contains both `LICENSE` and `NOTICE`.

## Build

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

Expected extension:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

## Verification

```bash
bash scripts/verify-sprint15-release-promotion.sh
```

Stable release artifacts include:
- `acra-burp-extension-0.3.0.jar`;
- `acra-0.3.0.zip`;
- `release-manifest.json`;
- `SHA256SUMS`.
