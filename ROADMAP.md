# Roadmap

## Current gate — 2026-09-25

Sprint 12 — **Reproduction & Interoperability Exports — IN PROGRESS** on
`s12-reproduction-interoperability-exports`.

Phases 1–2 are VERIFIED COMPLETE. Latest gate: GitHub Actions run `36122759287`.

Current Sprint 12 boundary:

- deterministic review-only reproduction package: VERIFIED;
- canonical JSON + SHA-256: VERIFIED;
- raw principal replaced by fingerprint: VERIFIED;
- candidate rationale structurally omitted: VERIFIED;
- candidate state / severity / confidence kept separate: VERIFIED;
- SARIF 2.1.0: VERIFIED;
- Burp Issue adapter: NOT YET IMPLEMENTED;
- real Burp desktop runtime: UNVERIFIED / DEFERRED.

Sprint 11 — **Research Evaluation & Ablation — SOFTWARE COMPLETE** on
`s11-research-evaluation-ablation`.

Dedicated final closure: GitHub Actions run `36066400016` — SUCCESS at closure-candidate source
`f832af1defde242530408589bd9f8732cef533d5`.

Current evidence boundary:

- A0–A7 protocol, controlled dataset, fixture layer, evidence collection, prediction execution, evaluation and reporting are verified;
- 15 controlled synthetic localhost cases: 8 positive / 7 negative;
- prediction campaign: 120 EXECUTED cells;
- A0–A5: TP8/TN0/FP7/FN0, precision .533333, recall 1, F1 .695652;
- A6–A7: TP8/TN7/FP0/FN0, precision/recall/F1 = 1 within the controlled dataset;
- evidence completeness = 1.0 for A0–A7;
- closure ZIP SHA-256: `6d14693aa4056ee149c4c2f9496f7bb3d044783c7962bdf43f4453e8f2c3aaba`;
- archive entries: 986; safe paths / duplicate detection / clean extraction / per-file SHA-256 equality: PASS;
- deterministic JSON/Markdown research reporting is verified and archived;
- real Burp desktop runtime remains UNVERIFIED / DEFERRED;
- controlled measurements are not real-world scanner-accuracy or novelty claims.

Sprint 10 remains the frozen previous SOFTWARE COMPLETE release base at
`59022c4a25718f38ea7ec2f010911344d1aa0698`.

Sprint 12 was subsequently started from the verified Sprint 11 closure head after a requirements-gap review. Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.
