# Roadmap

## Current gate — 2026-09-25

Sprint 12 — **Reproduction & Standards Export** is IN PROGRESS on
`s12-reproduction-standards-export`.

Phase 1 is VERIFIED COMPLETE by GitHub Actions run `36068186039`.

Current evidence boundary:

- deterministic review-only reproduction package;
- JSON export: VERIFIED;
- SARIF 2.1.0 export: VERIFIED;
- SARIF review/informational semantics: VERIFIED;
- Burp Issue-neutral projection: VERIFIED, `publishable=false`;
- raw principal/rationale/secret-bearing material excluded;
- real Burp issue publication and desktop runtime remain UNVERIFIED / DEFERRED.

Sprint 11 remains the frozen SOFTWARE COMPLETE release base at
`61441818179fed4aa1c1a143960bef53b6df9a11`.

Canonical roadmap: [`docs/sprints/ROADMAP.md`](docs/sprints/ROADMAP.md).

---

## Historical top-level roadmap notes

S5-03 function-level authorization reasoning foundation added.

S5 Batch 1 evidence integrity source work is present: reference validation resolves against the existing evidence store and fails closed for unknown, redacted, contradictory, ownership, project, and replay-lineage failures. Compilation and focused execution remain blocked pending a Java 21 compiler. Checkpoint packaging is independently verified; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 2 offline policy validation source work is present for explicit tenant, workflow, property, and conflict review. Policy is never inferred from names or missing context. Java 21 compilation and focused execution remain blocked; S5 SOFTWARE PARTIAL and S6 NOT STARTED are unchanged.

S5 Batch 3 final integration review is complete at source level. Policy observations are authenticated against the existing evidence store before a policy result can be supported. Security review found no new secret-bearing fields or duplicate evidence/replay architecture. Java 21 compilation, focused tests, Maven regression, and exact JDK 21 runtime verification remain blocked or unverified; S5 SOFTWARE PARTIAL and S6 NOT STARTED remain unchanged.

