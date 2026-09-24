# Sprint 11 — Research Evaluation & Ablation

Status: **IN PROGRESS — Phases 1–10 VERIFIED**  
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

## Phase 3 — deterministic treatment/prediction adapters

Implemented:

- `AblationPredictionState`;
- `AblationDimensionEvidence`;
- `AblationCaseEvidence`;
- `AblationPredictionResult`;
- `S11AblationPredictionAdapter`;
- deterministic evidence filtering by enabled variant dimensions;
- ground-truth-free prediction input schema;
- baseline-only A0 behavior;
- cumulative A1→A7 treatment refinement;
- explicit INCONCLUSIVE state for missing enabled dimension evidence;
- deterministic result IDs/fingerprints;
- duplicate dimension evidence rejection.

The adapter never receives `ResearchGroundTruth`. This is enforced in the Phase 3 test by reflecting the
prediction-input record schema and rejecting any ground-truth component/type.

### Phase 3 verification

GitHub Actions run `36057869888`: **SUCCESS** at source commit
`dc3c081bf033b14afcea3185c3d54fb241be1772`.

Verified:

- `Sprint11AblationPredictionAdapterTestSuite`: PASS, 59 assertions;
- Phase 1 protocol suite: PASS, 43 assertions;
- Phase 2 dataset suite: PASS, 41 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Phase 3 is **VERIFIED COMPLETE**.

## Execution readiness finding

Repository inspection after Phase 3 confirmed that the current S4 ACRA-Lab exposes only a subset of the behaviors
described by the 15-case research fixture. Therefore Sprint 11 does **not** promote the campaign to executed and
does not manufacture per-dimension evidence.

## Phase 4 — deterministic campaign plan and coverage denominator

Implemented:

- `AblationCampaignCellState`;
- `AblationCampaignCell`;
- `AblationCampaignCoverage`;
- `S11AblationCampaignPlan`;
- exact 15 × 8 = 120 campaign cells;
- deterministic case-major ordering with A0→A7 inside each case;
- deterministic unique cell IDs;
- required dimensions bound to the verified protocol;
- explicit campaign states: PLANNED / EVIDENCE_READY / EXECUTED / INCONCLUSIVE / BLOCKED;
- canonical initial state with all 120 cells PLANNED;
- no ground-truth or prediction fields in campaign cells;
- duplicate-cell and protocol-dimension-drift rejection.

### Phase 4 verification

GitHub Actions run `36058274408`: **SUCCESS** at source commit
`88f7cd78161267a1900ef4b663258938ee208d9b`.

Verified:

- `Sprint11AblationCampaignPlanTestSuite`: PASS, 416 assertions;
- Phase 3 prediction adapter: PASS, 59 assertions;
- Phase 2 dataset manifest: PASS, 41 assertions;
- Phase 1 protocol foundation: PASS, 43 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Canonical campaign coverage after planning:

- total: 120;
- PLANNED: 120;
- EVIDENCE_READY: 0;
- EXECUTED: 0;
- INCONCLUSIVE: 0;
- BLOCKED: 0.

Phase 4 is **VERIFIED COMPLETE**.

## Phase 5 — evidence-readiness mapping

Implemented:

- `GT-S11-EVIDENCE-READINESS`;
- `FixtureReadinessState`;
- `ResearchFixtureReadiness`;
- `S11EvidenceReadinessManifest`;
- exact mapping of all 15 dataset cases to current Sprint 4 lab/test evidence;
- conservative READY / PARTIAL / MISSING_FIXTURE taxonomy;
- deterministic readiness identity/fingerprint;
- explicit rule that fixture readiness does not equal ablation-dimension evidence readiness.

Current evidence-readiness result:

| State | Cases | Interpretation |
|---|---:|---|
| READY | 3 | dedicated/current live behavior sufficiently matches the registered control |
| PARTIAL | 4 | relevant live behavior exists, but two effects are currently combined |
| MISSING_FIXTURE | 8 | no dedicated executable fixture currently maps the registered positive case |

READY cases:

- `S4-FP-PUBLIC`;
- `S4-FP-SOFT-DENY`;
- `S4-FP-REPRESENTATION`.

