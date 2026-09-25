# Sprint 13 Phase 3 — Configurable Policy Generalization Protocol

**Protocol ID:** ACRA-S13-POLICY-GEN-v1  
**Branch:** `s13-policy-generalization`  
**Phase 2 base:** `2d030735401e336292ec5d18d7e01702c1e70039`  
**Algorithm freeze:** `c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd`  
**Development workflow:** `36148740338` — SUCCESS.  
**Scope:** synthetic internal authorization-policy research only.

## Research question

Can ACRA replace fixture-specific authorization assumptions with explicit, configurable policy semantics while preserving
automatic dimension discovery and reducing false positives on legitimate authorization behavior?

## Architecture

Phase 3 retains the frozen Sprint 12 A7 logic and frozen Phase 2 dimension-inference engine.

A new policy-semantics layer consumes:
- automatically inferred authorization dimension;
- endpoint/method;
- actor identity context;
- request structure;
- observed response structure;
- explicit configured policy registry.

It emits `ALLOW`, `DENY`, or `UNKNOWN` with policy ID and reasons.

Decision integration:
- explicit `ALLOW` => suppress locked-A7 candidate to NEGATIVE;
- explicit `DENY` + observed ALLOW => classify POSITIVE;
- `UNKNOWN` => retain locked A7 unchanged.

The policy engine does not infer organization policy from role/property names. Policy semantics must be supplied by an
explicit registry.

## Development stage

Development artifacts:
- `POL-S13-DEV-001.json`;
- `GT-S13-POLICY-DEV-FEATURES.json`;
- `GT-S13-POLICY-DEV-LABELS.json`.

The development corpus contains 24 cases:
- 8 positive unauthorized-ALLOW cases;
- 16 legitimate ALLOW controls;
- three cases per authorization dimension.

Development measurement at algorithm freeze:
- dimension inference: 24/24 correct;
- explicit policy decision: 24/24 correct;
- locked A7: TP=8/TN=4/FP=12/FN=0, P=.4/R=1/F1=.571429;
- policy-generalized G1: TP=8/TN=16/FP=0/FN=0, P=1/R=1/F1=1.

These values are development-only and are not final generalization evidence.

## Algorithm freeze

After the successful development run, the algorithm is frozen at:
`c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd`.

The untouched evaluation gate requires no changes after that commit to:
- `scripts/sprint13_policy_semantics.py`;
- `scripts/sprint13_dimension_inference.py`;
- `scripts/run-sprint12-ablation.py`.

Evaluation scripts and evaluation data may be added after the freeze, but the decision algorithms above may not change.

## Untouched evaluation corpus

Created only after algorithm freeze:
- `POL-S13-EVAL-001.json`;
- `GT-S13-POLICY-EVAL-FEATURES.json`;
- `GT-S13-POLICY-EVAL-LABELS.json`.

The 24-case evaluation uses new:
- resource names;
- actor names;
- tenant/workspace names;
- privileged roles;
- delegated role behavior;
- workflow names/states/actions;
- property names;
- routing paths;
- batch identifiers;
- indirect-reference identifiers.

The evaluation feature file contains no registered dimension or vulnerability label.

## Blind evaluation boundary

During each evaluation prediction pass:
- the sealed evaluation-label file is physically removed;
- dimension is inferred automatically;
- locked A7 is executed;
- configured policy is evaluated;
- G1 is produced;
- no labels are accessible.

Labels are restored only for scoring.

## Metrics

Report separately:
- dimension accuracy;
- policy decision accuracy;
- UNKNOWN policy count;
- locked A7 TP/TN/FP/FN, precision, recall, F1;
- G1 TP/TN/FP/FN, precision, recall, F1.

No minimum score is a completion gate.

## Reproducibility gates

Required:
- frozen-algorithm diff gate;
- label absence during both prediction passes;
- deterministic repeated predictions;
- deterministic repeated evaluation;
- SHA-256 sidecars;
- secret-material scan;
- Maven package success.

## Research rules after evaluation

1. The untouched evaluation result is immutable evidence.
2. No policy-engine or dimension-engine tuning may be performed in response to this result within EXP-S13-POLICY-GEN-001.
3. Any later redesign requires a new development corpus and a new untouched evaluation corpus.
4. The evaluation registry is explicit configured policy; this experiment does not prove automatic policy extraction.
5. Results remain synthetic internal evidence, not production or real-world scanner accuracy.
6. Cross-framework, real Burp desktop and authorized external-target validation remain separate future gates.
