# ROADMAP

## Current gate — 2026-09-24

**Sprint 7 SOFTWARE COMPLETE** and frozen at commit `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`.

Canonical Sprint 7 closure:
- GitHub Actions run `35961314354`
- checkpoint SHA-256 `5e77654d6467cc45c60825cb44bd7c3aa1d4f33c8705d41216d7f258a8187189`
- 791 entries, 0 unsafe paths, clean/per-file equality PASS

**Sprint 8 IN PROGRESS** on `s8-routing-normalization`.

Sprint 8 scope: **Routing Normalization & Authorization-Path Intelligence**.

Dependency order:
1. Phase 1 — evidence-backed staged normalization trace foundation;
2. controlled multi-stage route fixtures and provenance import;
3. authorization-path divergence reasoning without vulnerability promotion;
4. guarded route-variant planning only after explicit safety/equivalence contracts;
5. product UI/reporting/coverage;
6. security/performance validation and final closure.

Current Phase 1 remains a candidate until its dedicated Java 21 CI and retained Sprint 7 foundation gates pass.

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
