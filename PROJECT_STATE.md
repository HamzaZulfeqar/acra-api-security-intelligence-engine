# ACRA Project State

## Current state — 2026-09-25

**Current release boundary:** Sprint 11 — Research Evaluation & Ablation — SOFTWARE COMPLETE.  
**Decision:** S11 SOFTWARE COMPLETE — Phases 1–10 plus dedicated final closure VERIFIED.  
**Working branch:** `s11-research-evaluation-ablation`.  
**Sprint 11 closure-candidate source commit:** `f832af1defde242530408589bd9f8732cef533d5`.  
**Sprint 11 dedicated final closure:** GitHub Actions run `36066400016` — SUCCESS.  
**Sprint 11 closure-candidate ZIP SHA-256:** `6d14693aa4056ee149c4c2f9496f7bb3d044783c7962bdf43f4453e8f2c3aaba` — 986 entries, 0 unsafe paths, 0 duplicate entries, clean extraction PASS, per-file SHA-256 equality PASS.  
**Immutable Sprint 10 base:** `59022c4a25718f38ea7ec2f010911344d1aa0698`.  
**Sprint 10:** SOFTWARE COMPLETE and frozen as the previous release boundary.  
**Sprint 10 post-documentation final revalidation:** GitHub Actions run `36056032234` — SUCCESS.  
**Sprint 10 final-status checkpoint SHA-256:** `f8d15ed72dec06e8e66b14afe749f392a51a3fb628a9e70ebd290ef032df28d4`.  
**Sprint 11 Phase 1 verification:** GitHub Actions run `36056957706` — SUCCESS at source commit `c4cff92b7668a75db2eb70ea550e7dd807d5a755`.  
**Sprint 11 Phase 2 verification:** GitHub Actions run `36057476360` — SUCCESS at source commit `dff822e02299b887869bc43923d93a62d9e7d35f`.  
**Sprint 11 Phase 3 verification:** GitHub Actions run `36057869888` — SUCCESS at source commit `dc3c081bf033b14afcea3185c3d54fb241be1772`.  
**Sprint 11 Phase 4 verification:** GitHub Actions run `36058274408` — SUCCESS at source commit `88f7cd78161267a1900ef4b663258938ee208d9b`.  
**Sprint 11 Phase 5 verification:** GitHub Actions run `36058945211` — SUCCESS at source commit `022bf71f31f69633b5eac541b230283e8536decd`.  
**Sprint 11 Phase 6 verification:** GitHub Actions run `36060118721` — SUCCESS at source commit `ef25d5aeca247c32f2b94bc8faac56405cc8bb58`.  
**Sprint 11 Phase 7 verification:** GitHub Actions run `36064434979` — SUCCESS at source commit `9f9af8ce15e5035def2605c98d8f8153728c3eed`.  
**Sprint 11 Phase 8 verification:** GitHub Actions run `36064798175` — SUCCESS at source commit `c742014ab97d024be42425fb4e49866f565389ec`.  
**Sprint 11 Phase 9 verification:** GitHub Actions run `36065439110` — SUCCESS at source commit `2f17a994e4c481b48ae8ff11734a2ee17e32b84d`.  
**Sprint 11 Phase 10 verification:** GitHub Actions run `36065934911` — SUCCESS at source commit `bcc41630fa4d7c9a4f11023e9aeb8b6561f67310`.  
**Immutable Sprint 9 base:** `ce81220eb9ea41009973b4072c08d59927ee8c6b`.  
**Sprint 9:** SOFTWARE COMPLETE and frozen as the previous release boundary.  
**Sprint 9 post-documentation final revalidation:** GitHub Actions run `36003064254` — SUCCESS.  
**Sprint 10 Phase 1 verification:** GitHub Actions run `36004146212` — SUCCESS.  
**Sprint 10 Phase 2 verification:** GitHub Actions run `36004574331` — SUCCESS.  
**Sprint 10 Phase 3 verification:** GitHub Actions run `36048381112` — SUCCESS at source commit `608490cfc1211057e879f6c6457ea62fd648b405`.  
**Sprint 10 Phase 4 verification:** GitHub Actions run `36048837246` — SUCCESS at source commit `abb57d0320fcdc161992297415978bde6c769bee`.  
**Sprint 10 Phase 5 verification:** GitHub Actions run `36049247018` — SUCCESS at source commit `16169367c3e7c3b179a6d8541dfc88698fde6b81`.  
**Sprint 10 Phase 6 verification:** GitHub Actions run `36051856008` — SUCCESS at source commit `a33aaffccce80ba8251284a0b4de70f2f76e7a8a`.  
**Sprint 10 Phase 7 verification:** GitHub Actions run `36054653296` — SUCCESS at source commit `09d4edb23014c738d6856d46db6e0a72ed5a9e97`.  
**Sprint 10 Phase 8 verification:** GitHub Actions run `36055037223` — SUCCESS at source commit `751c45eca818031e4e73fb23b8c30a2712469fcb`.  
**Sprint 10 final closure:** GitHub Actions run `36055604554` — SUCCESS at closure-candidate source commit `a0c6ae56db84fdd9f79c56aaf8770261ac4fd529`.  
**Closure-candidate ZIP SHA-256:** `dbd67307bf56d3333f77d44ca72bb1b1062322cbff9b3f1c0bde58d5b01931e0` — 931 entries, 0 unsafe paths, 0 duplicate entries, clean extraction PASS, per-file SHA-256 equality PASS.

