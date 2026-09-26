# Sprint 11 — Standalone Finding Lifecycle, Reproduction & Research Readiness

**Branch:** `s11-standalone-finding-lifecycle`  
**Base:** verified standalone Sprint 10 head `9ff4bab2e3f6dc11eef54b0e1877245b45060dfe`  
**Immutable Sprint 10 closure:** `c94ddc6cf32175694bb0fbffe4c4fc7ce24050c2`  
**Status:** PHASE 5 VERIFIED — PHASE 6 ACTIVE

## Product gap being closed

Sprint 10 intentionally kept vulnerability output review-only. Sprint 11 adds a human-reviewed finding lifecycle and
reproduction/export layer while preserving that evidence boundary.

No candidate becomes a confirmed finding automatically.

## Phase 1 — Core Finding Lifecycle Foundation — VERIFIED

Verified:
- deterministic reviewed-finding identity;
- only `FindingCandidateState.CANDIDATE` may enter;
- supporting evidence required;
- NEEDS_REVIEW → VALIDATED → CONFIRMED path;
- explicit FALSE_POSITIVE;
- ACCEPTED_RISK only after CONFIRMED;
- terminal-state immutability;
- severity and confidence independent of lifecycle state;
- secret-safe transition metadata.

Canonical run: `36214703722` — SUCCESS.

## Phase 2 — Persistent Standalone Finding Review Workspace — VERIFIED

Verified:
- source intake restricted to completed controlled DENY→ALLOW / UNEXPECTED_CHANGE executions;
- persisted ACTIVE_EXECUTION artifact + Core evidence chain required;
- deterministic evidence-backed candidate projection;
- existing `AuthorizationSeverityEvaluator` reused;
- project-isolated reviewed-finding persistence;
- idempotent source-run intake;
- source candidate/risk/fingerprint drift rejection;
- restart persistence;
- review transition evidence validation;
- secret-bearing reviewer/reason rejection;
- CSRF-protected localhost API;
- functional Reviewed Findings GUI;
- direct NEEDS_REVIEW → CONFIRMED rejection.

Canonical head: `d0213af7d9f7002ffe44f43a516f0f4d9e2f0bcc`.  
Canonical run: `36215309359` — SUCCESS.

## Phase 3 — Reproduction Package + Deterministic JSON — VERIFIED

Verified:
- minimized reproduction package from reviewed finding state;
- stable source evidence and execution lineage;
- reviewer identity/reason excluded;
- deterministic canonical JSON and SHA-256;
- no fabricated raw HTTP or credentials;
- confirmedFinding follows explicit lifecycle only;
- restart determinism.

Canonical run: `36215524311` — SUCCESS.

## Phase 4 — SARIF + Optional Burp Issue Draft — VERIFIED

Verified:
- SARIF 2.1.0 projection from the persisted reproduction package;
- deterministic rule IDs, finding fingerprints and SHA-256;
- lifecycle-aware review / fail / pass semantics;
- accepted-risk suppression semantics;
- false-positive non-confirmation semantics;
- Core Burp Issue Draft projection with independent severity/confidence mapping;
- optional Montoya materialization adapter;
- no automatic Burp publication;
- canonical serializer passthrough retained in Core so SARIF uses the same deterministic/redaction primitive.

Canonical head: `9344b37dfab2c9404539041811bf51356dcb531a`.  
Canonical run: `36247738000` — SUCCESS.

## Phase 5 — Standalone Review & Reproduction Product Surface — VERIFIED

Verified:
- existing Reviewed Findings workspace retains evidence-backed intake and lifecycle transitions;
- review history remains visible per finding;
- read-only JSON reproduction preview;
- read-only SARIF preview;
- read-only Burp Issue Draft preview;
- deterministic digest / draft identity surfaced to the user;
- reviewer identity and review reason remain excluded from reproduction exports;
- no Burp publication action exists in the standalone browser;
- lifecycle transitions remain evidence-gated and CSRF-protected.

Canonical head: `6e0b090b037f037982d61bd3f62a2a3bcc54d521`.  
Canonical run: `36248083390` — SUCCESS.

## Phase 6 — Research Ground-Truth Readiness — ACTIVE

Next implementation must:
- register a balanced local authorization ground-truth dataset;
- preserve an oracle independent of ACRA output;
- cover object, tenant, RBAC, workflow, routing, property, batch and indirect-reference dimensions where the current product supports them;
- verify secure and deliberately vulnerable localhost expectations;
- mark A0-A7 as SOFTWARE_READY / RESEARCH_NOT_RUN;
- report no precision/recall/F1 until a later research sprint actually executes the comparison.

## Current non-claims

- no automatic vulnerability confirmation;
- no external-target validation;
- no real-world scanner accuracy claim;
- no real Burp desktop publication claim;
- no A0-A7 measured research metrics.
