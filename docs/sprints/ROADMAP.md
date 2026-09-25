# ROADMAP

## Current gate — 2026-09-25

**Sprint 12 SOFTWARE COMPLETE — final closure verified.**

Canonical Sprint 12 evidence:
- branch: `s12-reproduction-standards-export`;
- immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`;
- Sprint 11 post-documentation final closure: `36067012832` — SUCCESS;
- Sprint 11 final-status ZIP SHA-256: `ea3d65bbce6134dee6e03b75939b57b7b7cf26cf9988d22b17abef91aeb3622f`;
- Phase 1 verification: `36068186039` — SUCCESS at `a8d442aa95c5d8946035586cf2b212e27f9ebf79`;
- Phase 2 verification: `36068996138` — SUCCESS at `e57c8dbb0e62b4f1acc59210ff04ee8e8333b85a`;
- Phase 3 verification: `36069442716` — SUCCESS at `45554b8c912f9f5fb23b39267606c3fb5dd110a6`;
- Phase 4 verification: `36070071498` — SUCCESS at `a3328f7df32391f6aaa4a2f867de8590ba6fab52`;
- Phase 5 verification: `36075554684` — SUCCESS at `01aa13abd0385d6976eae15f583fd956afe69f5e`;
- final closure: `36076016842` — SUCCESS at `12916add6d3d9005746fac347904abb0cf678c11`;
- closure-candidate ZIP SHA-256: `b451ee8c6a5c1cac2fc0177d24e3fd46fb3c2cb52c16ab856a21c3d6f8efbe9b`;
- closure-candidate entries: 1026;
- standards foundation: PASS, 34 assertions;
- deterministic JSON reproduction export: PASS;
- deterministic SARIF 2.1.0 review export: PASS;
- non-publishable Burp Issue projection: PASS;
- real Burp issue publication: UNVERIFIED / NOT ENABLED.

Verified Phase 2:
- real Montoya 2026.7 AuditIssue compile boundary: PASS;
- explicit approval/candidate binding: PASS;
- absolute publication URL validation: PASS;
- INFORMATION/TENTATIVE mapping: PASS;
- publication method absent: PASS;
- real Burp publication: UNVERIFIED / NOT ENABLED.

Verified Phase 3:
- explicit approval-gated publication service: PASS;
- injected factory/sink boundary: PASS;
- denied/mismatch → zero publication side effects: PASS;
- approved → exactly one headless sink add: PASS;
- deterministic IMPORTED_REVIEW_CANDIDATE receipt: PASS;
- ACRAExtension auto-wiring absent: PASS;
- real desktop SiteMap publication: UNVERIFIED / DEFERRED.

Verified Phase 4:
- synchronized reproduction workspace: PASS;
- deterministic JSON/SARIF/Burp product projections: PASS;
- read-only Reproduction UI: PASS, 34 assertions;
- publication receipts separated from candidate/package state: PASS;
- publish/import/add-issue UI control absent: PASS;
- real desktop publication: UNVERIFIED / DEFERRED.

Verified Phase 5:
- endpoint query/fragment rejection: PASS;
- secret-bearing package metadata rejection: PASS;
- candidate-package drift rejection + idempotency: PASS;
- publication URL userinfo/query/fragment/scheme rejection: PASS;
- approval/receipt secret checks: PASS;
- receipt identity/fingerprint/state hardening: PASS;
- core hardening suite: PASS, 14 assertions;
- publication hardening suite: PASS, 14 assertions;
- retained Core / Sprint 2 / Sprint 3 workflows: PASS;
- real desktop SiteMap publication: UNVERIFIED / DEFERRED.

Final Sprint 12 closure:
- complete Sprint 12 + retained release/regression lanes: PASS;
- canonical JSON/SARIF artifacts: PASS;
- official Maven package: PASS;
- deterministic source checkpoint: PASS;
- unsafe paths / duplicate entries: 0 / 0;
- clean extraction / per-file hash equality: PASS / PASS;
- real Burp desktop SiteMap publication: UNVERIFIED / DEFERRED.

No later sprint scope is pre-claimed by this closure. The next roadmap item must be selected from remaining verified
backlog/debt on a separate branch.

## Previous release boundary — Sprint 11

**Sprint 11 SOFTWARE COMPLETE — Phases 1–10 plus dedicated final closure VERIFIED.**

Canonical Sprint 11 evidence:
- branch: `s11-research-evaluation-ablation`;
- immutable Sprint 10 base: `59022c4a25718f38ea7ec2f010911344d1aa0698`;
- Phase 1 run: `36056957706` — SUCCESS;
- Phase 2 run: `36057476360` — SUCCESS;
- Phase 3 run: `36057869888` — SUCCESS;
- Phase 4 run: `36058274408` — SUCCESS;
- Phase 5 run: `36058945211` — SUCCESS;
- Phase 6 run: `36060118721` — SUCCESS;
- Phase 7 run: `36064434979` — SUCCESS;
- Phase 8 run: `36064798175` — SUCCESS;
- Phase 9 run: `36065439110` — SUCCESS;
- Phase 10 run: `36065934911` — SUCCESS;
- dedicated final closure run: `36066400016` — SUCCESS at closure-candidate source `f832af1defde242530408589bd9f8732cef533d5`.

Final closure:
- complete Sprint 11 verifier: PASS;
- controlled research fixtures: PASS, 65 assertions;
- treatment evidence collection: PASS, 725 assertions;
- prediction execution: PASS, 384 assertions;
- controlled ablation evaluation: PASS, 99 assertions;
- research report/export: PASS, 31 assertions;
- retained Sprint 10 / 9 / 8 / 7 / 6 foundations: PASS;
- official Maven package: PASS;
- Sprint 2 / Sprint 3 regressions: PASS;
- retained Sprint 4 / 6 / 7 / 8 / 9 / 10 headless UI suites: PASS;
- deterministic source checkpoint: PASS;
- closure-candidate ZIP SHA-256: `6d14693aa4056ee149c4c2f9496f7bb3d044783c7962bdf43f4453e8f2c3aaba`;
- archive entries: 986;
- unsafe paths: 0;
- duplicate entries: 0;
- clean extraction equality: PASS;
- per-file SHA-256 equality: PASS.

Final controlled research state:
- registered dataset: 15 synthetic localhost cases, 8 positive / 7 negative;
- fixture readiness: 15 READY / 0 PARTIAL / 0 MISSING_FIXTURE;
- treatment evidence: complete for all A0–A7 cells;
- prediction campaign: 120 EXECUTED cells;
- A0–A5: TP=8, TN=0, FP=7, FN=0, precision=.533333, recall=1, F1=.695652;
- A6–A7: TP=8, TN=7, FP=0, FN=0, precision=1, recall=1, F1=1;
- evidence completeness: 1.0 for A0–A7;
- report JSON SHA-256: `595dba16b9d724d67dbdd1dc4faeacd76432661b52ead780b14054acee67c04d`;
- report Markdown SHA-256: `1d4c6b2f9f7f88f443f9d2cdef9b266cff4e9307d9967f789c44a570d5766d8b`.

Interpretation boundary:
- measurements apply only to the registered controlled synthetic localhost dataset;
- they do not establish real-world scanner accuracy, production vulnerability prevalence, external-target safety or research novelty;
- real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**.

No later sprint or roadmap scope is started or claimed by this closure. Any next scope requires a separate explicit
definition and evidence gate.

## Previous release boundary — Sprint 10

**Sprint 10 SOFTWARE COMPLETE — final closure verified.**

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
- Phase 8 verification run: `36055037223` — SUCCESS at `751c45eca818031e4e73fb23b8c30a2712469fcb`
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

Verified Phase 8:
- batch/indirect input and evidence hardening: PASS;
- raw-alias fingerprint boundary: PASS;
- coverage mismatch and policy-drift rejection: PASS;
- workspace family-confusion rejection: PASS;
- report non-confirmation/minimization/redaction invariants: PASS;
- security hardening suite: PASS, 20 assertions;
- bounded 100 / 1,000 / 10,000 engineering observations: PASS;
- performance observation suite: PASS, 22 assertions;
- performance CSV artifact: PASS.

Final Sprint 10 closure:
- dedicated closure run: `36055604554` — SUCCESS at `a0c6ae56db84fdd9f79c56aaf8770261ac4fd529`;
- full Sprint 10 plus retained Sprint 9/8/7/6 and S2/S3/S4 regressions: PASS;
- official Maven package: PASS;
- deterministic source checkpoint: PASS;
- closure-candidate ZIP SHA-256: `dbd67307bf56d3333f77d44ca72bb1b1062322cbff9b3f1c0bde58d5b01931e0`;
- entries: 931;
- unsafe paths: 0;
- duplicate entries: 0;
- clean extraction equality: PASS;
- per-file SHA-256 equality: PASS;
- real Burp desktop runtime remains UNVERIFIED / DEFERRED.

Sprint 11 was subsequently started on a separate branch after an explicit dependency review. The historical S10 closure itself did not pre-claim that scope.

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