### Sprint 11 final closure

Sprint 11 is **SOFTWARE COMPLETE**.

Dedicated final closure: GitHub Actions run `36066400016` — SUCCESS at closure-candidate source commit
`f832af1defde242530408589bd9f8732cef533d5`.

Final closure evidence:

- complete Sprint 11 verification stack: PASS;
- controlled research lab fixtures: PASS, 65 assertions;
- treatment evidence collection: PASS, 725 assertions;
- prediction execution: PASS, 384 assertions;
- ablation evaluation: PASS, 99 assertions;
- research report/export: PASS, 31 assertions;
- retained Sprint 10 and Sprint 9 foundation verification: PASS;
- retained Sprint 8 / Sprint 7 / Sprint 6 foundations: PASS;
- official Maven package: PASS;
- retained Sprint 2 and Sprint 3 regressions: PASS;
- Sprint 4 / Sprint 6 / Sprint 7 / Sprint 8 / Sprint 9 / Sprint 10 headless UI regressions: PASS;
- deterministic source checkpoint: PASS;
- archive entries: 986;
- unsafe paths: 0;
- duplicate entries: 0;
- clean extraction equality: PASS;
- per-file SHA-256 equality: PASS.

Closure-candidate checkpoint: `acra-sprint-11-final.zip`  
Closure-candidate ZIP SHA-256: `6d14693aa4056ee149c4c2f9496f7bb3d044783c7962bdf43f4453e8f2c3aaba`

Controlled research results remain scoped to the registered 15-case synthetic localhost dataset. They do not
establish real-world scanner accuracy, production vulnerability prevalence, external-target safety or research
novelty.

Real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**.

The source hash above identifies the successful pre-promotion closure-candidate snapshot. Final-status
documentation changes alter the source archive by definition; post-documentation revalidation is therefore
tracked as workflow evidence rather than recursively embedding a new source hash.

### Sprint 11 verified progress

Phase 1 deterministic research-ablation protocol is **VERIFIED COMPLETE**:

- exact A0→A7 cumulative methodology encoded in core;
- explicit dimensions: identity, ownership, tenant, role, workflow, semantic evidence, evidence correlation;
- exact required metric contract: TP, TN, FP, FN, precision, recall, F1, evidence completeness;
- deterministic protocol ID/fingerprint;
- protocol scope restricted to controlled registered ground truth;
- execution state remains NOT_RUN;
- invalid order, missing metrics and fingerprint tampering fail closed;
- retained S10/S9/S8/S7/S6/S5 foundations remain green.

Sprint 11 Phase 2 controlled evaluation dataset registration is **VERIFIED COMPLETE**:

- canonical `GT-S11-ABLATION-DATASET` registered;
- all 15 independently labelled S4 research fixtures included exactly once;
- ground-truth mapping is derived only from source `expected_candidate` booleans;
- 8 POSITIVE and 7 NEGATIVE controls;
- source-case provenance is explicit and deterministic;
- dataset identity/fingerprint is deterministic;
- treatment output is explicitly forbidden from modifying ground truth;
- S6/S7/S8/S9/S10 policy fixtures are excluded because policy ALLOW/DENY is not automatically a vulnerability label;
- dataset and A0–A7 execution state remain NOT_RUN.

Sprint 11 Phase 3 deterministic treatment/prediction adapters are **VERIFIED COMPLETE**:

- prediction inputs contain baseline/dimension evidence only and expose no ground-truth field/type;
- each A0–A7 variant consumes exactly its enabled cumulative dimensions;
- disabled later dimensions are excluded from prediction evidence;
- missing enabled dimension evidence fails closed as INCONCLUSIVE with no binary prediction;
- duplicate dimension evidence is rejected;
- result identity/fingerprint is deterministic;
- prediction result evidence remains explicit and ordered.

Sprint 11 Phase 4 deterministic campaign planning is **VERIFIED COMPLETE**:

- exact campaign denominator: 15 registered dataset cases × 8 ablation variants = 120 cells;
- deterministic case-major / A0→A7 ordering;
- deterministic unique cell IDs;
- required dimensions tied exactly to the verified protocol;
- initial coverage state: 120 PLANNED, 0 EVIDENCE_READY, 0 EXECUTED, 0 INCONCLUSIVE, 0 BLOCKED;
- campaign cell schema contains neither ground truth nor prediction fields;
- duplicate case/variant cells and protocol-dimension drift fail closed;
- campaign plan identity/fingerprint is deterministic.

