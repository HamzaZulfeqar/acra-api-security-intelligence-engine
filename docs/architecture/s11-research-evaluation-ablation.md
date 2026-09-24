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

## Phase 3 dependency

Treatment adapters must consume case evidence and enabled ablation dimensions without receiving the independent
ground-truth label as prediction input. Prediction generation and metric aggregation remain separate boundaries.
