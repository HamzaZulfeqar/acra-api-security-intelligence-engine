# EXP-DIFF-001 — Controlled Differential Baseline and Treatment

**Sprint:** 4  
**Candidate:** `0.4.0-rc1`  
**State:** SOFTWARE_READY / RESEARCH_DEFERRED  
**Execution boundary:** ACRA-Lab and synthetic localhost fixtures only  
**Dataset:** `lab/ground-truth/GT-S4-RESEARCH-FIXTURES.json`  
**Metrics:** NOT MEASURED

## Hypothesis

A controlled baseline/control/experiment structure can distinguish authorization-state differences more reliably than a simple HTTP status, response-length and body-difference baseline in a synthetic API laboratory.

## Comparison conditions

The research baseline records HTTP status, response length and a basic body difference. It is a comparison condition, not the ACRA decision engine.

The ACRA treatment uses an immutable baseline, expected-allow control, expected-deny control, one declared mutation, Security Context, semantic response analysis and an independently sourced expected policy. It produces an Observation, not a vulnerability finding.

## Dataset and ground truth

`GT-S4-RESEARCH-FIXTURES` contains labelled false-positive and false-negative control definitions for public access, application-level denial, volatile values, reordered/formatted responses, equivalent resource representations, same-status changes, soft denial, dynamic length, opaque identifiers, nested resources, small collections and non-standard authentication representation.

Ground-truth values are declared independently of ACRA output. The file intentionally contains no measured TP/TN/FP/FN result.

## Reproducibility contract

Each future run must record:

- experiment, dataset and lab versions;
- deterministic seed, test ordering and mutation ordering;
- configuration fingerprint;
- immutable execution IDs;
- expected and observed outcome per labelled case;
- separate baseline and treatment classifications.

The implemented `ExperimentRunMetadata`, `ResearchCaseDefinition`, `ResearchExecutionRecord` and `ResearchMetrics` types enforce those software records. Reruns retain distinct execution identities.

## Planned procedure

1. Start the isolated secure and deliberately vulnerable ACRA-Lab fixtures.
2. Validate expected-allow and expected-deny controls against independent ground truth.
3. Execute one controlled difference per case.
4. Record baseline comparison and ACRA treatment outcomes separately.
5. Label each binary result TP, TN, FP or FN from ground truth.
6. Calculate precision, recall and F1; report `N/A` when a denominator is zero.
7. Preserve raw execution/evidence IDs and configuration metadata.

## Current result

The software records and calculation semantics are verified by `Sprint4ProductCompletionTestSuite`. The broad labelled campaign has not been executed. Therefore TP, TN, FP, FN, precision, recall and F1 remain **NOT MEASURED**. The earlier `EXP-EXEC-001` localhost milestone remains separate evidence for the execution substrate only.

## Limitations

- Dataset definitions are synthetic and do not establish real-world accuracy.
- No broad research run or optimization measurement is claimed.
- Burp and authorized external validation are outside this local software slice.
- No Observation is promoted to a confirmed vulnerability.