Sprint 11 Phase 5 evidence-readiness mapping is **VERIFIED COMPLETE**:

- all 15 registered research cases mapped against actual Sprint 4 localhost/lab support;
- 3 cases READY: public control, HTTP-200 application denial, same-resource representation;
- 4 cases PARTIAL: timestamp, request ID, ordering and formatting because current tests combine those effects;
- 8 positive-control cases MISSING_FIXTURE;
- missing fixtures claim no route and no executable evidence reference;
- readiness identity/fingerprint is deterministic;
- readiness mapping does not promote any of the 120 campaign cells from PLANNED;
- experiment execution remains NOT_RUN.

Sprint 11 Phase 6 controlled research-lab expansion is **VERIFIED COMPLETE**:

- 4 previously PARTIAL negative controls now have isolated localhost fixtures;
- 8 previously MISSING positive controls now have dedicated localhost fixtures;
- new research routes use neutral case identifiers and do not expose positive/negative labels;
- independent ground truth remains in `GT-S11-RESEARCH-LAB-FIXTURES`;
- secure/vulnerable lab modes are live-started in CI on loopback only;
- controlled research lab suite: PASS, 65 assertions;
- readiness map is now 15 READY / 0 PARTIAL / 0 MISSING_FIXTURE / 0 EXECUTED;
- readiness suite: PASS, 72 assertions;
- campaign denominator remains 120 PLANNED / 0 EXECUTED;
- A0–A7 experiment state remains NOT_RUN.

Sprint 11 Phase 7 treatment-evidence collection is **VERIFIED COMPLETE**:

- live fixture pairs are converted to hash-only, secret-safe evidence references;
- raw bodies, raw principals, raw tenant values, ground truth and predictions are excluded from persisted evidence bundles;
- volatile legacy timestamps/request IDs are normalized before evidence hashing;
- every case receives deterministic baseline evidence;
- every case accounts for identity, ownership, tenant, role, workflow, semantic and correlation dimensions;
- non-applicable dimensions are explicit rather than silently absent;
- all 15 bundles are complete through A7;
- campaign readiness projection = 120 EVIDENCE_READY / 0 PLANNED / 0 EXECUTED;
- removing one case bundle leaves exactly its 8 A0–A7 cells PLANNED;
- treatment evidence suite: PASS, 725 assertions;
- A0–A7 experiment execution and metrics remain NOT_RUN.

Sprint 11 Phase 8 ground-truth-free prediction execution is **VERIFIED COMPLETE**:

- 120 evidence-ready case×variant cells produce exactly 120 deterministic prediction results;
- prediction execution requires every input cell to be EVIDENCE_READY;
- prediction result IDs/fingerprints and execution snapshot identity are deterministic;
- every completed result is RESOLVED with an explicit binary prediction in the controlled dataset;
- executed campaign state = 120 EXECUTED / 0 EVIDENCE_READY / 0 PLANNED;
- prediction execution snapshot contains no ground-truth or metric fields;
- missing case evidence and non-evidence-ready plans fail closed;
- Phase 8 suite: PASS, 382 assertions;
- A0–A7 confusion-matrix and aggregate metrics remain NOT_RUN.

Sprint 11 Phase 9 one-way ablation evaluation is **VERIFIED COMPLETE**:

- immutable prediction results are joined to immutable dataset labels only after Phase 8 execution;
- 120 labelled `ResearchExecutionRecord` values are produced;
- each A0–A7 variant evaluates all 15 cases with 0 inconclusive cells;
- evidence completeness is 1.0 for every variant and is tracked independently from classification accuracy;
- A0–A5: TP=8, TN=0, FP=7, FN=0, precision=0.533333..., recall=1.0, F1=0.695652...;
- A6–A7: TP=8, TN=7, FP=0, FN=0, precision=1.0, recall=1.0, F1=1.0;
- evaluation cannot mutate prediction execution identity, prediction fingerprints or classification outputs;
- deliberately removing one evidence bundle lowers evidence completeness without altering classification metrics;
- Phase 9 suite: PASS, 99 assertions.

During Phase 9, a cumulative-ablation defect was detected and corrected: semantic equivalence had been replacing
earlier ownership/tenant conflict evidence. The corrected A6/A7 logic preserves earlier contextual conflicts while
using semantic equivalence to suppress raw-difference-only false positives.

These results are valid **only for the registered 15-case controlled synthetic localhost dataset**. They do not
establish real-world API-scanner accuracy, production vulnerability prevalence, external-target safety, or novelty.

Sprint 11 Phase 10 deterministic research reporting/export is **VERIFIED COMPLETE**:

