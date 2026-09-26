# ROADMAP

## Current gate — 2026-09-26 — standalone Sprint 11

**Sprint 11 Phase 6 — RESEARCH GROUND-TRUTH READINESS: VERIFIED; PHASE 7 ACTIVE.**

Canonical Phase 6 evidence:
- branch: `s11-standalone-finding-lifecycle`
- workflow run: `36248296847` — SUCCESS
- GT-S11-AUTHORIZATION-RESEARCH: PASS
- cases: 16
- positive controls: 8
- negative controls: 8
- authorization dimensions: 8
- secure/vulnerable oracle execution: PASS
- A0-A7 state: SOFTWARE_READY / RESEARCH_NOT_RUN
- TP/TN/FP/FN/precision/recall/F1: NOT_MEASURED

Historical Phase 5 evidence:
- branch: `s11-standalone-finding-lifecycle`
- verified head: `6e0b090b037f037982d61bd3f62a2a3bcc54d521`
- workflow run: `36248083390` — SUCCESS
- finding lifecycle/review surface: PASS
- read-only JSON reproduction preview: PASS
- read-only SARIF preview: PASS
- read-only Burp Issue Draft preview: PASS
- no automatic Burp publication action: PASS
- browser JavaScript: PASS
- packaged standalone startup: PASS

Historical Phase 4 evidence:
- branch: `s11-standalone-finding-lifecycle`
- verified head: `9344b37dfab2c9404539041811bf51356dcb531a`
- workflow run: `36247738000` — SUCCESS
- full Maven reactor: PASS
- retained finding lifecycle/review/reproduction stack: PASS
- SARIF 2.1.0 projection: PASS
- Burp Issue Draft projection: PASS
- browser JavaScript syntax: PASS
- packaged standalone startup: PASS

Historical Phase 3 evidence:
- branch: `s11-standalone-finding-lifecycle`
- verified head: `7e8243451b740094e16b588425ad48cd9d7a9d29`
- workflow run: `36215524311` — SUCCESS
- minimized reproduction package: PASS
- reviewer/reason structural exclusion: PASS
- deterministic JSON + SHA-256: PASS
- restart determinism: PASS

Historical Phase 1–2 evidence:
- branch: `s11-standalone-finding-lifecycle`
- immutable Sprint 10 closure: `c94ddc6cf32175694bb0fbffe4c4fc7ce24050c2`
- Phase 1 workflow: `36214703722` — SUCCESS
- Phase 2 verified head: `d0213af7d9f7002ffe44f43a516f0f4d9e2f0bcc`
- Phase 2 workflow: `36215309359` — SUCCESS
- FindingLifecycle foundation: PASS
- FindingReviewWorkspace: PASS
- active-execution-only finding admission: PASS
- restart persistence / project isolation: PASS
- direct confirmation rejection: PASS
- CSRF finding API: PASS
- secret-bearing review rejection: PASS
- packaged Reviewed Findings UI: PASS

**Sprint 11 is NOT software-complete.** Phase 7 is active: security hardening, bounded engineering observation, retained regression and deterministic final closure.

## Current gate — 2026-09-26

**Sprint 10 SOFTWARE COMPLETE.**

Canonical Sprint 10 closure evidence:
- final closure run: `36213547092` — SUCCESS
- closure source commit: `c94ddc6cf32175694bb0fbffe4c4fc7ce24050c2`
- checkpoint: `S10-FINAL-SOFTWARE`
- source ZIP SHA-256: `296fcf420919c172c338988e320a06007aaa4640b90d2077a341759690fd5c0f`
- source entries: 951
- unsafe paths: 0
- duplicate entries: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- distribution bundle SHA-256: `0c6929384eb3110f018a57b45fb09729957e6b1f90e4ac8d9b3352bd12920f15`
- retained Sprint 9 final stack: PASS
- full current Maven reactor: PASS
- all Sprint 10 standalone / active / bridge suites: PASS
- extracted distribution startup: PASS
- Sprint 11: NOT STARTED
- real Burp desktop runtime: UNVERIFIED / DEFERRED

Historical Phase 8 evidence:
- branch: `s10-standalone-workbench`
- verified head: `ffca14dd80b22d44a03c084311e69695f01112c1`
- workflow run: `36213386173` — SUCCESS
- official Montoya Burp adapter build: PASS
- headless standalone bridge contract: PASS
- loopback-only bridge management URL: PASS
- distribution bundle build: PASS
- JAR SHA-256 manifest verification: PASS
- clean distribution extraction: PASS
- extracted standalone startup: PASS
- real Burp desktop runtime remains UNVERIFIED / DEFERRED

