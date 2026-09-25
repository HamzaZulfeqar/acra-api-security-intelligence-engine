# Roadmap

## Current gate — 2026-09-25

Sprint 12 — **Reproduction & Standards Export** is SOFTWARE COMPLETE on
`s12-reproduction-standards-export`.

Phases 1–5 plus dedicated final closure are VERIFIED COMPLETE. Closure run: `36076016842`.

Current evidence boundary:

- deterministic review-only reproduction package;
- JSON export: VERIFIED;
- SARIF 2.1.0 export: VERIFIED;
- SARIF review/informational semantics: VERIFIED;
- Burp Issue-neutral projection: VERIFIED, `publishable=false`;
- Montoya 2026.7 AuditIssue adapter with explicit approval gate: VERIFIED;
- explicit headless publication service + SiteMap sink wrapper: VERIFIED;
- synchronized read-only Reproduction workspace/UI: VERIFIED;
- no publish/import/add-issue UI control: VERIFIED;
- endpoint/query/fragment and secret-bearing metadata hardening: VERIFIED;
- publication approval/receipt URL and integrity hardening: VERIFIED;
- candidate-package drift protection: VERIFIED;
- retained Core/Sprint 2/Sprint 3 regressions: VERIFIED;
- service remains unregistered from ACRAExtension bootstrap;
- raw principal/rationale/secret-bearing material excluded;
- deterministic final source package: VERIFIED — 1026 entries;
- closure-candidate ZIP SHA-256: `b451ee8c6a5c1cac2fc0177d24e3fd46fb3c2cb52c16ab856a21c3d6f8efbe9b`;
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

