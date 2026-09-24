# Sprint 11 — Research Evaluation & Ablation

Status: **IN PROGRESS — Phases 1–2 VERIFIED**  
Branch: `s11-research-evaluation-ablation`  
Immutable Sprint 10 base: `59022c4a25718f38ea7ec2f010911344d1aa0698`  
Sprint 10 post-documentation final closure: run `36056032234` — SUCCESS.

## Why Sprint 11 exists

The repository already defines the A0–A7 ablation methodology and required research metrics, but the experiment
registry still records A0–A7 as NOT_RUN. Sprint 10 completed the authorization software needed to make a controlled
evaluation meaningful. The next dependency-ready milestone is therefore research evaluation, not new attack
surface expansion.

Sprint 11 does not treat planned experiments as measured evidence. It separates:

1. protocol definition;
2. registered controlled ground truth;
3. deterministic treatment adapters;
4. controlled execution;
5. metric capture and evidence completeness;
6. reproducible reporting and closure.

Real Burp desktop runtime remains a separate deferred validation lane and is not required to define the local
research protocol.

## Phase 1 — deterministic A0–A7 protocol foundation

Implemented:

- `AblationDimension`;
- `AblationExecutionState`;
- `ResearchMetricName`;
- `AblationVariant`;
- versioned `S11AblationProtocol`;
- exact cumulative A0→A7 ordering;
- A0 = naive differential baseline;
- A1 = + identity;
- A2 = + ownership;
- A3 = + tenant;
- A4 = + role;
- A5 = + workflow;
- A6 = + semantic evidence;
- A7 = + evidence correlation / full registered correlation stack;
- required metrics: TP, TN, FP, FN, precision, recall, F1, evidence completeness;
- deterministic protocol ID and SHA-256 fingerprint;
- explicit `NOT_RUN` execution state;
- controlled registered-ground-truth scope only;
- fail-closed validation for order, cumulative dimensions, incomplete metric contracts and fingerprint tampering.

### Phase 1 verification

GitHub Actions run `36056957706`: **SUCCESS** at source commit
`c4cff92b7668a75db2eb70ea550e7dd807d5a755`.

Verified evidence:

- `Sprint11ResearchAblationFoundationTestSuite`: PASS, 43 assertions;
- retained Sprint 10 foundation: PASS, 25 assertions;
- retained Sprint 9 property foundation: PASS, 15 assertions;
- retained Sprint 8 routing foundation: PASS, 16 assertions;
- retained Sprint 7 workflow foundation: PASS, 19 assertions;
- retained Sprint 6 policy foundation: PASS, 11 assertions;
- retained Sprint 5 closure suite: PASS, 21 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is **VERIFIED COMPLETE**.

## Research-claim boundary

Phase 1 does **not** claim:

- any A0–A7 experiment has executed;
- any TP/TN/FP/FN value for A0–A7;
- any precision, recall or F1 result for A0–A7;
- any evidence-completeness result for A0–A7;
- novelty validation;
- real-world scanner accuracy;
- real Burp desktop runtime validation.

The registry state is **PROTOCOL_DEFINED / NOT_RUN**.

## Phase 2 — controlled evaluation dataset manifest

Implemented:

- `GT-S11-ABLATION-DATASET`;
- `ResearchDatasetCase`;
- `S11EvaluationDatasetManifest`;
- deterministic 15-case ordering and dataset fingerprint;
- source provenance to `GT-S4-RESEARCH-FIXTURES`;
- exact source-label cross-check against `expected_candidate`;
- binary mapping: `true → POSITIVE`, `false → NEGATIVE`;
- 8 positive / 7 negative controls;
- explicit `NOT_RUN` state;
- explicit exclusion of S6–S10 policy ground truth from binary vulnerability labelling;
- fail-closed duplicate-source, single-class, self-promotion and fingerprint-tamper checks.

Why only the S4 research fixture is admitted:

`GT-S4-RESEARCH-FIXTURES` already declares an independent binary `expected_candidate` value for every case.
S6–S10 fixtures declare authorization policy outcomes such as ALLOW/DENY/INCONCLUSIVE; those are not automatically
equivalent to vulnerability-positive/vulnerability-negative research labels. Sprint 11 therefore excludes them
until a separate research-label mapping is explicitly registered.

### Phase 2 verification

GitHub Actions run `36057476360`: **SUCCESS** at source commit
`dff822e02299b887869bc43923d93a62d9e7d35f`.

Verified:

- JSON source/manifest cross-check: PASS, 15 cases / 8 positive / 7 negative;
- `Sprint11EvaluationDatasetManifestTestSuite`: PASS, 41 assertions;
- Phase 1 ablation foundation: PASS, 43 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Phase 2 is **VERIFIED COMPLETE**.

## Next dependency

Phase 3 must implement deterministic treatment/prediction adapters for A0–A7. Those adapters may transform
registered case evidence into `ResearchPrediction` values, but Phase 3 must still keep the overall campaign
state NOT_RUN and must not publish measured A0–A7 metrics until a later controlled execution phase.
