# Experiment Registry

| Experiment ID | Name | State | Dataset | Result |
|---|---|---|---|---|
| EXP-INTEGRATION-001 | Live traffic to Security Context | COMPLETED_PARTIAL | GT-INTEGRATION-001 | PARTIAL, local live HTTP PASS; Burp stage BLOCKED |
| EXP-BURP-INTEGRATION-001 | Real Burp/Montoya to ACRA-Lab runtime validation | BLOCKED | GT-INTEGRATION-001 | UNVERIFIED, Burp/Maven runtime unavailable |
| EXP-RECON-001 | Sprint 3 endpoint reconnaissance | COMPLETED_LOCAL | GT-RECON-S3 | PASS on 10-operation local fixture; Burp UNVERIFIED |
| EXP-ID-001 | Semantic identifier classification | COMPLETED_CONTROLLED | Six labeled ID fixtures | precision 1.0000, recall 1.0000 within fixture scope |
| EXP-OPENAPI-001 | OpenAPI/traffic correlation | COMPLETED_CONTROLLED | GT-RECON-S3 + OpenAPI fixture | 10/10 documented-observed locally |
| EXP-ROUTE-001 | Route equivalence | COMPLETED_CONTROLLED | Five labeled route pairs | accuracy 1.0000 within fixture scope |
| EXP-RESP-001 | Response semantic false-positive controls | COMPLETED_CONTROLLED | soft-200 + dynamic/reordered fixtures | selected controls PASS |
| EXP-CONTEXT-001 | Context reconstruction coverage | COMPLETED_CONTROLLED | GT-RECON-S3 | context coverage 0.8111; measured evidence completeness 1.0000 |
| EXP-S3-BURP-RECON-001 | Real Burp Sprint 3 reconnaissance | BLOCKED | GT-RECON-S3 | UNVERIFIED |
| EXP-EXEC-001 | Sprint 4 controlled localhost execution pipeline | COMPLETED_LOCAL | GT-EXEC-S4 | PASS for loopback execution substrate; secure controls PASS, vulnerable mutation mismatch observed as UNEXPECTED_CHANGE; no accuracy metric claimed |
| EXP-DIFF-001 | Controlled differential baseline/treatment and labelled FP/FN campaign | SOFTWARE_READY / RESEARCH_DEFERRED | GT-S4-RESEARCH-FIXTURES | NOT_RUN; labelled records and metric semantics pass, outcomes remain NOT MEASURED |
| EXP-REQ-EFF-001 | Sprint 4 candidate/dedup/scope/budget/execution efficiency | DEFERRED | 100/1,000/10,000 synthetic plans pending | NOT_RUN |
| EXP-S4-PERF-001 | Sprint 4 planning/queue/execution/differential/memory workload | DEFERRED | 100/1,000/10,000 synthetic plans pending | NOT_RUN |
| EXP-S6-TENANT-RBAC-001 | Controlled tenant/RBAC policy-aware baseline comparison | COMPLETED_CONTROLLED_LOCAL | GT-S6-TENANT-RBAC | baseline TP=2/TN=1/FP=7/FN=0, P=.222222/R=1/F1=.363636; ACRA TP=2/TN=8/FP=0/FN=0, P=1/R=1/F1=1 |
| EXP-A0 | Naive differential baseline | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A1 | + Identity | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A2 | + Ownership | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A3 | + Tenant | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A4 | + Role | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A5 | + Workflow | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A6 | + Semantic evidence | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |
| EXP-A7 | Full ACRA correlation | ADAPTER_VERIFIED | GT-S11-ABLATION-DATASET | NOT_RUN |

## Sprint 11 protocol status — 2026-09-25

GitHub Actions run `36056957706` verified the machine-enforced A0–A7 protocol contract. Registry state
`DATASET_REGISTERED` means both the deterministic treatment protocol and controlled independently labelled dataset are registered; it does **not** mean the experiment ran. All A0–A7 results remain NOT_RUN until treatment adapters and controlled execution evidence are separately verified.

## Sprint 11 dataset status — 2026-09-25

GitHub Actions run `36057476360` verified `GT-S11-ABLATION-DATASET`: 15 source-backed cases, 8 POSITIVE and
7 NEGATIVE. Labels are cross-checked against independent S4 `expected_candidate` booleans. S6–S10 policy fixtures
remain excluded from binary vulnerability labelling. No A0–A7 prediction or metric result is claimed by dataset
registration.

## Sprint 11 adapter status — 2026-09-25

GitHub Actions run `36057869888` verified the ground-truth-free A0–A7 prediction adapter contract. State
`ADAPTER_VERIFIED` means treatment filtering/prediction software is verified; it does **not** mean any A0–A7
campaign cell executed. Full research results remain NOT_RUN.

Sprint 3 measured metrics are deliberately limited to controlled local fixtures. They do not establish real-world scanner precision, authorization-vulnerability accuracy or novelty.

`EXP-EXEC-001` is Level 4 evidence only for the controlled local execution-substrate claim. It does not establish real-world vulnerability-detection accuracy, FP/FN rates, or Burp runtime behavior.

The Phase 3 software verification proves only the labelled-record and safe-metric contracts, including `N/A` for undefined denominators. It does not convert the deferred campaign into evidence. No measured TP/TN/FP/FN, precision, recall, F1, optimization percentage or Sprint 4 performance result is claimed.


## Sprint 6 controlled result — 2026-09-23

`EXP-S6-TENANT-RBAC-001` is the first measured Sprint 6 tenant/RBAC campaign. Metrics are valid only for the
ten labelled localhost cases in `GT-S6-TENANT-RBAC`; they do not establish real-world scanner accuracy.
