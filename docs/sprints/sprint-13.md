# Sprint 13 — Held-Out External Validity and Generalization

**Date:** 2026-09-25  
**Branch:** `s13-heldout-external-validity`  
**Base:** Sprint 12 head `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Phase 1 measured head:** `e515a31d91d775d5f0f35c32a48507455f84992d`  
**Successful workflow:** `36143281129`  
**Status:** PHASE 1 COMPLETE; PHASE 2 COMPLETE; PHASE 3 COMPLETE; PHASE 4 ADVERSARIAL / BASE-RATE STRESS COMPLETE.

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


## Phase 3 — Configurable policy generalization

**Algorithm freeze:** `c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd`  
**Development workflow:** `36148740338` — SUCCESS.  
**Untouched evaluation workflow:** `36149071484` — SUCCESS at `82ae165b702d18b0eae872d51f8f695f723fec13`.

Phase 3 replaces fixture-specific authorization assumptions with an explicit configured policy registry layered after
automatic dimension inference.

Decision integration:
- explicit policy ALLOW → suppress candidate;
- explicit policy DENY + observed ALLOW → positive authorization mismatch;
- UNKNOWN → fall back to locked A7.

Development corpus:
- 24 cases: 8 positive / 16 legitimate controls;
- dimension inference: 24/24;
- configured policy decisions: 24/24;
- locked A7: TP=8/TN=4/FP=12/FN=0, P=.4/R=1/F1=.571429;
- G1: TP=8/TN=16/FP=0/FN=0, P=1/R=1/F1=1.

Untouched evaluation corpus was created only after the algorithm freeze and uses different resource names, actor names,
tenant/workspace names, roles, transitions, properties, routing paths, batch identifiers and indirect references.

Untouched evaluation:
- dimension inference: 24/24;
- configured policy decisions: 24/24;
- UNKNOWN policy decisions: 0;
- locked A7: TP=8/TN=4/FP=12/FN=0, P=.4/R=1/F1=.571429;
- G1: TP=8/TN=16/FP=0/FN=0, P=1/R=1/F1=1;
- repeatability: PASS;
- label-absence gate: PASS;
- frozen-algorithm gate: PASS;
- Maven package: BUILD SUCCESS.

The 1.0 G1 fixture result is valid only for this synthetic internal configured-policy evaluation. It does not establish
automatic policy extraction, production scanner accuracy, or external validity.

**Next:** Phase 4 — larger, less-balanced and adversarial negative populations using frozen Phase 3 results as evidence,
not as a tuning target.


## Phase 4 — Adversarial / base-rate stress

**Canonical workflow:** `36168752869` — SUCCESS at `ed1601542739cce20be48c001ae4e663bfdcf890`.

Phase 4 froze the Phase 3 algorithms and evaluated a 96-case negative-heavy stress corpus:

- 8 positive authorization mismatches;
- 88 legitimate controls;
- measured prevalence = 8.333333%;
- 12 cases per authorization dimension;
- 40 explicit configured ALLOW controls;
- 16 missing-policy controls;
- 16 ambiguous-policy controls;
- 8 stale-policy controls;
- 8 incomplete-context controls.

Integrity and robustness:
- Phase 3 evidence lock: PASS;
- frozen algorithm lock: PASS;
- malformed-registry fail-closed checks: PASS;
- missing policy -> UNKNOWN: PASS;
- equal-priority ambiguity -> UNKNOWN: PASS;
- deterministic priority resolution: PASS;
- label file absent during both prediction passes: PASS;
- two-run byte-identical predictions/evaluation: PASS;
- Maven package: BUILD SUCCESS.

Automatic dimension inference remained 96/96 on this synthetic stress corpus.

### Phase 4 measured result

| Variant | TP | TN | FP | FN | Precision | Recall | Specificity | FPR | F1 | MCC |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Locked A7 | 8 | 20 | 68 | 0 | .105263 | 1.000000 | .227273 | .772727 | .190476 | .154672 |
| Policy-generalized G1 | 8 | 49 | 39 | 0 | .170213 | 1.000000 | .556818 | .443182 | .290909 | .307860 |

G1 improves substantially over locked A7 but remains unsuitable as a low-noise alerting decision under degraded policy
quality.

G1 condition-level false positives:
- EXPLICIT_ALLOW: 0 / 40;
- NO_POLICY: 12 / 16;
- AMBIGUOUS_POLICY: 12 / 16;
- STALE_POLICY: 8 / 8;
- INCOMPLETE_CONTEXT: 7 / 8.

All 8 positive cases remained detected; FN=0.

Policy coverage:
- decisive policy decisions: 58 / 96;
- UNKNOWN decisions: 38 / 96;
- expected-authorization correctness across all cases: 48 / 96;
- decisive expected-authorization correctness: 48 / 58 = .827586.

Wilson 95% intervals for G1:
- sensitivity: [.675592, 1.000000];
- specificity: [.452818, .656065];
- precision: [.088864, .301398];
- NPV: [.927302, 1.000000].

Projected G1 PPV from measured sensitivity/specificity:
- 1% prevalence: .022284;
- 5% prevalence: .106152;
- measured 8.333333% prevalence: .170213;
- 10% prevalence: .200456.

At 1% prevalence the projection implies ~448.75 alerts per 1,000 observations, ~438.75 of them false alerts.
This projection is mathematical, not an additional observed dataset.

### Phase 4 interpretation

The Phase 3 configured-policy success does not survive policy-quality degradation.

The dominant weakness is not automatic dimension discovery. It is **policy uncertainty governance**:
`NO_POLICY`, `AMBIGUOUS_POLICY`, stale policy and incomplete context currently either fall back to noisy locked A7 or
produce an incorrect decisive DENY.

Therefore `UNKNOWN` must not be treated as equivalent to confirmed authorization mismatch in a production-facing
finding lifecycle.

**Next dependency:** introduce explicit uncertainty / policy-health governance and separate CANDIDATE / INCONCLUSIVE /
POLICY_GAP states before cross-framework or external validation.
