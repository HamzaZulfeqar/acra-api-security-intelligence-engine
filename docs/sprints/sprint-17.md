# Sprint 17 — Standalone Localhost GUI

**Goal:** make ACRA usable without Burp as the primary runtime.

## Product requirement

Primary flow:

`git clone → one start command → browser opens localhost → submit/import authorized target data → automatic ACRA Core analysis → GUI results`

Burp remains an optional ingress/analyst adapter. It is not required to start the main ACRA GUI.

## Phase 1 vertical slice

Implemented:
- new Maven module `app/standalone`;
- localhost server bound to `127.0.0.1`;
- default port `8765`;
- browser auto-open when the desktop runtime supports it;
- `acra.sh` and `acra.ps1` launchers;
- standalone browser dashboard;
- target URL form with explicit authorization acknowledgement;
- one bounded read-only GET acquisition path;
- direct conversion to ACRA Core `HttpTransaction`;
- direct `SecurityContextEngine` analysis without Burp/Montoya;
- principal/role/tenant/resource/action/context/evidence output;
- in-memory analysis history;
- cloud metadata endpoint guard;
- 2 MiB response cap;
- localhost CI fixture proving Burp independence.

## Safety boundary

This first slice is not a crawler, brute-force scanner, credential tester, mutation engine, or automatic vulnerability
confirmation mechanism. It performs one authorized read-only GET per submitted URL and analyzes the observed exchange.

## Next phases

1. OpenAPI/Swagger import into endpoint inventory.
2. HAR import and multi-request analysis.
3. automatic context correlation across imported sessions.
4. policy/governance/finding workspaces in the web GUI.
5. evidence/reproduction/report downloads.
6. local project persistence.
7. optional Burp connector feeding the same standalone engine/UI.
8. packaged distribution so end users can start ACRA without Maven.

The published `v0.3.0` remains the stable Burp-centric release. This standalone path is post-`v0.3.0` development.
