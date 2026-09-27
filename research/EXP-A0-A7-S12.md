# EXP-A0-A7 — Sprint 12 Controlled Authorization Ablation Results

**State:** COMPLETED_CONTROLLED_LOCAL  
**Dataset:** `GT-S11-AUTHORIZATION-RESEARCH`  
**Workflow:** `36265834848` — SUCCESS  
**Executing source:** `d467137b3835e34ebd887cea328eda5300b72109`  
**Dataset SHA-256:** `f42783717bdd38e8d04b7f59cef98a41441005de426ec78ca1fba58098154d4b`

## Measured campaign

The campaign executed 16 frozen cases across eight cumulative variants, producing 128 prediction rows.

| Variant | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 1 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 |
| A1 | 8 | 1 | 7 | 0 | 0.533333 | 1.000000 | 0.695652 |
| A2 | 8 | 4 | 4 | 0 | 0.666667 | 1.000000 | 0.800000 |
| A3 | 8 | 5 | 3 | 0 | 0.727273 | 1.000000 | 0.842105 |
| A4 | 8 | 6 | 2 | 0 | 0.800000 | 1.000000 | 0.888889 |
| A5 | 8 | 7 | 1 | 0 | 0.888889 | 1.000000 | 0.941176 |
| A6 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |
| A7 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

## Artifact identity

- canonical JSON: `528b79b29a764a157e7b402e4202d84f6585c692ebc353cfae0ffd1c1ff364e7`
- CSV: `59333c885098c2cdd69eccab0d7ba6273b1d55fbaa2478b7dfaa05c7e51c3486`
- per-case JSONL: `7bb7c6780cb9360f63354f533e0ffc54a4d2db28c4b60ae2a858e9b417ba3d20`

The workflow executed the campaign twice and required byte-identical JSON, CSV and JSONL hashes.

## Interpretation within the registered fixture

- A1 produced no measurable change from A0 on this fixture.
- Adding ownership context at A2 removed three false positives.
- Tenant context at A3 removed one additional false positive.
- Role context at A4 removed one additional false positive.
- Workflow context at A5 removed one additional false positive.
- Semantic evidence at A6 removed the final false positive.
- A7 retained the same confusion matrix as A6, so this fixed dataset does not independently demonstrate additional classification value from the final correlation stage.

These are descriptive results for this exact controlled population only.

## Research boundary

The campaign does **not** establish:
- production scanner accuracy;
- real-world false-positive/false-negative rates;
- automatic authorization-dimension discovery;
- external validity across frameworks or products;
- superiority over another tool;
- real Burp desktop behavior.

The registered dimension is supplied to the runner, and the dataset is balanced synthetic localhost ground truth.