- minimized `s11-research-evaluation-report-v1` schema;
- protocol/dataset/prediction/evaluation provenance retained;
- per-variant TP/TN/FP/FN, precision, recall, F1 and evidence completeness retained;
- raw HTTP bodies, credentials and synthetic token material excluded;
- canonical JSON + deterministic Markdown exports;
- JSON SHA-256: `595dba16b9d724d67dbdd1dc4faeacd76432661b52ead780b14054acee67c04d`;
- Markdown SHA-256: `1d4c6b2f9f7f88f443f9d2cdef9b266cff4e9307d9967f789c44a570d5766d8b`;
- report/export suite: PASS, 31 assertions;
- CI report artifact upload: PASS.

The next dependency is Sprint 11 final security/reproducibility review and closure. Closure must re-run the complete
S11 verification stack, retained regressions, official Maven packaging, deterministic source checkpointing and
archive integrity checks before SOFTWARE COMPLETE can be claimed.

Real Burp desktop runtime remains **UNVERIFIED / DEFERRED** and is not inferred from S11 software verification.

### Sprint 10 verified progress

Phase 1 adds the remaining historical Property/Batch/Indirect roadmap scope after property authorization was
completed in Sprint 9.

Batch foundation:
- explicit per-item policy and observation models;
- item-level authorization assessment;
- mixed ALLOW/DENY outcomes preserved;
- no aggregate HTTP-success authorization shortcut;
- missing/ambiguous policy fails closed;
- cross-project provenance fails closed;
- deterministic analysis identity.

Indirect-reference foundation:
- SHA-256 reference fingerprint instead of raw key storage;
- evidence-backed resolved-resource model;
- authorization evaluated against resolved target resource;
- one fingerprint resolving to multiple resources remains explicit conflict;
- missing/ambiguous resolved-target policy fails closed;
- cross-project provenance fails closed;
- deterministic analysis identity.

Verification run `36004146212` passed the new 25-assertion S10 foundation suite, retained S9/S8/S6/S5
foundations, exact Java 21 compilation with warnings as errors and Maven core test compilation.

Sprint 10 Phase 2 controlled localhost batch/indirect ground truth is **VERIFIED COMPLETE**.

Sprint 10 Phase 3 safe planner/executor integration is **VERIFIED COMPLETE**:

- existing S4 `BATCH` and `INDIRECT_REFERENCE` contracts/mutation families reused;
- fixed policy-backed seeds only;
- existing request-equivalence, consent, scope, environment, budget, concurrency, rate-limit and kill-switch gates retained;
- secure mixed batch: aggregate ALLOW with item-level ALLOW/DENY and 0 candidates;
- vulnerable mixed batch: foreign item DENY→ALLOW and exactly 1 review candidate;
- secure fixed indirect alias `share-a → share-b`: resolved foreign target remains DENY and 0 candidates;
- vulnerable fixed indirect alias: resolved foreign target DENY→ALLOW and exactly 1 review candidate;
- indirect raw alias remains excluded from persisted resolution state and represented by SHA-256 fingerprint;
- cross-project provenance remains fail-closed;
- no identifier guessing, alias enumeration, external-target probing or automatic vulnerability confirmation.

Sprint 10 Phase 4 provenance-gated finding projection is **VERIFIED COMPLETE**:

- batch and indirect projection requests are secret-safe and evidence-bound;
- evidence plus observation lineage is revalidated before projection;
- projection-request/assessment mismatches fail closed as INCONCLUSIVE;
- verified secure controls project to REJECTED;
- verified DENY→ALLOW mismatches project to review-only CANDIDATE;
- deterministic candidate identifiers/fingerprints are preserved;
- cross-project provenance projects to INCONCLUSIVE;
- projected indirect candidates contain no raw alias material;
- no confirmed-vulnerability state is introduced.

Sprint 10 Phase 5 deterministic coverage accounting is **VERIFIED COMPLETE**:

- one explicit policy-universe denominator spans batch-item and indirect resolved-target policies;
- duplicate policy registration is deterministic and non-expanding;
- UNOBSERVED and OBSERVED_UNASSESSED gaps remain explicit;
- assessed contexts are separated into CANDIDATE, REJECTED and INCONCLUSIVE;
- aggregate HTTP success does not count as item/resolution coverage;
- coverage entries validate observation, assessment and finding consistency;
- combined coverage ordering and summary ratios are deterministic.

Sprint 10 Phase 6 read-only product workspace and headless Burp UI projection is **VERIFIED COMPLETE**:

- immutable `S10BatchIndirectProductSnapshot` and synchronized `S10BatchIndirectWorkspace`;
- separate batch and indirect policy/observation/assessment collections with shared candidate and coverage lifecycle;
- read-only `Batch & Indirect` top-level Burp tab;
- Overview / Policies / Observations / Assessments / Candidates / Coverage surfaces;
- no premature Phase 7 Report or JSON Export tabs;
- raw indirect aliases remain absent from UI tables; only fingerprints/resolved resources are rendered;
- aggregate HTTP success is explicitly separated from per-item authorization;
- candidate != confirmed vulnerability and unobserved != secure boundaries are rendered;
- retained S4/S6/S7/S8/S9 headless UI regressions remain green;
- real Burp desktop runtime/load/handler/UI validation remains UNVERIFIED / DEFERRED.