PARTIAL cases:

- `S4-FP-TIMESTAMP` — timestamp and request ID currently vary together;
- `S4-FP-REQUEST-ID` — request ID and timestamp currently vary together;
- `S4-FP-ORDERING` — order and formatting currently change together;
- `S4-FP-FORMATTING` — formatting and order currently change together.

All eight registered false-negative/positive research controls remain `MISSING_FIXTURE`.

### Phase 5 verification

GitHub Actions run `36058945211`: **SUCCESS** at source commit
`022bf71f31f69633b5eac541b230283e8536decd`.

Verified:

- repository-backed readiness contract: PASS, ready=3 / partial=4 / missing=8 / executed=0;
- `Sprint11EvidenceReadinessManifestTestSuite`: PASS, 57 assertions;
- Phase 4 campaign plan: PASS, 416 assertions;
- Phase 3 prediction adapter: PASS, 59 assertions;
- Phase 2 dataset manifest: PASS, 41 assertions;
- Phase 1 protocol: PASS, 43 assertions;
- exact Java 21 compilation with warnings as errors: PASS;
- Maven core `test-compile`: PASS.

Phase 5 is **VERIFIED COMPLETE**.

## Phase 6 — controlled research-lab expansion

Implemented:

- `GT-S11-RESEARCH-LAB-FIXTURES` with 12 new independently declared localhost fixtures;
- 4 isolated negative controls for timestamp, request ID, JSON ordering and formatting;
- 8 dedicated positive controls for same-status, soft-denial, dynamic-length, reordered-JSON, opaque-ID,
  nested-resource, collection-membership and nonstandard-authentication cases;
- neutral live routes `/api/v1/s11/research/case-NNN` that do not encode research labels;
- secure/vulnerable behavior implemented only in controlled loopback ACRA-Lab;
- `Sprint11ControlledResearchLabFixtureTestSuite`;
- live CI startup for secure port 18082 and vulnerable port 18081;
- independent live assertions for authorization semantics, structural/semantic equivalence, collection membership,
  nested resource evidence, opaque IDs and nonstandard synthetic authentication;
- explicit checks that live positive-fixture responses contain neither `POSITIVE` nor `expected_candidate`;
- promoted readiness map: 15 READY / 0 PARTIAL / 0 MISSING_FIXTURE.

### Phase 6 verification

GitHub Actions run `36060118721`: **SUCCESS** at source commit
`ef25d5aeca247c32f2b94bc8faac56405cc8bb58`.

Verified evidence:

- readiness contract: PASS, ready=15 / partial=0 / missing=0 / executed=0;
- research-lab ground truth: PASS, 12 fixtures = 4 isolated negative + 8 positive;
- secure/vulnerable localhost startup: PASS;
- `Sprint11ControlledResearchLabFixtureTestSuite`: PASS, 65 assertions;
- Phase 1 protocol: PASS, 43 assertions;
- Phase 2 dataset: PASS, 41 assertions;
- Phase 3 prediction adapter: PASS, 59 assertions;
- Phase 4 campaign plan: PASS, 416 assertions;
- Phase 5 readiness manifest: PASS, 72 assertions;
- Maven core `test-compile`: PASS;
- Core CI / Sprint 2 CI / Sprint 3 CI: PASS at the exact Phase 6 head.

Phase 6 is **VERIFIED COMPLETE**.

The canonical campaign still remains:

- 120 PLANNED;
- 0 EVIDENCE_READY;
- 0 EXECUTED;
- A0–A7 metrics NOT_RUN.

Fixture readiness is necessary but is still not equivalent to treatment-dimension evidence readiness.

## Phase 7 — treatment evidence collection and campaign readiness

Implemented:

- `AblationEvidenceDisposition`;
- `AblationDimensionEvidenceReference`;
- `AblationEvidenceBundle`;
- `S11TreatmentEvidenceCollector`;
- `S11CampaignEvidenceReadinessProjector`;
- live `Sprint11TreatmentEvidenceCollectionTestSuite`;
- stable response normalization before evidence hashing;
- hash-only persisted evidence references;
- explicit `OBSERVED` / `NOT_APPLICABLE` dimension accounting;
- deterministic baseline evidence and bundle fingerprints;
- ground-truth-free and prediction-free evidence-bundle schema;
- deterministic campaign promotion from PLANNED to EVIDENCE_READY only when required evidence is complete.

