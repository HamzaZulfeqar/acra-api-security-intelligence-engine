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
| EXP-A0 | Naive differential baseline | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=1/FP=7/FN=0, P=.533333/R=1/F1=.695652 |
| EXP-A1 | + Identity | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=1/FP=7/FN=0, P=.533333/R=1/F1=.695652 |
| EXP-A2 | + Ownership | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=4/FP=4/FN=0, P=.666667/R=1/F1=.800000 |
| EXP-A3 | + Tenant | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=5/FP=3/FN=0, P=.727273/R=1/F1=.842105 |
| EXP-A4 | + Role | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=6/FP=2/FN=0, P=.800000/R=1/F1=.888889 |
| EXP-A5 | + Workflow | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=7/FP=1/FN=0, P=.888889/R=1/F1=.941176 |
| EXP-A6 | + Semantic evidence | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=8/FP=0/FN=0, P=1/R=1/F1=1 |
| EXP-A7 | Full ACRA correlation | COMPLETED_CONTROLLED_LOCAL | GT-S11-AUTHORIZATION-RESEARCH | TP=8/TN=8/FP=0/FN=0, P=1/R=1/F1=1 |
| EXP-S13-HOLDOUT-A0-A7 | Locked A0-A7 held-out generalization evaluation | COMPLETED_CONTROLLED_HELDOUT | GT-S13-HOLDOUT-FEATURES + GT-S13-HOLDOUT-LABELS | A7 TP=8/TN=3/FP=5/FN=0, P=.615385/R=1/F1=.761905; 5 hard-negative FP |

Sprint 3 measured metrics are deliberately limited to controlled local fixtures. They do not establish real-world scanner precision, authorization-vulnerability accuracy or novelty.

`EXP-EXEC-001` is Level 4 evidence only for the controlled local execution-substrate claim. It does not establish real-world vulnerability-detection accuracy, FP/FN rates, or Burp runtime behavior.

The Phase 3 software verification proves only the labelled-record and safe-metric contracts, including `N/A` for undefined denominators. It does not convert the deferred campaign into evidence. No measured TP/TN/FP/FN, precision, recall, F1, optimization percentage or Sprint 4 performance result is claimed.


## Sprint 6 controlled result — 2026-09-23

`EXP-S6-TENANT-RBAC-001` is the first measured Sprint 6 tenant/RBAC campaign. Metrics are valid only for the
ten labelled localhost cases in `GT-S6-TENANT-RBAC`; they do not establish real-world scanner accuracy.


## Sprint 11 research-dataset readiness — 2026-09-25

`GT-S11-AUTHORIZATION-RESEARCH` is the registered dataset for the later A0-A7 campaign.
The dataset contains 16 controlled localhost cases: one positive and one negative case for each of
eight authorization dimensions (object, tenant, RBAC, workflow, routing, property, batch and
indirect-reference authorization).

Sprint 11 verification executes the secure and intentionally vulnerable ACRA-Lab fixtures only to
validate the independent ground-truth oracle. It does **not** execute A0-A7 detector configurations
and does not produce TP/TN/FP/FN, precision, recall or F1 results for those experiments.

At the Sprint 11 closure, EXP-A0 through EXP-A7 remained `NOT_RUN`; that statement is retained as historical context for the Sprint 11 checkpoint. Sprint 12 subsequently executed the registered campaign without modifying the frozen Sprint 11 dataset.


## Sprint 12 controlled A0-A7 result — 2026-09-25

GitHub Actions run `36065830981` is the first successful measured Sprint 12 execution on branch
`s12-a0-a7-research-evaluation`. It evaluated 16 frozen cases across eight cumulative ablation
variants, producing 128 labelled prediction rows.

The prediction path is separated from the ground-truth join. The predictor is explicitly denied access
to `groundTruth`, `secureExpected`, `vulnerableExpected` and `expectedCandidate`; it observes only
the intentionally vulnerable localhost fixture plus non-label case inputs. Ground-truth labels are joined
after prediction for metric calculation.

The registered authorization dimension is supplied to the campaign, so **dimension discovery is not
measured**. Results apply only to this balanced synthetic localhost fixture. They do not establish
real-world scanner accuracy, generalization, Burp runtime behavior or external-target safety.

Repeat execution produced byte-identical JSON, CSV and JSONL research artifacts. The measured progression
was A0/A1 7 FP, A2 4 FP, A3 3 FP, A4 2 FP, A5 1 FP and A6/A7 0 FP, with 8 TP and 0 FN in every variant.
A6 and A7 therefore tie on this fixed dataset; no broader superiority claim is made.


## Sprint 13 Phase 1 held-out result — 2026-09-25

GitHub Actions run `36143281129` completed successfully at
`e515a31d91d775d5f0f35c32a48507455f84992d`.

The 16-case held-out corpus was frozen before evaluation and split into a feature file and sealed label file.
The label file was physically removed during both prediction passes. The Sprint 12 A0-A7 implementation was locked
against base commit `bd944e83a6edefafba56caebaa35e89fc107c282`.

Measured held-out progression:

- A0/A1: TP=8, TN=0, FP=8, FN=0, P=.500000, R=1, F1=.666667;
- A2-A7: TP=8, TN=3, FP=5, FN=0, P=.615385, R=1, F1=.761905.

The five remaining false positives are hard negatives involving legitimate policy semantics absent from the Sprint 12
calibration fixture: platform-wide tenant administration, a new security-admin role, a DRAFT→PENDING requester
workflow transition, equivalent-route access by security-admin, and an allowed `nickname` property.

This result is retained as evidence of limited policy generalization. It must not be overwritten by later tuning.
The fixture remains synthetic localhost evidence authored within the same project; it is not independent third-party
replication, does not measure automatic dimension discovery, and does not establish production accuracy.
