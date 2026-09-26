# Sprint 10 — Standalone Localhost Security Workbench

## Objective
Make ACRA independently usable after a fresh Git clone without requiring Burp Suite for project creation, target onboarding or access to the product shell.

## Phase 1 — Standalone foundation

Implemented candidate scope:
- new `standalone` Maven module linked to `acra-core`;
- shaded executable `acra-standalone.jar`;
- localhost-only Java HTTP server;
- automatic browser launch where supported;
- persistent local project store;
- persistent authorized target store;
- HTTP(S)-only target validation;
- embedded-credential rejection;
- explicit authorization-reference requirement;
- declared environment and testing mode;
- no automatic scan on target registration;
- CSRF protection for state-changing localhost APIs;
- Host-header validation for DNS-rebinding resistance;
- Content Security Policy and response hardening;
- ACRA Core capability projection;
- browser product shell with target-management UI;
- Linux/macOS and Windows launchers;
- dedicated CI verification workflow.

## Explicit non-claims
Phase 1 does not claim that all historic ACRA analysis workspaces are already connected to the standalone host.
The Authorization, Object Access, Function Access, Property Access, Workflow, Routing, Evidence, Candidates, Coverage and Reports navigation entries are product-shell placeholders until their existing Core services are projected through standalone APIs.

Phase 1 does not perform network discovery or active vulnerability testing merely because a target is registered.

Real Burp desktop runtime remains a separate UNVERIFIED / DEFERRED lane and is no longer a prerequisite for standalone product startup.

## Next dependency-ordered slices
1. OpenAPI/HAR/raw HTTP import and canonical API inventory.
2. Security-context management: principals, roles, tenants, ownership and policies.
3. Projection of existing authorization/workflow/routing/property Core workspaces into standalone APIs.
4. Evidence and differential-comparison viewer.
5. Candidate triage, coverage and deterministic reporting.
6. Controlled active execution through existing ACRA safety gates.
7. Burp-to-standalone bridge and packaging hardening.

## Definition of Done for Phase 1
Phase 1 is verified only after CI proves:
- Maven reactor builds Core + standalone;
- standalone foundation suite passes;
- packaged JAR starts on loopback;
- health endpoint reports standalone/Core linkage and Burp independence;
- browser workbench resource is served;
- local project persistence works;
- authorized target persistence works;
- unsafe URL forms are rejected;
- CSRF protection is enforced.