Evidence completeness behavior:

- all 15 controlled cases produce bundles;
- every bundle contains baseline evidence;
- every bundle accounts for all 7 cumulative A1–A7 dimensions;
- all bundles are complete through A7;
- public controls explicitly mark identity/role as NOT_APPLICABLE;
- non-workflow research routes explicitly mark workflow as NOT_APPLICABLE;
- authenticated controls record identity/role as OBSERVED;
- persisted evidence IDs are SHA-256-derived and do not contain raw principal, tenant or response resource values.

### Phase 7 verification

GitHub Actions run `36064434979`: **SUCCESS** at source commit
`9f9af8ce15e5035def2605c98d8f8153728c3eed`.

Verified evidence:

- `Sprint11TreatmentEvidenceCollectionTestSuite`: PASS, 725 assertions;
- controlled research fixture suite: PASS, 65 assertions;
- Phase 1 protocol: PASS, 43 assertions;
- Phase 2 dataset: PASS, 41 assertions;
- Phase 3 prediction adapter: PASS, 59 assertions;
- Phase 4 campaign plan: PASS, 416 assertions;
- Phase 5 readiness manifest: PASS, 72 assertions;
- Maven core `test-compile`: PASS;
- Core CI / Sprint 2 CI: PASS at exact Phase 7 head;
- Sprint 3 CI remained running at the moment Phase 7 was recorded and is not used as the Phase 7 acceptance gate.

Canonical campaign state after evidence projection:

- EVIDENCE_READY: 120;
- PLANNED: 0;
- EXECUTED: 0;
- INCONCLUSIVE: 0;
- BLOCKED: 0.

Phase 7 is **VERIFIED COMPLETE**.

## Phase 8 — ground-truth-free prediction execution

Implemented:

- `S11TreatmentPredictionSignalBuilder`;
- `S11AblationPredictionExecutor`;
- `S11PredictionExecutionSnapshot`;
- live `Sprint11PredictionExecutionTestSuite`;
- deterministic baseline prediction from raw differential behavior;
- cumulative ownership/tenant refinements from response semantic evidence;
- semantic-treatment refinement using normalized semantic equivalence;
- execution only from a fully EVIDENCE_READY campaign;
- exactly one prediction result for every case×variant cell;
- deterministic result IDs/fingerprints and execution snapshot identity;
- executed campaign projection;
- explicit rejection of missing case evidence and non-evidence-ready plans;
- prediction-execution schema with no ground-truth or metric fields.

### Phase 8 verification

GitHub Actions run `36064798175`: **SUCCESS** at source commit
`c742014ab97d024be42425fb4e49866f565389ec`.

Verified evidence:

- `Sprint11PredictionExecutionTestSuite`: PASS, 382 assertions;
- treatment evidence collection: PASS, 725 assertions;
- controlled research fixture suite: PASS, 65 assertions;
- Phase 1 protocol: PASS, 43 assertions;
- Phase 2 dataset: PASS, 41 assertions;
- Phase 3 adapter: PASS, 59 assertions;
- Phase 4 campaign plan: PASS, 416 assertions;
- Phase 5 readiness manifest: PASS, 72 assertions;
- Maven core `test-compile`: PASS;
- Sprint 3 CI: PASS at exact Phase 8 head.

Canonical prediction-execution state:

- results: 120;
- EXECUTED cells: 120;
- EVIDENCE_READY cells: 0;
- PLANNED cells: 0;
- metric evaluation: NOT_RUN.

Phase 8 is **VERIFIED COMPLETE**.

## Phase 9 — one-way ablation evaluation and controlled metrics

Implemented:

