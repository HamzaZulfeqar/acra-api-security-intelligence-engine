# Sprint 6 Final Software Audit — Candidate

Audit state: **FINAL RUN PENDING**  
Branch: `s6-tenant-rbac`

## Candidate decision

The Sprint 6 tenant-isolation / advanced-RBAC software scope is implementation-complete at the source level and has focused executable evidence across policy resolution, lab validation, automated planning/execution, credential-safe role comparison, UI, reporting, performance observations and security hardening.

A final SOFTWARE COMPLETE decision is withheld until the dedicated final regression and reproducible-package workflow succeeds.

## Required final gates

1. exact Java 21 core compilation with warnings as errors;
2. retained core/S3/S4/S5/S6 verification;
3. S6 secure/vulnerable localhost experiment;
4. S6 reporting/export, performance and security suites;
5. official Maven package;
6. retained Sprint 2 local-stub regression;
7. retained Sprint 3 local-stub regression;
8. current Sprint 4 and Sprint 6 UI headless regression;
9. deterministic source package generation;
10. archive safe-path, clean-extraction and per-file SHA-256 equality verification.

## Explicit non-blocking exclusions

The following are not converted into Sprint 6 software failures:
- real Burp desktop runtime/load/handler/UI validation;
- external-target validation;
- later-sprint workflow/property/routing/authentication/multi-protocol modules;
- real-world scanner accuracy claims;
- SARIF and Burp Issue export targets.

These exclusions remain explicitly unverified/deferred rather than being reported as PASS.
