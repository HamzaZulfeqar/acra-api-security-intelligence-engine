# Roadmap

## Current gate — 2026-09-23

Sprint 5 is preserved as the immutable base and Sprint 6 is now **SOFTWARE COMPLETE** for its defined authorized local/synthetic scope.

Verified Sprint 6 checkpoint:
- branch: `s6-final-software-2026-09-23`
- commit: `8d996a2b2b1bb47372490945aec197dd9e20ff36`
- final workflow: `35891427324` SUCCESS
- final ZIP SHA-256: `a26a20758e4c2e6db98995d2b6ec64e4bb81d3b1b5e76b4cf54567994e8fb724`

Next dependency-ordered milestone: **Sprint 7 — NOT STARTED**. Workflow authorization, delegation/token-binding expansion and later roadmap modules must start from the frozen S6 checkpoint.

Historical S5/S6 planning notes below are retained only as history and do not supersede this gate.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).


S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.
