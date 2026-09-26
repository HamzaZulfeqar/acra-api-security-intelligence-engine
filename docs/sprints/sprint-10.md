# Sprint 10 — Standalone Localhost Security Workbench

## Objective
Make ACRA independently usable after a fresh Git clone without requiring Burp Suite for project creation, target onboarding, API-surface ingestion or local result review.

## Phase 1 — Standalone foundation — VERIFIED

Verified capability:
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

Canonical Phase 1 verification:
- standalone run `36207695944` — SUCCESS;
- retained Sprint 2 run `36207695914` — SUCCESS;
- retained Sprint 3 run `36207695811` — SUCCESS.

## Phase 2 — Import + Canonical API Inventory — VERIFIED

Verified capability:
- existing Core OpenAPI 3.x / Swagger 2.0 importer reused;
- conservative existing YAML subset retained with its documented limitations;
- new Core HAR endpoint importer;
- new Core raw HTTP request importer;
- imports bound to a selected registered/authorized target;
- imported scheme/host/port/base-path scope enforced fail-closed;
- 2 MiB decoded import-content limit;
- HAR entry ceiling of 10,000;
- raw HTTP Host-header consistency validation;
- concrete observed resource routes canonicalized through ACRA URI intelligence;
- OpenAPI declared templates normalized through the existing route-template engine;
- documented and observed endpoint records deduplicated into one project inventory;
- source provenance retained as OPENAPI / HAR / RAW_HTTP;
- observed response-status set retained;
- observation counts retained;
- documented-vs-observed state retained;
- project inventory persists across restart;
- functional localhost Import + API Inventory GUI;
- local file selection and pasted-content import;
- inventory filtering by method, host, route, source and response status;
- import operation remains offline and does not start active target interaction.

Canonical Phase 2 verification:
- verified code-bearing head: `32ed3b6843e653c4bf3e3d46a9b427e34fe32832`;
- Sprint 10 workflow run `36208517465` — SUCCESS;
- `Sprint10ImportFoundationTestSuite` — PASS;
- `StandaloneFoundationTestSuite` — PASS;
- `StandaloneImportInventoryTestSuite` — PASS;
- packaged localhost application verification — PASS;
- retained Core CI at import-foundation head `4b284bf9c25a215f971170a879b27b767d51d529`: run `36208244822` — SUCCESS;
- retained Sprint 2 at that Core head: run `36208244892` — SUCCESS;
- retained Sprint 3 at that Core head: run `36208244785` — SUCCESS.

## Phase 3 — Security Context Manager — VERIFIED

Verified capability:
- typed principal records with authentication-type metadata and no credential field;
- typed role and tenant records;
- typed resource records with owner-principal, tenant and state references;
- expected-authorization matrix bound to registered target + canonical inventory endpoint;
- explicit action and expected decision using ACRA Core enums;
- referential validation for principal / role / tenant / resource;
- duplicate logical IDs fail closed;
- unknown endpoint references fail closed;
- secret-bearing metadata rejected by the existing UniversalRedactor boundary;
- project-isolated persistent context store;
- functional localhost Security Context GUI;
- filtering of expected-authorization matrix;
- localhost context API for create/read workflows.

Canonical Phase 3 verification:
- verified code-bearing head: `1db56953725c358d6c60a3bb7abcb1e78c02ba8a`;
- Sprint 10 workflow run `36209126454` — SUCCESS;
- `StandaloneSecurityContextTestSuite` — PASS;
- `StandaloneSecurityContextApiTestSuite` — PASS;
- retained import/inventory and standalone foundation suites — PASS;
- packaged localhost application verification — PASS.

## Current explicit non-claims

Sprint 10 is **not software-complete**.

Current standalone work does not yet claim:
- persisted raw import files as formal Evidence objects;
- standalone projection of the existing authorization/workflow/routing/property assessment workspaces;
- candidate triage or final report generation from standalone mode;
- automatic endpoint crawling or network discovery;
- active vulnerability testing merely because a target or import exists.

Security Context is now functional. Authorization, Object Access, Function Access, Property Access, Workflow, Routing, Evidence, Candidates, Coverage and Reports remain product-shell placeholders until their existing Core services are projected through standalone APIs.

Real Burp desktop runtime remains a separate **UNVERIFIED / DEFERRED** lane and is no longer a prerequisite for standalone product startup.

## Next dependency-ordered slices
1. Projection of existing authorization/workflow/routing/property Core workspaces into standalone APIs.
2. Evidence and differential-comparison viewer with raw-import provenance.
3. Candidate triage, coverage and deterministic reporting.
4. Controlled active execution through existing ACRA safety gates.
5. Burp-to-standalone bridge and packaging hardening.

## Definition of Done for Phase 2
Phase 2 is verified only because CI proved:
- Maven reactor builds Core + standalone;
- OpenAPI/HAR/raw-HTTP Core import contracts pass;
- documented + observed route correlation passes;
- out-of-scope HAR import fails closed;
- inventory persistence passes;
- standalone foundation remains green;
- packaged JAR starts on loopback;
- GUI resources are served from the packaged application.
