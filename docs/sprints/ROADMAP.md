# ROADMAP

## Current gate — 2026-09-24

**Sprint 10 IN PROGRESS — Phases 1–7 VERIFIED COMPLETE.**

Canonical Sprint 10 Phase 1 evidence:
- branch: `s10-batch-indirect-authorization`
- immutable Sprint 9 base: `ce81220eb9ea41009973b4072c08d59927ee8c6b`
- Phase 1 verification run: `36004146212` — SUCCESS
- Phase 2 verification run: `36004574331` — SUCCESS
- Phase 3 verification run: `36048381112` — SUCCESS at `608490cfc1211057e879f6c6457ea62fd648b405`
- Phase 4 verification run: `36048837246` — SUCCESS at `abb57d0320fcdc161992297415978bde6c769bee`
- Phase 5 verification run: `36049247018` — SUCCESS at `16169367c3e7c3b179a6d8541dfc88698fde6b81`
- Phase 6 verification run: `36051856008` — SUCCESS at `a33aaffccce80ba8251284a0b4de70f2f76e7a8a`
- Phase 7 verification run: `36054653296` — SUCCESS at `09d4edb23014c738d6856d46db6e0a72ed5a9e97`
- per-item batch authorization reasoning: VERIFIED
- mixed batch outcomes preserved: VERIFIED
- indirect reference fingerprint/resolution reasoning: VERIFIED
- raw indirect key storage excluded: VERIFIED
- cross-project provenance rejection: VERIFIED
- Maven core test compilation: PASS

Sprint 9 remains SOFTWARE COMPLETE and its post-documentation final closure revalidation is PASS.

Roadmap reconciliation:
- historical S10 = Property / Batch / Indirect Authorization;
- Property = completed in Sprint 9;
- S10 current scope = remaining Batch + Indirect Authorization.

Verified Phase 2:
- fixed localhost batch/indirect ground truth: PASS;
- per-item batch authorization versus aggregate HTTP status: PASS;
- secure/vulnerable resolved-target authorization: PASS;
- authentication controls and non-persistent batch fixture: PASS;
- lab evidence upload: PASS.

Verified Phase 3:
- existing S4 BATCH / INDIRECT_REFERENCE contracts and mutation families reused: PASS;
- fixed policy-backed Sprint 10 seeds only: PASS;
- exact request-equivalence and existing S4 safety stack retained: PASS;
- controlled batch execution: secure candidates=0, vulnerable candidates=1;
- controlled indirect execution: secure DENY/candidates=0, vulnerable ALLOW/candidates=1;
- raw indirect alias excluded from persisted resolution state: PASS;
- cross-project provenance fail-closed behavior: PASS;
- no identifier guessing, alias enumeration or external-target probing: PASS.

Verified Phase 4:
- batch finding projection with provenance revalidation: PASS;
- indirect finding projection with provenance revalidation: PASS;
- secure controls → REJECTED: PASS;
- verified DENY→ALLOW → review-only CANDIDATE: PASS;
- cross-project and request mismatch → INCONCLUSIVE: PASS;
- deterministic candidate identity/fingerprint: PASS;
- raw indirect alias excluded from projected candidate: PASS;
- automatic confirmed-vulnerability state: NOT PRESENT.

Verified Phase 5:
- explicit combined batch/indirect policy coverage universe: PASS;
- batch vs indirect family counts: PASS;
- unobserved and observed-unassessed gaps: PASS;
- candidate/rejected/inconclusive assessed states: PASS;
- deterministic coverage ordering and ratios: PASS;
- aggregate HTTP success excluded from coverage inference: PASS.

Verified Phase 6:
- read-only Sprint 10 product workspace and immutable snapshot: PASS;
- Batch & Indirect top-level Burp tab: PASS;
- Overview / Policies / Observations / Assessments / Candidates / Coverage: PASS;
- raw indirect alias exclusion from all UI tables: PASS;
- candidate != confirmed vulnerability: PASS;
- aggregate HTTP success != per-item authorization: PASS;
- retained S6/S7/S8/S9 UI regressions: PASS;
- Sprint 10 headless UI: PASS, 218 assertions;
- real Burp desktop runtime remains UNVERIFIED / DEFERRED.

Verified Phase 7:
- versioned canonical Sprint 10 report model: PASS;
- report-specific minimization projections: PASS;
- raw indirect aliases / policySource / candidate rationale excluded: PASS;
- canonical JSON + stable SHA-256: PASS;
- deterministic Markdown: PASS;
- Reporter plugin adapter: PASS;
- Report / JSON Export UI projection: PASS;
- `confirmedFindingCount = 0`: PASS;
- unobserved coverage retained: PASS;
- Sprint 10 reporting suite: PASS, 43 assertions;
- Sprint 10 UI: PASS, 230 assertions.

Next dependency-ordered milestone:
1. add adversarial security-hardening tests across batch and indirect input/provenance/report boundaries;
2. verify workspace dimension rejection, coverage drift rejection and report non-confirmation invariants;
3. record bounded engineering observations for 100 / 1,000 / 10,000 explicit policy contexts;
4. measure workspace/coverage population and deterministic report generation without asserting benchmark/SLO claims;
5. archive performance observation evidence for final Sprint 10 closure.

No identifier guessing, alias enumeration or external-target probing is permitted.

Real Burp desktop runtime remains a separate UNVERIFIED / DEFERRED validation lane.

Historical phase evidence retained below.

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

Final closure status:
- dedicated Sprint 9 final verification: PASS;
- deterministic source checkpoint: PASS;
- archive safe paths / duplicates / clean extraction / per-file equality: PASS;
- closure-candidate source ZIP SHA-256: `04eaadbfb2e132eb386ab52ff775fb5e1c56f0313b610721a28f2fb63bf8e531`;
- Sprint 10 remains NOT STARTED by this closure.

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
