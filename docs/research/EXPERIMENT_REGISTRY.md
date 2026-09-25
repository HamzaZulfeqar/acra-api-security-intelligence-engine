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
| EXP-S13-DIMENSION-001 | Blind automatic authorization-dimension discovery + inferred-dimension A0-A7 | COMPLETED_CONTROLLED_LOCAL | GT-S13-DIMENSION-FEATURES + GT-S13-DIMENSION-LABELS | 31/32 dimensions correct; accuracy=.968750, macro-F1=.968254; S13 holdout 16/16; downstream A7 unchanged vs prior measured fixtures |
| EXP-S13-POLICY-GEN-001 | Configurable policy semantics with post-freeze untouched evaluation | COMPLETED_CONTROLLED_HELDOUT | GT-S13-POLICY-EVAL-FEATURES + GT-S13-POLICY-EVAL-LABELS | G1 TP=8/TN=16/FP=0/FN=0, P=1/R=1/F1=1 on 24-case internal configured-policy evaluation; locked A7 FP=12 |
| EXP-S13-BASERATE-001 | Adversarial negative-heavy policy/base-rate stress | COMPLETED_CONTROLLED_STRESS | GT-S13-BASERATE-FEATURES + GT-S13-BASERATE-LABELS | 96 cases, 8.33% prevalence; G1 TP=8/TN=49/FP=39/FN=0, P=.170213/R=1/F1=.290909; explicit-policy controls 0 FP, policy-degraded controls dominate FP |
| EXP-S13-GOVERNANCE-001 | Policy reliability and uncertainty governance with post-freeze untouched evaluation | COMPLETED_CONTROLLED_HELDOUT | GT-S13-GOV-EVAL-FEATURES + GT-S13-GOV-EVAL-LABELS | 64/64 dispositions; actionable TP=8/TN=48/FP=0/FN=8, P=1/R=.5; escalation coverage=1.0; silent positives=0 |
| EXP-S13-XFRAME-001 | Framework-shaped serialization normalization with post-freeze untouched evaluation | COMPLETED_CONTROLLED_HELDOUT | GT-S13-XFRAME-EVAL-FEATURES + GT-S13-XFRAME-EVAL-LABELS | raw dimension=60/64 and disposition=32/64; normalized dimension=64/64 and disposition=64/64; snapshot representations only |

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


## Sprint 13 Phase 2 dimension-discovery result — 2026-09-25

GitHub Actions run `36145805521` completed successfully at
`3f4b4238efe514b7ef1fe65b2cd477233c54fa73`.

The 32-case Phase 2 corpus contains no registered dimension or vulnerability label in the prediction feature file.
`GT-S13-DIMENSION-LABELS` was physically absent during both inference runs and joined only afterward.

Measured dimension-classification results:

- Sprint 12 calibration: 15/16 correct, accuracy=.937500, macro-F1=.933333;
- Sprint 13 holdout: 16/16 correct, accuracy=1.000000, macro-F1=1.000000;
- combined: 31/32 correct, accuracy=.968750, macro-F1=.968254;
- confidence: 30 HIGH, 2 MEDIUM.

The retained mismatch is the canonical Sprint 12 routing control `/api/v1/s8/admin`, which the observable-evidence
classifier maps to `RBAC_AUTHORIZATION` because the response exposes an explicit required-role signal and no route
anomaly. That ambiguity is retained rather than tuned away in this experiment.

Downstream A7 using only the inferred dimension:
- Sprint 12 calibration: TP=8/TN=8/FP=0/FN=0, P=1/R=1/F1=1 within that frozen calibration fixture;
- Sprint 13 holdout: TP=8/TN=3/FP=5/FN=0, P=.615385/R=1/F1=.761905.

This shows that the supplied-dimension dependency was removed for these fixtures without changing the measured A7
binary classification outcome. It does not resolve the five policy-generalization false positives from Phase 1 and
does not establish real-world dimension accuracy.


## Sprint 13 Phase 3 configurable-policy generalization — 2026-09-25

Development workflow `36148740338` succeeded before algorithm freeze. The policy semantics engine was then frozen at
`c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd`.

The untouched evaluation corpus and policy registry were created only after that freeze. Evaluation workflow
`36149071484` succeeded at `82ae165b702d18b0eae872d51f8f695f723fec13`.