Historical Phase 7 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `3125f71922e5c617bff62f35ac0b11d9f535fca7`
- standalone workflow run: `36212999470` — SUCCESS
- controlled live localhost execution: PASS
- four-way Core differential: PASS
- kill-switch no-dispatch behavior: PASS
- external/non-controlled target rejection: PASS
- transient credential non-persistence: PASS
- controlled Active API: PASS
- Active Validation browser syntax/package: PASS

Historical Phase 6 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `84c09f2e60c94093fc653d8a5a17873c45a25d5d`
- standalone workflow run: `36210452986` — SUCCESS
- Core candidate state boundary: PASS
- analyst review persistence: PASS
- no false TESTED coverage: PASS
- deterministic JSON/Markdown + SHA-256: PASS
- confirmedFindingCount=0: PASS
- Candidates/Coverage/Reports API + GUI: PASS

Historical Phase 5 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `8a340bf38fdaa58934d252abf6cdb2b8a30f14c3`
- standalone workflow run: `36209983492` — SUCCESS
- redacted evidence persistence: PASS
- original digest provenance: PASS
- out-of-scope non-archival: PASS
- Core passive response differential: PASS
- authorization context differential: PASS
- localhost evidence/differential APIs: PASS
- Evidence GUI / JavaScript syntax: PASS

Historical Phase 4 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `642601902941ff857dd26919d329c1c6bfc58c07`
- standalone workflow run: `36209482608` — SUCCESS
- Core policy projection: PASS
- EffectiveAuthorizationResolver: PASS
- BOLA/BFLA evidence-boundary projection: PASS
- localhost projection API: PASS
- Authorization GUI / JavaScript syntax: PASS
- packaged localhost application: PASS

Historical Phase 3 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `1db56953725c358d6c60a3bb7abcb1e78c02ba8a`
- standalone workflow run: `36209126454` — SUCCESS
- typed context persistence: PASS
- referential validation: PASS
- secret-bearing metadata rejection: PASS
- expected-authorization matrix: PASS
- localhost context API: PASS
- packaged localhost application: PASS

Historical Phase 2 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `32ed3b6843e653c4bf3e3d46a9b427e34fe32832`
- standalone workflow run: `36208517465` — SUCCESS
- Core import foundation: PASS
- OpenAPI / HAR / raw HTTP ingestion: PASS
- target scope enforcement: PASS
- documented + observed route canonical correlation: PASS
- project inventory persistence: PASS
- packaged localhost GUI + application: PASS
- retained Core CI run `36208244822` — SUCCESS
- retained Sprint 2 run `36208244892` — SUCCESS
- retained Sprint 3 run `36208244785` — SUCCESS

Historical Phase 1 evidence:
- branch: `s10-standalone-workbench`
- verified code-bearing head: `bbebeda5e7bc4e929b5c5c9b776504d0db059784`
- standalone workflow run: `36207695944` — SUCCESS
- retained Sprint 2 workflow run: `36207695914` — SUCCESS
- retained Sprint 3 workflow run: `36207695811` — SUCCESS
- Core + standalone Maven build: PASS
- whole-repository Maven package with official Montoya dependency: PASS
- standalone foundation suite: PASS
- packaged `acra-standalone.jar` localhost startup: PASS
- `/api/health` standalone/Core/Burp-independence contract: PASS
- browser workbench resource serving: PASS
- persistent project/target model: PASS
- authorization-reference and HTTP(S)-target validation: PASS
- CSRF localhost API protection: PASS

Phase 1 changes the product dependency model: Burp is now an optional adapter for ACRA, not a prerequisite for
standalone project and target onboarding.

**Sprint 10 is NOT software-complete.** Phase 7 is active: controlled active execution through existing ACRA safety gates.

The repaired Sprint 9 baseline remains closed and verified. Final repaired-baseline closure run:
`36207092874` — SUCCESS at `8f031571dd63cf83ee46d439d40edafd83afea38`.

Real Burp desktop runtime remains a separate **UNVERIFIED / DEFERRED** validation lane.

## Current gate — 2026-09-24

**Sprint 9 SOFTWARE COMPLETE.**

Canonical Sprint 9 closure evidence:
- branch: `s9-property-authorization`
- final closure run: `36002545177` — SUCCESS
- closure source commit: `9bffa59c1b360b3e74e0b5e97d2ce22a06dc73f5`
- closure-candidate checkpoint SHA-256: `04eaadbfb2e132eb386ab52ff775fb5e1c56f0313b610721a28f2fb63bf8e531`
- 873 entries, 0 unsafe paths, 0 duplicate entries
- clean extraction and per-file SHA-256 equality: PASS
- official Maven package and retained S2/S3/S4/S6/S7/S8/S9 verification: PASS

Sprint 8 remains SOFTWARE COMPLETE.

**Sprint 10 is NOT STARTED.** No Sprint 10 implementation is included in the Sprint 9 closure checkpoint.

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
