# Roadmap

## Current gate — 2026-09-25

Sprint 13 — **Finding Lifecycle Governance** is IN PROGRESS on
`s13-finding-lifecycle-governance`.

Phases 1–5 are VERIFIED COMPLETE. Latest authoritative gate: GitHub Actions run
`36080369571` at `3ba4f96f8fbf43b0e025a280cd8763fd28d6f75a`.

Current evidence boundary:

- FindingCandidate remains distinct from a confirmed vulnerability;
- only explicit human-reviewed lifecycle transitions can create CONFIRMED;
- deterministic governance workspace and queues are verified;
- read-only governance UI and append-only history are verified;
- deterministic governance JSON/Markdown reporting is verified;
- lifecycle/report security hardening is verified;
- bounded 100 / 1,000 / 10,000 engineering observations are recorded;
- report/UI rendering performs no lifecycle transition or Burp publication;
- real Burp desktop runtime/publication remains UNVERIFIED / DEFERRED;
- next dependency is final Sprint 13 traceability, retained regression and reproducible closure.

Sprint 12 remains the frozen SOFTWARE COMPLETE release base at
`9ced79ba0986ce90884b745342769e88c38a68c2`.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.



## Sprint 13 current gate

Phase 1 lifecycle foundation: VERIFIED.  
Phase 2 governance workspace: VERIFIED by run `36077517602`.

Next dependency: read-only governance UI/history projection. No automatic lifecycle action or Burp publication is
permitted by this phase.


## Sprint 13 Phase 3

Read-only Finding Governance UI: VERIFIED by run `36079250303` (55 assertions).

Next dependency: deterministic governance audit/report export from immutable lifecycle snapshots. No UI lifecycle
transition or automatic publication is introduced by this step.


## Sprint 13 Phase 4

Deterministic governance JSON/Markdown reporting and read-only UI projection: VERIFIED.

- source gate: `36079738535`;
- evidence-retention gate: `36079852878`;
- reporting suite: 37 assertions;
- governance UI: 66 assertions.

Next dependency: security hardening and bounded performance observations before final closure.
