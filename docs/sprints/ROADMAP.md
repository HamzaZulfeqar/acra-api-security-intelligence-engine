# ROADMAP

## Current gate — 2026-09-24

**Sprint 10 IN PROGRESS — Phase 1 VERIFIED COMPLETE.**

Canonical Sprint 10 Phase 1 evidence:
- branch: `s10-auth-session-intelligence`
- immutable Sprint 9 base: `9bffa59c1b360b3e74e0b5e97d2ce22a06dc73f5`
- Phase 1 verification run: `36031651777` — SUCCESS
- passive session/token-context model: VERIFIED
- verified-vs-unverified identity boundary: VERIFIED
- safe token rotation: VERIFIED
- principal/role/tenant/scope drift detection: VERIFIED
- retained S6/S7/S8/S9 foundations: PASS
- Maven core test compilation: PASS

Sprint 9 remains SOFTWARE COMPLETE.

Phase 2 verified by GitHub Actions run `36031909153`:
- session observation evidence-store binding: PASS;
- project/test/execution lineage validation: PASS;
- unknown/duplicate/cross-project/contradictory evidence rejection: PASS.

Next dependency-ordered milestone:
1. define controlled synthetic ACRA-Lab token/session rotation ground truth;
2. observe stable rotation and context-drift cases without raw credential persistence;
3. feed those observations through the verified Phase 1 analyzer and Phase 2 provenance gate;
4. preserve synthetic-lab authentication as non-production evidence.

Do not begin OAuth grant manipulation or external-target authentication testing in Phase 2.

Real Burp desktop runtime remains a separate UNVERIFIED / DEFERRED validation lane.

# ROADMAP

## Current gate — 2026-09-24

**Sprint 9 IN PROGRESS — Phases 1–8 VERIFIED COMPLETE; final closure pending.**

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
- Phase 8 verification run: `36002106088` — SUCCESS
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

Verified Phase 8:
- property metadata / provenance / mutation hardening: PASS;
- exact-one property BODY mutation safety: PASS;
- report non-confirmation boundary: PASS;
- 100 / 1,000 / 10,000 property-policy engineering observations: PASS;
- performance evidence artifact upload: PASS.

Final closure milestone:
1. freeze the Sprint 9 requirements traceability matrix;
2. execute full S9 + retained S8/S7/S6 foundations;
3. run official Maven package and retained Sprint 2/Sprint 3 regressions;
4. run S4/S6/S7/S8/S9 headless UI regressions;
5. generate deterministic Sprint 9 source checkpoint, manifest and SHA-256;
6. verify safe archive paths, no duplicate entries, clean extraction and per-file SHA-256 equality;
7. publish final software audit only after the dedicated closure gate passes.

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
