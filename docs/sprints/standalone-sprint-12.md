# Sprint 12 — Controlled A0–A7 Authorization Ablation Evaluation

**Branch:** `s12-standalone-a0-a7-research-evaluation`  
**Base:** verified Sprint 11 standalone head `2e86b17afa5e06b08f3b9ad580a1b02fdc7d815d`  
**Immutable Sprint 11 closure source:** `1384b86e5e4d33f5ff3c0c97fdc0db667d0b9322`  
**Status:** S12 CONTROLLED RESEARCH COMPLETE — FINAL CLOSURE VERIFIED

## Objective

Execute the A0–A7 authorization ablation campaign registered by Sprint 11 against the frozen
`GT-S11-AUTHORIZATION-RESEARCH` dataset.

Sprint 12 is a controlled research/evaluation sprint. It does not change finding truth, does not
perform external-target testing, and does not claim production scanner accuracy.

## Frozen research population

- dataset: `lab/ground-truth/GT-S11-AUTHORIZATION-RESEARCH.json`;
- 16 total cases;
- 8 positive controls;
- 8 negative controls;
- 8 authorization dimensions;
- one positive and one negative control per dimension;
- ground truth authored independently of ACRA output;
- Sprint 11 state remains `A0_A7_NOT_RUN / NOT_MEASURED` until this sprint executes successfully.

## Cumulative variants

| Variant | Capability boundary |
|---|---|
| A0 | observed-response baseline |
| A1 | A0 + identity |
| A2 | A1 + ownership |
| A3 | A2 + tenant |
| A4 | A3 + role |
| A5 | A4 + workflow |
| A6 | A5 + semantic evidence |
| A7 | A6 + correlation |

Capabilities are cumulative. A later variant may not silently remove an earlier capability.

## Execution schedule

### Phase 1 — Harness & label-isolation verification — VERIFIED

Deliverables:
- deterministic runner;
- frozen dataset digest;
- prediction view excluding ground-truth labels and secure-oracle expected values;
- vulnerable-fixture-only prediction execution;
- explicit statement that dimension discovery is not measured;
- 16 × 8 = 128 per-case prediction rows;
- independent metric recomputation;
- secret-material scan;
- byte-repeatability verification.

Gate:
- dedicated Sprint 12 workflow must be green;
- product Maven packaging must remain green.

### Phase 2 — Measured artifact freeze — VERIFIED

First measured standalone workflow: `36265834848` — SUCCESS.

Measured result:

| Variant | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 1 | 7 | 0 | .533333 | 1.000000 | .695652 |
| A1 | 8 | 1 | 7 | 0 | .533333 | 1.000000 | .695652 |
| A2 | 8 | 4 | 4 | 0 | .666667 | 1.000000 | .800000 |
| A3 | 8 | 5 | 3 | 0 | .727273 | 1.000000 | .842105 |
| A4 | 8 | 6 | 2 | 0 | .800000 | 1.000000 | .888889 |
| A5 | 8 | 7 | 1 | 0 | .888889 | 1.000000 | .941176 |
| A6 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |
| A7 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

First measured-run artifacts:
- JSON SHA-256: `528b79b29a764a157e7b402e4202d84f6585c692ebc353cfae0ffd1c1ff364e7`;
- CSV SHA-256: `59333c885098c2cdd69eccab0d7ba6273b1d55fbaa2478b7dfaa05c7e51c3486`;
- case JSONL SHA-256: `7bb7c6780cb9360f63354f533e0ffc54a4d2db28c4b60ae2a858e9b417ba3d20`;
- dataset SHA-256: `f42783717bdd38e8d04b7f59cef98a41441005de426ec78ca1fba58098154d4b`;
- 128 rows independently recomputed and verified;
- two-run byte repeatability: PASS;
- product Maven package: PASS.

The frozen Sprint 11 dataset itself remains unchanged.

### Phase 3 — Interpretation & limitations — VERIFIED

Within this fixture:
- A1 does not improve the A0 confusion matrix;
- A2 ownership context removes three false positives;
- A3 tenant context removes one additional false positive;
- A4 role context removes one additional false positive;
- A5 workflow context removes one additional false positive;
- A6 semantic evidence removes the final false positive;
- A7 retains the A6 confusion matrix.

Therefore this fixture demonstrates cumulative false-positive reduction through A6, but it does **not** independently
demonstrate additional classification value from A7 correlation beyond A6.

This interpretation is limited to the balanced synthetic localhost fixture. Dimension discovery is not measured.

### Phase 4 — Research closure — VERIFIED

Canonical final closure:
- source commit: `29ce5317921b23ab3d33fee8e742079cc6a8252a`;
- final closure run: `36266044578` — SUCCESS;
- checkpoint: `S12-FINAL-RESEARCH`;
- source ZIP SHA-256: `e089f93287f01201f9682701ca3523e12312bfe084138984b71670e94f98c572`;
- source entries: 1012;
- unsafe paths: 0;
- duplicate entries: 0;
- clean extraction equality: PASS;
- per-file SHA-256 equality: PASS;
- Sprint 12 distribution SHA-256: `cc847d2a4b172041ca8d622f4e6f6ac86afb2cd691b74170ba51204f61ddbca6`;
- closure-run research JSON SHA-256: `aa9187baaa4dd81ef380749e72498a0355d795d249447081844069b0ca7b6aea`;
- CSV SHA-256: `59333c885098c2cdd69eccab0d7ba6273b1d55fbaa2478b7dfaa05c7e51c3486`;
- case JSONL SHA-256: `7bb7c6780cb9360f63354f533e0ffc54a4d2db28c4b60ae2a858e9b417ba3d20`;
- full Maven reactor: PASS;
- retained Sprint 10/11 standalone/finding regression stack: PASS;
- retained Sprint 2/Sprint 3 local compatibility: PASS;
- extracted standalone startup: PASS.

The research JSON digest differs from the first measured run because the artifact includes the exact executing source
commit. The confusion matrices and stable CSV/per-case outputs remained unchanged.

## Non-claims

Measured metrics are valid only for this controlled fixture. They do not establish:
- production or real-world scanner accuracy;
- real-world precision/recall;
- no external-target validation;
- no dimension-discovery result;
- no comparison/ranking against commercial tools.

## Final decision

Sprint 12 is **CONTROLLED RESEARCH COMPLETE** for the registered synthetic/localhost boundary.

Sprint 13 is **NOT STARTED** by this closure.

The closure does not establish production scanner accuracy, external validity, automatic dimension discovery,
real Burp desktop behavior, or superiority over another security tool.
