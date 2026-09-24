# Sprint 11 Research Evaluation & Ablation Architecture

## Boundary

Sprint 11 turns the existing research methodology into executable, deterministic research infrastructure. It does
not add a new vulnerability family and does not broaden active target scope.

```text
Registered methodology
        |
        v
S11AblationProtocol
        |
        +--> A0 naive differential
        +--> A1 + identity
        +--> A2 + ownership
        +--> A3 + tenant
        +--> A4 + role
        +--> A5 + workflow
        +--> A6 + semantic evidence
        +--> A7 + evidence correlation
        |
        v
S11EvaluationDatasetManifest
        |
        v
future controlled treatment adapters
        |
        v
ResearchExecutionRecord
        |
        v
ResearchMetrics + evidence completeness
```

## Phase 1 invariants

1. A0 through A7 exist exactly once and in deterministic order.
2. Every treatment is cumulative; a later treatment cannot silently remove an earlier dimension.
3. A0 enables no contextual dimensions.
4. A7 enables the full registered correlation stack.
5. Required metrics are TP, TN, FP, FN, precision, recall, F1 and evidence completeness.
6. Protocol identity and fingerprint are deterministic.
7. A protocol definition is not an experiment result.
8. Phase 1 execution state remains `NOT_RUN`.
9. No metric may be promoted from NOT_RUN without registered ground truth and actual controlled execution.
10. Phase 1 scope is controlled registered ground truth only; it does not authorize external-target execution.
11. Real Burp desktop validation remains a separate runtime lane.

## Why A7 adds evidence correlation

The repository methodology describes A7 as “full ACRA correlation.” The machine protocol represents the final
increment explicitly as `EVIDENCE_CORRELATION`, after semantic evidence at A6. This keeps each ablation step
machine-auditable and prevents “full” from becoming an undefined label.

## Phase 1 verification

GitHub Actions run `36056957706` — SUCCESS at
`c4cff92b7668a75db2eb70ea550e7dd807d5a755`.

## Phase 2 dataset boundary

Sprint 11 now registers `GT-S11-ABLATION-DATASET` and a deterministic
`S11EvaluationDatasetManifest`.

Phase 2 invariants:

1. Dataset labels are independent of treatment output.
2. Every dataset case points back to exactly one source case in `GT-S4-RESEARCH-FIXTURES`.
3. Source `expected_candidate=true` maps to POSITIVE and `false` maps to NEGATIVE.
4. All 15 source cases are included exactly once.
5. Dataset composition is 8 POSITIVE and 7 NEGATIVE controls.
6. Dataset order, identity and fingerprint are deterministic.
7. The dataset manifest cannot self-promote execution from NOT_RUN.
8. S6–S10 policy-decision fixtures are excluded from binary vulnerability labelling unless a separate mapping is registered.
9. Dataset registration does not calculate or publish A0–A7 metrics.

Phase 2 verification: GitHub Actions run `36057476360` — SUCCESS at
`dff822e02299b887869bc43923d93a62d9e7d35f`.

## Phase 3 prediction boundary

`S11AblationPredictionAdapter` consumes `AblationCaseEvidence`, which contains no ground-truth component.

```text
baseline prediction + evidence
        |
        +--> identity evidence
        +--> ownership evidence
        +--> tenant evidence
        +--> role evidence
        +--> workflow evidence
        +--> semantic evidence
        +--> correlation evidence
        |
        v
variant-specific evidence filter
        |
        +--> complete enabled evidence -> RESOLVED prediction
        |
        +--> missing enabled evidence -> INCONCLUSIVE / no binary prediction
```

Phase 3 invariants:

1. Ground truth is not an input to prediction.
2. A variant cannot consume dimensions beyond its enabled cumulative set.
3. Every consumed dimension contributes explicit evidence IDs.
4. Missing enabled evidence fails closed as INCONCLUSIVE.
5. INCONCLUSIVE results carry no binary prediction.
6. Prediction identity/fingerprint is deterministic.
7. Prediction generation remains separate from metric aggregation.

Phase 3 verification: GitHub Actions run `36057869888` — SUCCESS at
`dc3c081bf033b14afcea3185c3d54fb241be1772`.

## Phase 4 campaign-plan boundary

`S11AblationCampaignPlan` fixes the experiment denominator before evidence collection:

```text
15 dataset cases
      x
8 ablation variants
      =
120 deterministic campaign cells
```

