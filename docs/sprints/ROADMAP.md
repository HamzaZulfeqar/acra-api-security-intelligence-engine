# ROADMAP

## Current productization gate — Sprint 14 — 2026-09-26

**TECHNICAL RELEASE CANDIDATE HARDENING COMPLETE.**

Canonical engineering evidence:
- branch: `s14-productization-release-hardening`;
- frozen Sprint 13 base: `d6797222d31984a45fbc80674193d0388757b24c`;
- release-hardening run: `36185472934` — SUCCESS;
- security run: `36185473086` — CodeQL SUCCESS / Gitleaks SUCCESS;
- candidate version: `0.3.0-rc1`;
- retained executable regression chain: PASS;
- Maven clean verify: BUILD SUCCESS;
- deterministic fixed-input release packaging: PASS;
- release manifest/checksums/JAR identity: PASS;
- dependency inventory: generated;
- release artifact: uploaded.

**Public-release state: BLOCKED.**

Hard blocker:
- no software license has been selected.

Therefore Sprint 14 does not create a tag, GitHub Release, or version promotion.

The next release-promotion decision is external to the completed technical hardening gate:
1. explicitly choose a software license;
2. review release notes;
3. explicitly decide whether to retain `0.3.0-rc1` or promote to `0.3.0`;
4. only then create a public tag/release workflow.

## Previous research gate — Sprint 13 final freeze

## Final Sprint 13 research state — 2026-09-26

**FINAL RESEARCH & REPRODUCIBILITY FREEZE COMPLETE.**

Canonical closure:
- branch: `s13-final-research-freeze`;
- pre-freeze base: `7786097a4ea94bba7f7285218ed1c91d5d4062cd`;
- freeze validation run: `36183344395`;
- validated candidate commit: `3bc7df2f19d8b208f4586dfd4469d9b045e275f8`;
- canonical completed Sprint 13 experiments: 8;
- tracked evidence files hashed: 158;
- Sprint 13 research/policy JSON parsed: 33;
- deterministic verifier repeatability: PASS;
- Maven package: BUILD SUCCESS;
- Phase 7 implementation lock: PASS;
- Phase 8 external-target validation: **NOT PERFORMED**.

Sprint 13 is frozen at the evidence actually collected. No external-target, production-accuracy, production-safety or
independent real-world validation claim is permitted.

There are no remaining mandatory Sprint 13 gates.

Any later authorized external-target validation must start as a new post-freeze experiment/version and must not rewrite
Sprint 13 evidence.

## Previous gate — Sprint 13 Phase 7

**Sprint 13 Phase 7 REAL BURP DESKTOP / MONTOYA RUNTIME VALIDATION COMPLETE.**

Canonical evidence:
- branch: `s13-burp-montoya-runtime`;
- Phase 6B completion base: `d35e2a54891357e680253e2cfc766c3e4bb6f88d`;
- successful real-Burp development workflow: `36179678105`;
- Phase 7 development freeze: `4bb7b952fb5cd0692efc6153d51b6885a7d38f9d`;
- successful post-freeze evaluation workflow: `36180569483`;
- measured evaluation head: `e24a91297bf33bfe18a47453e8c191432b496b20`;
- Burp Suite Community Edition 2026.7.3 JAR checksum: PASS;
- real Montoya extension initialization: PASS;
- real Burp Proxy request callbacks: PASS;
- real Burp Proxy response callbacks: PASS;
- ACRA passive pipeline processing: PASS;
- development `GT-INTEGRATION-001` context reconstruction: PASS;
- untouched evaluation contexts: 2/2 in each of two independent Burp runs;
- label absence during evaluation traffic: PASS;
- secret-exclusion gate: PASS;
- semantic repeatability: PASS;
- review/publication boundary: PASS;
- publication-eligibility guard: PASS;
- no automatic issue-publication wiring: PASS;
- active ACRA execution remained disabled.

The Phase 7 evidence probe is opt-in and disabled during normal extension operation.

**Next gate — Sprint 13 Phase 8: Explicitly Authorized External-Target Validation**

Phase 8 must:
- use only a target the user owns or is explicitly authorized to test;
- define written scope and prohibited actions before traffic generation;
- begin with passive/read-only validation;
- preserve Phase 7 and all earlier research evidence unchanged;
- separate target behavior from ACRA inference/policy/governance errors;
- retain review-only semantics for uncertainty;
- record exact target/application version and authorization basis without storing secrets;
- stop on scope ambiguity, destructive behavior, or unexpected state change.

