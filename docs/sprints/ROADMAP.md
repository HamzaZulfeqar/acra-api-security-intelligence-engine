# ROADMAP

## Current gate — 2026-09-24

**Sprint 8 SOFTWARE COMPLETE.**

Canonical closure evidence:
- branch: `s8-routing-normalization`
- final closure run: `35971753523` — SUCCESS
- closure source commit: `f6a0c19358b00672711532ec7effe1eed3800e3e`
- closure-candidate checkpoint SHA-256: `e063217d3c8795a59ce1cd7e052a2c9b0475a86836239973ef1d470a6c627af7`
- 836 entries, 0 unsafe paths, 0 duplicate entries
- clean extraction and per-file SHA-256 equality: PASS
- official Maven package and retained S2/S3/S4/S6/S7/S8 verification: PASS

Sprint 7 remains frozen at `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`.

**Sprint 9 is NOT STARTED.** No Sprint 9 implementation is included in the Sprint 8 closure checkpoint.

Real Burp desktop runtime remains a separate UNVERIFIED / DEFERRED validation lane.

The historical roadmap entries below are retained for provenance.

## Current gate — 2026-09-09

S5 SOFTWARE PARTIAL. Defensive guard/correlation/serialization work is verified by the current test artifact. Missing context normalization/binding and tenant/workflow/property/finding/severity/orchestration/end-to-end work remain explicit in `sprint-05-final-software-closure.md`. S6 has not started. The earlier completion list below is historical and is superseded by the current source audit.

## Sprint 5

Completed:
- S5-01 Authorization Context Foundation
- S5-02 Object-Level Authorization Reasoning Foundation

Pending:
- Future authorization reasoning slices


S5-04: Evidence correlation aggregation foundation added.


## Sprint 7 — in progress

Scope:
- workflow authorization
- transition policy/state reasoning
- separation of duties and approval conditions
- S6 delegation reuse
- credential-safe token-context binding
- controlled WORKFLOW_TRANSITION planning/execution
- workflow UI/reporting/research validation

Phase 1 foundation is implemented and verified on `s7-workflow-token-binding` by run `35894118859` (19 focused S7 assertions + retained S6 foundation PASS).

### Sprint 7 verified progression — 2026-09-24

- Phase 1 workflow/token-binding foundation: COMPLETE
- Phase 2 assessment/finding/risk + ground truth: COMPLETE
- Phase 3 controlled WORKFLOW_TRANSITION planning/execution: COMPLETE
- Phase 4 deterministic workflow coverage + Burp product UI: COMPLETE
- Next dependency: deterministic workflow report/export, then security/performance and final closure