Sprint 10 Phase 7 deterministic report/export and read-only UI projection is **VERIFIED COMPLETE**:

- versioned `s10-batch-indirect-report-v1` canonical report model;
- report-specific minimized policy, observation, assessment, candidate and coverage projections;
- `policySource`, raw aliases and candidate rationale structurally excluded from exported schema;
- indirect references represented only by SHA-256 fingerprint plus resolved resource;
- deterministic report identity derived from report state rather than render timestamp;
- canonical JSON and deterministic Markdown exports with stable SHA-256 digests;
- stable `s10-batch-indirect-json-v1` Reporter plugin adapter;
- `confirmedFindingCount = 0` and explicit unobserved coverage retained;
- read-only Report and JSON Export views use the canonical workspace exporter;
- canonical JSON / SHA-256 / Markdown evidence archived by Sprint 10 CI;
- real Burp desktop runtime remains UNVERIFIED / DEFERRED.

Sprint 10 Phase 8 security hardening and bounded performance observations is **VERIFIED COMPLETE**:

- malformed batch query/fragment endpoints rejected;
- batch observations without evidence or with duplicate evidence rejected;
- secret-bearing batch metadata rejected;
- malformed indirect query/fragment endpoints rejected;
- raw aliases rejected where SHA-256 fingerprints are required;
- duplicate indirect evidence rejected;
- batch resource mismatch and indirect resolved-target mismatch rejected by coverage accounting;
- policy decision drift for stable coverage identity rejected for both families;
- Sprint 10 workspace rejects non-S10 and dual-family FindingCandidate projections;
- report non-confirmation invariant retained;
- report schema minimization and redaction boundaries retained;
- bounded 100 / 1,000 / 10,000 mixed policy-context engineering observations recorded.

Observed CI values from run `36055037223`:
- 100 contexts: population 90 ms, report 28 ms, approx memory delta 3,691,056 bytes;
- 1,000 contexts: population 95 ms, report 14 ms, approx memory delta 5,843,720 bytes;
- 10,000 contexts: population 387 ms, report 77 ms, approx memory delta 144,125,280 bytes.

These are one-run engineering observations only, not benchmarks, SLOs, release thresholds, scanner-accuracy evidence or real-world capacity claims.

Sprint 10 final closure is **VERIFIED COMPLETE**:

- exact Temurin Java 21 Sprint 10 verification and all Phase 1–8 suites: PASS;
- retained Sprint 9 / Sprint 8 / Sprint 7 / Sprint 6 foundations: PASS;
- official Maven package: PASS;
- retained Sprint 2 local-contract regression: PASS, 52 tests;
- retained Sprint 3 core regression: PASS, 47 tests;
- retained Sprint 3 adapter regression: PASS, 11 tests;
- Sprint 4 UI: PASS, 26 tests;
- Sprint 6 Authorization UI: PASS, 27 assertions;
- Sprint 7 Workflow UI: PASS, 20 assertions;
- Sprint 8 Routing UI: PASS, 23 assertions;
- Sprint 9 Property UI: PASS, 59 assertions;
- Sprint 10 Batch & Indirect UI: PASS, 230 assertions;
- deterministic Sprint 10 source checkpoint: PASS;
- archive safe paths / duplicate detection / clean extraction / per-file SHA-256 equality: PASS.

Closure-candidate checkpoint: `acra-sprint-10-final.zip`  
Closure-candidate source ZIP SHA-256: `dbd67307bf56d3333f77d44ca72bb1b1062322cbff9b3f1c0bde58d5b01931e0`  
Entries: **931**

The source hash identifies the successful pre-promotion closure-candidate snapshot. Final-status documentation changes
alter the source archive by definition; the canonical post-documentation digest is emitted through the external
`.sha256` sidecar rather than embedded recursively.

At the Sprint 10 closure boundary, no Sprint 11 scope was yet started or claimed. Sprint 11 was subsequently created from the verified S10 release head.

Real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**.

### Sprint 9 verified progress

Phase 1 adds an evidence-backed property-authorization layer on top of the existing Sprint 5 contracts:

- `PropertyAccessObservation` with no property-value storage;
- deterministic `S9PropertyAuthorizationAnalyzer`;
- `S9PropertyAuthorizationAnalysis` aggregate;
- reuse of `PropertyAuthorizationAssessment`, `PropertyPolicy`, `AuthorizationDimensionEvaluator` and
  `EvidenceReferenceValidator`;