Each cell carries only case ID, variant ID, required dimensions and lifecycle state. It carries neither
`ResearchGroundTruth` nor a prediction.

Phase 4 invariants:

1. Every dataset case has exactly eight A0–A7 cells.
2. Every variant spans exactly fifteen cases.
3. Required dimensions must match the verified protocol exactly.
4. Cell IDs and campaign fingerprints are deterministic.
5. Initial canonical coverage is 120 PLANNED and 0 EXECUTED.
6. Planning cannot generate research metrics.
7. Ground truth remains outside the campaign-cell schema.
8. Duplicate case/variant cells fail closed.

Phase 4 verification: GitHub Actions run `36058274408` — SUCCESS at
`88f7cd78161267a1900ef4b663258938ee208d9b`.

## Phase 5 fixture-readiness boundary

Fixture readiness is intentionally separate from treatment evidence readiness.

```text
registered dataset case
        |
        v
existing localhost route/test support
        |
        +--> READY
        +--> PARTIAL
        +--> MISSING_FIXTURE
        |
        v
NO automatic campaign-cell promotion
```

Phase 5 invariants:

1. Every dataset case has exactly one readiness record.
2. READY/PARTIAL states require explicit routes and executable-evidence references.
3. MISSING_FIXTURE claims neither route nor evidence reference.
4. Existing combined-effect tests are PARTIAL rather than overstated as isolated controls.
5. Readiness mapping remains NOT_RUN and executes no experiment.
6. Fixture readiness alone cannot move a campaign cell to EVIDENCE_READY.
7. Readiness identity/fingerprint is deterministic.

Verified mapping:

- READY: 3;
- PARTIAL: 4;
- MISSING_FIXTURE: 8;
- EXECUTED: 0.

Phase 5 verification: GitHub Actions run `36058945211` — SUCCESS at
`022bf71f31f69633b5eac541b230283e8536decd`.

## Phase 6 controlled-lab boundary

Sprint 11 now exposes dedicated loopback-only fixtures for every registered dataset case.

```text
independent research ground truth
        |
        v
neutral /case-NNN route
        |
        +--> secure localhost mode
        +--> vulnerable localhost mode
        |
        v
live fixture verification
        |
        v
fixture readiness = READY
```

Phase 6 invariants:

1. Route names do not encode POSITIVE/NEGATIVE, FP/FN or expected-candidate labels.
2. Independent research labels remain in ground-truth manifests, not live responses.
3. All new fixtures are synthetic and loopback-only.
4. Negative controls isolate timestamp, request ID, ordering and formatting individually.
5. Positive controls cover same-status, soft-denial, dynamic-length, reordered JSON, opaque ID, nested resource,
   collection membership and nonstandard authentication.
6. Live fixture verification checks secure/vulnerable behavior directly.
7. Fixture readiness becomes 15 READY / 0 PARTIAL / 0 MISSING_FIXTURE.
8. Fixture readiness still does not promote campaign cells or calculate research metrics.

Phase 6 verification: GitHub Actions run `36060118721` — SUCCESS at
`ef25d5aeca247c32f2b94bc8faac56405cc8bb58`.

## Phase 7 treatment-evidence boundary

Phase 7 persists only evidence references and applicability state.

```text
live response pair + non-secret request context
        |
        v
stable response normalization
        |
        v
hash-only baseline evidence
        |
        +--> identity
        +--> ownership
        +--> tenant
        +--> role
        +--> workflow
        +--> semantic evidence
        +--> correlation
        |
        v
AblationEvidenceBundle
        |
        v
campaign readiness projection
        |
        +--> complete -> EVIDENCE_READY
        +--> incomplete -> PLANNED
```

Phase 7 invariants:

1. Ground truth is not a collector input or evidence-bundle field.
2. Prediction is not an evidence-bundle field.
3. Raw response bodies are not persisted as evidence references.
4. Raw principal/tenant/resource values do not appear in evidence IDs.
5. Volatile response fields are normalized before deterministic hashing.
6. NOT_APPLICABLE is explicit and still evidence-backed.
7. A cell may become EVIDENCE_READY only when baseline and all required dimensions are accounted for.
8. Evidence projection cannot produce EXECUTED state.
9. Removing one case bundle removes readiness from exactly that case's eight A0–A7 cells.

