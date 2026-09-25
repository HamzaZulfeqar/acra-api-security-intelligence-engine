# Sprint 13 Phase 4 — Adversarial / Base-Rate Stress Protocol

**Protocol ID:** ACRA-S13-BASERATE-v1  
**Branch:** `s13-adversarial-base-rate`  
**Phase 3 completion base:** `53e1995b22c29f1a4672cf16ffa420f5cf74fde0`  
**Frozen decision algorithms:** `c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd`  
**Scope:** synthetic internal negative-heavy policy/detection stress only.

## Research question

How do locked A7 and policy-generalized G1 behave when:
- vulnerability prevalence is much lower than prior fixtures;
- legitimate authorization controls dominate;
- configured policy is missing;
- policy matches are ambiguous;
- policy configuration is stale;
- required policy evidence is incomplete;
- malformed registries are presented?

Phase 4 does not tune the Phase 3 policy or dimension algorithms.

## Frozen corpus

`GT-S13-BASERATE-FEATURES` contains 96 cases:

- 8 positive authorization mismatches;
- 88 legitimate controls;
- measured prevalence: 8/96 = 8.333333%;
- 12 cases per authorization dimension.

Negative-control policy conditions:
- 40 explicit configured ALLOW controls;
- 16 NO_POLICY controls;
- 16 AMBIGUOUS_POLICY controls;
- 8 STALE_POLICY controls;
- 8 INCOMPLETE_CONTEXT controls.

Labels and policy-condition annotations are stored separately in
`GT-S13-BASERATE-LABELS.json` and are physically absent during both prediction passes.

## System under test

Frozen components:
- Sprint 12 locked A7;
- Sprint 13 Phase 2 dimension inference;
- Sprint 13 Phase 3 configurable policy semantics.

No changes to these algorithms are permitted inside Phase 4.

G1 behavior remains:
- explicit policy ALLOW -> NEGATIVE;
- explicit policy DENY + observed ALLOW -> POSITIVE;
- UNKNOWN policy -> locked A7 fallback.

## Policy-quality adversaries

### Missing policy
No registry entry matches the observable endpoint. Expected engine behavior: `UNKNOWN / NO_POLICY`.

### Ambiguous policy
Two equal-priority policies match the same endpoint/dimension. Expected engine behavior:
`UNKNOWN / AMBIGUOUS_POLICY`.

### Stale policy
A registry entry exists but contains an outdated privileged role, transition or property rule. This condition is
intentionally capable of producing a false positive even when the policy engine itself behaves exactly as configured.

### Incomplete context
A matching policy exists but owner/tenant/workflow/request context needed for a confident decision is missing or
incomplete. Dimensions whose existing policy semantics cannot express UNKNOWN for the missing fact may instead produce
a decisive configured DENY; this outcome is measured rather than corrected.

## Malformed-registry robustness

Separate fail-closed tests cover:
- bad schema;
- empty policy collection;
- non-list policy collection;
- duplicate policy ID;
- invalid dimension;
- missing regex;
- invalid regex;
- equal-priority ambiguity;
- missing policy;
- deterministic priority resolution;
- UNKNOWN fallback invariance;
- explicit ALLOW suppression;
- explicit DENY + observed ALLOW promotion.

## Primary observed metrics

For locked A7 and G1:
- TP / TN / FP / FN;
- sensitivity / recall;
- specificity;
- false-positive rate;
- false-negative rate;
- precision / PPV;
- NPV;
- accuracy;
- balanced accuracy;
- F1;
- Matthews correlation coefficient;
- Wilson 95% intervals for sensitivity, specificity, precision and NPV.

## Policy metrics

- decisive policy count/rate;
- UNKNOWN count/rate;
- expected-authorization decision accuracy across all cases;
- expected-authorization decision accuracy among decisive cases;
- policy-status distribution;
- policy-decision distribution.

## Diagnostic breakdowns

Results are broken down by:
- authorization dimension;
- policy condition.

This allows false positives caused by detector behavior to be separated from missing, ambiguous, stale or incomplete
configured policy.

## Base-rate projections

Using observed sensitivity and specificity only, mathematically project:
- PPV;
- NPV;
- alerts per 1,000;
- true alerts per 1,000;
- false alerts per 1,000

at prevalence:
- 1%;
- 5%;
- measured 8.333333%;
- 10%.

These are projections, not additional observed datasets.

## Integrity gates

Required:
- Phase 3 evidence files/scripts unchanged from Phase 3 completion;
- Phase 3 decision algorithms unchanged from algorithm freeze;
- 96 frozen feature cases;
- 96 sealed labels;
- label file absent during both prediction passes;
- policy registry robustness PASS;
- byte-identical repeated predictions;
- byte-identical repeated evaluation;
- SHA-256 sidecars;
- secret-material scan;
- Maven product package success.

## Claim boundary

This phase remains synthetic internal research. It does not establish:
- production scanner accuracy;
- real-world vulnerability prevalence;
- automatic extraction of application policy;
- cross-framework generalization;
- Burp desktop runtime behavior;
- external-target safety or effectiveness.

No post-result tuning is permitted inside this Phase 4 experiment. Any redesign requires a new development set and a new
untouched stress corpus.
