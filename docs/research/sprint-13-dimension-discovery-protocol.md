# Sprint 13 Phase 2 — Automatic Authorization-Dimension Discovery Protocol

**Protocol ID:** ACRA-S13-DIMENSION-v1  
**Branch:** `s13-dimension-discovery`  
**Phase 1 base:** `30acba767e4ce969909f1d018b23535e9c12b582`  
**Locked Sprint 12 rule base:** `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Scope:** synthetic localhost S12 calibration + S13 holdout fixtures only.

## Research question

Can ACRA infer the authorization dimension of an observed API interaction without receiving the registered
`dimension` field, using only endpoint, request, response, identity and structural evidence, and can the existing
A0-A7 authorization logic operate when its dimension input comes only from that inference?

The eight target classes are:

- `OBJECT_AUTHORIZATION`;
- `TENANT_AUTHORIZATION`;
- `RBAC_AUTHORIZATION`;
- `WORKFLOW_AUTHORIZATION`;
- `ROUTING_AUTHORIZATION`;
- `PROPERTY_AUTHORIZATION`;
- `BATCH_AUTHORIZATION`;
- `INDIRECT_REFERENCE_AUTHORIZATION`.

## Dataset separation

Phase 2 freezes a derived 32-case corpus:

- `GT-S13-DIMENSION-FEATURES.json` — observable features only;
- `GT-S13-DIMENSION-LABELS.json` — registered dimension + vulnerability label only.

The feature corpus contains no:

- `dimension`;
- `groundTruth`;
- `secureExpected`;
- `vulnerableExpected`;
- `expectedCandidate`.

It combines the 16 frozen Sprint 12 calibration cases and 16 frozen Sprint 13 held-out cases without changing either
source dataset.

## Blind inference boundary

The inference process uses only:

- HTTP method;
- endpoint path and path structure;
- actor identity context;
- request-body structure;
- observed vulnerable-fixture response structure;
- explicit structural/policy signals visible in the response.

The inference runner has no code reference to the sealed dimension-label filename.

The shell verification gate physically removes `GT-S13-DIMENSION-LABELS.json` during both inference/prediction passes.
Labels are restored only for scoring.

## Inference model

Phase 2 uses deterministic, interpretable evidence scoring rather than a trained statistical model.

Examples of structural evidence include:

- duplicate path separators / route-form evidence → routing;
- list-valued selections plus item-level decisions → batch;
- action/from-state/to-state structures → workflow;
- resolved-resource/alias structures → indirect reference;
- PATCH/PUT + field mutation + applied-properties evidence → property;
- privileged/admin/audit surfaces + role requirements → RBAC;
- explicit tenant-scoped paths + tenant context → tenant;
- direct resource ID/ownership structures → object.

Each inference records:

- all eight scores;
- winning dimension;
- confidence: HIGH / MEDIUM / LOW;
- score margin;
- evidence that supported the winning class.

## Downstream integration

The existing Sprint 12 A0-A7 prediction logic remains locked.

For each case:

1. remove registered dimension and labels;
2. observe the vulnerable localhost fixture;
3. infer the dimension;
4. construct the downstream ACRA input with the inferred dimension only;
5. execute all eight A0-A7 variants;
6. join the registered dimension and vulnerability label only after prediction.

This allows separate measurement of:

1. dimension-classification quality;
2. downstream vulnerability-classification behavior.

## Dimension metrics

For S12 calibration, S13 holdout and the combined 32-case corpus:

- accuracy;
- macro-F1;
- eight-class confusion matrix;
- per-class support;
- per-class precision;
- per-class recall;
- per-class F1.

No target accuracy is a completion gate. Incorrect classifications are retained as evidence.

## Downstream metrics

For each dataset and A0-A7:

- TP;
- TN;
- FP;
- FN;
- precision;
- recall;
- F1.

The registered dimension is never substituted back into downstream prediction.

## Reproducibility and integrity

The verification gate requires:

- Sprint 12 runner/verifier unchanged from the frozen Sprint 12 base;
- Phase 1 fixture, holdout data and Phase 1 runner/verifier unchanged from the Phase 1 base;
- 32 dimension-free feature cases;
- 32 sealed labels;
- 32 dimension inferences;
- 256 downstream predictions = 32 × 8;
- sealed label file absent during both prediction runs;
- byte-identical repeated prediction artifacts;
- byte-identical repeated evaluation artifacts;
- SHA-256 sidecars;
- secret-material scan;
- Maven product package success.

## Threats to validity

1. The inference rules are authored within the ACRA project and are not independently trained or replicated.
2. The dataset remains small and balanced: four examples per class across two related localhost fixtures.
3. Path tokens are observable evidence but may encode naming conventions that do not generalize across organizations.
4. Both source fixtures are Python standard-library HTTP servers; cross-framework generalization is not measured.
5. No real Burp desktop behavior is measured.
6. No external authorized target is measured.
7. No production prevalence, benchmark, superiority or real-world accuracy claim is permitted.

## Completion decision

Phase 2 completion means the blind dimension-discovery experiment executed reproducibly with valid label isolation and
downstream integration. It does not require a perfect dimension score.

Any later rule tuning must preserve this original result and use a new untouched evaluation corpus.