Untouched evaluation result:
- 24 cases: 8 positive / 16 legitimate controls;
- automatic dimension inference: 24/24;
- explicit configured-policy decision: 24/24;
- UNKNOWN policy decisions: 0;
- locked A7: TP=8/TN=4/FP=12/FN=0, P=.4/R=1/F1=.571429;
- policy-generalized G1: TP=8/TN=16/FP=0/FN=0, P=1/R=1/F1=1;
- two-run prediction/evaluation repeatability: PASS;
- sealed labels absent during both prediction passes: PASS;
- frozen algorithm diff gate: PASS;
- Maven package: BUILD SUCCESS.

The result demonstrates that explicit configured policy semantics can remove these synthetic false positives without
loss of recall on this internal corpus. It does **not** establish automatic policy discovery, production accuracy,
third-party replication or external validity. No post-result algorithm tuning is permitted inside this experiment.


## Sprint 13 Phase 4 adversarial / base-rate stress — 2026-09-25

GitHub Actions run `36168752869` completed successfully at
`ed1601542739cce20be48c001ae4e663bfdcf890`.

The 96-case corpus contains 8 positive cases and 88 legitimate controls (8.333333% measured prevalence). The Phase 3
decision algorithms were frozen and the Phase 3 evidence set was protected by an immutable-history gate.

Measured results:
- locked A7: TP=8/TN=20/FP=68/FN=0, P=.105263/R=1/SPEC=.227273/FPR=.772727/F1=.190476/MCC=.154672;
- G1: TP=8/TN=49/FP=39/FN=0, P=.170213/R=1/SPEC=.556818/FPR=.443182/F1=.290909/MCC=.307860.

G1 false positives by policy condition:
- explicit configured ALLOW: 0/40;
- no policy: 12/16;
- ambiguous policy: 12/16;
- stale policy: 8/8;
- incomplete context: 7/8.

Policy decisions were decisive for 58/96 cases and UNKNOWN for 38/96. Expected-authorization correctness was 48/96
overall and 48/58 among decisive decisions.

The experiment demonstrates that configured policy semantics can strongly reduce false positives when policy is healthy,
but current UNKNOWN fallback and stale/incomplete policy handling produce an unacceptable alert rate under degraded
policy quality. The result is frozen evidence; no post-result tuning is permitted inside this experiment.


## Sprint 13 Phase 5 policy reliability & uncertainty governance — 2026-09-25

Development workflow `36169960606` succeeded with 64/64 dimensions and 64/64 expected dispositions. The governance
design was then frozen at `11afbb80be24116c9facd0d4b0791f4a12efed3a`.

Untouched evaluation workflow `36170353179` succeeded at
`46f77a8d76b160cec70f7aef3f612e0d63af56da`.

Evaluation result:
- dimension inference: 64/64;
- expected disposition: 64/64;
- frozen A7: TP=16/TN=21/FP=27/FN=0, P=.372093/R=1/F1=.542373;
- legacy G1: TP=16/TN=25/FP=23/FN=0, P=.410256/R=1/F1=.581818;
- governed actionable findings: TP=8/TN=48/FP=0/FN=8, P=1/R=.5/F1=.666667;
- positive review count=8;
- negative review count=32;
- escalation coverage=1.0;
- silent positive count=0;
- review rate=.625;
- actionable-candidate rate=.125.

The eight positive cases not promoted to actionable candidates were explicitly routed to review-required `POLICY_GAP`
states. Thus candidate recall and escalation coverage are intentionally different metrics.

The result demonstrates only the controlled governance contract: degraded policy evidence can be separated from
actionable vulnerability findings without silently dropping positives in this synthetic corpus. It does not establish
real-world analyst workload, production accuracy, automatic policy-health detection or cross-framework generalization.


## Sprint 13 Phase 6A cross-framework-shaped normalization — 2026-09-25

Development workflow `36173526384` succeeded after adding the framework-neutral normalization boundary. The normalizer
was frozen at `4011b9c05b99b14da66733aea47de43256060990`.

Untouched evaluation workflow `36173838561` succeeded at
`f3180a1877aaffd224a6d3a2734ddc25bf28c24b`.

Evaluation result:
- raw dimension: 60/64;
- raw governed disposition: 32/64;
- normalized dimension: 64/64;
- normalized governed disposition: 64/64.

Per framework style:
- FastAPI-shaped: raw 16/16 dimension + 16/16 disposition; normalized 16/16 + 16/16;
- Flask-shaped: raw 16/16 + 16/16; normalized 16/16 + 16/16;
- Express-shaped: raw 14/16 + 0/16; normalized 16/16 + 16/16;
- Spring-shaped: raw 14/16 + 0/16; normalized 16/16 + 16/16.

This experiment validates only the tested request/response serialization representations. It does not establish actual
framework runtime behavior, middleware/router behavior or framework-version compatibility. Phase 6B remains required for
real local framework runtime validation.
