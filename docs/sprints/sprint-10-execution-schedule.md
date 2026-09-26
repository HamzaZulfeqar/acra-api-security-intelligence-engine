# Sprint 10 — Dependency-Ordered Execution Schedule

## Operating rule

Sprint 10 executes one phase at a time.

A phase may advance only when:
1. its implementation is complete enough to satisfy the phase Definition of Done;
2. its focused tests pass;
3. the packaged standalone application still starts and serves the localhost workbench;
4. retained compatibility gates affected by the phase remain green;
5. project state and roadmap are updated with exact evidence;
6. known limitations and non-claims are recorded.

No later phase may be represented as implemented merely because its navigation shell exists.

## Completed

### Phase 1 — Standalone Localhost Foundation — VERIFIED
Purpose:
- make Burp optional for ACRA startup and target onboarding.

Evidence:
- standalone workflow `36207695944` — SUCCESS;
- retained Sprint 2 `36207695914` — SUCCESS;
- retained Sprint 3 `36207695811` — SUCCESS.

### Phase 2 — Import + Canonical API Inventory — VERIFIED
Purpose:
- import authorized API-surface evidence locally and normalize it into a persistent project inventory.

Evidence:
- code-bearing head `32ed3b6843e653c4bf3e3d46a9b427e34fe32832`;
- standalone workflow `36208517465` — SUCCESS;
- documentation/package head workflow `36208617855` — SUCCESS.

### Phase 3 — Security Context Manager — VERIFIED
Goal:
- give the standalone workbench explicit, persisted authorization context instead of inferring everything from raw traffic.

Deliverables:
- principal management;
- role management;
- tenant management;
- resource + ownership management;
- authentication-type metadata without storing credentials;
- expected-authorization matrix bound to endpoint/action/context;
- referential validation across principal/role/tenant/resource records;
- ALLOW / DENY / CONDITIONAL / UNKNOWN expected decisions;
- project isolation;
- localhost Security Context workspace;
- focused persistence and API tests.

Definition of Done:
- context entities persist across restart;
- invalid references fail closed;
- no secret credential values are required or stored;
- expected-decision rows are deterministic and project-bound;
- GUI can create/review/filter context rows;
- packaged localhost application remains green.

### Phase 4 — Existing Core Authorization Engines → Standalone — VERIFIED
Dependencies:
- verified Phase 3 context records;
- verified Phase 2 inventory.

Deliverables:
- hydrate Core authorization context from standalone project state;
- project existing object/function/tenant/property/workflow/routing analyzers into localhost APIs;
- preserve Core result taxonomy and evidence boundaries;
- do not duplicate analyzer logic in web code.

Gate:
- focused Core-to-standalone projection tests + retained regression.

### Phase 5 — Evidence + Differential Comparison — VERIFIED
Dependencies:
- Phase 4 analysis projection.

Deliverables:
- persist raw import provenance safely;
- evidence lineage viewer;
- request/response structured comparison;
- context/authorization differential views;
- secret-safe rendering.

Gate:
- provenance integrity + redaction + project-isolation tests.

## Active

### Phase 6 — Candidates + Coverage + Reports
Dependencies:
- Phases 4–5.

Deliverables:
- review-only candidate triage;
- tested / untested / partial / inconclusive coverage;
- deterministic standalone reports;
- JSON / Markdown exports first;
- existing no-auto-confirmation boundary retained.

Gate:
- deterministic export hashes + candidate/coverage regressions.

### Phase 7 — Controlled Active Execution
Dependencies:
- explicit context, evidence and candidate workflows.

Deliverables:
- standalone request workbench;
- route every active request through existing ACRA scope/consent/budget/rate/concurrency/safety gates;
- no unrestricted HTTP bypass path;
- controlled ACRA-Lab execution first;
- explicit state-changing safety classification.

Gate:
- secure/vulnerable localhost fixtures + kill-switch/scope regressions.

### Phase 8 — Burp Bridge + Packaging Hardening
Dependencies:
- standalone workflows proven independently.

Deliverables:
- Send/Open in ACRA bridge;
- shared project/evidence handoff contract;
- Burp remains optional;
- Windows/Linux packaging hardening;
- startup diagnostics and recovery behavior.

Gate:
- headless adapter regressions; real Burp desktop remains separately labeled unless actually validated.

### Final Sprint 10 Closure
Required:
- full dependency-ordered regression;
- deterministic source checkpoint/package;
- clean extraction/per-file equality;
- exact limitations/deferred validation;
- roadmap/project-state freeze.

## Current next action

Execute **Phase 6 — Candidates + Coverage + Reports** only. Phase 7 does not open until the Phase 6 verification gate is green.
