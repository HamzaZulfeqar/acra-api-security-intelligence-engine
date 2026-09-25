# Sprint 13 Held-Out External-Validity Protocol

**Protocol ID:** ACRA-S13-HOLDOUT-v1  
**Branch:** `s13-heldout-external-validity`  
**Locked rule base:** Sprint 12 head `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Scope:** localhost synthetic authorization research only.

## Research question

Do the cumulative A0-A7 authorization-classification rules measured in Sprint 12 retain their behavior on a separately
frozen held-out fixture containing unseen actor/resource names and deliberately harder legitimate controls?

This phase is designed to expose overfitting. It does not require A7 to retain the 1.0 fixture metrics measured in Sprint 12.

## Hold-out construction

Prediction inputs and labels are stored separately:

- `lab/ground-truth/GT-S13-HOLDOUT-FEATURES.json`
- `lab/ground-truth/GT-S13-HOLDOUT-LABELS.json`

The feature corpus contains 16 cases across the same eight registered authorization dimensions, with one positive and one
hard negative per dimension. The labels file contains the ground truth and secure/vulnerable expected authorization
decisions.

The prediction runner does not reference the label filename. The shell verification gate physically removes the label
file before each prediction pass, proving that predictions can be generated without access to the labels.

## Locked-rule requirement

Sprint 13 Phase 1 evaluates the Sprint 12 rule implementation without tuning it to the hold-out set.

The verification wrapper requires no diff from the Sprint 12 base for:

- `scripts/run-sprint12-ablation.py`
- `scripts/verify-sprint12-research.py`

The Sprint 13 predictor dynamically imports the locked Sprint 12 prediction logic and records its SHA-256 digest.

## Hard-negative design

The negative controls intentionally include legitimate policy semantics that were absent from the Sprint 12 calibration fixture:

- a `platform-admin` role that may legitimately cross tenant boundaries;
- a `security-admin` role that may legitimately access the admin audit surface;
- a valid requester workflow transition `DRAFT -> PENDING`;
- a legitimate `security-admin` request through the equivalent duplicate-separator route;
- an allowed profile property named `nickname`.

These cases are intended to reveal fixture-specific assumptions rather than protect prior metrics.

## Prediction boundary

The predictor receives:

- case ID;
- registered dimension;
- HTTP method/path;
- actor context;
- request body where applicable;
- response-decision oracle definition;
- observation from the intentionally vulnerable hold-out fixture.

It does not receive:

- ground truth;
- secure expected decision;
- vulnerable expected decision;
- expected candidate label.

The registered authorization dimension remains supplied, so automatic dimension discovery is **not measured** in Phase 1.

## Oracle verification

After prediction, the verifier restores the sealed label file and executes both secure and vulnerable hold-out fixtures.
Each case must match its frozen secure/vulnerable expected authorization decisions before its label may be used for
metric computation.

A positive case is defined as secure DENY plus vulnerable ALLOW for the same unauthorized operation.
A negative case is a legitimate control whose secure and vulnerable authorization decisions agree.

## Metrics

For every locked variant A0-A7, the verifier computes:

- TP;
- TN;
- FP;
- FN;
- precision;
- recall;
- F1;
- hard-negative false-positive count.

No minimum score is a completion gate. A lower held-out score is valid research evidence and must not be tuned away inside
this experiment.

## Reproducibility

The gate requires:

- exact source commit recording;
- feature and label SHA-256 identities;
- locked Sprint 12 runner SHA-256 recording;
- 16 x 8 = 128 prediction rows;
- two blind prediction passes with byte-identical artifacts;
- two label-join/evaluation passes with byte-identical artifacts;
- secret-material scan;
- Maven product packaging after the research harness executes.

## Threats to validity

1. The fixture is separately authored inside the same project; this is not independent third-party replication.
2. The dataset remains small and balanced.
3. Registered dimensions are supplied, so dimension discovery remains unmeasured.
4. The fixture uses Python's standard-library HTTP server and does not establish cross-framework generalization.
5. Real Burp desktop behavior is outside this protocol.
6. External authorized targets are outside this protocol.
7. No production prevalence, confidence interval, benchmark, superiority, or real-world accuracy claim is permitted.

## Next research gates

After this held-out gate, Sprint 13 should proceed to:

1. automatic authorization-dimension discovery;
2. larger and less-balanced hard-negative populations;
3. cross-framework fixture validation;
4. real Burp desktop runtime validation;
5. explicitly authorized external-target validation, if available and ethically appropriate.
