# Sprint 11 — Standalone Finding Lifecycle, Reproduction & Research Readiness

## Objective

Continue from the verified Sprint 10 standalone product boundary by converting evidence-backed review candidates into
human-reviewed, reproducible finding records without introducing automatic vulnerability confirmation.

Immutable Sprint 10 software closure:
- source commit: `c94ddc6cf32175694bb0fbffe4c4fc7ce24050c2`
- final closure run: `36213547092` — SUCCESS
- current post-closure standalone head used as Sprint 11 branch base: `9ff4bab2e3f6dc11eef54b0e1877245b45060dfe`

Historical Sprint 11 work from divergent branches is reference material only. It must not be merged blindly.

## Execution rule

One phase at a time. The next phase opens only after:
1. focused tests pass;
2. retained Sprint 10 standalone regression stays green;
3. packaged localhost application remains healthy;
4. source-of-truth docs record exact evidence and residual boundaries.

## Phase 1 — Core Finding Lifecycle Foundation — VERIFIED

Deliverables:
- `FindingLifecycleState`;
- `FindingReviewTransition`;
- `ReviewedFinding`;
- `FindingLifecycleService`;
- deterministic finding identity;
- only `FindingCandidateState.CANDIDATE` may enter;
- mandatory supporting evidence;
- `NEEDS_REVIEW → VALIDATED → CONFIRMED`;
- explicit `FALSE_POSITIVE`;
- `ACCEPTED_RISK` only after CONFIRMED;
- severity/confidence immutable through lifecycle;
- secret-safe review metadata.

Gate:
- Sprint11 lifecycle foundation suite;
- Maven Core compile/test-compile;
- retained Sprint 10 workflow compatibility.

## Phase 2 — Persistent Standalone Finding Review Workspace — VERIFIED

Deliverables:
- project-isolated reviewed-finding persistence;
- idempotent intake from Sprint 10 candidate IDs;
- risk/candidate consistency;
- review transitions over localhost API;
- no reviewer credentials or raw secret material persisted;
- standalone Findings workspace.

Gate:
- restart persistence;
- cross-project fail-closed behavior;
- direct candidate→confirmed rejection;
- CSRF API verification.

## Phase 3 — Reproduction Package + Deterministic JSON — VERIFIED

Deliverables:
- minimized reproduction model;
- finding/candidate/provenance references;
- expected/observed decisions;
- review trail without reviewer identity leakage;
- canonical JSON + SHA-256;
- no fabricated raw HTTP.

## Phase 4 — SARIF + Optional Burp Issue Draft — ACTIVE

Deliverables:
- SARIF 2.1.0 projection;
- deterministic rule/fingerprint mapping;
- confirmed / false-positive / accepted-risk semantics;
- Core Burp Issue draft;
- optional Montoya materialization adapter;
- no automatic Burp publication.

## Phase 5 — Standalone Review & Reproduction Product Surface

Deliverables:
- Findings;
- Review History;
- JSON Reproduction;
- SARIF;
- Burp Issue Draft;
- explicit review actions gated by evidence and CSRF;
- no automatic confirmation.

## Phase 6 — Research Ground-Truth Readiness

Deliverables:
- balanced controlled local authorization dataset;
- independent secure/vulnerable oracle;
- object / tenant / RBAC / workflow / routing / property / batch / indirect dimensions where supported;
- A0-A7 registry marked SOFTWARE_READY / RESEARCH_NOT_RUN.

## Phase 7 — Security / Performance / Final Closure

Deliverables:
- secret-hardening regressions;
- deterministic export identity;
- bounded engineering observations;
- full Sprint 10 regression;
- deterministic source checkpoint;
- clean extraction/per-file equality;
- exact deferred boundaries.

## Explicit boundaries

Sprint 11 does not claim:
- automatic vulnerability confirmation;
- external-target validation;
- real-world scanner accuracy;
- real Burp desktop publication unless separately executed and verified;
- A0-A7 measured metrics until a later research sprint actually runs them.