- `S11VariantEvaluation`;
- `S11AblationEvaluationReport`;
- `S11AblationEvaluator`;
- live `Sprint11AblationEvaluationTestSuite`;
- one-way prediction→ground-truth join after Phase 8 execution;
- 120 labelled `ResearchExecutionRecord` entries;
- one independent evaluation summary per A0–A7 variant;
- TP / TN / FP / FN;
- precision / recall / F1 with existing `OptionalDouble` denominator semantics;
- evidence completeness kept separate from classification accuracy;
- deterministic evaluation identity/fingerprint;
- immutability checks proving evaluation does not modify prediction execution artifacts.

### Phase 9 verification

GitHub Actions run `36065439110`: **SUCCESS** at source commit
`2f17a994e4c481b48ae8ff11734a2ee17e32b84d`.

Verified evidence:

- `Sprint11AblationEvaluationTestSuite`: PASS, 99 assertions;
- prediction execution: PASS, 384 assertions;
- treatment evidence collection: PASS, 725 assertions;
- controlled research fixture suite: PASS, 65 assertions;
- Phase 1 protocol: PASS, 43 assertions;
- Phase 2 dataset: PASS, 41 assertions;
- Phase 3 adapter: PASS, 59 assertions;
- Phase 4 campaign plan: PASS, 416 assertions;
- Phase 5 readiness manifest: PASS, 72 assertions;
- Maven core `test-compile`: PASS.

### Controlled measured results

| Variant | TP | TN | FP | FN | Precision | Recall | F1 | Evidence completeness |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A1 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A2 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A3 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A4 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A5 | 8 | 0 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 | 1.000000 |
| A6 | 8 | 7 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 | 1.000000 |
| A7 | 8 | 7 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 | 1.000000 |

Interpretation is intentionally limited: on this controlled synthetic dataset, semantic evidence at A6 removes
the raw-differential false positives while cumulative contextual conflict evidence prevents semantic equivalence
from erasing authorization-relevant collection-membership changes. The dataset is too small and synthetic to
support real-world accuracy or novelty claims.

### Defect found by the evaluation gate

The first Phase 9 run exposed a cumulative-ablation defect: A6 semantic equivalence replaced earlier contextual
ownership/tenant signals, producing one false negative in the collection-membership control. The implementation was
corrected so semantic evidence is cumulative rather than destructive, and a Phase 8 regression now protects that
case.

Phase 9 is **VERIFIED COMPLETE**.

## Phase 10 — deterministic research report/export

Implemented:

- `S11ResearchVariantMetric`;
- `S11ResearchReport`;
- `S11ResearchReportGenerator`;
- `S11ResearchExportArtifact`;
- `S11ResearchReportExporter`;
- `Sprint11ResearchReportExportTestSuite`;
- minimized report schema with protocol/dataset/prediction/evaluation provenance only;
- canonical JSON export;
- deterministic Markdown export;
- mandatory controlled-dataset and non-generalization limitations;
- raw HTTP bodies, Authorization material, bearer tokens and synthetic fixture payload identifiers excluded;
- CI evidence artifact upload.

### Phase 10 verification

GitHub Actions run `36065934911`: **SUCCESS** at source commit
`bcc41630fa4d7c9a4f11023e9aeb8b6561f67310`.

Verified:

- report/export suite: PASS, 31 assertions;
- prediction execution: PASS, 384 assertions;
- ablation evaluation: PASS, 99 assertions;
- full retained Sprint 11 verifier: PASS;
- Maven core `test-compile`: PASS;
- JSON/Markdown artifact upload: PASS.

Canonical digests:

- JSON: `595dba16b9d724d67dbdd1dc4faeacd76432661b52ead780b14054acee67c04d`;
- Markdown: `1d4c6b2f9f7f88f443f9d2cdef9b266cff4e9307d9967f789c44a570d5766d8b`.

Phase 10 is **VERIFIED COMPLETE**.

## Next dependency

Sprint 11 final closure must re-run the complete S11 stack plus retained regressions, execute official Maven
packaging, create a deterministic source checkpoint, verify archive path/duplicate/extraction/per-file integrity,
and preserve the controlled-research scope limitations. Only that closure gate may promote Sprint 11 to SOFTWARE
COMPLETE.
