# Roadmap

## Current gate — 2026-09-25

Sprint 11 — **Research Evaluation & Ablation** is IN PROGRESS on
`s11-research-evaluation-ablation`.

Phases 1–7 are VERIFIED COMPLETE. Latest gate: GitHub Actions run `36064434979`.

Current evidence boundary:

- A0–A7 cumulative protocol is machine-defined and deterministic;
- controlled dataset is registered: 15 cases, 8 positive / 7 negative;
- ground-truth-free prediction adapters are verified;
- deterministic campaign denominator is fixed at 120 cells;
- campaign coverage is 120 PLANNED / 0 EXECUTED;
- experiment execution state remains NOT_RUN;
- no A0–A7 scientific result or real-world accuracy claim exists yet;
- fixture readiness is 15 READY / 0 PARTIAL / 0 MISSING_FIXTURE;
- treatment evidence projection is 120 EVIDENCE_READY / 0 EXECUTED;
- next dependency is ground-truth-free A0–A7 prediction execution; metrics remain deferred.

Sprint 10 remains the frozen SOFTWARE COMPLETE release base at
`59022c4a25718f38ea7ec2f010911344d1aa0698`.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.
