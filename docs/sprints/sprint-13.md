# Sprint 13 — Held-Out External Validity and Generalization

**Date:** 2026-09-25  
**Branch:** `s13-heldout-external-validity`  
**Base:** Sprint 12 head `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Phase 1 measured head:** `e515a31d91d775d5f0f35c32a48507455f84992d`  
**Successful workflow:** `36143281129`  
**Status:** PHASE 1 COMPLETE; PHASE 2 COMPLETE; PHASE 3 COMPLETE; PHASE 4 COMPLETE; PHASE 5 COMPLETE; PHASE 6A COMPLETE; PHASE 6B COMPLETE; PHASE 7 REAL BURP DESKTOP / MONTOYA RUNTIME VALIDATION COMPLETE.

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

Phases 1–7 are complete under their recorded controlled evidence boundaries.

Remaining:
- Phase 8 — explicitly authorized external-target validation;
- Final gate — research freeze, limitations, reproducibility package and release-candidate evidence consolidation.

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


## Phase 5 — Policy reliability & uncertainty governance

**Development workflow:** `36169960606` — SUCCESS.  
**Governance freeze:** `11afbb80be24116c9facd0d4b0791f4a12efed3a`.  
**Untouched evaluation workflow:** `36170353179` — SUCCESS at `46f77a8d76b160cec70f7aef3f612e0d63af56da`.

Phase 5 stops treating degraded policy quality as equivalent to a confirmed authorization candidate.

Governed dispositions:
- `VULNERABILITY_CANDIDATE`;
- `AUTHORIZED_CONTROL`;
- `CONTROL_ENFORCED`;
- `POLICY_GAP`;
- `AMBIGUOUS_POLICY`;
- `STALE_POLICY`;
- `INCOMPLETE_CONTEXT`;
- `INCONCLUSIVE`.

Only `VULNERABILITY_CANDIDATE` is actionable. Policy gaps, ambiguity, stale policy, incomplete context and other
uncertainty are retained as review-required evidence instead of being promoted through legacy A7 fallback.

### Development result

The 64-case development corpus measured:
- dimension inference: 64/64;
- expected disposition: 64/64;
- actionable candidate: TP=8/TN=48/FP=0/FN=8, P=1/R=.5/F1=.666667;
- positive review count: 8;
- escalation coverage (candidate or review): 1.0;
- silent positive count: 0;
- repeatability: PASS;
- label-absence gate: PASS;
- Maven package: BUILD SUCCESS.

The lower actionable recall is intentional: eight positive cases without authoritative policy are routed to
`POLICY_GAP` for review rather than mislabeled as confirmed findings.

### Untouched evaluation result

The 64-case evaluation was created after the governance freeze and uses new business vocabulary and policy metadata.

Measured:
- dimension inference: 64/64;
- expected disposition: 64/64;
- frozen A7: TP=16/TN=21/FP=27/FN=0, P=.372093/R=1/F1=.542373;
- legacy G1: TP=16/TN=25/FP=23/FN=0, P=.410256/R=1/F1=.581818;
- governed actionable candidates: TP=8/TN=48/FP=0/FN=8, P=1/R=.5/F1=.666667;
- positive review count: 8;
- negative review count: 32;
- escalation coverage: 1.0;
- silent positive count: 0;
- review rate: .625;
- actionable-candidate rate: .125.

Disposition distribution:
- `VULNERABILITY_CANDIDATE`: 8;
- `AUTHORIZED_CONTROL`: 8;
- `CONTROL_ENFORCED`: 8;
- `POLICY_GAP`: 16;
- `AMBIGUOUS_POLICY`: 8;
- `STALE_POLICY`: 8;
- `INCOMPLETE_CONTEXT`: 8.

Integrity:
- Phase 4 evidence lock: PASS;
- governance/upstream algorithm freeze: PASS;
- label absence during both prediction passes: PASS;
- two-run byte-identical prediction/evaluation: PASS;
- secret-material scan: PASS;
- Maven package: BUILD SUCCESS.

### Phase 5 interpretation

Phase 5 resolves the central Phase 4 failure mode: uncertainty no longer creates false actionable vulnerability findings
inside this controlled corpus.

The cost is explicit analyst/review load. A 62.5% review rate in this constructed uncertainty-heavy evaluation is not a
production workload estimate and must not be represented as one.

The next research dependency is cross-framework generalization while preserving the same governance semantics and
finding-state boundaries.


## Phase 6A — Cross-framework-shaped normalization

**Development workflow:** `36173526384` — SUCCESS.  
**Normalization freeze:** `4011b9c05b99b14da66733aea47de43256060990`.  
**Untouched evaluation workflow:** `36173838561` — SUCCESS at `f3180a1877aaffd224a6d3a2734ddc25bf28c24b`.

Phase 6A tests serialization/transport representations shaped like FastAPI, Flask, Express and Spring APIs without
claiming that those framework runtimes were actually launched.

A framework-neutral normalization layer canonicalizes common aliases such as:
- `ownerId -> owner_id`;
- `tenantId -> tenant_id`;
- `requiredRole -> required_role`;
- `fromState/toState -> from_state/to_state`;
- `appliedProperties -> applied_properties`;
- `resolvedResourceId -> resolved_resource_id`;
- `resourceId/resourceIds -> resource_id/resource_ids`;
- `userId/principalId -> sub`;
- `routeForm -> route_form`.

The normalizer does not consume framework labels, registered dimensions or vulnerability labels.

### Development result

64 framework-shaped cases:
- raw dimension: 60/64;
- raw governed disposition: 32/64;
- normalized dimension: 64/64;
- normalized governed disposition: 64/64.

Per framework:
- FastAPI: raw 16/16 dimension + 16/16 disposition; normalized 16/16 + 16/16;
- Flask: raw 16/16 + 16/16; normalized 16/16 + 16/16;
- Express: raw 14/16 + 0/16; normalized 16/16 + 16/16;
- Spring: raw 14/16 + 0/16; normalized 16/16 + 16/16.

### Untouched evaluation result

A new 64-case corpus with different resource, organization, role, workflow, property and identity vocabulary was created
after the normalization freeze.

Measured:
- raw dimension: 60/64;
- raw disposition: 32/64;
- normalized dimension: 64/64;
- normalized disposition: 64/64.

The per-framework pattern reproduced exactly:
- FastAPI: raw and normalized 16/16 dimension + disposition;
- Flask: raw and normalized 16/16 dimension + disposition;
- Express: raw 14/16 dimension + 0/16 disposition; normalized 16/16 + 16/16;
- Spring: raw 14/16 dimension + 0/16 disposition; normalized 16/16 + 16/16.

Integrity:
- Phase 5 evidence lock: PASS;
- normalization/upstream reasoning freeze: PASS;
- label absence during both prediction passes: PASS;
- repeated predictions/evaluations byte-identical: PASS;
- Maven package: BUILD SUCCESS.

### Phase 6A interpretation

The tested camelCase transport representations break the canonical policy/governance path even when most dimensions can
still be inferred. A neutral normalization boundary restores the frozen reasoning contract on both development and
untouched snapshot corpora.

This is **not** actual FastAPI/Flask/Express/Spring runtime evidence.

**Next dependency:** Phase 6B — launch real local framework fixtures in CI, capture live HTTP behavior and feed that
evidence through the frozen Phase 6A normalizer + Phase 5 reasoning/governance stack.


## Phase 6B — Actual local framework runtime validation

**Development workflow:** `36175284755` — SUCCESS.  
**Development freeze:** `683933836d2c2fa5ec155bc8e224be87e6958e95`.  
**Untouched evaluation workflow:** `36175649455` — SUCCESS at `e4d50d69161b8c2af8df30c334a0f51e3599c925`.

Phase 6B moves beyond framework-shaped JSON snapshots and launches real localhost applications in CI:

- FastAPI / Uvicorn;
- Flask;
- Express / Node.js;
- Spring Boot / Java 21.

### Development runtime result

The development campaign issued 64 real HTTP requests across the four running services.

The first development pass measured 56/64 dimensions and 56/64 dispositions because the test-only route
`/routing/audit` carried strong RBAC evidence. Before freeze, the fixture was corrected to a routing-specific
`/routing/equivalent` surface without changing the frozen normalizer or upstream authorization reasoning.

Final development result:
- runtime health checks: 4/4 PASS;
- live HTTP 200: 64/64;
- dimension inference: 64/64;
- governed disposition: 64/64;
- FastAPI: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- Flask: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- Express: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- Spring Boot: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- two-run repeatability: PASS;
- blind-label gate: PASS;
- Maven product build: SUCCESS.

### Untouched runtime evaluation

After the development freeze, separate runtime applications were created with new:
- ports;
- resource names;
- organization names;
- identity names;
- roles;
- workflow action/state vocabulary;
- property names;
- policy identifiers and versions.

The development runtimes and development research artifacts were locked during evaluation.

Untouched measured result:
- health checks: 4/4 PASS;
- live HTTP 200: 64/64;
- dimension inference: 64/64;
- governed disposition: 64/64;
- FastAPI: 16/16 HTTP, dimension and disposition;
- Flask: 16/16 HTTP, dimension and disposition;
- Express: 16/16 HTTP, dimension and disposition;
- Spring Boot: 16/16 HTTP, dimension and disposition;
- repeated live execution: PASS;
- label absence during prediction: PASS;
- development freeze gate: PASS;
- Phase 6A normalizer/upstream freeze: PASS;
- Maven product build: SUCCESS.

### Phase 6B interpretation

The frozen normalization + authorization-reasoning + uncertainty-governance stack preserved the expected dimension and
disposition across the tested real localhost framework runtimes.

This evidence is limited to the exact controlled framework/runtime versions and endpoints exercised by CI. It does not
establish arbitrary framework/version compatibility, production deployment behavior, Burp desktop integration,
external-target effectiveness or real-world vulnerability accuracy.

**Next dependency:** Phase 7 — real Burp desktop / Montoya runtime validation against the controlled local lab.


## Phase 7 — Real Burp Desktop / Montoya runtime validation

**Branch:** `s13-burp-montoya-runtime`.  
**Successful development workflow:** `36179678105`.  
**Development freeze:** `4bb7b952fb5cd0692efc6153d51b6885a7d38f9d`.  
**Successful untouched evaluation workflow:** `36180110271`.  
**Measured evaluation head:** `b1cf0336c530a3678de64aee699067a4793e3857`.  
**Burp Desktop:** Community Edition 2026.7.3.  
**Burp SHA-256:** `c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069`.  
**Montoya compile contract:** 2026.7.

Phase 7 validates the actual ACRA shaded extension inside the real Burp Desktop / Montoya runtime rather than using a
mocked Montoya interface.

### Development result

After explicit user authorization to accept the Burp Community Edition EULA, the development gate measured:
- Phase 6B evidence lock: PASS;
- Maven/shaded extension build: BUILD SUCCESS;
- pinned Burp binary checksum: PASS;
- controlled secure localhost ACRA-Lab health: PASS;
- real ACRA Montoya initialization: PASS;
- Burp Proxy request callbacks: 2;
- Burp Proxy response callbacks: 2;
- ACRA passive pipeline processed events: 2;
- `GT-INTEGRATION-001` context reconstruction: PASS;
- secret-bearing material excluded from the runtime probe: PASS.

Development workflow: `36179678105`.

### Post-freeze untouched evaluation

After development freeze, a separate localhost fixture was created with unseen synthetic identities/resources:
- `tenant-c / user-c / document 3001`;
- `tenant-d / user-d / document 4001`.

Expected contexts were stored separately in `GT-S13-BURP-EVAL-LABELS` and physically absent during both real-Burp
traffic passes.

Untouched evaluation workflow `36180110271` measured, on each of two independent Burp runs:
- real Burp initialization: 1/1;
- Proxy request callbacks: 2/2;
- Proxy response callbacks: 2/2;
- ACRA pipeline processed events: 2/2;
- expected context reconstruction: 2/2;
- all target traffic: loopback-only;
- response tool source: `PROXY`;
- label absence during traffic generation: PASS;
- secret-exclusion gate: PASS;
- semantic repeatability between the two runs: PASS;
- extension-source development freeze: PASS;
- Maven build: BUILD SUCCESS.

Raw runtime evidence is intentionally not byte-identical because Burp message IDs/timestamps are runtime observations.
The normalized semantic evaluation artifacts are identical across both passes.

### Phase 7 implementation boundary

The runtime evidence probe is opt-in through:

`-Dacra.phase7.probeFile=<path>`

It records non-secret runtime metadata only. Normal extension use does not enable the probe. In automated probe mode,
passive collection is temporarily widened to `ALL_TRAFFIC` so the test does not depend on interactive Target-scope
configuration; active execution remains disabled.

### Phase 7 claim boundary

The result establishes controlled real Burp Desktop 2026.7.3 / Montoya runtime operation for ACRA against localhost
traffic. It does not establish arbitrary Burp-version compatibility, production scanner accuracy, external-target
effectiveness, automatic confirmed-vulnerability publication, or independent third-party replication.

**Next gate:** Phase 8 — explicitly authorized external-target validation.