Phase 7 verification: GitHub Actions run `36064434979` — SUCCESS at
`9f9af8ce15e5035def2605c98d8f8153728c3eed`.

Verified campaign readiness: 120 EVIDENCE_READY / 0 EXECUTED.

## Phase 8 prediction-execution boundary

Phase 8 introduces an explicit one-way transition from evidence-ready inputs to executed predictions:

```text
EVIDENCE_READY campaign + case prediction evidence
        |
        v
S11AblationPredictionAdapter
        |
        v
120 deterministic AblationPredictionResult records
        |
        v
S11PredictionExecutionSnapshot
        |
        v
campaign state = EXECUTED
```

Phase 8 invariants:

1. Prediction execution accepts only a fully EVIDENCE_READY campaign.
2. Independent ground truth is not an execution input.
3. Metric computation is not an execution responsibility.
4. Every campaign cell receives exactly one explicit prediction result.
5. Complete controlled evidence produces RESOLVED predictions.
6. Missing case evidence fails closed.
7. Execution identity and fingerprint are deterministic.
8. The execution snapshot contains neither ground-truth nor metric fields.
9. Prediction execution may change campaign state to EXECUTED but cannot evaluate correctness.

Phase 8 verification: GitHub Actions run `36064798175` — SUCCESS at
`c742014ab97d024be42425fb4e49866f565389ec`.

Verified state: 120 prediction results / 120 EXECUTED cells / metrics NOT_RUN.

## Phase 9 evaluation boundary

Ground truth enters Sprint 11 only after predictions are complete:

```text
immutable prediction execution
        +
immutable GT-S11-ABLATION-DATASET
        |
        v
S11AblationEvaluator
        |
        +--> 120 labelled ResearchExecutionRecord values
        +--> A0..A7 confusion matrices
        +--> precision / recall / F1
        +--> evidence completeness
        |
        v
deterministic S11AblationEvaluationReport
```

Phase 9 invariants:

1. Ground truth is introduced only after prediction execution.
2. Evaluation cannot change prediction IDs, fingerprints or values.
3. Every variant keeps a fixed fifteen-case denominator.
4. Evidence completeness is independent of classification metrics.
5. Missing evidence can reduce evidence completeness without silently changing TP/TN/FP/FN.
6. Evaluation identity/fingerprint is deterministic.
7. Controlled metrics cannot be generalized beyond the registered synthetic localhost dataset.
8. Ablation treatment is cumulative: semantic evidence may suppress raw-only differences but cannot erase earlier
   ownership/tenant conflict evidence.

Phase 9 verification: GitHub Actions run `36065439110` — SUCCESS at
`2f17a994e4c481b48ae8ff11734a2ee17e32b84d`.

Measured controlled results:

- A0–A5: TP=8, TN=0, FP=7, FN=0, precision=.533333, recall=1, F1=.695652;
- A6–A7: TP=8, TN=7, FP=0, FN=0, precision=1, recall=1, F1=1;
- evidence completeness: 1.0 for every variant.

## Phase 10 reporting boundary

The reporting layer projects only minimized evaluation state:

```text
protocol + dataset + immutable evaluation
        |
        v
S11ResearchReportGenerator
        |
        v
minimized S11ResearchReport
        |
        +--> canonical JSON + SHA-256
        +--> deterministic Markdown + SHA-256
```

Phase 10 invariants:

1. Report generation does not recompute predictions or metrics.
2. Report identity derives from immutable protocol/dataset/evaluation provenance and metric rows.
3. Raw HTTP request/response bodies are not report fields.
4. Authorization headers, bearer tokens and synthetic token material are not report fields.
5. Dataset-scope limitations are mandatory.
6. Real-world scanner accuracy and novelty are explicitly not claimed.
7. JSON and Markdown exports are deterministic and digest-addressable.

Phase 10 verification: GitHub Actions run `36065934911` — SUCCESS at
`bcc41630fa4d7c9a4f11023e9aeb8b6561f67310`.

- JSON SHA-256: `595dba16b9d724d67dbdd1dc4faeacd76432661b52ead780b14054acee67c04d`;
- Markdown SHA-256: `1d4c6b2f9f7f88f443f9d2cdef9b266cff4e9307d9967f789c44a570d5766d8b`.

## Final-closure dependency

Sprint 11 may be promoted to SOFTWARE COMPLETE only after a dedicated closure workflow verifies the entire S11
stack, retained regressions, official package build and deterministic archive integrity.