- exact endpoint/property/operation matching;
- role/tenant applicability without inferred policy precedence;
- missing policy → INCONCLUSIVE;
- multiple applicable policies → explicit ambiguity;
- cross-project provenance → INCONCLUSIVE;
- explicit property DENY + observed ALLOW → review-only property candidate;
- property-specific expected decision separated from object-level request authorization;
- deterministic analysis identity;
- secret-bearing observation metadata rejected;
- no automatic confirmed-vulnerability state.

Verification run `35985518829` passed exact Java 21 compilation with warnings as errors, the new Sprint 9
foundation suite, retained Sprint 5 final closure, Sprint 6 policy foundation, Sprint 7 workflow foundation,
Sprint 8 routing-normalization foundation, and Maven core test compilation.

Sprint 9 is **SOFTWARE COMPLETE**. The dedicated final closure passed full regression, official Maven packaging and reproducible checkpoint verification. Sprint 10 is NOT STARTED by this closure; real Burp desktop runtime remains separately UNVERIFIED / DEFERRED.

Real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**.

### Sprint 8 verified progress

The existing Sprint 3 route/template/equivalence layer is reused. New source adds an evidence-backed processing
trace across `RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION`.

Current candidate capabilities:
- explicit processing-stage observations with provenance evidence;
- deterministic stage ordering and missing-stage accounting;
- representation change vs route-family variation vs canonical divergence;
- stage-gap attribution marked INCONCLUSIVE;
- path-only observation boundary;
- duplicate-stage and provenance-free observations rejected;
- no bypass generation, external-target probing or vulnerability promotion.

Phase 1 verification: GitHub Actions run `35961851498` — SUCCESS.

Phase 2 verification: GitHub Actions run `35965612498` — SUCCESS.

Phase 2 adds:
- evidence-backed path/method/host/API-version/authorization observations across routing stages;
- routing-only vs authorization-only vs combined boundary differential states;
- fail-closed handling for stage gaps and unknown authorization context;
- host/path input hardening;
- retained Sprint 3 route/core and Sprint 7 foundation regressions.

No bypass generation, external-target probing or vulnerability promotion is claimed by Phase 2.

Phase 3 verification: GitHub Actions run `35966112820` — SUCCESS.

Phase 3 adds:
- controlled routing ground truth for canonical and duplicate-separator route representations;
- safe read-only `EQUIVALENT_ROUTE_REPRESENTATION` execution through the existing active engine;
- explicit scope authorization for both route forms;
- secure DENY / `NO_CHANGE` control;
- deliberately vulnerable ALLOW / `UNEXPECTED_CHANGE` differential;
- retained Sprint 3 core and Sprint 7 regression PASS.

Phase 3 remains localhost-only and does not establish real-world routing/proxy behavior.

Phase 4 verification: GitHub Actions run `35969974964` — SUCCESS.

Phase 4 adds:
- routing-specific assessment state separated from raw differential classification;
- candidate promotion only for evidence-backed routing change plus explicit DENY→ALLOW mismatch;
- existing evidence-store project/test/execution/observation lineage validation;
- cross-project provenance failure → INCONCLUSIVE;
- secure control → FindingCandidate REJECTED;
- deliberately vulnerable controlled case → review-only FindingCandidate CANDIDATE;
- retained Sprint 3 and Sprint 7 regressions PASS.

FindingCandidate remains review-only and is not an automatically confirmed vulnerability.

Phase 5 verification: GitHub Actions run `35970306700` — SUCCESS.

Phase 5 adds:
- S8 routing product workspace and immutable snapshot;
- Burp Routing area with Overview / Stage Traces / Boundary Matrix / Assessments / Candidates;
- backward-compatible suite-tab constructor integration;
- retained Sprint 4/6/7 UI regressions;
- Maven extension compilation and controlled routing log artifact upload.

The Routing UI is read-only and introduces no new active-execution capability.

Phase 6 verification: GitHub Actions run `35970917885` — SUCCESS.

Phase 6 adds:
- deterministic routing report model and summary;
- canonical JSON/SHA-256 and Markdown exports;
- Reporter plugin adapter;
- Routing Report / JSON Export product views;
- confirmedFindingCount fixed at 0;
- shared redaction hardening preventing embedded Authorization text from truncating later JSON fields;
- retained core/live and S4/S6/S7/S8 UI regressions PASS.

Reports preserve routing observations and review-only candidates without automatic finding confirmation.

Phase 7 verification: GitHub Actions run `35971212640` — SUCCESS.

Phase 7 adds:
- fail-closed routing/host/stage/mutation hardening;
- regression coverage for non-truncating embedded-secret redaction;
- 100 / 1,000 / 10,000 routing-context analysis/report observations;
- performance CSV artifact;
- retained core/live/report/UI regressions.

Observed timing/memory values are engineering observations only and are not release thresholds.

### Sprint 8 final closure

Sprint 8 is **SOFTWARE COMPLETE**.

