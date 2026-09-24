# Sprint 12 — Controlled A0-A7 Authorization Ablation Evaluation

**Date:** 2026-09-25  
**Branch:** `s12-a0-a7-research-evaluation`  
**Immutable software base:** Sprint 11 head `db78f9e458eeabcabb389abf1949a4f597761564`  
**Status:** CONTROLLED RESEARCH EXECUTION COMPLETE for the registered synthetic/localhost boundary.

## Objective

Sprint 12 executes the research campaign that Sprint 11 deliberately registered but did not run. The objective is
to measure how a cumulative sequence of authorization-context capabilities changes binary candidate classification
on the frozen `GT-S11-AUTHORIZATION-RESEARCH` fixture.

This is an ablation/evaluation sprint, not a claim that ACRA has been validated as a production scanner.

## Frozen dataset

The campaign consumes the Sprint 11 dataset unchanged:

- 16 total cases;
- 8 positive and 8 negative labels;
- 8 registered authorization dimensions;
- one positive and one negative control per dimension;
- dataset SHA-256: `f42783717bdd38e8d04b7f59cef98a41441005de426ec78ca1fba58098154d4b`.

Sprint 12 writes results to separate research artifacts. It does not rewrite the dataset's historical
`A0_A7_NOT_RUN / NOT_MEASURED` registration state.

## Prediction/ground-truth separation

The predictor receives only a restricted case view and the vulnerable localhost observation. It is not permitted to
consume:

- `groundTruth`;
- `secureExpected`;
- `vulnerableExpected`;
- `expectedCandidate`.

The ground-truth label is joined only after prediction. This prevents a trivial secure-vulnerable oracle comparison
from masquerading as detector performance.

The registered dimension is supplied to the runner. Therefore this sprint measures classification under known
dimension assignment; **dimension discovery is not measured**.

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

Each variant has a deterministic configuration fingerprint. Capabilities are cumulative; a later variant cannot
silently remove an earlier capability.

## Execution and artifacts

Canonical runner and verifier:

- `scripts/run-sprint12-ablation.py`;
- `scripts/verify-sprint12-research.py`;
- `scripts/verify-sprint12-research.sh`;
- `.github/workflows/sprint12-research.yml`.

Measured artifacts:

- `build/s12-research/EXP-A0-A7.json`;
- `build/s12-research/EXP-A0-A7.csv`;
- `build/s12-research/EXP-A0-A7-cases.jsonl`;
- SHA-256 sidecars for all three.

The verification gate recomputes every metric from per-case rows, verifies exact campaign cardinality, checks
configuration fingerprints, rejects credential leakage and executes the entire campaign twice to require byte-identical
artifacts.

## Measured result

First successful measured workflow: `36065830981`.

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

## Interpretation within fixture scope

A1 does not change the A0 confusion matrix, so identity presence alone provides no measurable classification delta on
this specific fixture. Ownership, tenant, role and workflow context remove successive false positives. The semantic
evidence stage removes the final false positive. A7 retains the same confusion matrix as A6, so the fixed dataset does
not independently demonstrate additional classification value from correlation.

These are descriptive results for this fixture only. The sprint does not rank ACRA against external tools and does not
establish a production-accuracy claim.

## Completion gates

- frozen Sprint 11 ground truth revalidated: PASS;
- 16 × 8 = 128 prediction rows: PASS;
- label-isolation boundary: PASS;
- independent metric recomputation: PASS;
- deterministic configuration fingerprints: PASS;
- JSON/CSV/JSONL SHA sidecars: PASS;
- secret-material scan: PASS;
- two-run byte repeatability: PASS;
- Maven product package after research execution: BUILD SUCCESS.

## Residual research work

A stronger external-validity program requires held-out/blinded cases, independently authored fixtures, larger and
less-balanced negative populations, automatic dimension discovery, additional frameworks/proxies/API styles, real
Burp desktop validation and explicitly authorized external targets. Until those gates exist, the Sprint 12 measurements
remain controlled synthetic evidence.
