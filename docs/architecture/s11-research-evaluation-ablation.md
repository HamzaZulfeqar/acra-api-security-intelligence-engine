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

## Phase 6 dependency

The localhost lab must add isolated controls for the four PARTIAL cases and dedicated behaviors for the eight
MISSING_FIXTURE positive cases. Those fixtures must be independently live-tested before readiness is upgraded.