Dedicated final closure: GitHub Actions run `35971753523` — SUCCESS at source commit
`f6a0c19358b00672711532ec7effe1eed3800e3e`.

Final closure evidence:
- exact Temurin Java 21 verification: PASS
- all S8 core/live/assessment/report/security/performance suites: PASS
- retained Sprint 6 and Sprint 7 foundation: PASS
- official Maven package: PASS
- retained Sprint 2: PASS, 52 tests
- retained Sprint 3: PASS, 47 core + 11 adapter tests
- retained Sprint 4 / Sprint 6 / Sprint 7 / Sprint 8 UI: PASS, 26 / 27 / 20 / 23
- deterministic source checkpoint: PASS
- checkpoint entries: 836
- unsafe paths: 0
- duplicate entries: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- closure-candidate ZIP SHA-256:
  `e063217d3c8795a59ce1cd7e052a2c9b0475a86836239973ef1d470a6c627af7`

Real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**. Sprint 9 is
**NOT STARTED** by this closure.

### Sprint 7 frozen closure

- canonical final workflow: GitHub Actions run `35961314354` — SUCCESS
- canonical final checkpoint: `acra-sprint-07-final.zip`
- canonical SHA-256: `5e77654d6467cc45c60825cb44bd7c3aa1d4f33c8705d41216d7f258a8187189`
- entries: 791
- unsafe paths: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- real Burp desktop runtime: UNVERIFIED / DEFERRED

### Implemented S6 capabilities

- authorization scope taxonomy
- tenant membership and multi-role assignment
- role hierarchy with cycle-safe inheritance
- permission and role-permission models
- explicit ALLOW / DENY rules
- explicit default decision and evidence-backed precedence
- global, shared and delegated authorization
- effective role and permission resolution
- tenant relationship classification
- tenant-isolation and RBAC assessments
- explicit privileged-action role-escalation assessment
- policy-conflict assessment
- policy coverage and effective authorization matrix
- S6 composition into the existing S5 FindingCandidate and severity/risk architecture
- policy hydration into the existing SecurityContextGraph with atomic evidence preflight
- deterministic root-cause grouping foundation
- policy-import plugin boundary and manual policy builder
- controlled S6 ACRA-Lab ground truth and secure/vulnerable tenant/RBAC fixtures
- policy-aware planning recommendations
- live controlled tenant/RBAC experiment and measurement pipeline
- policy-aware S6 planning bridge into the existing S4 planner/queue/executor
- deterministic safe read-only CROSS_TENANT test generation
- live generated-test execution against secure localhost ACRA-Lab
- S6 authorization product workspace and Burp UI projection
- Authorization Overview / Policy / Tenant Map / Roles / Hierarchy / Permissions / Effective Permissions / Conflicts / Coverage views
- deterministic S6 authorization report model
- canonical secret-safe JSON report export + SHA-256
- deterministic Markdown review report
- Reporter plugin adapter and UI Report / JSON Export projections
- credential-safe authenticated-context substitution implemented for safe read-only ROLE_COMPARISON
- ROLE_COMPARISON now generates and executes through explicit contextRef controls without raw credentials in Mutation

### Newly verified planner/execution evidence

GitHub Actions run `35882729813` completed successfully on exact Temurin JDK 21.0.12.1.
`Sprint6PlannerExecutionIntegrationTestSuite` passed 15 assertions and proved a generated CROSS_TENANT test can
flow through the existing S4 planner, queue, safety validation and executor into the secure localhost ACRA-Lab.
Generated test `S6-AUTO-TENANT-a915a78d9e3fd8c06e49c4e9` completed with expected DENY,
`EXPECTED_CHANGE`, and queue state `COMPLETED`.

### Newly measured S6 evidence

GitHub Actions run `35878508170` completed successfully on exact Temurin JDK 21.0.12.1.

`EXP-S6-TENANT-RBAC-001` executed ten labelled localhost cases.

- naive baseline: TP=2, TN=1, FP=7, FN=0, precision=0.222222, recall=1.000000, F1=0.363636
- ACRA S6: TP=2, TN=8, FP=0, FN=0, precision=1.000000, recall=1.000000, F1=1.000000

These are controlled-fixture measurements only. They do not establish real-world scanner accuracy.

The campaign also exposed and closed a SHARED-scope isolation defect before the successful run.

### S6 authorization UI verification

GitHub Actions run `35885676715` completed successfully at commit
`7458b37706bb5ebe24839f3bb7464ef88ed41f4b`.

- Sprint 6 core/live verification: PASS
- Maven extension test compilation: PASS
- retained `Sprint4UiTestSuite`: PASS, 26 checks
- `Sprint6AuthorizationUiTestSuite`: PASS, 22 assertions
- Authorization product area now renders policy, tenant map, role assignments, hierarchy, permissions,
  effective authorization matrix, conflict assessments and coverage without inventing missing policy state.

