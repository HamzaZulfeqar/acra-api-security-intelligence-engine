# Sprint 16 — Public Onboarding & Repository Hardening

**Date:** 2026-09-26  
**Branch:** `s16-public-onboarding-hardening`  
**Base:** published `main` at `10e34aa690bb0947bcb94198df0ce7b284d84cde`  
**Release:** `v0.3.0`  
**State:** IMPLEMENTATION IN PROGRESS

## Objective

Make the public ACRA repository understandable and usable from:

`git clone → build → load in Burp → verify passive operation`

while adding repeatable public-onboarding CI and documenting the remaining repository-admin hardening actions.

## Scope

### Repository content

- README Quick Start;
- official stable clone command;
- latest-main contributor clone command;
- build prerequisites and JAR location;
- Burp manual-install guide;
- first-run verification;
- troubleshooting;
- public release/checksum path;
- repository-hardening runbook;
- CODEOWNERS;
- public clone/build/release smoke CI.

### Product onboarding correctness

- startup message must identify `v0.3.0`;
- ACRA tab/load expectations must match source;
- normal scope behavior must be documented as `IN_SCOPE_ONLY`;
- active execution must remain documented as disabled by default.

### GitHub admin settings

Two settings require repository administration and cannot be changed by the connected GitHub automation used in this
sprint:
- protect `main`;
- enable Dependency Graph.

They are tracked in `docs/operations/GITHUB_REPOSITORY_HARDENING.md` and a GitHub issue.

## Definition of Done

Repository-side Sprint 16 implementation is complete when:
- onboarding verifier passes;
- Maven build passes;
- public branch clone/build smoke passes;
- published v0.3.0 JAR remains publicly downloadable with the recorded SHA-256;
- README/docs match actual extension behavior;
- PR security checks pass.

Full repository-hardening closure additionally requires owner-side confirmation that:
- `main` is protected;
- Dependency Graph is enabled;
- Dependency Review performs real analysis.
