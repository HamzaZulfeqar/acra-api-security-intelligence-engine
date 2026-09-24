# ROADMAP

## Current gate — 2026-09-24

**Sprint 9 IN PROGRESS — Phases 1–7 VERIFIED COMPLETE.**

Canonical Sprint 9 Phase 1 evidence:
- branch: `s9-property-authorization`
- immutable Sprint 8 base: `21635d900ad80cd27e4f9212b8448bbf5b4cd7f2`
- Phase 1 verification run: `35985518829` — SUCCESS
- Phase 2 verification run: `35986020761` — SUCCESS
- Phase 3 verification run: `35986668193` — SUCCESS
- Phase 4 verification run: `35986888660` — SUCCESS
- Phase 5 verification run: `35987236797` — SUCCESS
- Phase 6 verification run: `36001111468` — SUCCESS
- Phase 7 verification run: `36001743072` — SUCCESS
- evidence-backed property observations: VERIFIED
- deterministic property-policy correlation: VERIFIED
- cross-project provenance rejection: VERIFIED
- missing/ambiguous policy fail-closed behavior: VERIFIED
- retained S5/S6/S7/S8 foundations: PASS
- Maven core test compilation: PASS

Sprint 8 remains SOFTWARE COMPLETE.

Verified Phase 2:
- five-case controlled property ground truth: PASS;
- secure/vulnerable localhost READ exposure behavior: PASS;
- secure/vulnerable privileged UPDATE behavior: PASS;
- positive allowed-property UPDATE control: PASS;
- cross-object control remains DENY: PASS.

Verified Phase 3:
- existing PROPERTY contract/mutation family reused;
- state-changing BODY mutation routed through S4 safety/execution stack;
- secure explicit DENY observed DENY: PASS;
- vulnerable explicit DENY observed ALLOW: PASS;
- live executor evidence → property policy assessment: PASS;
- cross-project provenance rejection: PASS.

Verified Phase 4:
- property-specific FindingCandidate projector: PASS;
- secure verified control → REJECTED: PASS;
- verified DENY→ALLOW property mismatch → CANDIDATE: PASS;
- cross-project provenance → INCONCLUSIVE: PASS;
- no automatic confirmed-vulnerability state: PASS.

Verified Phase 5:
- explicit property-policy coverage universe: PASS;
- READ/UPDATE separation: PASS;
- unobserved and observed-unassessed states: PASS;
- candidate/rejected/inconclusive assessed states: PASS;
- deterministic coverage snapshot: PASS.

Verified Phase 6:
- S9 property product workspace and immutable snapshot: PASS;
- Burp Properties area with Overview / Policies / Observations / Assessments / Candidates / Coverage: PASS;
- retained S4/S6/S7/S8 headless UI regressions: PASS;
- property values excluded from UI projection: PASS;
- real Burp desktop runtime remains separately UNVERIFIED / DEFERRED.

Verified Phase 7:
- deterministic property report model: PASS;
- canonical JSON/SHA-256 and Markdown export: PASS;
- Reporter plugin adapter: PASS;
- report/UI coverage-gap preservation: PASS;
- confirmedFindingCount fixed at 0: PASS;
- property values excluded from report/export views: PASS.

Next dependency-ordered milestone:
1. security-hardening suite for property metadata, policy ambiguity, provenance isolation and mutation safety;
2. bounded 100 / 1,000 / 10,000 property-policy workspace/report observations;
3. write performance CSV as engineering evidence only, not benchmark/SLO claims;
4. retain S5/S6/S7/S8 core and S4/S6/S7/S8/S9 UI regressions;
5. only after Phase 8 passes, perform final traceability, packaging and Sprint 9 software audit.

Do not claim Burp desktop validation from headless UI tests.

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
