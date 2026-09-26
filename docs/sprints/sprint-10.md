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

## Phase 4 — Existing Core Authorization Engines → Standalone — VERIFIED

Verified capability:
- standalone context matrix projected into existing Core `AuthorizationPolicySnapshot`;
- existing `EffectiveAuthorizationResolver` used for policy resolution;
- configured binary ALLOW / DENY expectations resolve through Core policy rules;
- CONDITIONAL / UNKNOWN expectations remain non-binary and are not silently converted to grants;
- existing BOLA and BFLA evaluators invoked from the standalone projection;
- BOLA/BFLA correctly remain INCONCLUSIVE without authenticated observation/execution/evidence provenance;
- existing S6 authorization workspace receives the projected policy;
- existing S7 workflow, S8 routing and S9 property workspaces are projected into localhost readiness state;
- functional localhost Authorization workspace;
- effective roles, configured/resolved decision, policy-resolution state and reasons exposed;
- projection API and browser JavaScript syntax verified.

Canonical Phase 4 verification:
- verified code-bearing head: `642601902941ff857dd26919d329c1c6bfc58c07`;
- Sprint 10 workflow run `36209482608` — SUCCESS;
- `StandaloneCoreProjectionTestSuite` — PASS;
- `StandaloneCoreProjectionApiTestSuite` — PASS;
- browser JavaScript `node --check` — PASS;
- all retained Phase 1–3 standalone suites — PASS;
- packaged localhost application verification — PASS.

## Phase 5 — Evidence + Differential Comparison — VERIFIED

Verified capability:
- validated imports archived as project-isolated evidence only after scope checks pass;
- original content SHA-256 retained for provenance;
- only UniversalRedactor-processed content stored for browser inspection;
- HAR HTTP response samples persisted with lineage to evidence/project/target;
- raw HTTP imports represented as request samples without inventing responses;
- existing Core `PassiveDifferentialComparator` used for HTTP response comparison;
- RAW / NORMALIZED / STRUCTURAL / SEMANTIC comparison modes available;
- standalone authorization-context differential identifies changed context dimensions;
- out-of-scope import failure occurs before evidence archive mutation;
- Evidence workspace provides redacted preview, sample inventory and two differential workbenches;
- localhost evidence API never returns original secret-bearing bytes.

Canonical Phase 5 verification:
- verified code-bearing head: `8a340bf38fdaa58934d252abf6cdb2b8a30f14c3`;
- Sprint 10 workflow run `36209983492` — SUCCESS;
- `StandaloneEvidenceDifferentialTestSuite` — PASS;
- `StandaloneEvidenceApiTestSuite` — PASS;
- secret redaction / restart persistence / out-of-scope non-archival — PASS;
- browser JavaScript syntax — PASS;
- all retained Phase 1–4 suites — PASS;
- packaged localhost application — PASS.

## Phase 6 — Candidates + Coverage + Reports — VERIFIED

Verified capability:
- Core-backed FindingCandidate records remain INCONCLUSIVE until observed authorization evidence exists;
- analyst review state stored separately from Core candidate state;
- review states: NEW / UNDER_REVIEW / NEEDS_MORE_EVIDENCE / REJECTED;
- secret-bearing review notes rejected;
- endpoint coverage ledger: TESTED / UNTESTED / PARTIAL / INCONCLUSIVE / NOT_APPLICABLE;
- Phase 6 does not emit TESTED without authenticated active execution;
- deterministic standalone JSON and Markdown reports;
- repeated unchanged-state reports produce identical SHA-256 digests;
- `confirmedFindingCount = 0` enforced in report output;
- functional Candidates / Coverage / Reports localhost workspaces and APIs.

Canonical Phase 6 verification:
- verified code-bearing head: `84c09f2e60c94093fc653d8a5a17873c45a25d5d`;
- Sprint 10 workflow run `36210452986` — SUCCESS;
- `StandaloneReviewReportingTestSuite` — PASS;
- `StandaloneReviewReportingApiTestSuite` — PASS;
- all retained Phase 1–5 suites — PASS;
- browser JavaScript syntax — PASS;
- packaged localhost application — PASS.

## Phase 4 — Existing Core Authorization Engines → Standalone — VERIFIED

Verified capability:
- standalone Security Context projected into the existing ACRA `AuthorizationPolicySnapshot`;
- existing `EffectiveAuthorizationResolver` reused for configured ALLOW / DENY policy resolution;
- existing BOLA and BFLA evaluators reused without bypass logic;
- configured authorization resolves while BOLA/BFLA remain INCONCLUSIVE without execution provenance;
- existing S6 authorization workspace projected;
- existing S7 workflow workspace projected with zero runtime resolutions until observations exist;
- existing S8 routing workspace projected with zero runtime assessments until observations exist;
- existing S9 property workspace projected with zero runtime assessments until observations exist;
- functional localhost Authorization workspace;
- localhost Core projection API;
- browser JavaScript syntax verification in CI.

