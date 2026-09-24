# Sprint 12 A0-A7 Controlled Research Protocol

**Protocol ID:** ACRA-S12-A0-A7-v1  
**Dataset:** `GT-S11-AUTHORIZATION-RESEARCH`  
**Dataset SHA-256:** `f42783717bdd38e8d04b7f59cef98a41441005de426ec78ca1fba58098154d4b`  
**Scope:** localhost synthetic authorization research only.

## Research question

Within the frozen Sprint 11 fixture, how does adding cumulative identity, ownership, tenant, role, workflow, semantic
evidence and correlation context change binary candidate classification relative to a naive observed-response baseline?

## Independence rule

The predictor must never read the registered label or expected-candidate fields. The implementation constructs a
restricted view that excludes `groundTruth`, `secureExpected`, `vulnerableExpected` and `expectedCandidate`.
Only after prediction is complete is the frozen `groundTruth` label joined for evaluation.

The campaign executes the vulnerable localhost fixture only in the prediction path. The secure fixture remains part of
the independent Sprint 11 oracle validation and is not used by the Sprint 12 classifier to derive its prediction.

## Experimental unit

One unit is one frozen case evaluated under one ablation variant. There are:

- 16 cases;
- 8 variants;
- 128 prediction rows.

Case ordering and variant definitions are deterministic.

## Variants

`A0 ⊂ A1 ⊂ A2 ⊂ A3 ⊂ A4 ⊂ A5 ⊂ A6 ⊂ A7`, where each later configuration retains the earlier capabilities.

A0 is an observed-ALLOW candidate baseline. A1 adds identity presence. A2 adds resource ownership context. A3 adds
tenant context. A4 adds privileged-role context. A5 adds the registered workflow policy. A6 adds semantic
property/routing/resolved-target evidence. A7 adds the final correlation stage.

## Metrics

For each variant the verifier recomputes:

- TP: positive label + positive prediction;
- TN: negative label + negative prediction;
- FP: negative label + positive prediction;
- FN: positive label + negative prediction;
- precision = TP / (TP + FP), undefined when the denominator is zero;
- recall = TP / (TP + FN), undefined when the denominator is zero;
- F1 = harmonic mean of defined non-zero precision and recall; otherwise undefined.

Undefined values must remain explicit rather than converted to zero.

## Reproducibility

The output records:

- exact executing Git commit;
- dataset identity and SHA-256;
- per-variant configuration fingerprint;
- per-case prediction/reasons;
- aggregate confusion matrix and metrics.

The verification wrapper runs the full campaign twice and requires byte-identical JSON, CSV and JSONL outputs. Each
artifact has a SHA-256 sidecar.

## Data-minimization and secret boundary

Synthetic Authorization headers are used only for localhost execution. Raw tokens and Authorization values are not
persisted. Research outputs are scanned for bearer material and known synthetic secret forms.

## Threats to validity

1. **Construct validity:** the registered authorization dimension is supplied; dimension discovery is not evaluated.
2. **Internal validity:** later ablations encode fixture-specific policy semantics. The study measures component effects
   under those controlled semantics, not an unconstrained learned detector.
3. **External validity:** the dataset contains 16 synthetic localhost cases and does not represent deployment prevalence,
   API framework diversity or adversarial production behavior.
4. **Statistical conclusion validity:** the dataset is small and balanced; no confidence interval, significance test or
   prevalence-adjusted precision claim is made.
5. **Tool/runtime validity:** real Burp desktop execution is outside this protocol.
6. **Researcher bias:** ground-truth labels are frozen before Sprint 12 execution and unavailable to the predictor, but
   the fixture and ablation design are authored within the same project. Independent replication remains future work.

## Claim boundary

Permitted claim: the measured confusion matrices and metrics occurred on the registered 16-case synthetic localhost
fixture under the stated cumulative configurations.

Not permitted from this protocol alone: real-world scanner accuracy, production safety, superiority over another tool,
generalization to unseen APIs, or validated Burp runtime behavior.
