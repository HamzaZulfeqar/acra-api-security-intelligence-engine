# Roadmap

## Current gate — 2026-09-25

Sprint 11 — **Research Evaluation & Ablation** is IN PROGRESS on
`s11-research-evaluation-ablation`.

Phase 1 is VERIFIED COMPLETE by GitHub Actions run `36056957706`.

Current evidence boundary:

- A0–A7 cumulative protocol is machine-defined and deterministic;
- required metrics are TP/TN/FP/FN, precision, recall, F1 and evidence completeness;
- experiment execution state remains NOT_RUN;
- no A0–A7 scientific result or real-world accuracy claim exists yet;
- Phase 2 must register the controlled evaluation dataset before execution.

Sprint 10 remains the frozen SOFTWARE COMPLETE release base at
`59022c4a25718f38ea7ec2f010911344d1aa0698`.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.
