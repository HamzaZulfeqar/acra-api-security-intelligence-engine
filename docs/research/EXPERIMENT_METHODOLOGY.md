# Experiment Methodology

## Control

Naive differential analysis based on isolated request mutation and basic response comparison.

## Treatment

Context-aware analysis using API understanding, Security Context, controlled differential testing, response semantics, authorization reasoning, and evidence correlation.

## Ablation sequence

- A0: naive differential
- A1: A0 + identity
- A2: A1 + resource ownership
- A3: A2 + tenant
- A4: A3 + role
- A5: A4 + workflow
- A6: A5 + semantic evidence
- A7: full ACRA correlation

## Required metrics

- TP
- FP
- TN
- FN
- Precision
- Recall
- F1
- Evidence completeness

No metric value may be recorded as an actual result until the corresponding experiment has executed against registered ground truth.

## Sprint 11 machine-enforced protocol

The methodology above is now represented by `S11AblationProtocol` version
`s11-ablation-protocol-v1`.

| Variant | Added dimension | Execution state |
|---|---|---|
| A0 | Naive differential baseline | NOT_RUN |
| A1 | Identity | NOT_RUN |
| A2 | Ownership | NOT_RUN |
| A3 | Tenant | NOT_RUN |
| A4 | Role | NOT_RUN |
| A5 | Workflow | NOT_RUN |
| A6 | Semantic evidence | NOT_RUN |
| A7 | Evidence correlation / full registered correlation stack | NOT_RUN |

The protocol requires all eight registered metrics and a deterministic SHA-256 fingerprint. Variant ordering is
cumulative and fail-closed. Protocol verification is software evidence only; scientific metric values remain
forbidden until actual controlled execution against registered ground truth.

Phase 1 verification: GitHub Actions run `36056957706` — SUCCESS.

