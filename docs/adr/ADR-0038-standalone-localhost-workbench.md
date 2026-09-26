# ADR-0038 — Standalone Localhost Workbench

## Status
Accepted for Sprint 10 implementation.

## Context
ACRA's verified security logic resides in ACRA Core while the existing interactive product surface is coupled to the Burp extension. Real Burp desktop runtime validation is a separate deferred lane. Requiring Burp for every user interaction also makes local project review, evidence triage and future headless use unnecessarily dependent on a third-party UI.

## Decision
ACRA will provide a standalone localhost product host as a separate Maven module.

The standalone host:
- depends on ACRA Core;
- binds to `127.0.0.1` by default;
- opens the default browser when supported;
- provides persistent local project and target onboarding;
- requires an authorization reference for every target;
- accepts only absolute HTTP(S) target URLs;
- rejects credentials embedded in target URLs;
- does not start a scan when a target is registered;
- exposes same-origin localhost APIs for the browser workbench;
- applies CSRF and Host-header checks;
- keeps Burp as an optional adapter rather than a prerequisite.

The first implementation slice deliberately does **not** duplicate authorization, workflow, routing or property analysis logic in the web layer. Those existing Core capabilities will be projected into the standalone host in subsequent dependency-ordered slices.

## Consequences
Positive:
- fresh-clone standalone path becomes possible;
- Burp runtime is no longer a product-start dependency;
- ACRA Core remains the single source of security-analysis logic;
- localhost UI and future CLI/CI hosts can share services;
- target onboarding has an explicit authorization boundary.

Tradeoffs:
- another deployable module must be tested and packaged;
- browser/server security becomes part of the attack surface;
- existing Core product-workspace projections require adapter work before every historical analysis view becomes functional.

## Security boundary
The standalone workbench is not an Internet-wide scanner. Target registration and future active testing remain separate operations. Active execution must continue to pass existing scope, consent, budget, concurrency, rate and safety controls.
