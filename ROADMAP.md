# Roadmap

## Current gate — 2026-09-25

Sprint 13 — **Finding Lifecycle Governance** is IN PROGRESS on
`s13-finding-lifecycle-governance`.

Phase 1 is VERIFIED COMPLETE by GitHub Actions run `36077162629`.

Current evidence boundary:

- governed lifecycle is explicitly defined;
- entry state is REVIEW_REQUIRED;
- only explicit human-review transitions can produce CONFIRMED;
- severity and confidence remain independent from lifecycle state;
- CRITICAL prioritization cannot auto-confirm;
- lifecycle history is deterministic and append-only;
- invalid/stale/tampered transitions fail closed;
- next dependency is governance workspace / review-queue integration.

Sprint 12 remains the frozen SOFTWARE COMPLETE release base at
`9ced79ba0986ce90884b745342769e88c38a68c2`.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.

