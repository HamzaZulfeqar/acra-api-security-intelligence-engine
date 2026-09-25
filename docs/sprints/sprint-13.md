# Sprint 13 — Held-Out External Validity and Generalization

**Date:** 2026-09-25  
**Branch:** `s13-heldout-external-validity`  
**Base:** Sprint 12 head `bd944e83a6edefafba56caebaa35e89fc107c282`  
**Status:** PHASE 1 IMPLEMENTED / MEASUREMENT PENDING CI.

## Objective

Sprint 13 strengthens the research validity of ACRA after Sprint 12's controlled 16-case calibration experiment.

Phase 1 is intentionally a failure-capable held-out evaluation. It freezes new inputs and labels before execution,
locks the Sprint 12 A0-A7 rules, removes labels from the prediction workspace, evaluates previously unseen policy
semantics, then joins labels only after prediction.

## Phase 1 deliverables

- separate held-out API fixture: `lab/heldout-api/server.py`;
- frozen feature corpus: `GT-S13-HOLDOUT-FEATURES`;
- sealed label set: `GT-S13-HOLDOUT-LABELS`;
- blind prediction runner;
- post-prediction label/oracle verifier;
- hard-negative false-positive accounting;
- deterministic two-run verification;
- locked Sprint 12 rule-diff gate;
- CI workflow and research artifact upload.

## Research rule

Sprint 13 Phase 1 must not tune A0-A7 to improve held-out metrics.

If the held-out evaluation reveals false positives or false negatives, those failures are recorded as evidence.
Any later rule revision must be a separately versioned experiment with the original held-out result retained.

## Remaining Sprint 13 program

Phase 2 — automatic authorization-dimension discovery.  
Phase 3 — larger, less-balanced and adversarial negative populations.  
Phase 4 — cross-framework API fixtures.  
Phase 5 — real Burp desktop runtime validation.  
Phase 6 — explicitly authorized external-target validation where available.  
Phase 7 — final research freeze, limitations and reproducibility package.

No broader external-validity claim is made until the relevant phase has actually executed.