Canonical Phase 4 verification:
- verified code-bearing head: `642601902941ff857dd26919d329c1c6bfc58c07`;
- Sprint 10 workflow run `36209482608` — SUCCESS;
- `StandaloneCoreProjectionTestSuite` — PASS;
- `StandaloneCoreProjectionApiTestSuite` — PASS;
- browser `node --check` — PASS;
- packaged localhost application — PASS;
- all Phase 1–3 focused regressions retained — PASS.

## Phase 7 — Controlled Active Execution — VERIFIED

Verified capability:
- guarded standalone route-equivalence active validation over the existing ACRA Core active engine;
- loopback target restriction: localhost / 127.0.0.1 / ::1 only;
- registered target must be `LAB` + `CONTROLLED_LAB`;
- READ expectation with explicit ALLOW / DENY policy required;
- GET / HEAD / OPTIONS only in this Phase 7 slice;
- concrete path must remain inside the registered target base path and canonicalize to the selected inventory endpoint;
- trailing-slash representation is the only generated route mutation in the standalone Phase 7 UI;
- explicit operator confirmation required before execution;
- independent transient tested credential and known-ALLOW positive-control credential required;
- credentials are used in memory only and are not persisted or returned by localhost APIs;
- existing `MutationValidator`, hard scope, environment, consent, equivalence, budget, concurrency, rate-limit and kill-switch controls reused;
- existing `LocalhostHttpTransport` reused with redirects disabled;
- existing `TestExecutor` produces four-way differential evidence;
- controlled execution summaries persist with redacted ACTIVE_EXECUTION evidence lineage;
- kill switch blocks before dispatch and explicit reset is required;
- external/non-controlled targets fail closed;
- functional Active Validation localhost GUI and API.

Canonical Phase 7 verification:
- verified code-bearing head: `3125f71922e5c617bff62f35ac0b11d9f535fca7`;
- Sprint 10 workflow run `36212999470` — SUCCESS;
- `StandaloneControlledExecutionTestSuite` — PASS;
- `StandaloneControlledExecutionApiTestSuite` — PASS;
- browser JavaScript syntax — PASS;
- all retained Phase 1–6 standalone suites — PASS;
- packaged localhost application — PASS.

## Phase 8 — Optional Burp Bridge + Packaging Hardening — VERIFIED

Verified capability:
- optional Burp-to-standalone bridge client with loopback-only standalone management URL enforcement;
- health / CSRF handshake before explicit handoff;
- explicit RAW_HTTP transaction import into an already-selected standalone project/target;
- no automatic forwarding;
- bounded in-memory passive transaction retention in the Burp adapter;
- request serializer preserving Montoya raw request bytes when available;
- dedicated Burp `Standalone Bridge` tab with Probe / Open / Send Selected controls;
- official Montoya dependency build remains green;
- headless bridge contract verifies loopback enforcement, handshake, one-shot handoff, serialization and bounded retention;
- source startup launchers provide missing-tool diagnostics and `--no-build` mode;
- standalone startup reports Java runtime, loopback management bind, port fallback, optional Burp bridge and active-validation boundary;
- prebuilt distribution contains standalone JAR, optional Burp adapter JAR, Windows/Linux launchers, README and SHA-256 manifest;
- distribution ZIP checksum emitted;
- clean extraction verifies included JAR hashes;
- extracted standalone JAR starts successfully and reports healthy loopback service.

Canonical Phase 8 verification:
- verified head: `ffca14dd80b22d44a03c084311e69695f01112c1`;
- Sprint 10 workflow run `36213386173` — SUCCESS;
- official Montoya Burp adapter build — PASS;
- `Sprint10StandaloneBridgeTestSuite` — PASS;
- all retained standalone Phase 1–7 suites — PASS;
- distribution build — PASS;
- clean extraction / SHA-256 verification — PASS;
- extracted distribution startup — PASS.

## Current explicit non-claims

Sprint 10 is **not software-complete**.

Current standalone work does not yet claim:
- original secret-bearing import bytes are retained (only digest + redacted representation are stored);
- broad or unrestricted active scanning;
- non-loopback active execution from the standalone Phase 7 workbench;
- automatic endpoint crawling or network discovery;
- real Burp desktop runtime validation.

Security Context and Authorization are now functional. Workflow, Routing and Property use real Core workspace readiness but remain unpopulated until evidence exists. Evidence, Candidates, Coverage and Reports are the next standalone product layers.

Real Burp desktop runtime remains a separate **UNVERIFIED / DEFERRED** lane and is no longer a prerequisite for standalone product startup.

## Next dependency-ordered slice
1. Final Sprint 10 regression and deterministic closure checkpoint.

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