After Phase 8, one final gate remains:
- final research/reproducibility freeze and release-candidate evidence consolidation.

## Previous gate — Sprint 13 Phase 6B

**Sprint 13 Phase 6B ACTUAL LOCAL FRAMEWORK RUNTIME VALIDATION COMPLETE.**

Canonical evidence:
- branch: `s13-cross-framework-runtime`;
- Phase 6A completion base: `84fff082a63c62d847d8b9df4bb17421d7d4bdb2`;
- successful development workflow: `36175284755`;
- development freeze: `683933836d2c2fa5ec155bc8e224be87e6958e95`;
- successful untouched evaluation workflow: `36175649455`;
- measured evaluation head: `e4d50d69161b8c2af8df30c334a0f51e3599c925`;
- real localhost FastAPI/Uvicorn: PASS;
- real localhost Flask: PASS;
- real localhost Express/Node.js: PASS;
- real localhost Spring Boot/Java 21: PASS;
- live HTTP responses: 64/64;
- automatic dimension inference: 64/64;
- governed dispositions: 64/64;
- per framework: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- development freeze gate: PASS;
- Phase 6A normalizer/upstream freeze: PASS;
- blind-label gate: PASS;
- repeatability: PASS;
- Maven product package: BUILD SUCCESS.

Phase 6B establishes controlled localhost runtime compatibility for the exact framework/runtime versions exercised in CI.
It does not establish Burp desktop integration, arbitrary framework-version compatibility, production accuracy or
external-target effectiveness.

**Next gate — Sprint 13 Phase 7: Real Burp Desktop / Montoya Runtime Validation**

Required direction:
- load the built ACRA extension into a real Burp Suite desktop/runtime environment;
- validate Montoya API extension initialization;
- route controlled localhost traffic through Burp;
- confirm ACRA receives real Burp request/response objects;
- verify normalized evidence reaches the frozen dimension/policy/governance pipeline;
- validate finding/review materialization without auto-publishing unverified issues;
- retain localhost-only authorization and scope controls;
- preserve Phase 6B and all earlier research evidence unchanged.

After Phase 7, two gates remain:
1. explicitly authorized external-target validation;
2. final research/reproducibility freeze.

## Previous gate — Sprint 13 Phase 6A

**Sprint 13 Phase 6A CROSS-FRAMEWORK-SHAPED NORMALIZATION COMPLETE.**

Canonical Phase 6A evaluation:
- workflow: `36173838561`;
- normalized dimension: 64/64;
- normalized disposition: 64/64;
- snapshot representation only.

## Previous gate — Sprint 13 Phase 5

**Sprint 13 Phase 5 POLICY RELIABILITY & UNCERTAINTY GOVERNANCE COMPLETE.**

Canonical Phase 5 evaluation:
- workflow: `36170353179`;
- 64/64 dimensions;
- 64/64 governed dispositions;
- governed actionable FP=0;
- escalation coverage=1.0;
- silent positive count=0.

## Previous gate — Sprint 13 Phase 4

**Sprint 13 Phase 4 ADVERSARIAL / BASE-RATE STRESS COMPLETE.**

Canonical Phase 4 evidence:
- workflow: `36168752869`;
- measured head: `ed1601542739cce20be48c001ae4e663bfdcf890`;
- 96 cases / 8 positive / 88 negative;
- G1 TP=8/TN=49/FP=39/FN=0;
- policy-degraded controls dominated false positives;
- Phase 4 remains frozen evidence.

## Previous gate — Sprint 13 Phase 3

**Sprint 13 Phase 3 CONFIGURABLE POLICY GENERALIZATION COMPLETE.**

Canonical Phase 3 evaluation:
- workflow: `36149071484`;
- 24 cases / 8 positive / 16 negative;
- G1 TP=8/TN=16/FP=0/FN=0;
- configured-policy result remains frozen internal evidence.

## Previous gate — Sprint 13 Phase 2

**Sprint 13 Phase 2 AUTOMATIC AUTHORIZATION-DIMENSION DISCOVERY COMPLETE.**

