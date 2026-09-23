# Roadmap

## Current gate — 2026-09-23

Sprint 5 software is complete on `s5-s6-completion` and must be checkpointed before Sprint 6 work begins.

Next dependency-ordered milestone:
1. Preserve the reproducible S5 ZIP + SHA-256.
2. Keep historical Burp/runtime debt separate from S5.
3. Start Sprint 6 on a new branch/checkpoint.
4. Sprint 6 scope: tenant isolation, advanced RBAC, role inheritance/multi-role policy, delegated administration, global-vs-tenant scope, explicit deny and policy-conflict reasoning.

Current gate: S5 SOFTWARE PARTIAL; S6 NOT STARTED. Source-backed remaining S5 work is in `docs/sprints/sprint-05-final-software-closure.md`. Previous short completion notes below are historical and do not supersede that audit.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).


S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.
