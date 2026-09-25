# Sprint 13 Phase 5 — Policy Reliability & Uncertainty Governance Protocol

**Protocol ID:** ACRA-S13-GOVERNANCE-v1  
**Branch:** `s13-policy-uncertainty-governance`  
**Phase 4 completion base:** `a07cf497e6689c9b1d161ae2e22c747fdbac33f7`  
**Governance freeze:** `11afbb80be24116c9facd0d4b0791f4a12efed3a`  
**Scope:** synthetic internal governance validation only.

## Research question

Can ACRA stop conflating degraded policy quality with confirmed authorization vulnerabilities while still preserving
all positive cases for either actionable finding or explicit review?

## Governed dispositions

Phase 5 adds a separate governance layer after frozen A7, frozen dimension inference and frozen Phase 3 policy semantics.

Possible dispositions:
- `VULNERABILITY_CANDIDATE`;
- `AUTHORIZED_CONTROL`;
- `CONTROL_ENFORCED`;
- `POLICY_GAP`;
- `AMBIGUOUS_POLICY`;
- `STALE_POLICY`;
- `INCOMPLETE_CONTEXT`;
- `INCONCLUSIVE`.

Only `VULNERABILITY_CANDIDATE` is an actionable finding.

Review-required states:
- `POLICY_GAP`;
- `AMBIGUOUS_POLICY`;
- `STALE_POLICY`;
- `INCOMPLETE_CONTEXT`;
- `INCONCLUSIVE`.

## Conservative decision order

1. no matching policy -> `POLICY_GAP`;
2. equal-priority ambiguity -> `AMBIGUOUS_POLICY`;
3. non-current policy-health metadata -> `STALE_POLICY` or `INCONCLUSIVE`;
4. missing authorization evidence -> `INCOMPLETE_CONTEXT`;
5. UNKNOWN policy result -> `INCONCLUSIVE`;
6. LOW dimension confidence -> `INCONCLUSIVE`;
7. current explicit ALLOW -> `AUTHORIZED_CONTROL`;
8. current explicit DENY + observed ALLOW -> `VULNERABILITY_CANDIDATE`;
9. current explicit DENY + observed DENY -> `CONTROL_ENFORCED`.

The Phase 4 behavior of blindly falling back from policy UNKNOWN to locked A7 is not used for actionable-finding
promotion in Phase 5.

## Policy health evidence

Phase 5 policy registries attach:
- health status: CURRENT / STALE;
- policy version;
- source revision;
- last validated timestamp.

This metadata is preserved in governance evidence. Phase 5 does not automatically determine whether organizational
policy is stale; it validates how explicit policy-health metadata should govern finding disposition.

## Development stage

The 64-case development corpus contains eight cases per authorization dimension:
- current-policy positive;
- current-policy legitimate ALLOW;
- current-policy enforced DENY;
- policy-gap positive;
- policy-gap negative;
- ambiguous-policy negative;
- stale-policy negative;
- incomplete-context negative.

Development run on the frozen design measured:
- dimension inference 64/64;
- expected disposition 64/64;
- actionable candidate TP=8 / TN=48 / FP=0 / FN=8;
- actionable precision=1.0;
- actionable recall=.5;
- positive review count=8;
- escalation coverage (candidate or review)=1.0;
- silent positive count=0.

Candidate recall is intentionally lower than escalation coverage because positive cases without authoritative policy are
not promoted to confirmed vulnerability candidates.

## Untouched evaluation

Created after governance freeze with new:
- identities;
- role vocabulary;
- resource names;
- tenant/workspace names;
- workflow names/actions/states;
- property names;
- policy versions and source revisions;
- endpoint paths.

The evaluation contains the same governance-condition categories but does not reuse development case identities or
business vocabulary.

## Primary metrics

- dimension accuracy;
- exact disposition accuracy;
- actionable candidate TP/TN/FP/FN, precision, recall, F1;
- frozen A7 and legacy G1 binary metrics for comparison;
- positive review count;
- negative review count;
- candidate rate;
- review rate;
- escalation coverage = positive cases that are either actionable candidates or review-required;
- silent positive count;
- disposition distribution.

## Acceptance interpretation

A lower actionable-candidate recall is not automatically a failure when the missed positive case is intentionally routed
to a review-required uncertainty state.

The critical safety failures are:
- false actionable findings;
- silent positive misses;
- incorrect uncertainty disposition;
- post-freeze algorithm modification;
- label leakage.

## Integrity gates

Required:
- Phase 4 evidence unchanged;
- governance engine and frozen upstream algorithms unchanged from governance freeze;
- labels absent during both prediction passes;
- byte-identical repeated predictions;
- byte-identical repeated evaluation;
- SHA-256 sidecars;
- secret-material scan;
- Maven package success.

## Claim boundary

This experiment does not establish:
- production accuracy;
- real analyst workload;
- automatic policy-health determination;
- automatic policy extraction;
- cross-framework generalization;
- Burp desktop runtime behavior;
- external-target safety/effectiveness.

No post-result governance tuning is permitted inside this Phase 5 experiment.
