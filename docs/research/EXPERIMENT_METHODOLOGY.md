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