### S6 reporting/export verification

GitHub Actions run `35887862337` completed successfully at commit `b051f08612730a455375e75312ca70d6e5fc9f6f`.

- `Sprint6ReportingExportTestSuite`: PASS, 19 assertions
- canonical report JSON / digest / Markdown artifact generation: PASS
- Maven extension compilation: PASS
- retained `Sprint4UiTestSuite`: PASS, 26 checks
- `Sprint6AuthorizationUiTestSuite`: PASS, 27 assertions
- report UI and JSON export UI are backed by the same S6 authorization product workspace
- report boundary preserves FindingCandidate as review-only and records confirmedFindingCount=0

### S6 performance/security closure verified

GitHub Actions run `35890873379` completed successfully at commit `76389e13b996a02c9f7011b262364b5b03a9231d`.

- `Sprint6PerformanceObservationTestSuite`: PASS, 12 assertions
- `Sprint6SecurityHardeningTestSuite`: PASS, 13 assertions
- 100 / 1,000 / 10,000 policy-resolution workloads completed
- policy-aware recommendation and report-generation workloads completed
- missing / ambiguous / spoofed authenticated-context references rejected
- path / non-authentication header / resource drift rejected
- unchanged authentication material rejected
- raw viewer/admin credentials excluded from serialized test state
- conflicting policy produces no executable S6 active seed

Observed CI timings are engineering observations only:
- 100: resolve 34 ms, planning 1 ms, report 9 ms, approx memory delta 2,948,768 bytes
- 1,000: resolve 57 ms, planning 2 ms, report 6 ms, approx memory delta 19,825,512 bytes
- 10,000: resolve 169 ms, planning 4 ms, report 29 ms, approx memory delta 10,003,728 bytes

Memory deltas are JVM-process observations affected by allocation/GC and are not monotonic or release thresholds.

### Legacy Sprint 2 / Sprint 3 CI closure

The retained Sprint 2 and Sprint 3 local-contract test/lab sources were restored from repository commit
`07a3f60bc2bd2218d0749e497b4f0b9dc57a9c2c`; they were not reconstructed from chat.

Current CI:
- Sprint 2 run `35890873262`: SUCCESS; Maven package PASS, Sprint2 tests=52, EXP-INTEGRATION-001 local-lab PASS
- Sprint 3 run `35890873276`: SUCCESS; Maven package PASS, Sprint2 tests=52, Sprint3 core=47, Sprint3 adapter=11, EXP-RECON-001 local-lab PASS
- official Maven test compilation excludes the legacy local-Montoya-stub suites because those suites target the preserved stub contract rather than the current official Montoya test interface
- real Burp desktop runtime remains a separate UNVERIFIED/BLOCKED gate

### Sprint 7 verified progress

Phases 1–6 and the final Sprint 7 closure are complete on `s7-workflow-token-binding`.

Latest verified gate: GitHub Actions run `35960826621`.

Phase 3 now adds:
- controlled secure/vulnerable workflow transition fixtures
- automatic policy-aware `WORKFLOW_TRANSITION` planning
- one-variable target-state mutation with request-equivalence invariants
- state-changing safety classification without destructive auto-execution
- existing Sprint 4 planner/queue/safety/executor reuse
- secure DENY / `EXPECTED_CHANGE` control
- deliberately vulnerable ALLOW / `UNEXPECTED_CHANGE` control
- secret-safe generated-test serialization
- fail-closed conflict handling
- retained Sprint 6 regression PASS

Phase 4 now adds:
- deterministic workflow transition coverage identities and lifecycle
- policy/resolution → plan → execution → observation evidence tracking
- explicit unresolved/planning/observation coverage gaps and ratios
- fail-closed policy-resolution drift handling
- S7 workflow product workspace
- Burp Workflow Overview / Workflow Map / Transition Matrix / Policy Conflicts / Coverage views
- Maven extension compilation plus retained S4/S6 UI regressions

These are controlled localhost validation results only and do not establish real-world scanner accuracy.

### Sprint 7 final closure

- final workflow: GitHub Actions run `35960826621` — SUCCESS
- closure candidate checkpoint: `acra-sprint-07-final.zip`
- closure candidate SHA-256: `0f69ce2e770dc5010f17fe00474455d18f34de11e02460889d428a87a7af9b4d`
- package entries: 791
- unsafe paths: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- official Maven package: PASS
- retained Sprint 2 / Sprint 3 local-contract regressions: PASS
- retained Sprint 4 / Sprint 6 / Sprint 7 UI regressions: PASS
- Sprint 8: NOT STARTED

### Residual validation debt

Real Burp desktop runtime/load/handler/UI validation remains UNVERIFIED / DEFERRED. Controlled localhost fixtures
do not establish real-world scanner accuracy, production authentication behavior or external-target safety.

Canonical final-status checkpoint SHA-256: external sidecar generated by the final-status verification run.
