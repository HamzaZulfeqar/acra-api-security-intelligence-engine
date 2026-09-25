# Sprint 13 — Held-Out External Validity and Generalization

**Date:** 2026-09-25  
**Branch:** `s13-heldout-external-validity`  
**Base:** Sprint 12 head `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Phase 1 measured head:** `e515a31d91d775d5f0f35c32a48507455f84992d`  
**Successful workflow:** `36143281129`  
**Status:** PHASE 1 COMPLETE; PHASE 2 DIMENSION DISCOVERY COMPLETE.

## Objective

Sprint 13 strengthens the research validity of ACRA after Sprint 12's controlled 16-case calibration experiment.

Phase 1 is intentionally a failure-capable held-out evaluation. It freezes new inputs and labels before execution,
locks the Sprint 12 A0-A7 rules, removes labels from the prediction workspace, evaluates previously unseen policy
semantics, then joins labels only after prediction.

## Phase 1 integrity gates

- separate held-out API fixture: PASS;
- frozen feature corpus: PASS;
- sealed labels stored separately from prediction inputs: PASS;
- label file physically absent during both prediction passes: PASS;
- Sprint 12 runner/verifier unchanged from the Sprint 12 base: PASS;
- 16 cases × 8 variants = 128 prediction rows: PASS;
- secure/vulnerable oracle expectations verified for all 16 cases: PASS;
- deterministic prediction artifacts across two blind runs: PASS;
- deterministic evaluation artifacts across two label-join runs: PASS;
- secret-material scan: PASS;
- Maven product package: BUILD SUCCESS.

Feature corpus SHA-256:
`fcd31dba730b728c31cf3b42bb674c49e31a41cd983de2a0c0ebb1040cd745ce`.

## Measured held-out result

| Variant | TP | TN | FP | FN | Precision | Recall | F1 | Hard-negative FP |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 0 | 8 | 0 | .500000 | 1.000000 | .666667 | 8 |
| A1 | 8 | 0 | 8 | 0 | .500000 | 1.000000 | .666667 | 8 |
| A2 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A3 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A4 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A5 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A6 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A7 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |

## Interpretation

The held-out corpus preserved recall for all eight positive cases but exposed five false positives that the Sprint 12
calibration fixture did not reveal. The locked A7 rules therefore do not generalize cleanly to the new legitimate policy
semantics.

The five hard-negative failures are deliberate and diagnostically useful:

1. a `platform-admin` role legitimately crossing a tenant boundary;
2. a new `security-admin` privileged role;
3. a valid requester workflow transition `DRAFT -> PENDING`;
4. legitimate `security-admin` access through the equivalent duplicate-separator route;
5. an allowed profile property named `nickname`.

A2 resolves the three controls whose ownership evidence transfers cleanly. A3-A7 do not reduce the remaining five false
positives because their policy semantics are fixture-specific. This is evidence of overfitting/limited policy
generalization, not a reason to modify the frozen result.

## Research rule

Sprint 13 Phase 1 does not tune A0-A7 to improve these held-out metrics.

Any revised detector must be versioned as a later experiment and evaluated against a new development/tuning set while
retaining this original hold-out result unchanged.

## Remaining Sprint 13 program

Phase 2 — automatic authorization-dimension discovery — COMPLETE.  
Phase 3 — policy-generalization redesign using explicit learned/configured policy semantics, followed by a new non-tuned evaluation set.  
Phase 4 — larger, less-balanced and adversarial negative populations.  
Phase 5 — cross-framework API fixtures.  
Phase 6 — real Burp desktop runtime validation.  
Phase 7 — explicitly authorized external-target validation where available.  
Phase 8 — final research freeze, limitations and reproducibility package.

## Claim boundary

The result is valid only for the separately frozen synthetic localhost hold-out fixture. The fixture is separately
authored inside the same project and is not independent third-party replication. Registered authorization dimensions
are still supplied, so dimension discovery remains unmeasured. No production accuracy, real-world safety, superiority,
or external-target claim is established.


## Phase 2 — Automatic authorization-dimension discovery

Phase 2 removes the supplied-dimension assumption from the measured prediction path.

New frozen research inputs:
- `GT-S13-DIMENSION-FEATURES`: 32 cases with no registered dimension or vulnerability label;
- `GT-S13-DIMENSION-LABELS`: sealed post-prediction scoring labels.

New execution components:
- `scripts/sprint13_dimension_inference.py`;
- `scripts/run-sprint13-dimension-discovery.py`;
- `scripts/verify-sprint13-dimension-discovery.py`;
- `scripts/verify-sprint13-dimension-discovery.sh`;
- `.github/workflows/sprint13-dimension-discovery.yml`.

The inference engine uses observable endpoint/request/response/identity structure and emits an interpretable evidence
score, winning dimension, confidence and score margin. A0-A7 then receive only the inferred dimension. Registered
dimensions are joined afterward for multiclass scoring.

### Phase 2 measured result

Canonical successful workflow: GitHub Actions `36145805521` at
`3f4b4238efe514b7ef1fe65b2cd477233c54fa73`.

Dimension discovery:
- Sprint 12 calibration: 15/16 correct, accuracy=.937500, macro-F1=.933333;
- Sprint 13 holdout: 16/16 correct, accuracy=1.000000, macro-F1=1.000000;
- combined: 31/32 correct, accuracy=.968750, macro-F1=.968254;
- confidence distribution: 30 HIGH, 2 MEDIUM.

The one registered-dimension mismatch is the Sprint 12 canonical routing control
`/api/v1/s8/admin`. With no duplicate-separator anomaly in that control and an explicit
`required_role=admin` response signal, the observable-evidence classifier selects
`RBAC_AUTHORIZATION` rather than the registered `ROUTING_AUTHORIZATION`. The mismatch is retained as
an ambiguity/generalization finding rather than tuned away inside Phase 2.

Downstream A7 using inferred dimensions only:
- Sprint 12 calibration: TP=8/TN=8/FP=0/FN=0, P=1/R=1/F1=1 within that calibration fixture;
- Sprint 13 holdout: TP=8/TN=3/FP=5/FN=0, P=.615385/R=1/F1=.761905.

Therefore removing the supplied-dimension assumption did not degrade the measured A7 classification result on either
frozen corpus. It also did not solve the five Phase 1 policy-generalization false positives, confirming that dimension
discovery and policy generalization are distinct problems.

Integrity gates:
- dimension-label file physically absent during both inference passes: PASS;
- 32 dimension-free cases: PASS;
- 32 sealed dimension/vulnerability labels: PASS;
- 256 A0-A7 downstream predictions: PASS;
- evaluation rows: 288: PASS;
- repeatability: PASS;
- Sprint 12 / Phase 1 immutable-history gates: PASS;
- Maven product package: BUILD SUCCESS.