Canonical Phase 2 evidence:
- branch: `s13-dimension-discovery`;
- measured workflow: `36145805521`;
- dimension-free corpus: 32 cases;
- combined dimension accuracy=.968750, macro-F1=.968254;
- downstream inferred-dimension A7 preserved prior binary results.

## Previous gate — Sprint 13 Phase 1

**Sprint 13 Phase 1 HELD-OUT EXTERNAL-VALIDITY EVALUATION COMPLETE.**

Canonical Phase 1 evidence:
- branch: `s13-heldout-external-validity`;
- locked Sprint 12 base: `bd944e83a6edefafba56caebaa35e89fc107c282`;
- measured head: `e515a31d91d775d5f0f35c32a48507455f84992d`;
- successful workflow: `36143281129`;
- feature corpus: `GT-S13-HOLDOUT-FEATURES`, 16 cases / 8 positive / 8 hard-negative controls;
- labels sealed separately and physically absent during prediction: PASS;
- 128 locked A0-A7 prediction rows: PASS;
- two-run prediction/evaluation repeatability: PASS;
- secure/vulnerable oracle validation: PASS;
- Maven package: BUILD SUCCESS.

Held-out A7 result:
- TP=8 / TN=3 / FP=5 / FN=0;
- precision=.615385 / recall=1.000000 / F1=.761905;
- five hard-negative false positives expose limited policy generalization.

The original held-out result is frozen as research evidence and must not be tuned away. Registered authorization
dimensions were supplied in Phase 1; Phase 2 subsequently removed that dependency.

## Previous gate — Sprint 12

**Sprint 12 CONTROLLED RESEARCH EXECUTION COMPLETE — registered synthetic/localhost scope.**

Canonical Sprint 12 evidence:
- branch: `s12-a0-a7-research-evaluation`;
- immutable software base: Sprint 11 head `db78f9e458eeabcabb389abf1949a4f597761564`;
- first measured successful workflow: `36065830981`;
- dataset: `GT-S11-AUTHORIZATION-RESEARCH`, 16 cases / 8 positive / 8 negative / 8 registered dimensions;
- 8 cumulative variants and 128 prediction rows;
- label isolation: predictor cannot consume ground-truth/expected-candidate fields;
- vulnerable-only localhost treatment observation, labels joined after prediction;
- deterministic JSON / CSV / JSONL research artifacts and SHA-256 sidecars;
- repeatability: PASS;
- Maven product package after research harness: PASS.

Measured controlled progression:
- A0: TP=8 / TN=1 / FP=7 / FN=0, P=.533333 / R=1 / F1=.695652;
- A1: TP=8 / TN=1 / FP=7 / FN=0, P=.533333 / R=1 / F1=.695652;
- A2: TP=8 / TN=4 / FP=4 / FN=0, P=.666667 / R=1 / F1=.800000;
- A3: TP=8 / TN=5 / FP=3 / FN=0, P=.727273 / R=1 / F1=.842105;
- A4: TP=8 / TN=6 / FP=2 / FN=0, P=.800000 / R=1 / F1=.888889;
- A5: TP=8 / TN=7 / FP=1 / FN=0, P=.888889 / R=1 / F1=.941176;
- A6: TP=8 / TN=8 / FP=0 / FN=0, P=1 / R=1 / F1=1;
- A7: TP=8 / TN=8 / FP=0 / FN=0, P=1 / R=1 / F1=1.

Research boundary after Sprint 12:
- registered dimension is supplied; dimension discovery is NOT MEASURED;
- synthetic balanced localhost data only;
- no real-world accuracy/generalization claim;
- real Burp desktop and external authorized-target validation remain UNVERIFIED / DEFERRED;
- next research expansion should use blinded/held-out cases, broader negative controls and independently authored fixtures before any external-validity claim.

See:
- `sprint-12.md`;
- `../research/sprint-12-a0-a7-protocol.md`;
- `../research/EXPERIMENT_REGISTRY.md`.

## Previous Sprint 11 gate

Sprint 11 remains **SOFTWARE COMPLETE**. Its historical A0-A7 state was correctly
`SOFTWARE_READY / RESEARCH_NOT_RUN`; Sprint 12 subsequently executed that registered campaign without
backfilling the frozen Sprint 11 dataset.

## Previous Sprint 10 gate

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

No Sprint 11 scope is started or claimed by this closure. Any next roadmap scope requires a separate explicit
definition and evidence gate.

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
