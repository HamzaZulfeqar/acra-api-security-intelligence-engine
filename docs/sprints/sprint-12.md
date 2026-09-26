# Sprint 12 — Controlled A0–A7 Authorization Ablation Evaluation

**Branch:** `s12-standalone-a0-a7-research-evaluation`  
**Base:** verified Sprint 11 standalone head `2e86b17afa5e06b08f3b9ad580a1b02fdc7d815d`  
**Immutable Sprint 11 closure source:** `1384b86e5e4d33f5ff3c0c97fdc0db667d0b9322`  
**Status:** IN PROGRESS — RESEARCH EXECUTION NOT YET VERIFIED

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

### Phase 1 — Harness & label-isolation verification — ACTIVE

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

### Phase 2 — Measured artifact freeze

After Phase 1 succeeds:
- record exact TP/TN/FP/FN, precision, recall and F1 for A0–A7;
- record JSON/CSV/JSONL digests;
- update experiment registry from NOT_RUN only with measured CI evidence;
- preserve Sprint 11 dataset unchanged.

### Phase 3 — Interpretation & limitations

Document:
- which cumulative capabilities changed false-positive/false-negative behavior;
- whether later variants add measurable value on this fixture;
- synthetic/localhost scope;
- known-dimension limitation;
- no external-validity claim;
- no real Burp-runtime claim.

### Phase 4 — Research closure

Required:
- repeated deterministic campaign;
- artifact sidecars verified;
- no credential leakage;
- retained Sprint 11 ground-truth verification;
- retained product build;
- exact source/research artifact provenance;
- final state freeze.

## Non-claims

Until measured and verified:
- A0–A7 metrics are **NOT_MEASURED**;
- no production accuracy claim;
- no real-world precision/recall claim;
- no external-target validation;
- no dimension-discovery result;
- no comparison/ranking against commercial tools.
