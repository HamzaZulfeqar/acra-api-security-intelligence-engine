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


## Sprint 11 measured controlled evaluation — 2026-09-25

A0–A7 were evaluated only after prediction execution was frozen and joined one-way to
`GT-S11-ABLATION-DATASET`.

- Dataset: 15 synthetic localhost cases (8 positive, 7 negative).
- A0–A5: TP=8, TN=0, FP=7, FN=0, precision=.533333, recall=1, F1=.695652.
- A6–A7: TP=8, TN=7, FP=0, FN=0, precision=1, recall=1, F1=1.
- Evidence completeness: 1.0 for all variants.

The initial evaluation exposed a cumulative-composition defect in which A6 semantic equivalence could erase an
earlier ownership/tenant conflict. That defect was corrected before accepting the measurement. The result therefore
measures the corrected cumulative treatment contract.

These numbers are controlled synthetic measurements only. They must not be presented as real-world scanner
accuracy, external API prevalence, production effectiveness or proof of research novelty.

